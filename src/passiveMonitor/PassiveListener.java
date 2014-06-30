package passiveMonitor;

import java.util.*;
import java.io.*;

import logging.LogHelper;

import data.Contact;
import experiment.ConnectionExperiment;
import net.*;
import net.Node.NodeErrorCode;

public class PassiveListener implements Runnable {

	private ConnectionListener listener;
	private Set<Node> bootStrapNode;
	private Set<Node> connectedNodes;
	private Set<Node> historicalNodes;
	private int failedConnectionCounter;

	private static final int REPORT_INTERVAL = 60000;

	public PassiveListener(Set<Contact> bootStrapNodes) throws IOException {
		this.bootStrapNode = new HashSet<Node>();
		this.connectedNodes = new HashSet<Node>();
		this.historicalNodes = new HashSet<Node>();
		this.failedConnectionCounter = 0;
		this.listener = new ConnectionListener(this);

		this.addNewBootstrapNodes(bootStrapNodes);

		// TODO set this up so we can exit more cleanly
		Thread listenerThread = new Thread(this.listener);
		listenerThread.setDaemon(true);
		listenerThread.start();
	}

	public void addNewBootstrapNodes(Set<Contact> bootStrapNodes) {
		synchronized (this.connectedNodes) {
			for (Contact tContact : bootStrapNodes) {
				Node tNode = new Node(tContact);
				if (this.bootStrapNode.contains(tNode) || this.connectedNodes.contains(tNode)
						|| this.historicalNodes.contains(tNode)) {
					continue;
				}

				if (tNode.connect()) {
					this.bootStrapNode.add(tNode);
				}
			}
		}
	}

	public void reportNewNode(Node incNode) {
		if (incNode.connect()) {
			synchronized (this.connectedNodes) {
				this.connectedNodes.add(incNode);
			}
		} else {
			this.failedConnectionCounter++;
			incNode.shutdownNode(null);
		}
	}

	public void testNodeStatus() {
		Set<Node> newlyDeadNodes = new HashSet<Node>();

		/*
		 * Clear out any dead nodes that called us and then hung up
		 */
		synchronized (this.connectedNodes) {
			for (Node tNode : this.connectedNodes) {
				if (!tNode.thinksConnected()) {
					newlyDeadNodes.add(tNode);
				}
			}

			this.connectedNodes.removeAll(newlyDeadNodes);
			this.historicalNodes.addAll(newlyDeadNodes);
		}

		/*
		 * Clear out nodes we've introduced ourself to
		 */
		newlyDeadNodes.clear();
		for (Node tNode : this.bootStrapNode) {
			if (!tNode.thinksConnected()) {
				newlyDeadNodes.add(tNode);
				continue;
			}

			if (!tNode.testConnectionLiveness()) {
				newlyDeadNodes.add(tNode);
			}
		}
		this.bootStrapNode.removeAll(newlyDeadNodes);
		this.historicalNodes.addAll(newlyDeadNodes);

	}

	public void run() {
		try {
			while (true) {
				this.testNodeStatus();
				System.out.println("Outgoing connections: " + this.bootStrapNode.size());
				System.out.println("Incoming connections: " + this.connectedNodes.size());
				System.out.println("Failed connection attempts: " + this.failedConnectionCounter);
				this.failureReasonReport();
				Thread.sleep(PassiveListener.REPORT_INTERVAL);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void failureReasonReport() {
		HashMap<String, Double> errorValues = new HashMap<String, Double>();

		for (Node tNode : this.historicalNodes) {
			String errString = Node.getErrorNoMessage(tNode.getErronNo());
			if (!errorValues.containsKey(errString)) {
				errorValues.put(errString, 0.0);
			}
			errorValues.put(errString, errorValues.get(errString) + 1.0);
		}
		
		List<String> orderList = LogHelper.buildDecendingList(errorValues);
		System.out.println("Error summary");
		for(String tError: orderList){
			System.out.println(tError + " " + errorValues.get(tError));
		}
	}

	public static void main(String args[]) throws IOException {
		Constants.initConstants();
		Set<Contact> dnsNodes = ConnectionExperiment.dnsBootStrap();
		System.out.println("starting with " + dnsNodes.size());
		PassiveListener self = new PassiveListener(dnsNodes);
		Thread selfThread = new Thread(self);
		selfThread.setDaemon(false);
		selfThread.start();
	}
}
