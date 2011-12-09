package dk.frv.aisrecorder;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import dk.frv.ais.proprietary.GatehouseFactory;
import dk.frv.ais.reader.RoundRobinAisTcpReader;

public class Settings {

	private static final Logger LOG = Logger.getLogger(Settings.class);

	private Properties props;
	private Map<String, RoundRobinAisTcpReader> aisSources = new HashMap<String, RoundRobinAisTcpReader>();
	private int doubleFilterWindow;
	private int downsamplingRate;
	private int queueSize;
	private int batchSize;
	private String dbHost;
	private int dbPort;
	private String dbName;
	private String dbUsername;
	private String dbPassword;
	private int liveTargetTtl;
	private int satTargetTtl;
	private int pastTrackTime;
	private int pastTrackMinDist;

	public Settings() {

	}

	public void load(String filename) throws IOException {
		props = new Properties();
		URL url = ClassLoader.getSystemResource(filename);
		if (url == null) {
			throw new IOException("Could not find properties file: " + filename);
		}
		props.load(url.openStream());
		
		// Create AIS sources
		String sourcesStr = props.getProperty("ais_sources", "");
		for (String name : StringUtils.split(sourcesStr, ",")) {
			RoundRobinAisTcpReader reader = new RoundRobinAisTcpReader();
			reader.setName(name);
			String hostsStr = props.getProperty("ais_source_hosts." + name, "");
			reader.setCommaseparatedHostPort(hostsStr);
			if (reader.getHostCount() == 0) {
				LOG.error("No valid AIS source hosts given");
				System.exit(1);
			}
			
			reader.setTimeout(getInt("ais_source_timeout." + name, "10"));
			reader.setReconnectInterval(getInt("ais_source_reconnect_interval." + name, "5") * 1000);
			
			// Register proprietary handlers
			reader.addProprietaryFactory(new GatehouseFactory());
			
			aisSources.put(name, reader);
			
			LOG.info("Added TCP reader " + name + " (" + hostsStr + ")");			
		}
		
		// Determine doublet filtering
		doubleFilterWindow = getInt("doublet_filtering", "10");
		
		// Determine down sampling
		downsamplingRate = getInt("downsampling", "0");
		
		// Determine queue size
		queueSize = getInt("queue_size", "100");
		
		// Determine batch size
		batchSize = getInt("batch_size", "1");
		
		// Database details
		dbHost = props.getProperty("db_host", "localhost");
		dbPort = getInt("db_port", "3306");
		dbName = props.getProperty("db_name", "");
		dbUsername = props.getProperty("db_username","");
		dbPassword = props.getProperty("db_password", "");
		
		// Determine LIVE target TTL
		liveTargetTtl = getInt("live_target_ttl", "1200");
		
		// Determine SAT target TTL
		satTargetTtl = getInt("sat_target_ttl", "172800");
		
		// Determine past track time
		pastTrackTime = getInt("past_track_time", "7200");
		
		// Determine min dist between track points
		pastTrackMinDist = getInt("past_track_min_dist", "100");
	}
	
	public Properties getProps() {
		return props;
	}
	
	private int getInt(String key, String defaultValue) {
		String val = props.getProperty(key, defaultValue);
		return Integer.parseInt(val);
	}
	
	public int getDoubleFilterWindow() {
		return doubleFilterWindow;
	}
	
	public int getDownsamplingRate() {
		return downsamplingRate;
	}
	
	public Map<String, RoundRobinAisTcpReader> getAisSources() {
		return aisSources;
	}

	public int getQueueSize() {
		return queueSize;
	}
	
	public int getBatchSize() {
		return batchSize;
	}
	
	public String getDbHost() {
		return dbHost;
	}
	
	public int getDbPort() {
		return dbPort;
	}
	
	public String getDbUsername() {
		return dbUsername;
	}
	
	public String getDbPassword() {
		return dbPassword;
	}
	
	public String getDbName() {
		return dbName;
	}

	public int getLiveTargetTtl() {
		return liveTargetTtl;
	}
	
	public int getSatTargetTtl() {
		return satTargetTtl;
	}
	
	public int getPastTrackTime() {
		return pastTrackTime;
	}
	
	public int getPastTrackMinDist() {
		return pastTrackMinDist;
	}
	
	public void setPastTrackTime(int pastTrackTime) {
		this.pastTrackTime = pastTrackTime;
	}
	
}
