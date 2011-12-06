package dk.frv.aisrecorder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

public class DatabaseUpdaterNew extends Thread {
	
	private static final Logger LOG = Logger.getLogger(DatabaseUpdaterNew.class);
	
	private BlockingQueue<QueueEntry> queue;
	private Settings settings;
	private int batchSize = 1;
	private int targetTtl;
	private int pastTrackTime;
	
	private long messageCount = 0;
	
	public DatabaseUpdaterNew(BlockingQueue<QueueEntry> queue, Settings settings) {
		this.queue = queue;
		this.settings = settings;
		this.batchSize = settings.getBatchSize();
		this.targetTtl = settings.getTargetTtl();
		this.pastTrackTime = settings.getPastTrackTime();
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
		// Transaction start
				
		// Handle individual message
		for (QueueEntry queueEntry : batch) {
			messageHandle(queueEntry);
		}
		
		// Transaction end
	}
	
	private void messageHandle(QueueEntry queueEntry) {
		messageCount++;
	
	
		if (messageCount % 1000 == 0) {
			System.out.println("date: " + (new Date()) + " messages: " + messageCount);
		}
	
	}

	
	
	
}
