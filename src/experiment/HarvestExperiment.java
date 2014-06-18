package experiment;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.logging.*;

import zmap.ZmapSelf;
import zmap.ZmapSupplicant;

import logging.LogHelper;

import net.Constants;
import net.Node;
import data.Contact;
import experiment.threading.HarvestThread;

//TODO in general we could make logging more parser friendly
public class HarvestExperiment {

	private Set<Node> nodesToTest;
	private Set<Contact> harvestedContacts;
	private ExperimentContainer holdingContainer;
	private Logger expLogger;

	private static final int THREAD_COUNT = 10;
	public static final int MIN_GAIN = 50;

	public HarvestExperiment() {
		Constants.initConstants();

		/*
		 * Build handlers
		 */
		FileHandler logHandler = null;
		FileHandler summaryHandler = null;
		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
		String tsString = df.format(new Date());
		try {

			logHandler = new FileHandler(Constants.LOG_DIR + "harvest-" + tsString + ".out");
			summaryHandler = new FileHandler(Constants.LOG_DIR + "summary-" + tsString + ".out");
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logHandler.setLevel(Level.CONFIG);
		logHandler.setFormatter(new SimpleFormatter());
		summaryHandler.setLevel(Level.INFO);
		summaryHandler.setFormatter(new SimpleFormatter());
		ConsoleHandler conHandler = new ConsoleHandler();
		conHandler.setLevel(Level.WARNING);

		/*
		 * Get logger, add handlers
		 */
		this.expLogger = Logger.getLogger(Constants.HARVEST_LOG);
		this.expLogger.setUseParentHandlers(false);
		this.expLogger.setLevel(Level.FINE);
		this.expLogger.addHandler(logHandler);
		this.expLogger.addHandler(conHandler);
		this.expLogger.addHandler(summaryHandler);

		this.nodesToTest = new HashSet<Node>();
		this.harvestedContacts = new HashSet<Contact>();

		/*
		 * Build experiment container and threads, start them up
		 */
		this.holdingContainer = new ExperimentContainer();
		for (int counter = 0; counter < HarvestExperiment.THREAD_COUNT; counter++) {
			Thread tThread = new Thread(new HarvestThread(this.holdingContainer));
			tThread.setDaemon(true);
			tThread.start();
		}
	}

	public void pushNodesToTest(Set<Node> targets) {
		this.nodesToTest.clear();
		this.harvestedContacts.clear();
		this.nodesToTest.addAll(targets);
	}

	public Set<Contact> getHarvestedContacts() {
		Set<Contact> cloneSet = new HashSet<Contact>();
		cloneSet.addAll(this.harvestedContacts);
		return cloneSet;
	}

	public void run() throws InterruptedException {

		long startTime = System.currentTimeMillis();

		for (Node tNode : this.nodesToTest) {
			this.holdingContainer.nodeReadyToWork(tNode);
		}

		for (int counter = 0; counter < this.nodesToTest.size(); counter++) {
			Node finishedNode = this.holdingContainer.fetchCompleted();
			this.harvestedContacts.addAll(finishedNode.getContacts());
		}

		long time = (System.currentTimeMillis() - startTime);

		this.expLogger.warning("Harvest took " + LogHelper.formatMili(time));
		this.expLogger.info("Found nodes: " + this.harvestedContacts.size());
	}

	public void manageMultiRoundCollection(ConnectionExperiment connTest) throws InterruptedException, IOException {
		Set<Contact> allKnownNodes = new HashSet<Contact>();
		Set<Node> reachableNodes = new HashSet<Node>();
		Set<Node> toHarvest = new HashSet<Node>();
		int newNodesFound = 0;
		long startTime = System.currentTimeMillis();

		//ZmapSelf zmapper = new ZmapSelf();
		ZmapSupplicant zmapper = new ZmapSupplicant();

		// TODO this should be thresholded
		for (int counter = 0; counter < 3; counter++) {
			this.expLogger.warning("starting round " + counter);
			if (counter == 0) {
				Set<Contact> toProbe = ConnectionExperiment.dnsBootStrap();
				newNodesFound = toProbe.size();
				allKnownNodes.addAll(toProbe);
				connTest.pushNodesToTest(toProbe);
				connTest.run();

				toHarvest.addAll(connTest.getReachableNodes());
				reachableNodes.addAll(toHarvest);
			} else {

				this.pushNodesToTest(toHarvest);
				this.run();

				/*
				 * Let's not leave all of those threads running if we don't need
				 * them
				 */
				for (Node tNode : toHarvest) {
					tNode.shutdownNode();
				}

				Set<Contact> harvestNodes = this.getHarvestedContacts();
				harvestNodes.removeAll(allKnownNodes);
				allKnownNodes.addAll(harvestNodes);
				newNodesFound = harvestNodes.size();

				long start = System.currentTimeMillis();
				this.expLogger.warning("Starting zmap on: " + harvestNodes.size() + " nodes");
				Set<Contact> zmapPassedNodes = zmapper.checkAddresses(harvestNodes);
				this.expLogger.warning("zmap found " + zmapPassedNodes.size() + " in "
						+ LogHelper.formatMili(System.currentTimeMillis() - start));

				/*
				 * Sleep for a bit to let the network bits "recover" from the zmap
				 */
				Thread.sleep(60000);
				
				connTest.pushNodesToTest(zmapPassedNodes);
				connTest.run();

				toHarvest.clear();
				toHarvest.addAll(connTest.getReachableNodes());
				reachableNodes.addAll(toHarvest);
			}

			/*
			 * Do logging
			 */
			this.expLogger.info("total nodes known " + allKnownNodes.size() + " round " + counter);
			this.expLogger.info("New nodes " + newNodesFound + " round " + counter);
			this.expLogger.info("Reachable nodes known " + reachableNodes.size() + " round " + counter);
			this.expLogger.info("New reachable nodes " + toHarvest.size() + " rount " + counter);
		}

		long time = System.currentTimeMillis() - startTime;
		this.expLogger.warning("Total experiment time: " + LogHelper.formatMili(time));
	}

	public static void main(String args[]) throws IOException, InterruptedException {

		HarvestExperiment self = new HarvestExperiment();
		ConnectionExperiment connTest = new ConnectionExperiment(false);
		self.manageMultiRoundCollection(connTest);
	}
}
