package experiment.threading;

import experiment.ExperimentContainer;
import data.NodeBooleanPair;
import net.Node;

public class PingThread implements Runnable {

	private ExperimentContainer<Node, NodeBooleanPair> myParent;

	public PingThread(ExperimentContainer<Node, NodeBooleanPair> parent) {
		this.myParent = parent;
	}

	@Override
	public void run() {

		try {
			while (true) {
				Node toTest = this.myParent.fetchWork();
				boolean pingResult = toTest.testConnectionLiveness();
				this.myParent.returnCompletedNode(new NodeBooleanPair(toTest, pingResult));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
