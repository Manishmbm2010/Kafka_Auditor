package kafka.auidtor;

public class MapKey {

	private Long timestampBucket;
	private String dataTopic;
	private String server;
	private String tier;

	public MapKey() {

	}

	public MapKey(Long timestampBucket, String dataTopic, String server, String tier) {
		this.timestampBucket = timestampBucket;
		this.dataTopic = dataTopic;
		this.server = server;
		this.tier = tier;
	}

	@Override
	public boolean equals(Object object) {
		if (((MapKey) object).timestampBucket == null && ((MapKey) object).dataTopic == null
				&& ((MapKey) object).server == null && ((MapKey) object).tier == null) {
			//System.out.println("block1");
			return true;
		}
		if (((MapKey) object).timestampBucket.equals(this.timestampBucket)
				&& ((MapKey) object).dataTopic.equals(this.dataTopic) && ((MapKey) object).server.equals(this.server)
				&& ((MapKey) object).tier.equals(this.tier)) {
			//System.out.println("block4");
			return true;
		}
		//System.out.println("fale block");
		return false;
	}

	@Override
	public int hashCode() {
		int hashCodeTimeStampBucket = this.timestampBucket == null ? 0 : this.timestampBucket.hashCode();
		int hashCodeDataTopic = this.dataTopic == null ? 0 : this.dataTopic.hashCode();
		int hashCodeServer = this.server == null ? 0 : this.server.hashCode();
		int hashCodeTier = this.tier == null ? 0 : this.tier.hashCode();
		int finalHashCode = hashCodeTimeStampBucket + hashCodeDataTopic + hashCodeTier + hashCodeServer;
		// System.out.println(hashCodeTimeStampBucket);
		//System.out.println("final hash code is " + finalHashCode);
		return finalHashCode;
	}

	public String getDataTopic() {
		return dataTopic;
	}

	public void setDataTopic(String dataTopic) {
		this.dataTopic = dataTopic;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public long getTimestampBucket() {
		return timestampBucket;
	}

	public void setTimestampBucket(long timestampBucket) {
		this.timestampBucket = timestampBucket;
	}

}
