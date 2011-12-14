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

	public PastTrackCleanup(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
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
		Date now = new Date();

		SqlSession session = sqlSessionFactory.openSession(false);
		try {
			AisVesselTrackMapper aisVesselTrackMapper = session.getMapper(AisVesselTrackMapper.class);
			int deleted = aisVesselTrackMapper.deleteOld(now);
			LOG.debug("Past track deleted: " + deleted);
			session.commit();
		} finally {
			session.close();
		}
	}

}
