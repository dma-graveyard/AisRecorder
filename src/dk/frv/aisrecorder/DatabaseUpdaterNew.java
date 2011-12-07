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
import dk.frv.ais.message.AisMessage;
import dk.frv.aisrecorder.persistence.domain.AisVesselTarget;
import dk.frv.aisrecorder.persistence.mapper.AisVesselTargetMapper;

public class DatabaseUpdaterNew extends Thread {

	private static final Logger LOG = Logger.getLogger(DatabaseUpdaterNew.class);

	private BlockingQueue<QueueEntry> queue;
	private Settings settings;
	private int batchSize = 1;
	private int targetTtl;
	private int pastTrackTime;
	private long startTime;
	private long messageCount = 0;

	private SqlSessionFactory sqlSessionFactory;
	private SqlSession session;

	public DatabaseUpdaterNew(BlockingQueue<QueueEntry> queue, Settings settings) {
		this.queue = queue;
		this.settings = settings;
		this.batchSize = settings.getBatchSize();
		this.targetTtl = settings.getTargetTtl();
		this.pastTrackTime = settings.getPastTrackTime();
	}

	private void createSessionFactory() throws IOException {
		String resource = "dk/frv/aisrecorder/persistence/xml/Configuration.xml";
		Reader reader = Resources.getResourceAsReader(resource);
		sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, settings.getProps());
	}

	@Override
	public void run() {
		startTime = System.currentTimeMillis();

		try {
			createSessionFactory();
		} catch (IOException e) {
			LOG.error("Could not create SqlSessionFactory: " + e.getMessage());
			System.exit(1);
		}

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

	}
	
	private void staticHandle(QueueEntry queueEntry, AisVesselTarget vesselTarget) {
		AisMessage aisMessage = queueEntry.getAisMessage();

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
