package experiment.threading;

import java.util.*;
import java.util.logging.*;

import net.Constants;
import net.Node;
import data.Contact;
import experiment.ExperimentContainer;
import experiment.HarvestExperiment;

public class HarvestThread implements Runnable {

	private ExperimentContainer parent;
	private Logger expLog;

	public HarvestThread(ExperimentContainer holdingContainer) {
		this.parent = holdingContainer;
		this.expLog = Logger.getLogger(Constants.HARVEST_LOG);
	}

	@Override
	public void run() {
		try {
			while (true) {
				Node nodeToHarvest = this.parent.fetchWork();
				nodeToHarvest.clearContacts();

				int gain = HarvestExperiment.MIN_GAIN;
				int counter = 0;
				int lastSeenNodeSize = 0;
				while (gain >= HarvestExperiment.MIN_GAIN) {
					counter++;
					int oldSize = nodeToHarvest.getContactCount();
					nodeToHarvest.querryForNodes();
					lastSeenNodeSize = nodeToHarvest.getContactCount();
					gain = lastSeenNodeSize - oldSize;
				}

				this.expLog.config("Took " + counter + " iterations.");
				this.expLog.config("final size " + lastSeenNodeSize);
				this.parent.returnCompletedNode(nodeToHarvest);
			}
		} catch (InterruptedException e) {
			/*
			 * Just telling the thread to shut down, nothing fancy
			 */
		}

	}

}
