package experiment;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import scijava.stats.CDF;

import logging.LogHelper;

import zmap.ZmapSupplicant;
import net.Constants;
import net.Node;
import data.Contact;

public class IntersectionExperiment {

	/**
	 * Map that stores all the nodes we learn about. The map is keyed by the
	 * node in question, and maps to the set of all nodes that know about it.
	 */
	private HashMap<Contact, Set<Contact>> contactToConnected;

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
	private HashSet<Node> historicalNodes;

	private ConnectionExperiment connTester;
	private HarvestExperiment harvester;
	private ZmapSupplicant zmapper;
	private Contact selfContact;

	private static final long SAMPLE_INTERVAL = 300000;

	public IntersectionExperiment() throws InterruptedException, UnknownHostException {
		Constants.initConstants();
		LogHelper.initLogger();

		this.contactToConnected = new HashMap<Contact, Set<Contact>>();
		this.lastActivityMap = new HashMap<Node, HashMap<Contact, Long>>();
		this.advancingNodes = new HashMap<Node, Set<Contact>>();
		this.advancingWindowList = new LinkedList<Double>();
		this.timeSkewFix = new HashMap<Node, Long>();

		this.activeConnections = new HashSet<Node>();
		this.historicalNodes = new HashSet<Node>();

		this.connTester = new ConnectionExperiment(false);
		this.harvester = new HarvestExperiment(false);
		this.zmapper = new ZmapSupplicant();
		this.selfContact = new Contact(InetAddress.getLocalHost(), Constants.DEFAULT_PORT);

		Set<Contact> dnsNodes = ConnectionExperiment.dnsBootStrap();
		this.boostrap(dnsNodes);
	}

	public void boostrap(Set<Contact> testNodes) {
		this.connTester.pushNodesToTest(testNodes);
		try {
			this.connTester.run();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		this.activeConnections.addAll(this.connTester.getReachableNodes());

		for (Node tNode : this.activeConnections) {
			if (!this.advancingNodes.containsKey(tNode)) {
				this.advancingNodes.put(tNode, new HashSet<Contact>());
				this.lastActivityMap.put(tNode, new HashMap<Contact, Long>());
			}
		}

		/*
		 * XXX This code works our really gross because there are multiple
		 * contact objects floating around for a single contact, work to fix
		 * that in the future?
		 */
		for (Contact tCon : this.contactToConnected.keySet()) {
			for (Node tNode : this.activeConnections) {
				if (tNode.getContactObject().equals(tCon)) {
					tCon.setLastSeenDirect(true);
				}
			}
		}
	}

	private void refresh() {
		/*
		 * Make sure connected nodes are alive, ask them for nodes they know
		 * about
		 */
		this.testNodes();
		this.harvest();

		Set<Contact> newNodes = new HashSet<Contact>();
		for (Node tNode : this.activeConnections) {
			Set<Contact> knownNodes = tNode.getContacts();

			for (Contact tContact : knownNodes) {
				if (!this.contactToConnected.containsKey(tContact)) {
					this.contactToConnected.put(tContact, new HashSet<Contact>());
					newNodes.add(tContact);
				}
				this.contactToConnected.get(tContact).add(tNode.getContactObject());
			}
		}

		if (newNodes.size() > 500) {
			try {
				newNodes = this.zmapper.checkAddresses(newNodes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.boostrap(newNodes);
	}

	private void testNodes() {
		// TODO ping nodes, move dead nodes out
	}

	private void harvest() {
		this.harvester.pushNodesToTest(this.activeConnections);
		try {
			this.harvester.run(false);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (Node tNode : this.activeConnections) {
			Set<Contact> harvestedNodes = tNode.getContacts();
			HashMap<Contact, Long> timeMap = this.lastActivityMap.get(tNode);
			Set<Contact> advanceSet = this.advancingNodes.get(tNode);
			for (Contact tContact : harvestedNodes) {
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
				
				if(tContact.equals(this.selfContact)){
					if(!this.timeSkewFix.containsKey(tNode)){
						this.timeSkewFix.put(tNode, tContact.getLastSeen());
					}
				}
			}
		}
	}

	private void printAllLearnedNodes() {
		try {
			BufferedWriter logOut = new BufferedWriter(new FileWriter(Constants.LOG_DIR + "learnedOut.txt"));

			for (Contact tContact : this.contactToConnected.keySet()) {

				Set<Contact> nodesWhoKnew = this.contactToConnected.get(tContact);
				logOut.write("Contact " + tContact.getLoggingString() + "," + nodesWhoKnew.size() + "\n");
				for (Contact tKnowing : nodesWhoKnew) {
					logOut.write(tKnowing.toString() + "\n");
				}
				logOut.write("\n");
			}

			logOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printAllActiveNodes() {
		try {
			BufferedWriter logOut = new BufferedWriter(new FileWriter(Constants.LOG_DIR + "activeOut.txt"));

			for (Node tNode : this.advancingNodes.keySet()) {
				logOut.write("Contact " + tNode.getContactObject().getLoggingString() + "\n");
				Set<Contact> advNodes = this.advancingNodes.get(tNode);
				for (Contact tContact : advNodes) {
					logOut.write(tContact.getLoggingString() + "\n");
				}
				logOut.write("\n");
			}

			logOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printOtherStats(){
		try{
			CDF.printCDF(this.advancingWindowList, Constants.LOG_DIR + "advWindowCDF.csv");
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void printTimeSkewTest(){
		try{
			BufferedWriter outBuff = new BufferedWriter(new FileWriter(Constants.LOG_DIR + "timeSkewTest.csv"));
			
			for(Node tNode: this.activeConnections){
				outBuff.write(tNode.getContactObject().getLoggingString() + "," + this.timeSkewFix.get(tNode) + "\n");
			}
			
			outBuff.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException, UnknownHostException {
		IntersectionExperiment self = new IntersectionExperiment();
		self.refresh();
		self.refresh();
//		for (int counter = 0; counter < 6; counter++) {
//			Thread.sleep(IntersectionExperiment.SAMPLE_INTERVAL);
//			self.refresh();
//		}
		self.printAllLearnedNodes();
		self.printAllActiveNodes();
		self.printOtherStats();
		self.printTimeSkewTest();
	}

}
