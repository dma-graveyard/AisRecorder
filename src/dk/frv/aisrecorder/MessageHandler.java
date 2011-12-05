package dk.frv.aisrecorder;

import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import dk.frv.ais.handler.IAisHandler;
import dk.frv.ais.message.AisMessage;

public class MessageHandler implements IAisHandler {

	private static final Logger LOG = Logger.getLogger(MessageHandler.class);

	private Date lastInsertErrorReported = new Date(0);
	private BlockingQueue<QueueEntry> queue;

	public MessageHandler(BlockingQueue<QueueEntry> queue) {
		this.queue = queue;
	}

	@Override
	public synchronized void receive(AisMessage aisMessage) {
		// Make new queueEntry
		QueueEntry queueEntry = new QueueEntry(aisMessage, new Date());

		// Try to add to queue
		try {
			queue.add(queueEntry);			
		} catch (IllegalStateException e) {
			Date now = new Date();
			if (now.getTime() - lastInsertErrorReported.getTime() > 5000) {
				lastInsertErrorReported = now;
				LOG.error("Failed to insert message to queue: Queue overflow");
			}
			
		}

	}

}
