package experiment.threading;

import net.Node;
import experiment.ExperimentContainer;

public class ConnectorThread implements Runnable {

	//FIXME should prob not reach directly into the guts of another data structure..
	private ExperimentContainer<Node, Node> parent;

	public ConnectorThread(ExperimentContainer<Node, Node> myParent) {
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
