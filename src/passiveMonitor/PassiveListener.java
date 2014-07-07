package passiveMonitor;

import java.util.*;
import java.io.*;

import logging.LogHelper;

import data.Contact;
import experiment.ConnectionExperiment;
import net.*;

public class PassiveListener implements Runnable {

	private ConnectionListener listener;
	private Set<Node> bootStrapNode;
	private Set<Node> connectedNodes;
	private Set<Node> historicalIncomingNodes;
	private Set<Node> abortiveNodes;
	private Set<Node> historicalNodes;
	private int failedConnectionCounter;

	private BufferedWriter outBuff;

	private NodeTester pingTestMaster;

	private static final int REPORT_INTERVAL = 60000;

	public PassiveListener(Set<Contact> bootStrapNodes) throws IOException {
		this.outBuff = new BufferedWriter(new FileWriter("passiveLog.txt"));

		this.bootStrapNode = new HashSet<Node>();
		this.connectedNodes = new HashSet<Node>();
		this.historicalIncomingNodes = new HashSet<Node>();
		this.abortiveNodes = new HashSet<Node>();
		this.historicalNodes = new HashSet<Node>();
		this.failedConnectionCounter = 0;
		this.listener = new ConnectionListener(this);

		this.addNewBootstrapNodes(bootStrapNodes);

		// TODO set this up so we can exit more cleanly
		Thread listenerThread = new Thread(this.listener);
		listenerThread.setDaemon(true);
		listenerThread.start();

		this.pingTestMaster = new NodeTester();
	}

	public void addNewBootstrapNodes(Set<Contact> incomingNodes) {
		synchronized (this.connectedNodes) {
			for (Contact tContact : incomingNodes) {
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

		/*
		 * Update the last block constant so we look more normal
		 */
		long lastBlock = 0;
		for (Node tNode : this.bootStrapNode) {
			lastBlock = Math.max(lastBlock, tNode.getLastBlockSeen());
		}
		Constants.LAST_BLOCK = lastBlock;
		System.out.println("Last block set to: " + Constants.LAST_BLOCK);
	}

	public void reportNewNode(Node incNode) {
		if (incNode.connect()) {
			synchronized (this.connectedNodes) {
				this.connectedNodes.add(incNode);
			}
		} else {
			this.abortiveNodes.add(incNode);
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
			newlyDeadNodes = this.pingTestMaster.runNodeTest(this.connectedNodes);
			this.connectedNodes.removeAll(newlyDeadNodes);
			this.historicalIncomingNodes.addAll(newlyDeadNodes);
		}

		/*
		 * Clear out nodes we've introduced ourself to
		 */
		newlyDeadNodes.clear();
		newlyDeadNodes = this.pingTestMaster.runNodeTest(this.bootStrapNode);
		this.bootStrapNode.removeAll(newlyDeadNodes);
		this.historicalNodes.addAll(newlyDeadNodes);

	}

	public void run() {
		long startTime = System.currentTimeMillis();
		try {
			this.outBuff.write("time,connected,dead,failed\n");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			while (true) {
				this.testNodeStatus();
				System.out.println("Outgoing connections: " + this.bootStrapNode.size() + " active, "
						+ this.historicalNodes + " dead");
				System.out.println("Incoming connections: " + this.connectedNodes.size() + " active, "
						+ this.historicalIncomingNodes + " dead");
				System.out.println("Failed connection attempts: " + this.failedConnectionCounter);
				System.out.println("Abort reasons:");
				this.failureReasonReport(this.abortiveNodes);
				System.out.println("D/C reasons:");
				this.failureReasonReport(this.historicalNodes);
				System.out.println("Incoming D/C reaseons: ");
				this.failureReasonReport(this.historicalIncomingNodes);

				long currTime = (System.currentTimeMillis() - startTime) / 1000;
				this.outBuff.write("" + currTime + "," + this.connectedNodes.size() + ","
						+ this.historicalIncomingNodes.size() + "," + this.failedConnectionCounter + "\n");
				this.outBuff.flush();

				Thread.sleep(PassiveListener.REPORT_INTERVAL);
			}
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

	private void failureReasonReport(Set<Node> nodeSet) {
		HashMap<String, Double> errorValues = new HashMap<String, Double>();

		for (Node tNode : nodeSet) {
			String errString = tNode.getErrorMsg(false);
			if (!errorValues.containsKey(errString)) {
				errorValues.put(errString, 0.0);
			}
			errorValues.put(errString, errorValues.get(errString) + 1.0);
		}

		List<String> orderList = LogHelper.buildDecendingList(errorValues);
		System.out.println("Error summary");
		for (String tError : orderList) {
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
