package kafka.auidtor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AuditReporter {

	ObjectMapper jsonMapper = new ObjectMapper();
	Producer<String, String> producer;
	String auditTopic = "Audit_Topic";
	Properties propKafka;
	Properties propFile;
	FileInputStream fin = null;
	MapKey mapKey;

	public void createKafkaAuditProducer(String auditorPropertiesFile) throws IOException, Exception {
		// Loading Properties files from File
		propFile = new Properties();
		fin = new FileInputStream(auditorPropertiesFile);
		propFile.load(fin);
		fin.close();

		// Setting up kafka audit producer properties
		propKafka = new Properties();
		propKafka.put("bootstrap.servers", propFile.getProperty("audit.producer.bootstrap.servers"));
		propKafka.put("key.serializer", propFile.getProperty("audit.producer.key.serializer"));
		propKafka.put("value.serializer", propFile.getProperty("audit.producer.value.serializer"));
		propKafka.put("acks", propFile.getProperty("audit.producer.acks"));
		propKafka.put("retries", propFile.getProperty("audit.producer.retires"));
		propKafka.put("max.block.ms", propFile.getProperty("audit.producer.max.block.ms"));
		producer = new KafkaProducer(propKafka);
		auditTopic = propFile.getProperty("aduit.producer.kafka.topic");

	}

	public Producer<String, String> getKafkaAuditProducer() {
		return producer;
	}

	Map<MapKey, AtomicLong> produce(Map<MapKey, AtomicLong> auditMap) {

		if (auditMap.size() == 0) {
			System.out.println("No Audit Message to publish");
			return auditMap;
		}

		System.out.println("publishing the audit message");
		for (Map.Entry<MapKey, AtomicLong> entry : auditMap.entrySet()) {
			mapKey = entry.getKey();
			AuditMessage auditMessage = new AuditMessage(mapKey.getTier(), mapKey.getDataTopic(), mapKey.getServer());
			try {
				auditMessage.setrecordCount(entry.getValue());
				auditMessage.setTimestampBucket(mapKey.getTimestampBucket());
				//System.out.println("Audit entry" + entry.getKey() + " Value" + entry.getValue());
				getKafkaAuditProducer().send(
						new ProducerRecord<String, String>(auditTopic, jsonMapper.writeValueAsString(auditMessage)))
						.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return auditMap;
	}

}
