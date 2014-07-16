package data;

public class UptimeEntry {

	private long goesUpTime;
	private long goesDownTime;

	private static final long NO_DATA = -1;

	public UptimeEntry(long timeFirstSeenOnline) {
		this.goesUpTime = timeFirstSeenOnline;
		this.goesDownTime = UptimeEntry.NO_DATA;
	}

	public boolean isEntryClosed() {
		return this.goesDownTime == UptimeEntry.NO_DATA;
	}

	public void setOfflineTime(long offlineTimeStamp) {
		if (this.goesUpTime > offlineTimeStamp) {
			throw new RuntimeException("Node came in a TARDIS");
		}

		this.goesDownTime = offlineTimeStamp;
	}

	public String toString() {
		String downStr;

		if (this.isEntryClosed()) {
			downStr = "" + goesDownTime;
		} else {
			downStr = "Never";
		}

		return "Up at " + this.goesUpTime + ", Down at " + downStr;
	}

}
