package experiment;

import java.util.*;
import java.io.*;
import java.net.*;

import data.Contact;

import net.Constants;
import net.Node;

import experiment.threading.ConnectorThread;

public class ConnectionExperiment {

	private static final String[] HOSTS = { "seed.bitcoinstats.com", "bitseed.xf2.org", "seed.bitcoin.sipa.be",
			"dnsseed.bitcoin.dashjr.org" };

	private static final int THREAD_COUNT = 20;

	private Set<Contact> nodesToTest;
	private Set<Node> successfulNodes;
	private BufferedWriter logFile;
	private ExperimentContainer workHolder;

	public ConnectionExperiment(BufferedWriter log) {
		this.nodesToTest = new HashSet<Contact>();
		this.successfulNodes = new HashSet<Node>();
		this.logFile = log;

		/*
		 * Build container and worker threads, start those threads
		 */
		this.workHolder = new ExperimentContainer();
		for (int counter = 0; counter < THREAD_COUNT; counter++) {
			Thread tThread = new Thread(new ConnectorThread(this.workHolder));
			tThread.setDaemon(true);
			tThread.start();
		}
	}

	public void shutdown() {

		try {
			this.logFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TODO close nodes?

		// TODO shutdown threads?
	}

	public void pushNodesToTest(Set<Contact> targets) {
		this.successfulNodes.clear();
		this.nodesToTest.addAll(targets);
	}

	public Set<Node> getReachableNodes() {
		return this.successfulNodes;
	}

	public void run() throws InterruptedException {
		/*
		 * Make sure we actually have data
		 */
		if (this.nodesToTest.size() == 0) {
			System.err.println("Tried to run Connection Experiment with no nodes.");
			return;
		}

		/*
		 * Prune out IPv6 Addresses
		 */
		Set<Contact> ipv4Addresses = new HashSet<Contact>();
		for (Contact tContact : this.nodesToTest) {
			if (tContact.getIp() instanceof Inet4Address) {
				ipv4Addresses.add(tContact);
			}
		}

		System.out.println("Unique IPv4 Addresses: " + ipv4Addresses.size() + " (" + (double) ipv4Addresses.size()
				/ (double) this.nodesToTest.size() + ")");

		long start = System.currentTimeMillis();

		/*
		 * Pass nodes to workers
		 */
		for (Contact tContact : ipv4Addresses) {
			this.workHolder.nodeReadyToWork(tContact.getNodeForContact());
		}

		/*
		 * Get nodes back, check for success, file into the correct set
		 */
		int passed = 0;
		for (int counter = 0; counter < ipv4Addresses.size(); counter++) {
			Node doneNode = this.workHolder.fetchCompleted();
			if (doneNode.thinksConnected()) {
				passed++;
				this.successfulNodes.add(doneNode);
			}
		}
		long stop = System.currentTimeMillis();

		System.out.println("Time taken: " + (stop - start));
		System.out.println("Reachable: " + (double) passed / (double) ipv4Addresses.size());

		/*
		 * Do round logging
		 */
		try {
			this.logFile.write("IPv4," + ipv4Addresses.size() + "," + this.nodesToTest.size() + "\n");
			this.logFile.write("reachable," + passed + "," + ipv4Addresses.size() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static Set<Contact> dnsBootStrap() {
		HashSet<Contact> dnsHarvestedNodes = new HashSet<Contact>();

		for (String tCanonName : ConnectionExperiment.HOSTS) {
			try {
				InetAddress[] hosts = InetAddress.getAllByName(tCanonName);
				for (InetAddress tHost : hosts) {
					dnsHarvestedNodes
							.add(new Contact(tHost, Constants.DEFAULT_PORT, System.currentTimeMillis(), false));
				}
			} catch (UnknownHostException e) {
				System.err.println("Error with canonical name: " + tCanonName);
			}
		}

		return dnsHarvestedNodes;
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		Set<Contact> testNodes = ConnectionExperiment.dnsBootStrap();
		BufferedWriter outBuff = new BufferedWriter(new FileWriter("logs/testCon.log"));
		ConnectionExperiment test = new ConnectionExperiment(outBuff);
		test.pushNodesToTest(testNodes);
		test.run();
		test.shutdown();
	}

}
