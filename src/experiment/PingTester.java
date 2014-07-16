package experiment;

import java.util.*;

import experiment.ExperimentContainer;
import experiment.threading.PingThread;
import data.NodeBooleanPair;
import net.Constants;
import net.Node;

public class PingTester {

	public PingTester() {

	}

	public Set<Node> runNodeTest(Set<Node> nodesToTest) {
		Set<Node> workingNodes = new HashSet<Node>();
		List<Node> testList = new ArrayList<Node>(nodesToTest.size());

		/*
		 * Send a ping to each node
		 */
		for (Node tNode : testList) {
			tNode.initiatePing();
		}

		/*
		 * Wait a "reasonable" amount of time
		 */
		try {
			Thread.sleep(Constants.TRANSACTION_TIMEOUT);
		} catch (InterruptedException e) {
			System.err.println("Sleep interrupted in Ping Tester.");
		}

		/*
		 * Check for ping replies, this will cause any nodes that fail to shut
		 * down.
		 */
		for (Node tNode : testList) {
			if (tNode.testPing()) {
				workingNodes.add(tNode);
			}
		}

		return workingNodes;
	}
}
