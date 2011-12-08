package dk.frv.aisrecorder;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import dk.frv.ais.filter.MessageDoubletFilter;
import dk.frv.ais.filter.MessageDownSample;
import dk.frv.ais.handler.IAisHandler;
import dk.frv.ais.reader.RoundRobinAisTcpReader;

public class AisRecorder {

	private static Logger LOG;
	private static Settings settings = new Settings();

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		DOMConfigurator.configure("log4j.xml");
		LOG = Logger.getLogger(AisRecorder.class);
		LOG.info("Starting AisRecorder");

		// Load configuration
		String propsFile = "aisrecorder.properties";
		if (args.length > 0) {
			propsFile = args[0];
		}
		try {
			settings.load(propsFile);
		} catch (IOException e) {
			LOG.error("Failed to load settings: " + e.getMessage());
			System.exit(-1);
		}

		// Message queue
		BlockingQueue<QueueEntry> queue = new ArrayBlockingQueue<QueueEntry>(settings.getQueueSize());

		// Create and start consumer
		DatabaseUpdater databaseUpdater = new DatabaseUpdater(queue, settings);
		databaseUpdater.start();

		// Create the basic handler
		MessageHandler messageHandler = new MessageHandler(queue);
		IAisHandler handler = messageHandler;

		// Maybe insert down sampling filter
		if (settings.getDownsamplingRate() > 0) {
			LOG.info("Enabling down sampling with rate " + settings.getDownsamplingRate());
			MessageDownSample downsample = new MessageDownSample(settings.getDownsamplingRate());
			downsample.registerReceiver(messageHandler);
			handler = downsample;
		}

		// Maybe insert doublet filtering
		if (settings.getDoubleFilterWindow() > 0) {
			LOG.info("Enabling doublet filtering with window " + settings.getDoubleFilterWindow());
			MessageDoubletFilter doubletFilter = new MessageDoubletFilter();
			doubletFilter.setWindowSize(settings.getDoubleFilterWindow());
			doubletFilter.registerReceiver(handler);
			handler = doubletFilter;
		}

		// Start readers
		for (RoundRobinAisTcpReader reader : settings.getAisSources().values()) {
			reader.registerHandler(handler);
			reader.start();
		}

		// Wait for all readers to finish
		for (RoundRobinAisTcpReader reader : settings.getAisSources().values()) {
			reader.join();
		}

	}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
