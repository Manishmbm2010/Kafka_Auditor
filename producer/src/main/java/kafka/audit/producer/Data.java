package kafka.audit.producer;

public class Data {

	private String hostname;
	private long unixEpoch;

	public Data(String hostname, long unixEpoch) {
		this.hostname = hostname;
		this.unixEpoch = unixEpoch;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public long getUnixEpoch() {
		return unixEpoch;
	}

	public void setUnixEpoch(long unixEpoch) {
		this.unixEpoch = unixEpoch;
	}
}
