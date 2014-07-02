package experiment;

import java.util.*;

import java.util.logging.*;
import java.io.*;
import java.net.*;

import data.Contact;
import logging.LogHelper;
import net.Constants;
import net.Node;

import experiment.threading.ConnectorThread;

public class ConnectionExperiment {

	private static final String[] HOSTS = { "seed.bitcoinstats.com", "bitseed.xf2.org", "seed.bitcoin.sipa.be" };

	private static final int THREAD_COUNT = 20;

	private Set<Contact> nodesToTest;
	private Set<Node> successfulNodes;
	private Logger expLogger;
	private ExperimentContainer<Node, Node> workHolder;

	public ConnectionExperiment(boolean generateOwnLogger) {
		Constants.initConstants();

		this.nodesToTest = new HashSet<Contact>();
		this.successfulNodes = new HashSet<Node>();

		this.expLogger = Logger.getLogger(Constants.HARVEST_LOG);
		if (generateOwnLogger) {
			// TODO impl
		}

		/*
		 * Build container and worker threads, start those threads
		 */
		this.workHolder = new ExperimentContainer<Node, Node>();
		for (int counter = 0; counter < THREAD_COUNT; counter++) {
			Thread tThread = new Thread(new ConnectorThread(this.workHolder));
			tThread.setDaemon(true);
			tThread.start();
		}
	}

	public void shutdown() {

		// TODO close nodes?

		// TODO shutdown threads?
	}

	public void pushNodesToTest(Set<Contact> targets) {
		this.nodesToTest.clear();
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
		int failed = 0;
		HashMap<String, Integer> errorCounts = new HashMap<String, Integer>();
		for (int counter = 0; counter < ipv4Addresses.size(); counter++) {
			Node doneNode = this.workHolder.fetchCompleted();
			if (doneNode.thinksConnected()) {
				passed++;
				this.successfulNodes.add(doneNode);
			} else {
				failed++;
				String ec = doneNode.getErrorMsg(false);
				
				if(!errorCounts.containsKey(ec)){
					errorCounts.put(ec, 0);
				}
				errorCounts.put(ec, errorCounts.get(ec) + 1);
			}
		}
		long stop = System.currentTimeMillis();

		/*
		 * Do round logging
		 */
		this.expLogger.warning("Connection took: " + LogHelper.formatMili(stop - start));
		this.expLogger.info("IPv4," + ipv4Addresses.size() + "," + this.nodesToTest.size());
		this.expLogger.info("reachable," + passed + "," + ipv4Addresses.size());
		this.expLogger.info("failed to conn " + failed);
		for(String tError: errorCounts.keySet()){
			this.expLogger.info("failure " + errorCounts.get(tError) + " : " + tError);
		}
		
		/*
		 * Log user agent population
		 */
		HashMap<String, Double> uaMap = this.buildUserAgentCount();
		List<String> orderList = LogHelper.buildDecendingList(uaMap);
		for (int counter = 0; counter < orderList.size(); counter++) {
			this.expLogger.config("ua position " + (counter + 1) + " " + orderList.get(counter) + " with "
					+ uaMap.get(orderList.get(counter)));
		}
	}

	private HashMap<String, Double> buildUserAgentCount() {
		HashMap<String, Double> retMap = new HashMap<String, Double>();

		for (Node tNode : this.successfulNodes) {
			String uaStr = tNode.getRemoteUserAgent();
			if (!retMap.containsKey(uaStr)) {
				retMap.put(uaStr, 0.0);
			}
			retMap.put(uaStr, retMap.get(uaStr) + 1.0);
		}

		return retMap;
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

		ConnectionExperiment test = new ConnectionExperiment(true);
		test.pushNodesToTest(testNodes);
		test.run();

		Set<Node> reachableNodes = test.getReachableNodes();
		long headOfBlockChain = 0;
		int reportingThisBlock = 0;
		HashMap<String, Integer> uaCounts = new HashMap<String, Integer>();
		for (Node tNode : reachableNodes) {
			headOfBlockChain = Math.max(headOfBlockChain, tNode.getLastBlockSeen());
			if (tNode.getLastBlockSeen() == headOfBlockChain) {
				reportingThisBlock++;
			}
			String ua = tNode.getRemoteUserAgent();
			if (ua != null) {
				if (!uaCounts.containsKey(ua)) {
					uaCounts.put(ua, 0);
				}
				uaCounts.put(ua, uaCounts.get(ua) + 1);
			}
		}

		System.out.println("Block chain head: " + headOfBlockChain + "(" + reportingThisBlock + " nodes)");
		List<String> uaList = new ArrayList<String>(uaCounts.size());
		for (int counter = 0; counter < uaCounts.size(); counter++) {
			int largest = 0;
			String bestUA = null;
			for (String tUA : uaCounts.keySet()) {
				if (uaList.contains(tUA)) {
					continue;
				}
				if (uaCounts.get(tUA) > largest) {
					largest = uaCounts.get(tUA);
					bestUA = tUA;
				}
			}
			uaList.add(bestUA);
		}
		System.out.println("UA List: ");
		for (int counter = 0; counter < uaList.size(); counter++) {
			System.out.println(uaList.get(counter) + " : " + uaCounts.get(uaList.get(counter)));
		}

		test.shutdown();
	}

}
