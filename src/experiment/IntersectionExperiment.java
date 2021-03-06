package experiment;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import logging.LogHelper;
import zmap.ZmapSupplicant;
import net.Constants;
import net.Node;
import data.Contact;
import experiment.threading.DNSUser;
import experiment.threading.DNSRefreshThread;

public class IntersectionExperiment implements DNSUser {

	/**
	 * Map that stores the time stamps for all known nodes. The map is keyed by
	 * the node giving the time stamp, mapping to an internal map which stores
	 * between client and timestamp. Slight duplication of data since the
	 * timestamp is in the contact object, but makes life cleaner.
	 */
	private HashMap<Node, HashMap<Contact, Long>> lastActivityMap;

	/**
	 * Map that stores nodes which we see advance, map is keyed by the node who
	 * reported advancment, mapped to a set of clients that advanced (are
	 * online)
	 */
	private HashMap<Node, Set<Contact>> advancingNodes;

	private HashMap<Node, Long> timeSkewFix;

	private List<Double> advancingWindowList;

	private HashSet<Node> activeConnections;
	private HashSet<Node> newConnectionHoldingPen;

	private HashSet<Contact> activeContacts;
	private HashSet<Contact> historicalContacts;
	// TODO periodically clear bellow so we try people once in a while
	// TODO better idea....do what we were trying to do (cutdown on work) in a
	// smart manner
	private HashSet<Contact> bootstrapConsideredContacts;

	private ConnectionExperiment connTester;
	private HarvestExperiment harvester;
	private PingTester pinger;
	private ZmapSupplicant zmapper;
	private Contact selfContact;

	private BufferedWriter logBuffer;

	private static final long INTER_SAMPLE_TIME = 300000;
	private static final int SAMPLE_COUNT = 10;

	public IntersectionExperiment() throws InterruptedException, UnknownHostException, IOException {
		Constants.initConstants();
		LogHelper.initLogger();

		/*
		 * Turn off the system logger used by other experiments to save disk
		 * space
		 */
		Logger expLogger = Logger.getLogger(Constants.HARVEST_LOG);
		expLogger.setLevel(Level.SEVERE);

		this.lastActivityMap = new HashMap<Node, HashMap<Contact, Long>>();
		this.advancingNodes = new HashMap<Node, Set<Contact>>();
		this.advancingWindowList = new LinkedList<Double>();
		this.timeSkewFix = new HashMap<Node, Long>();

		this.activeConnections = new HashSet<Node>();
		this.newConnectionHoldingPen = new HashSet<Node>();

		this.activeContacts = new HashSet<Contact>();
		this.historicalContacts = new HashSet<Contact>();
		this.bootstrapConsideredContacts = new HashSet<Contact>();

		this.connTester = new ConnectionExperiment(false);
		this.harvester = new HarvestExperiment(false);
		this.pinger = new PingTester();
		this.zmapper = new ZmapSupplicant();
		this.selfContact = new Contact(InetAddress.getLocalHost(), Constants.DEFAULT_PORT);

		this.logBuffer = new BufferedWriter(new FileWriter(Constants.LOG_DIR + "network-status.log"));

		/*
		 * Start off by fetching peers, after that do a pair of refreshes, that
		 * should give us knowledge about roughly all the nodes
		 */
		this.dnsRefresh();
		this.refresh();
		this.refresh();

		/*
		 * Now that we're ready to go start up the DNS refresher thread
		 */
		DNSRefreshThread dnsRefresh = new DNSRefreshThread(this);
		Thread dnsThread = new Thread(dnsRefresh);
		dnsThread.setDaemon(true);
		dnsThread.setName("DNS Refresh Thread");
		dnsThread.start();
	}

	private void refresh() {

		long start = System.currentTimeMillis();

		/*
		 * Pull any new nodes we've connected to into our sample set
		 */
		this.integrateNewNodes();

		/*
		 * Harvest information from nodes we're connected to
		 */
		this.harvest();

		/*
		 * Test failed connections, then try to reconnect to anyone we lost
		 * connection to
		 */
		this.testNodes();

		/*
		 * Take a snapshot of historical nodes, and then try to reconnect to
		 * them
		 */
		HashSet<Contact> historicalSnapshot = new HashSet<Contact>();
		synchronized (this.historicalContacts) {
			historicalSnapshot.addAll(this.historicalContacts);
		}
		this.boostrap(historicalSnapshot);

		/*
		 * Small amount of runtime logging
		 */
		long runtime = System.currentTimeMillis() - start;
		System.out.println("*************************************");
		System.out.println("Run time: " + LogHelper.formatMili(runtime));
		System.out.println("*************************************");
		this.doRoundLogging();
		this.printNodeJournal();
	}

