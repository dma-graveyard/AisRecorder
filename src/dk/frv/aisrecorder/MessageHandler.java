package dk.frv.aisrecorder;

import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import dk.frv.ais.handler.IAisHandler;
import dk.frv.ais.message.AisMessage;
import dk.frv.ais.proprietary.IProprietarySourceTag;

public class MessageHandler implements IAisHandler {

	private static final Logger LOG = Logger.getLogger(MessageHandler.class);

	private Date lastInsertErrorReported = new Date(0);
	private BlockingQueue<QueueEntry> queue;
	private long queuedMessages = 0;
	private long overflowMessages = 0;

	public MessageHandler(BlockingQueue<QueueEntry> queue) {
		this.queue = queue;
	}

	@Override
	public synchronized void receive(AisMessage aisMessage) {
		// Make new queueEntry
		QueueEntry queueEntry = new QueueEntry(aisMessage, new Date());
		
		// Try to determine source
		String source = "LIVE";
		IProprietarySourceTag tag = aisMessage.getSourceTag();
		if (tag != null) {
			String region = tag.getRegion();
			if (region.equals("802") || region.equals("804")) {
				source = "SAT";
			}
		}
		queueEntry.setSource(source);

		// Try to add to queue
		try {
			queue.add(queueEntry);
			queuedMessages++;
		} catch (IllegalStateException e) {
			overflowMessages++;
			Date now = new Date();			
			if (now.getTime() - lastInsertErrorReported.getTime() > 60000) {
				lastInsertErrorReported = now;
				LOG.error("Failed to insert message to queue: Queue overflow (" + overflowMessages + "/" + queuedMessages + ")");
			}
			
		}

	}

}
