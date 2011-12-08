package dk.frv.aisrecorder;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import dk.frv.aisrecorder.persistence.domain.AisVesselTarget;
import dk.frv.aisrecorder.persistence.mapper.AisVesselTargetMapper;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("TEST");
		
		Properties props = new Properties();
		URL url = ClassLoader.getSystemResource("aisrecorder.properties");
		if (url == null) {
			throw new IOException("Could not find properties file");
		}
		props.load(url.openStream());
		
		System.out.println(props.getProperty("db_name"));
		
		String resource = "dk/frv/aisrecorder/persistence/xml/Configuration.xml";
		Reader reader = Resources.getResourceAsReader(resource);
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, props); 
		SqlSession session = sqlSessionFactory.openSession();
		
		
		AisVesselTargetMapper mapper = session.getMapper(AisVesselTargetMapper.class);
		AisVesselTarget target = mapper.selectByPrimaryKey(219000000);
		
		System.out.println("target: " + target);
		

	}

}
