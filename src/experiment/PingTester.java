package experiment;

import java.util.*;

import experiment.ExperimentContainer;
import experiment.threading.PingThread;
import data.NodeBooleanPair;
import net.Node;

public class PingTester {

	private static final int TESTER_THREADS = 10;

	private ExperimentContainer<Node, NodeBooleanPair> testContainer;

	public PingTester() {
		this.testContainer = new ExperimentContainer<Node, NodeBooleanPair>();

		for (int counter = 0; counter < PingTester.TESTER_THREADS; counter++) {
			PingThread tChild = new PingThread(this.testContainer);
			Thread tThread = new Thread(tChild);
			tThread.setDaemon(true);
			tThread.start();
		}
	}

	/**
	 * Runs a multithreaded test attempting a bitcoin ping to all the nodes,
	 * returns the nodes that are NOT responsive.
	 * 
	 * @param nodesToTest
	 *            the set of nodes to ping
	 * @return a set containing all nodes that FAILED to correctly respond
	 */
	public Set<Node> runNodeTest(Set<Node> nodesToTest) {
		Set<Node> workingNodes = new HashSet<Node>();

		this.testContainer.nodesReadyToWork(nodesToTest);
		for (int counter = 0; counter < nodesToTest.size(); counter++) {
			try {
				NodeBooleanPair result = this.testContainer.fetchCompleted();
				if (!result.getBoolean()) {
					workingNodes.add(result.getNode());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return workingNodes;
	}
}
