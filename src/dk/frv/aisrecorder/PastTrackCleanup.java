package dk.frv.aisrecorder;

import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;

import dk.frv.aisrecorder.persistence.mapper.AisVesselTrackMapper;

public class PastTrackCleanup extends Thread {
	
	private static final Logger LOG = Logger.getLogger(PastTrackCleanup.class);
	
	private static final long PAST_TRACK_CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 min
	
	private SqlSessionFactory sqlSessionFactory;
	private int pastTrackTime;
	
	public PastTrackCleanup(SqlSessionFactory sqlSessionFactory, int pastTrackTime) {		
		this.sqlSessionFactory = sqlSessionFactory;
		this.pastTrackTime = pastTrackTime;
	}
	
	@Override
	public void run() {
		while (true) {
			AisRecorder.sleep(PAST_TRACK_CLEANUP_INTERVAL);
			try {
				pastTrackCleanup();
			} catch (Exception e) {
				LOG.error("Failed to cleanup past track: " + e.getMessage());
			}
		}
	}
	
	
	private void pastTrackCleanup() {
		long now = System.currentTimeMillis();
		Date cleanupDate = new Date(now - pastTrackTime * 1000);

		SqlSession session = sqlSessionFactory.openSession(false);
		try {
			AisVesselTrackMapper aisVesselTrackMapper = session.getMapper(AisVesselTrackMapper.class);
			int deleted = aisVesselTrackMapper.deleteOld(cleanupDate);
			System.out.println("deleted: " + deleted);
			session.commit();
		} finally {
			session.close();
		}
	}


}
