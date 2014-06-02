package experiment.threading;

import net.Node;
import experiment.ExperimentContainer;

public class ConnectorThread implements Runnable {

	private ExperimentContainer parent;

	public ConnectorThread(ExperimentContainer myParent) {
		this.parent = myParent;
	}

	public void run() {
		try {
			while (true) {
				Node nodeToConnect = this.parent.fetchWork();
				nodeToConnect.connect();
				this.parent.returnCompletedNode(nodeToConnect);
			}
		} catch (InterruptedException e) {
			//TODO some logging?
		}
	}

}
