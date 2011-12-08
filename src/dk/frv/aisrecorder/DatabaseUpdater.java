package dk.frv.aisrecorder;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

import dk.frv.ais.country.CountryMapper;
import dk.frv.ais.country.MidCountry;
import dk.frv.ais.geo.GeoLocation;
import dk.frv.ais.message.AisMessage;
import dk.frv.ais.message.AisMessage18;
import dk.frv.ais.message.AisMessage24;
import dk.frv.ais.message.AisMessage5;
import dk.frv.ais.message.AisPositionMessage;
import dk.frv.ais.message.ShipTypeCargo;
import dk.frv.ais.proprietary.IProprietarySourceTag;
import dk.frv.aisrecorder.persistence.domain.AisClassAPosition;
import dk.frv.aisrecorder.persistence.domain.AisClassAStatic;
import dk.frv.aisrecorder.persistence.domain.AisVesselPosition;
import dk.frv.aisrecorder.persistence.domain.AisVesselStatic;
import dk.frv.aisrecorder.persistence.domain.AisVesselTarget;
import dk.frv.aisrecorder.persistence.domain.AisVesselTrack;
import dk.frv.aisrecorder.persistence.mapper.AisClassAPositionMapper;
import dk.frv.aisrecorder.persistence.mapper.AisClassAStaticMapper;
import dk.frv.aisrecorder.persistence.mapper.AisVesselPositionMapper;
import dk.frv.aisrecorder.persistence.mapper.AisVesselStaticMapper;
import dk.frv.aisrecorder.persistence.mapper.AisVesselTargetMapper;
import dk.frv.aisrecorder.persistence.mapper.AisVesselTrackMapper;

public class DatabaseUpdater extends Thread {

	private static final Logger LOG = Logger.getLogger(DatabaseUpdater.class);
	
	private BlockingQueue<QueueEntry> queue;
	private int batchSize = 1;
	private int targetTtl;
	private long startTime;
	private long messageCount = 0;
	private SqlSessionFactory sqlSessionFactory;
	private SqlSession session;
	private PastTrackCleanup pastTrackCleanup;

	public DatabaseUpdater(BlockingQueue<QueueEntry> queue, Settings settings) {
		try {
			String resource = "dk/frv/aisrecorder/persistence/xml/Configuration.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, settings.getProps());
		} catch (IOException e) {
			LOG.error("Could not create SqlSessionFactory: " + e.getMessage());
			System.exit(1);
		}
		
		this.queue = queue;
		this.batchSize = settings.getBatchSize();
		this.targetTtl = settings.getTargetTtl();		
		pastTrackCleanup = new PastTrackCleanup(sqlSessionFactory, settings.getPastTrackTime());
		pastTrackCleanup.start();
	}

	@Override
	public void run() {
		startTime = System.currentTimeMillis();

		List<QueueEntry> batch = new ArrayList<QueueEntry>();

		while (true) {
			batch.clear();

			// Try to take as many as possible without blocking
			queue.drainTo(batch, batchSize);

			// LOG.info("In batch: " + batch.size() + " size after drain: " +
			// queue.size());

			// Read batchSize messages
			while (batch.size() < batchSize) {
				try {
					QueueEntry entry = queue.take();
					batch.add(entry);
				} catch (InterruptedException e) {
					LOG.error("Failed to get message of queue: " + e.getMessage());
				}
			}
			
			// Handle messages
			try {
				batchHandle(batch);
			} catch (Exception e) {
				LOG.error("Error while handling batch: " + e.getMessage());				
			}

		}

	}

	private void batchHandle(List<QueueEntry> batch) {
		// Open session
		try {
			session = sqlSessionFactory.openSession(false);
		} catch (Exception e) {
			LOG.error("Could not open DB session: " + e.getMessage());
			AisRecorder.sleep(5000);
			return;
		}

		try {
			// Handle individual message
			for (QueueEntry queueEntry : batch) {
				messageHandle(queueEntry);
			}
			// Commit
			session.commit();
		} finally {
			session.close();
		}

	}

