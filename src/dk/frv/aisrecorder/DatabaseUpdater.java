package dk.frv.aisrecorder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;

import dk.frv.ais.geo.GeoLocation;
import dk.frv.ais.message.AisMessage;
import dk.frv.ais.message.AisMessage18;
import dk.frv.ais.message.AisPositionMessage;
import dk.frv.ais.proprietary.IProprietarySourceTag;
import dk.frv.aisrecorder.entities.AisClassAPosition;
import dk.frv.aisrecorder.entities.AisVesselPosition;
import dk.frv.aisrecorder.entities.AisVesselTarget;

public class DatabaseUpdater extends Thread {

	private static final Logger LOG = Logger.getLogger(DatabaseUpdater.class);

	private BlockingQueue<QueueEntry> queue;
	private int batchSize = 1;
	private EntityManager entityManager = null;

	public DatabaseUpdater(BlockingQueue<QueueEntry> queue, int batchSize) {
		this.queue = queue;
		this.batchSize = batchSize;
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
			AisPositionMessage posMessage = (AisPositionMessage)aisMessage;
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
				vesselPosition.setHeading((double)posMessage.getTrueHeading());
			}
			// Pos acc
			vesselPosition.setPosAcc((byte)posMessage.getPosAcc());
			// Raim
			vesselPosition.setRaim((byte)posMessage.getRaim());
			// UTC sec
			vesselPosition.setUtcSec((byte)posMessage.getUtcSec());

			// Class A info
			classAPosition = vesselPosition.getAisClassAPosition();
			if (classAPosition == null) {
				classAPosition = new AisClassAPosition();
				classAPosition.setMmsi(vesselPosition.getMmsi());
			}
			if (posMessage.isRotValid()) {
				classAPosition.setRot((double)posMessage.getRot());
			}
			classAPosition.setNavStatus((byte)posMessage.getNavStatus());
			classAPosition.setSpecialManIndicator((byte)posMessage.getSpecialManIndicator());
			
		} else {
			AisMessage18 posMessage = (AisMessage18)aisMessage;			
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
				vesselPosition.setHeading((double)posMessage.getTrueHeading());
			}
			// Pos acc
			vesselPosition.setPosAcc((byte)posMessage.getPosAcc());
			// Raim 
			vesselPosition.setRaim((byte)posMessage.getRaimFlag());
			// UTC sec
			vesselPosition.setUtcSec((byte)posMessage.getUtcSec());
		}
		
		vesselPosition.setAisVesselTarget(vesselTarget);
		entityManager.merge(vesselPosition);
		
		// Handle class A
		if (classAPosition != null) {
			classAPosition.setAisVesselPosition(vesselPosition);
			entityManager.merge(classAPosition);
		}
		
	}

	private void staticHandle(QueueEntry queueEntry, AisVesselTarget vesselTarget) {
		// TODO Auto-generated method stub
		
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

	private void prepareEntityManager() {
		if (entityManager != null) {
			return;
		}
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("aisrecorder");
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
