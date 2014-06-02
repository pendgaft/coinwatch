package experiment.threading;

import net.Node;
import experiment.ExperimentContainer;
import experiment.HarvestExperiment;

public class HarvestThread implements Runnable {
	
	private ExperimentContainer parent;
	
	public HarvestThread(ExperimentContainer holdingContainer){
		this.parent = holdingContainer;
	}
	
	@Override
	public void run() {
		try{
			while(true){
				Node nodeToHarvest = this.parent.fetchWork();
				nodeToHarvest.clearContacts();
				
				int gain = HarvestExperiment.MIN_GAIN;
				int counter = 0;
				while(gain >= HarvestExperiment.MIN_GAIN){
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
