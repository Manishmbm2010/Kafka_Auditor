package kafka.audit.consumer;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;

import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.auidtor.KafkaAuditor;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class SimpleConsumer {

	public static void main(String[] args) throws Exception {
		int i=0;
		String topicName;
		ObjectMapper jsonMapper;
		String propertiesFilePath;
		Properties propFile;
		FileInputStream fin = null;
		Properties propKafka;
		KafkaConsumer<String, String> consumer = null;
		KafkaAuditor auditor = null;
		boolean withAudit = false;

		try {
			propFile = new Properties();
			propertiesFilePath = args[0];
			fin = new FileInputStream(propertiesFilePath);
			propFile.load(fin);
			fin.close();

			// Setting up kafka data producer properties
			propKafka = new Properties();
			propKafka.put("bootstrap.servers", propFile.getProperty("data.consumer.bootstrap.servers"));
			propKafka.put("key.deserializer", propFile.getProperty("data.consumer.key.deserializer"));
			propKafka.put("value.deserializer", propFile.getProperty("data.consumer.value.deserializer"));
			propKafka.put("group.id", propFile.getProperty("data.consumer.group.id"));
			consumer = new KafkaConsumer(propKafka);
			topicName = propFile.getProperty("data.consumer.kafka.topic");
			consumer.subscribe(Arrays.asList(topicName));
			jsonMapper = new ObjectMapper();
			
			try {
				//auditor = new KafkaAuditor("consume", topicName, hostname);
				auditor=new KafkaAuditor();
				auditor.setAuditorProperty(propertiesFilePath);
				withAudit = true;
			} catch (Exception e) {
				System.out.println("Error in Creating Audit producer record , Please find full stack strace below");
				e.printStackTrace();
				System.out.println("Continuting consuming the records without Auditing");
			}

			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(1000);
				for (ConsumerRecord<String, String> record : records) {
					Data data = jsonMapper.readValue(record.value(), Data.class);
					// System.out.println(record.value());
					if (withAudit == true) {
						try {
							//auditor.audit(data.getUnixEpoch());
							auditor.audit(data.getUnixEpoch(), data.getHostname(), topicName, "consume");
						} catch (Exception e) {
							System.out.println(
									record.value() + " Cannot be audited , Please find full stack trace below");
							e.printStackTrace();
						}
						
					}
					System.out.println("Consumer records " + ++i);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (consumer != null) {
				consumer.close();
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