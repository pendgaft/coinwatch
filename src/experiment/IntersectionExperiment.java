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

		this.boostrap();
	}

	public void boostrap() throws InterruptedException {
		Set<Contact> dnsNodes = ConnectionExperiment.dnsBootStrap();
		this.connTester.pushNodesToTest(dnsNodes);
		this.connTester.run();
		this.activeConnections.addAll(this.connTester.getReachableNodes());
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
		
		//TODO try connecting to new nodes
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
				logOut.write("Contact" + tContact.getLoggingString() + "\n");
				Set<Contact> nodesWhoKnew = this.contactToConnected.get(tContact);
				for(Contact tKnowing: nodesWhoKnew){
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
		self.printDump();
	}

}