	private void messageHandle(QueueEntry queueEntry) {
		messageCount++;
		if (messageCount % 5000 == 0) {
			long elasped = System.currentTimeMillis() - startTime;
			double msgSec = (double) messageCount / (double) (elasped / 1000);
			LOG.debug(String.format(Locale.US, "Msg/sec: %.2f", msgSec));
		}

		AisMessage aisMessage = queueEntry.getAisMessage();

		// Only handle pos and static messages
		boolean posMessage = isPosMessage(aisMessage);
		boolean staticMessage = isStaticMessage(aisMessage);
		if (!posMessage && !staticMessage) {
			return;
		}
		
		// Get or create AisVesselTarget
		AisVesselTargetMapper mapper = session.getMapper(AisVesselTargetMapper.class);
		AisVesselTarget vesselTarget = mapper.selectByPrimaryKey((int) aisMessage.getUserId());
		if (vesselTarget == null) {
			vesselTarget = new AisVesselTarget();
			vesselTarget.setCreated(new Date());
			vesselTarget.setMmsi((int) aisMessage.getUserId());
		}
		vesselTarget.setLastReceived(queueEntry.getReceived());
		String vesselClass = "A";
		if (aisMessage.getMsgId() == 18 || aisMessage.getMsgId() == 24) {
			vesselClass = "B";
		}
		vesselTarget.setVesselClass(vesselClass);

		// Set valid to
		vesselTarget.setValidTo(new Date(queueEntry.getReceived().getTime() + targetTtl * 1000));

		// Determine country
		String str = Long.toString(aisMessage.getUserId());
		if (str.length() == 9) {
			str = str.substring(0, 3);
			MidCountry country = CountryMapper.getInstance().getByMid(Integer.parseInt(str));
			if (country != null) {
				vesselTarget.setCountry(country.getThreeLetter());
			}
		}

		vesselTarget.setSource("LIVE");
		
		// Insert or update
		if (vesselTarget.getId() == null) {
			mapper.insert(vesselTarget);
		} else {
			mapper.updateByPrimaryKey(vesselTarget);
		}
		
		if (posMessage) {
			// Handle position message
			positionHandle(queueEntry, vesselTarget);
		} else if (staticMessage) {
			// Handle static message
			staticHandle(queueEntry, vesselTarget);
		}

	}
	
