package kafka.auidtor;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class KafkaAuditor {
	private Map<MapKey, AtomicLong> auditMap = new ConcurrentHashMap<MapKey, AtomicLong>();
	private ReadWriteLock timeBucketsRWLock;
	private boolean isAuditingInProgress = false;
	private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
	private ThreadPoolExecutor executors = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
	private AuditReporter auditReporter;
	FileInputStream fin = null;
	String auditorPropertiesFile;
	Properties auditorProperties;
	int nFlooredMinute;
	long intialDelayForFirstRunOfAuditSchedular;
	long auditSchedularFrequency;

	public AuditReporter getAuditReporter() {
		return auditReporter;
	}

	public ThreadPoolExecutor getExecutors() {
		return executors;
	}

	public Map<MapKey, AtomicLong> getAuditmap() {
		return auditMap;
	}

	public KafkaAuditor() throws Exception {
		timeBucketsRWLock = new ReentrantReadWriteLock();
		auditReporter = new AuditReporter();
	}

	public void setAuditorProperty(String auditorPropertiesFile) throws Exception {
		auditorProperties = new Properties();
		fin = new FileInputStream(auditorPropertiesFile);
		auditorProperties.load(fin);
		fin.close();
		nFlooredMinute = Integer.parseInt(auditorProperties.getProperty("audit.timebucket.size"));
		intialDelayForFirstRunOfAuditSchedular = Long
				.parseLong(auditorProperties.getProperty("audit.first.flush.duration"));
		auditSchedularFrequency = Long.parseLong(auditorProperties.getProperty("audit.auditdata.flush.frequency"));
		auditReporter.createKafkaAuditProducer(auditorPropertiesFile);
		System.out.println("Schduling Audit Reporter Job");
		publishAuditMessageToKafkaTopic();
	}

	private void publishAuditMessageToKafkaTopic() throws Exception {

		scheduledExecutor.scheduleAtFixedRate(() -> {
			try {
				System.out.println("Running Scheduled publish job");
				produce();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Audit data publisher thread is crahed");
			}
		}, intialDelayForFirstRunOfAuditSchedular, auditSchedularFrequency, TimeUnit.MILLISECONDS);
	}

	void calcuateBucket(long unixEpoch, String server, String topicName, String tier) throws Exception {
		long flooredMinute = Math.floorDiv(unixEpoch, 60 * nFlooredMinute);
		MapKey mapKey = new MapKey(flooredMinute, topicName, server, tier);
		timeBucketsRWLock.writeLock().lock();
		try {
			if (auditMap.get(mapKey) == null) {
				auditMap.put(mapKey, new AtomicLong(1));
			} else {
				auditMap.get(mapKey).incrementAndGet();
			}
		} finally {
			timeBucketsRWLock.writeLock().unlock();
		}

	}

	public void audit(long unixEpoch, String server, String topicName, String tier) throws Exception {
		executors.submit(() -> {
			try {
				calcuateBucket(unixEpoch, server, topicName, tier);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public Map<MapKey, AtomicLong> produce() throws Exception {
		isAuditingInProgress = true;
		timeBucketsRWLock.writeLock().lock();
		Map<MapKey, AtomicLong> publishedAuditMap = new HashMap<>();
		try {
			publishedAuditMap.putAll(auditReporter.produce(auditMap));
			auditMap.clear();
		} finally {
			timeBucketsRWLock.writeLock().unlock();
		}
		isAuditingInProgress = false;
		return publishedAuditMap;
	}

	public void closeAllRunningThreadsInControlledWay() throws Exception {
		System.out.println("Controlled cleanup ongoing");
		// First Clearing the Task queue in an controlled way
		while (true) {
			System.out.println("Executor queue size " + executors.getQueue().size());
			if (executors.getQueue().size() == 0 && executors.getActiveCount() == 0) {
				System.out.println("Executor Graceful Shutdown Initiated");
				executors.shutdown();
				try {
					if (!executors.awaitTermination(100, TimeUnit.MILLISECONDS)) { // optional
																					// *
						System.out.println("Executor did not terminate in the specified time.");
						System.out.println("Shutting down executors forcefully");
						executors.shutdownNow();
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				System.out.println("Executor Shutdown completed");
				break;
			}
		}
		// Second closing the scheduled thread and auditing the current data
		// from Audit MapF
		while (true) {
			if (isAuditingInProgress == false) {
				System.out.println("Scheduled Executor Shutdown Initiated");
				scheduledExecutor.shutdownNow();
				try {
					while (!scheduledExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
						System.out.println("Scheduled Executor did not terminate in the specified time.");
						System.out.println("Waiting for 100 more milliseconds");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("Scheduled Executor Shutdown Completed");
				produce();
				break;
			}
		}
	}
}
