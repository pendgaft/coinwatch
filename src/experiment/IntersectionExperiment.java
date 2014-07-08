package experiment;

import java.io.*;
import java.util.*;

import logging.LogHelper;

import zmap.ZmapSupplicant;
import net.Constants;
import net.Node;
import data.Contact;

public class IntersectionExperiment {

	private HashMap<Contact, Set<Contact>> contactToConnected;
	private HashSet<Node> activeConnections;
	private HashSet<Node> historicalNodes;

	private ConnectionExperiment connTester;
	private HarvestExperiment harvester;
	private ZmapSupplicant zmapper;

	public IntersectionExperiment() throws InterruptedException {
		Constants.initConstants();
		LogHelper.initLogger();

		this.contactToConnected = new HashMap<Contact, Set<Contact>>();
		this.activeConnections = new HashSet<Node>();
		this.historicalNodes = new HashSet<Node>();

		this.connTester = new ConnectionExperiment(false);
		this.harvester = new HarvestExperiment(false);
		this.zmapper = new ZmapSupplicant();

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

		/*
		 * XXX This code works our really gross because there are multiple contact
		 * objects floating around for a single contact, work to fix that in the
		 * future?
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
			Set<Contact> knownNodes = tNode.getContacts(false);

			for (Contact tContact : knownNodes) {
				if (!this.contactToConnected.containsKey(tContact)) {
					this.contactToConnected.put(tContact, new HashSet<Contact>());
					newNodes.add(tContact);
				}
				this.contactToConnected.get(tContact).add(tNode.getContactObject());
			}
		}

		if (newNodes.size() > 1000) {
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
	}

	private void printDump() {
		try {
			BufferedWriter logOut = new BufferedWriter(new FileWriter(Constants.LOG_DIR + "intOut.txt"));

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

		}
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		IntersectionExperiment self = new IntersectionExperiment();
		self.refresh();
		self.refresh();
		self.printDump();
	}

}