	private void positionHandle(QueueEntry queueEntry, AisVesselTarget vesselTarget) {
		AisMessage aisMessage = queueEntry.getAisMessage();
		
		// Mappers
		AisVesselPositionMapper aisVesselPositionMapper = session.getMapper(AisVesselPositionMapper.class);
		AisClassAPositionMapper aisClassAPositionMapper = session.getMapper(AisClassAPositionMapper.class);
		AisVesselTrackMapper aisVesselTrackMapper = session.getMapper(AisVesselTrackMapper.class);
		boolean createPos = false;
		boolean createAPos = false;
		
		// Get or create AisVesselPosition
		AisVesselPosition vesselPosition =  aisVesselPositionMapper.selectByPrimaryKey(vesselTarget.getMmsi());			
		if (vesselPosition == null) {
			createPos = true;
			vesselPosition = new AisVesselPosition();
			vesselPosition.setMmsi(vesselTarget.getMmsi());
			vesselPosition.setCreated(new Date());
		}
		vesselPosition.setReceived(queueEntry.getReceived());
		// Get proprietary source tag
		IProprietarySourceTag tag = queueEntry.getAisMessage().getSourceTag();
		if (tag != null) {
			vesselPosition.setSourceTimestamp(tag.getTimestamp());
		}
		
		AisClassAPosition classAPosition = null;

		if (aisMessage instanceof AisPositionMessage) {
			AisPositionMessage posMessage = (AisPositionMessage) aisMessage;
			// Set position
			if (posMessage.isPositionValid()) {
				GeoLocation pos = posMessage.getPos().getGeoLocation();
				vesselPosition.setLat(pos.getLatitude());
				vesselPosition.setLon(pos.getLongitude());
			}
			// Set sog
			if (posMessage.isSogValid()) {
				vesselPosition.setSog(posMessage.getSog() / 10.0);
			}
			// Set cog
			if (posMessage.isCogValid()) {
				vesselPosition.setCog(posMessage.getCog() / 10.0);
			}
			// Set heading
			if (posMessage.isHeadingValid()) {
				vesselPosition.setHeading((double) posMessage.getTrueHeading());
			}
			// Pos acc
			vesselPosition.setPosAcc((byte) posMessage.getPosAcc());
			// Raim
			vesselPosition.setRaim((byte) posMessage.getRaim());
			// UTC sec
			vesselPosition.setUtcSec((byte) posMessage.getUtcSec());

			// Class A info
			classAPosition = aisClassAPositionMapper.selectByPrimaryKey(vesselTarget.getMmsi());
			if (classAPosition == null) {
				createAPos = true;
				classAPosition = new AisClassAPosition();
				classAPosition.setMmsi(vesselPosition.getMmsi());
			}
			if (posMessage.isRotValid()) {
				classAPosition.setRot((double) posMessage.getRot());
			}
			classAPosition.setNavStatus((byte) posMessage.getNavStatus());
			classAPosition.setSpecialManIndicator((byte) posMessage.getSpecialManIndicator());

		} else {
			AisMessage18 posMessage = (AisMessage18) aisMessage;
			// Set position
			if (posMessage.isPositionValid()) {
				GeoLocation pos = posMessage.getPos().getGeoLocation();
				vesselPosition.setLat(pos.getLatitude());
				vesselPosition.setLon(pos.getLongitude());
			}
			// Set sog
			if (posMessage.isSogValid()) {
				vesselPosition.setSog(posMessage.getSog() / 10.0);
			}
			// Set cog
			if (posMessage.isCogValid()) {
				vesselPosition.setCog(posMessage.getCog() / 10.0);
			}
			// Set heading
			if (posMessage.isHeadingValid()) {
				vesselPosition.setHeading((double) posMessage.getTrueHeading());
			}
			// Pos acc
			vesselPosition.setPosAcc((byte) posMessage.getPosAcc());
			// Raim
			vesselPosition.setRaim((byte) posMessage.getRaimFlag());
			// UTC sec
			vesselPosition.setUtcSec((byte) posMessage.getUtcSec());
		}
		
		// Insert or update position
		if (createPos) {
			aisVesselPositionMapper.insert(vesselPosition);
		} else {
			aisVesselPositionMapper.updateByPrimaryKey(vesselPosition);
		}
		
		if (classAPosition != null) {
			// Insert or update
			if (createAPos) {
				aisClassAPositionMapper.insert(classAPosition);
			} else {
				aisClassAPositionMapper.updateByPrimaryKey(classAPosition);
			}
		}
		
		// Add to track
		if (vesselPosition.getLat() == null || vesselPosition.getLon() == null) {
			return;
		}
		
		Double cog = vesselPosition.getCog();
		Double sog = vesselPosition.getSog();
		if (cog == null) {
			cog = 0d;
		}
		if (sog == null) {
			sog = 0d;
		}
		
		AisVesselTrack aisVesselTrack = new AisVesselTrack();
		aisVesselTrack.setCreated(new Date());
		aisVesselTrack.setMmsi(vesselPosition.getMmsi());
		aisVesselTrack.setLat(vesselPosition.getLat());
		aisVesselTrack.setLon(vesselPosition.getLon());
		// Time is source timestamp if it exists
		if (vesselPosition.getSourceTimestamp() != null) {
			aisVesselTrack.setTime(vesselPosition.getSourceTimestamp());
		} else {
			aisVesselTrack.setTime(queueEntry.getReceived());
		}
		aisVesselTrack.setCog(cog);
		aisVesselTrack.setSog(sog);
		
		// Insert past track
		aisVesselTrackMapper.insert(aisVesselTrack);		
	}
		
