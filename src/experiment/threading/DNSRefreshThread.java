package experiment.threading;

public class DNSRefreshThread implements Runnable {

	private DNSUser myParent;

	private static final long DNS_GOAL_INTERVAL = 60000;

	public DNSRefreshThread(DNSUser parent) {
		this.myParent = parent;
	}

	@Override
	public void run() {

		try {
			while (true) {

				long startTime = System.currentTimeMillis();
				this.myParent.dnsRefresh();
				long totalTime = System.currentTimeMillis() - startTime;

				/*
				 * Wait until the next sample interval
				 */
				if (totalTime < DNSRefreshThread.DNS_GOAL_INTERVAL) {
					Thread.sleep(DNSRefreshThread.DNS_GOAL_INTERVAL - totalTime);
				}
			}
		} catch (InterruptedException e) {
			System.err.println("DNS Refresh Thread dying.");
		}

	}

}
