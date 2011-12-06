package dk.frv.aisrecorder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import dk.frv.ais.country.CountryMapper;
import dk.frv.ais.country.MidCountry;
import dk.frv.ais.geo.GeoLocation;
import dk.frv.ais.message.AisMessage;
import dk.frv.ais.message.AisMessage18;
import dk.frv.ais.message.AisMessage24;
import dk.frv.ais.message.AisMessage5;
import dk.frv.ais.message.AisPositionMessage;
import dk.frv.ais.proprietary.IProprietarySourceTag;
import dk.frv.aisrecorder.entities.AisClassAPosition;
import dk.frv.aisrecorder.entities.AisClassAStatic;
import dk.frv.aisrecorder.entities.AisVesselPosition;
import dk.frv.aisrecorder.entities.AisVesselStatic;
import dk.frv.aisrecorder.entities.AisVesselTarget;
import dk.frv.aisrecorder.entities.AisVesselTrack;

public class DatabaseUpdater extends Thread {

	private static final Logger LOG = Logger.getLogger(DatabaseUpdater.class);
	
	private static final long PAST_TRACK_CLEANUP_INTERVAL = 60 * 1000; // 1 min

	private BlockingQueue<QueueEntry> queue;
	private int batchSize = 1;
	private Settings settings;
	private EntityManager entityManager = null;
	private int targetTtl;
	private int pastTrackTime;
	private long lastPastTrackCleanup = 0;

	public DatabaseUpdater(BlockingQueue<QueueEntry> queue, Settings settings) {
		this.queue = queue;
		this.settings = settings;
		this.batchSize = settings.getBatchSize();
		this.targetTtl = settings.getTargetTtl();
		this.pastTrackTime = settings.getPastTrackTime();
		prepareEntityManager();
	}

	@Override
	public void run() {

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
			batchHandle(batch);

		}

	}

	private void batchHandle(List<QueueEntry> batch) {
		// Make sure entity manager exists
		prepareEntityManager();

		try {
			// Start transaction
			EntityTransaction entr = entityManager.getTransaction();
			entr.begin();

			// Handle individual message
			for (QueueEntry queueEntry : batch) {
				messageHandle(queueEntry);
			}

			// Commit transaction
			entr.commit();
		} catch (Exception e) {
			LOG.error("Caught exception " + e.getClass() + ": " + e.getMessage());
			e.printStackTrace();
			invalidateEntityManager();
		}

	}

	private void messageHandle(QueueEntry queueEntry) {
		AisMessage aisMessage = queueEntry.getAisMessage();

		// Only handle pos and static messages
		boolean posMessage = isPosMessage(aisMessage);
		boolean staticMessage = isStaticMessage(aisMessage);
		if (!posMessage && !staticMessage) {
			return;
		}

		// Get or create AisVesselTarget
		AisVesselTarget vesselTarget = entityManager.find(AisVesselTarget.class, (int) aisMessage.getUserId());
		if (vesselTarget == null) {
			vesselTarget = new AisVesselTarget();
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

		// Merging the changes to database
		entityManager.merge(vesselTarget);

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

		// Get or create AisVesselPosition
		AisVesselPosition vesselPosition = vesselTarget.getAisVesselPosition();
		if (vesselPosition == null) {
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
			classAPosition = vesselPosition.getAisClassAPosition();
			if (classAPosition == null) {
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

		vesselPosition.setAisVesselTarget(vesselTarget);
		entityManager.merge(vesselPosition);

		// Handle class A
		if (classAPosition != null) {
			classAPosition.setAisVesselPosition(vesselPosition);
			entityManager.merge(classAPosition);
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
		aisVesselTrack.setMmsi(vesselPosition.getMmsi());
		aisVesselTrack.setLat(vesselPosition.getLat());
		aisVesselTrack.setLon(vesselPosition.getLon());
		aisVesselTrack.setTime(queueEntry.getReceived());
		aisVesselTrack.setCog(cog);
		aisVesselTrack.setSog(sog);
		
		entityManager.persist(aisVesselTrack);
		
		// Maybe cleanup
		pastTrackCleanup();

	}
	
	private void pastTrackCleanup() {
		long now = System.currentTimeMillis();
		long elapsed = now - lastPastTrackCleanup;
		if (elapsed < PAST_TRACK_CLEANUP_INTERVAL) {
			return;
		}		
		Date cleanupDate = new Date(now - pastTrackTime * 1000);
		Query query = entityManager.createQuery("DELETE FROM AisVesselTrack vt WHERE vt.time < :cleanupDate");
		query.setParameter("cleanupDate", cleanupDate);
		query.executeUpdate();
		lastPastTrackCleanup = now;
	}

	private void staticHandle(QueueEntry queueEntry, AisVesselTarget vesselTarget) {
		AisMessage aisMessage = queueEntry.getAisMessage();

		// Get or create AisVesselStatic
		AisVesselStatic vesselStatic = vesselTarget.getAisVesselStatic();
		if (vesselStatic == null) {
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

			// Class A specifics
			classAStatic = vesselStatic.getAisClassAStatic();
			if (classAStatic == null) {
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

		vesselStatic.setAisVesselTarget(vesselTarget);
		entityManager.merge(vesselStatic);

		// Handle class A
		if (classAStatic != null) {
			classAStatic.setAisVesselStatic(vesselStatic);
			entityManager.merge(classAStatic);
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

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setTargetTtl(int targetTtl) {
		this.targetTtl = targetTtl;
	}

	private void prepareEntityManager() {
		if (entityManager != null) {
			return;
		}
		Map<String, String> dbProps = new HashMap<String, String>();
		dbProps.put("hibernate.connection.url", "jdbc:mysql://" + settings.getDbHost() + ":" + settings.getDbPort() + "/"
				+ settings.getDbName());
		dbProps.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
		dbProps.put("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
		dbProps.put("hibernate.connection.username", settings.getDbUsername());
		dbProps.put("hibernate.connection.password", settings.getDbPassword());
		dbProps.put("hibernate.hbm2ddl.auto", "update");
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("aisrecorder", dbProps);
		entityManager = emf.createEntityManager();
	}

	private void invalidateEntityManager() {
		if (entityManager != null) {
			try {
				entityManager.close();
			} catch (Exception e) {
				LOG.error("Failed to close entity manager: " + e.getMessage());
			}
			entityManager = null;
		}
	}

}
