package passiveMonitor;

import data.NodeBooleanPair;
import net.Node;

public class NodeTesterChild implements Runnable {

	private NodeTester myParent;

	public NodeTesterChild(NodeTester parent) {
		this.myParent = parent;
	}

	@Override
	public void run() {

		try {
			while (true) {
				Node toTest = this.myParent.getWork();
				boolean pingResult = toTest.testConnectionLiveness();
				this.myParent.reportResult(new NodeBooleanPair(toTest, pingResult));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
