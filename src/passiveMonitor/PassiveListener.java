package passiveMonitor;

import java.util.*;
import java.io.*;

import data.Contact;
import experiment.ConnectionExperiment;
import net.*;

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
			if (!tNode.testConnectionLiveness()) {
				newlyDeadNodes.add(tNode);
			}
		}
		this.bootStrapNode.removeAll(newlyDeadNodes);

	}

	public void run() {
		try {
			while (true) {
				this.testNodeStatus();
				System.out.println("Introduced ourself to: " + this.bootStrapNode.size());
				System.out.println("Connected nodes: " + this.connectedNodes.size());
				System.out.println("Failed connection attempts: " + this.failedConnectionCounter);
				Thread.sleep(PassiveListener.REPORT_INTERVAL);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) throws IOException{
		Set<Contact> dnsNodes = ConnectionExperiment.dnsBootStrap();
		PassiveListener self = new PassiveListener(dnsNodes);
		Thread selfThread = new Thread(self);
		selfThread.setDaemon(false);
		selfThread.start();
	}
}