	private void staticHandle(QueueEntry queueEntry, AisVesselTarget vesselTarget) {
		AisMessage aisMessage = queueEntry.getAisMessage();
		
		// Mappers
		AisVesselStaticMapper aisVesselStaticMapper = session.getMapper(AisVesselStaticMapper.class);
		AisClassAStaticMapper aisClassAStaticMapper = session.getMapper(AisClassAStaticMapper.class);
		boolean createStatic = false;
		boolean createStaticA = false;
		
		AisVesselStatic vesselStatic = aisVesselStaticMapper.selectByPrimaryKey(vesselTarget.getMmsi());
		if (vesselStatic == null) {
			createStatic = true;
			vesselStatic = new AisVesselStatic();
			vesselStatic.setMmsi(vesselTarget.getMmsi());
			vesselStatic.setCreated(new Date());
		}
		vesselStatic.setReceived(queueEntry.getReceived());
		
		AisClassAStatic classAStatic = null;
		
		if (aisMessage instanceof AisMessage24) {
			// Class B
			AisMessage24 msg24 = (AisMessage24) aisMessage;
			if (msg24.getPartNumber() == 0) {
				vesselStatic.setName(msg24.getName());
			} else {
				vesselStatic.setCallsign(msg24.getCallsign());
				vesselStatic.setDimBow((short) msg24.getDimBow());
				vesselStatic.setDimPort((byte) msg24.getDimPort());
				vesselStatic.setDimStarboard((byte) msg24.getDimStarboard());
				vesselStatic.setDimStern((short) msg24.getDimStern());
				vesselStatic.setShipType((byte) msg24.getShipType());								
				ShipTypeCargo shipTypeCargo = new ShipTypeCargo(msg24.getShipType());
				vesselStatic.setDecodedShipType((byte)shipTypeCargo.getShipType().ordinal());
				vesselStatic.setCargo((byte)shipTypeCargo.getShipCargo().ordinal());
			}
		} else {
			// Class A
			AisMessage5 msg5 = (AisMessage5) aisMessage;
			vesselStatic.setName(msg5.getName());
			vesselStatic.setCallsign(msg5.getCallsign());
			vesselStatic.setDimBow((short) msg5.getDimBow());
			vesselStatic.setDimPort((byte) msg5.getDimPort());
			vesselStatic.setDimStarboard((byte) msg5.getDimStarboard());
			vesselStatic.setDimStern((short) msg5.getDimStern());
			vesselStatic.setShipType((byte) msg5.getShipType());
			ShipTypeCargo shipTypeCargo = new ShipTypeCargo(msg5.getShipType());
			vesselStatic.setDecodedShipType((byte)shipTypeCargo.getShipType().ordinal());
			vesselStatic.setCargo((byte)shipTypeCargo.getShipCargo().ordinal());

			// Class A specifics
			classAStatic = aisClassAStaticMapper.selectByPrimaryKey(vesselTarget.getMmsi());			
			if (classAStatic == null) {
				createStaticA = true;
				classAStatic = new AisClassAStatic();
				classAStatic.setMmsi(vesselStatic.getMmsi());
			}
			classAStatic.setDestination(AisMessage.trimText(msg5.getDest()));
			classAStatic.setDraught((short) msg5.getDraught());
			classAStatic.setDte((byte) msg5.getDte());
			classAStatic.setEta(msg5.getEtaDate());
			classAStatic.setImo((int) msg5.getImo());
			classAStatic.setPosType((byte) msg5.getPosType());
			classAStatic.setVersion((byte) msg5.getVersion());
		}

		// Trim name and callsign
		vesselStatic.setName(AisMessage.trimText(vesselStatic.getName()));
		vesselStatic.setCallsign(AisMessage.trimText(vesselStatic.getCallsign()));

		if (createStatic) {
			aisVesselStaticMapper.insert(vesselStatic);
		} else {
			aisVesselStaticMapper.updateByPrimaryKey(vesselStatic);
		}
		
		if (classAStatic != null) {
			if (createStaticA) {
				aisClassAStaticMapper.insert(classAStatic);
			} else {
				aisClassAStaticMapper.updateByPrimaryKey(classAStatic);
			}
		}
		
	}	
	
	private static boolean isStaticMessage(AisMessage aisMessage) {
		return (aisMessage.getMsgId() == 5 || aisMessage.getMsgId() == 24);
	}

	private static boolean isPosMessage(AisMessage aisMessage) {
		switch (aisMessage.getMsgId()) {
		case 1:
		case 2:
		case 3:
		case 18:
			return true;
		default:
			return false;
		}
	}

}
