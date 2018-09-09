package kafka.audit.producer;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import org.apache.kafka.clients.producer.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.auidtor.KafkaAuditor;

public class SimpleProducer {

	public static void main(String[] args) throws Exception {
		String topicName = null;
		ObjectMapper jsonMapper;
		String propertiesFilePath;
		Properties propFile;
		FileInputStream fin = null;
		Properties propKafka;
		Producer<String, String> producer = null;
		String hostname;
		KafkaAuditor auditor = null;
		boolean withAudit = false;

		try {

			jsonMapper = new ObjectMapper();
			propertiesFilePath = args[0];
			// Loading Properties files from File
			propFile = new Properties();
			fin = new FileInputStream(propertiesFilePath);
			propFile.load(fin);
			fin.close();

			// Setting up kafka data producer properties
			propKafka = new Properties();
			propKafka.put("bootstrap.servers", propFile.getProperty("data.producer.bootstrap.servers"));
			propKafka.put("key.serializer", propFile.getProperty("data.producer.key.serializer"));
			propKafka.put("value.serializer", propFile.getProperty("data.producer.value.serializer"));
			propKafka.put("acks", propFile.getProperty("data.producer.acks"));
			propKafka.put("retries", propFile.getProperty("data.producer.retires"));
			propKafka.put("max.block.ms", propFile.getProperty("data.producer.max.block.ms"));
			producer = new KafkaProducer(propKafka);
			topicName = propFile.getProperty("data.producer.kafka.topic");
			hostname = InetAddress.getLocalHost().getHostName();

			try {
				// auditor = new KafkaAuditor("produce", topicName, hostname);
				auditor = new KafkaAuditor();
				auditor.setAuditorProperty(propertiesFilePath);
				withAudit = true;
			} catch (Exception e) {
				System.out.println("Error in Creating Audit producer record , Please find full stack strace below");
				e.printStackTrace();
				System.out.println("Continuting producing the records without Auditing");
			}
			for (long i = 1; i <= 1000; i++) {

				long unixEpoch = Instant.now().getEpochSecond();
				Thread.sleep(100);
				Data data = new Data(hostname, unixEpoch);
				String message = jsonMapper.writeValueAsString(data);
				producer.send(new ProducerRecord(topicName, "", message)).get();
				if (withAudit == true) {
					try {
						// auditor.audit(data.getUnixEpoch());
						auditor.audit(data.getUnixEpoch(), data.getHostname(), topicName, "produce");
					} catch (Exception e) {
						System.out.println(message + " Cannot be audited , Please find full stack trace below");
						e.printStackTrace();
					}
				}
				System.out.println("Produced records " + i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (producer != null) {
				producer.close();
			}
			if (fin != null) {
				fin.close();
			}
			if (auditor != null) {
				auditor.closeAllRunningThreadsInControlledWay();
			}
			System.out.println("Producer and Auditor Gracefull shutdown finished.");
		}
	}
}