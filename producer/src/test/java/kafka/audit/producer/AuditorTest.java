package kafka.audit.producer;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import kafka.auidtor.KafkaAuditor;
import kafka.auidtor.MapKey;

public class AuditorTest {

	String topic = "test";
	KafkaAuditor auditor;
	String hostname = "localhost";
	Map<MapKey, AtomicLong> publishedAuditMap;
	MapKey mapKey = null;
	String propertiesFilePath;
	Properties propFile;
	FileInputStream fin = null;

	@Before
	public void setUp() throws Exception {
		propertiesFilePath = System.getProperty("fileName");
		auditor = new KafkaAuditor();
		auditor.setAuditorProperty(propertiesFilePath);
		System.out.println("Property file name is" + propertiesFilePath);
	}

	@Test
	public void testA_checkNoOfMessage_in_auditMap_multipleUnixEpoch() throws Exception {
		System.out.println("Test 2 started");
		long unixEpoch = Instant.now().getEpochSecond();
		for (int i = 0; i < 10; i++) {
			auditor.audit(unixEpoch, hostname, topic, "produce");
		}
		mapKey = new MapKey(Math.floorDiv(unixEpoch, 60 * 1), topic, hostname, "produce");
		for (int i = 0; i < 20; i++) {
			auditor.audit(unixEpoch + 60, hostname, topic, "produce");
		}
		MapKey mapKey2 = new MapKey(Math.floorDiv(unixEpoch + 60, 60 * 1), topic, hostname, "produce");
		while (auditor.getExecutors().getActiveCount() != 0 || auditor.getExecutors().getQueue().size() != 0) {
			// System.out.println("Auditor Queue size" +
			// auditor.getExecutors().getQueue().size());
		}
		publishedAuditMap = auditor.produce();
		// System.out.println(publishedAuditMap);
		// System.out.println("Print the value of
		// map"+publishedAuditMap.get(mapKey));
		assertEquals(10, publishedAuditMap.get(mapKey).get());
		assertEquals(20, publishedAuditMap.get(mapKey2).get());
	}
}