	private void testNodes() {
		/*
		 * Run pings to every active node, extract failures
		 */
		Set<Node> failedNodes = this.pinger.runNodeTest(this.activeConnections);
		Set<Contact> failedContacts = this.getContactsForNodeSet(failedNodes);

		/*
		 * Update data structures
		 */
		synchronized (this.historicalContacts) {
			this.historicalContacts.addAll(failedContacts);
		}
		this.activeConnections.removeAll(failedNodes);
		synchronized (this.activeContacts) {
			this.activeContacts.removeAll(failedContacts);
		}
	}

	public void boostrap(Set<Contact> testContacts) {

		/*
		 * First off, update all contacts in the test set as having been seen by
		 * the bootstrap function
		 */
		synchronized (this.bootstrapConsideredContacts) {
			testContacts.removeAll(this.bootstrapConsideredContacts);
			this.bootstrapConsideredContacts.addAll(testContacts);
		}

		/*
		 * Remove anyone we currently have a connection going with, because
		 * trying to start a second is a waste of time at best, and broken at
		 * worst as we would have two connections to them
		 */
		synchronized (this.activeContacts) {
			testContacts.removeAll(this.activeContacts);
		}

		/*
		 * If we have more than 500 contacts to try, get some help from zmap to
		 * prune down the number of connections we're trying, by only trying to
		 * connect to those that send SYNs on 8333
		 */
		// TODO handle clients that don't use default 8333 (small number)
		if (testContacts.size() > 500) {
			synchronized (this.zmapper) {
				try {
					testContacts = this.zmapper.checkAddresses(testContacts);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Run the contacts through the testing grinder
		 */
		Set<Node> newNodes = null;
		synchronized (this.connTester) {
			this.connTester.pushNodesToTest(testContacts);
			try {
				this.connTester.run();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			newNodes = this.connTester.getReachableNodes();
		}

		/*
		 * Update our node storage, along with our contact bookkeeping; adding
		 * all of the new nodes
		 */
		Set<Contact> newContacts = this.getContactsForNodeSet(newNodes);
		synchronized (this.newConnectionHoldingPen) {
			this.newConnectionHoldingPen.addAll(newNodes);
		}
		synchronized (this.activeContacts) {
			this.activeContacts.addAll(newContacts);
		}
		synchronized (this.historicalContacts) {
			this.historicalContacts.removeAll(newContacts);
		}

		/*
		 * Setup clean data structures for newly connected nodes
		 */
		for (Node tNode : newNodes) {
			this.advancingNodes.put(tNode, new HashSet<Contact>());
			this.lastActivityMap.put(tNode, new HashMap<Contact, Long>());
		}
	}

	private void harvest() {

		/*
		 * Obvious first step, push currently connected nodes to the harvester
		 * machinery
		 */
		this.harvester.pushNodesToTest(this.activeConnections);
		try {
			this.harvester.run(false);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		/*
		 * Process all the new contact info we pull, searching for nodes we've
		 * never heard of, and updating who we think is online/active
		 */
		Set<Contact> firstTimeContacts = new HashSet<Contact>();
		for (Node tNode : this.activeConnections) {
			Set<Contact> harvestedNodes = tNode.getContacts();
			HashMap<Contact, Long> timeMap = this.lastActivityMap.get(tNode);
			Set<Contact> advanceSet = this.advancingNodes.get(tNode);

			for (Contact tContact : harvestedNodes) {
				/*
				 * Add all learned contacts to first time contacts, we'll clear
				 * it out later in the method, done so we only have to acquire
				 * the lock on bootstrap considered contacts once
				 */
				firstTimeContacts.add(tContact);

				/*
				 * If we don't have a past time for this guy from this node, add
				 * it in, if the time for this guy has moved forward, then this
				 * node has seen activity, record it, as that's what we're
				 * looking for after all
				 */
				if (!timeMap.containsKey(tContact)) {
					timeMap.put(tContact, tContact.getLastSeen());
				} else if (tContact.getLastSeen() > timeMap.get(tContact)) {
					this.advancingWindowList.add((double) (tContact.getLastSeen() - timeMap.get(tContact)));

					/*
					 * Clear out any old objects, put in the new one
					 */
					advanceSet.remove(tContact);
					advanceSet.add(tContact);
					timeMap.put(tContact, tContact.getLastSeen());
				}

				/*
				 * Try to record the remote node's view of our last time, helps
				 * us figure out any clock skew
				 */
				// TODO record the last time we send a message that would move
				// last update forward, don't forget the whole 20 min thing...
				if (tContact.equals(this.selfContact)) {
					if (!this.timeSkewFix.containsKey(tNode)) {
						this.timeSkewFix.put(tNode, tContact.getLastSeen());
					}
				}
			}
		}

		this.boostrap(firstTimeContacts);
	}

	private void integrateNewNodes() {
		synchronized (this.newConnectionHoldingPen) {
			this.activeConnections.addAll(this.newConnectionHoldingPen);
			this.newConnectionHoldingPen.clear();
		}
	}

	// TODO investigate if this is the source of our
	// "zero contacts to connection experiment" issues
	public void dnsRefresh() {
		Set<Contact> dnsNodes = ConnectionExperiment.dnsBootStrap();
		this.boostrap(dnsNodes);
	}

	/**
	 * Helper function to build a set of contacts from a set of node objects,
	 * meerly builds a set of their parent objects.
	 * 
	 * @param nodeSet
	 * @return
	 */
	private Set<Contact> getContactsForNodeSet(Set<Node> nodeSet) {
		Set<Contact> retSet = new HashSet<Contact>();
		for (Node tNode : nodeSet) {
			retSet.add(tNode.getContactObject());
		}
		return retSet;
	}

	private void doRoundLogging() {
		try {
			this.logBuffer.write("Network Status at: " + LogHelper.buildTSString() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		synchronized (this.activeContacts) {
			try {
				this.logBuffer.write("Active connectionss: " + this.activeContacts.size() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		synchronized (this.historicalContacts) {
			try {
				this.logBuffer.write("Historical connectionss: " + this.historicalContacts.size() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		synchronized (this.bootstrapConsideredContacts) {
			try {
				this.logBuffer.write("Known contacts: " + this.bootstrapConsideredContacts.size() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		HashSet<Contact> allActiveContacts = new HashSet<Contact>();
		int usefulConnections = 0;
		for (Set<Contact> tSet : this.advancingNodes.values()) {
			allActiveContacts.addAll(tSet);
			if (tSet.size() > 0) {
				usefulConnections++;
			}
		}
		try {
			this.logBuffer.write("Active contacts this refresh: " + allActiveContacts.size() + " seen from "
					+ usefulConnections + "\n");
			this.logBuffer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printNodeJournal() {
		try {
			String tsStr = LogHelper.buildTSString();
			BufferedWriter journalBuffer = new BufferedWriter(
					new FileWriter(Constants.LOG_DIR + "nodeJournal-" + tsStr));

			for (Node reportingNode : this.advancingNodes.keySet()) {
				Set<Contact> advancingSet = this.advancingNodes.get(reportingNode);
				if (advancingSet.size() == 0) {
					continue;
				}

				journalBuffer.write("R: " + reportingNode.getContactObject().toString() + "\n");
				for (Contact advancingContact : advancingSet) {
					journalBuffer.write("A: " + advancingContact.getLoggingString() + "\n");
				}
			}

			journalBuffer.close();

			BufferedWriter connectionBuffer = new BufferedWriter(new FileWriter(Constants.LOG_DIR
					+ "connectionJournal-" + tsStr));

			synchronized (this.activeContacts) {
				for (Contact connectedPeer : this.activeContacts) {
					connectionBuffer.write(connectedPeer.toString() + "\n");
				}
			}

			connectionBuffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {
		/*
		 * Initialize experimental machines
		 */
		IntersectionExperiment self = new IntersectionExperiment();

		/*
		 * Run a number of rounds, attempt to take a sample no more often than
		 * once every sample window, if the last sample took longer, then
		 * samples are done back to back
		 */
		for (int counter = 0; counter < IntersectionExperiment.SAMPLE_COUNT; counter++) {
			long startTime = System.currentTimeMillis();
			self.refresh();
			long delta = System.currentTimeMillis() - startTime;

			if (delta < IntersectionExperiment.INTER_SAMPLE_TIME && counter < IntersectionExperiment.SAMPLE_COUNT - 1) {
				Thread.sleep(IntersectionExperiment.INTER_SAMPLE_TIME - delta);
			}
		}

		// Yeah, poor form reaching inside a data structure like this, sue me...
		self.logBuffer.close();
	}

}
