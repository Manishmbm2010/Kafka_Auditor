package kafka.auidtor;

import java.util.concurrent.atomic.AtomicLong;

public class AuditMessage {

	private String dataTopic;
	private String server;
	private String tier;
	private long timestampBucket;
	private AtomicLong recordCount;

	public AuditMessage(String tier, String topicName, String server) {
		this.tier = tier;
		this.dataTopic = topicName;
		this.server = server;
	}

	public String getDataTopic() {
		return dataTopic;
	}

	public String getServer() {
		return server;
	}

	public String getTier() {
		return tier;
	}

	public long getTimestampBucket() {
		return timestampBucket;
	}

	public void setTimestampBucket(long timestampBucket) {
		this.timestampBucket = timestampBucket;
	}

	public long getrecordCount() {
		return recordCount.get();
	}

	public void setrecordCount(AtomicLong recordCount) {
		this.recordCount = recordCount;
	}
}
