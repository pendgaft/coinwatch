package experiment.threading;

import net.Node;
import experiment.ExperimentContainer;

public class HarvestThread implements Runnable {
	
	private ExperimentContainer parent;
	
	private static final int MIN_GAIN = 50;
	
	public HarvestThread(ExperimentContainer holdingContainer){
		this.parent = holdingContainer;
	}
	
	@Override
	public void run() {
		try{
			while(true){
				Node nodeToHarvest = this.parent.fetchWork();
				nodeToHarvest.clearContacts();
				
				int gain = HarvestThread.MIN_GAIN;
				int counter = 0;
				while(gain >= HarvestThread.MIN_GAIN){
					counter++;
					int oldSize = nodeToHarvest.getContacts().size();
					nodeToHarvest.querryForNodes();
					gain = nodeToHarvest.getContacts().size() - oldSize;
				}
				
				System.out.println("Took " + counter + " iterations.");
				this.parent.returnCompletedNode(nodeToHarvest);
			}
		}catch(InterruptedException e){
			//TODO log closing?
		}

	}

}
