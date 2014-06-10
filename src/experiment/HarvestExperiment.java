package experiment;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.logging.*;

import net.Constants;
import net.Node;
import data.Contact;
import experiment.threading.HarvestThread;

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
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss");
			logHandler = new FileHandler("logs/harvest-" + df.format(new Date()) + ".out");
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logHandler.setLevel(Level.CONFIG);
		logHandler.setFormatter(new SimpleFormatter());
		ConsoleHandler conHandler = new ConsoleHandler();
		conHandler.setLevel(Level.INFO);

		/*
		 * Get logger, add handlers
		 */
		this.expLogger = Logger.getLogger(Constants.HARVEST_LOG);
		this.expLogger.setUseParentHandlers(false);
		this.expLogger.setLevel(Level.FINE);
		this.expLogger.addHandler(logHandler);
		this.expLogger.addHandler(conHandler);

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
		return this.harvestedContacts;
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

		long time = (System.currentTimeMillis() - startTime) / 1000;

		this.expLogger.info("Harvest took " + time + " seconds.");
		this.expLogger.info("Found nodes: " + this.harvestedContacts.size());
	}
	
	public void manageMultiRoundCollection(ConnectionExperiment connTest) throws InterruptedException{
		Set<Contact> allKnownNodes = new HashSet<Contact>();
		Set<Node> reachableNodes = new HashSet<Node>();
		Set<Contact> toProbe = new HashSet<Contact>();

		toProbe = ConnectionExperiment.dnsBootStrap();
		allKnownNodes.addAll(toProbe);
		this.expLogger.info("know " + allKnownNodes.size() + " round 0");
		
		for (int counter = 0; counter < 3; counter++) {
			connTest.pushNodesToTest(toProbe);
			connTest.run();
			
			Set<Node> connectedNodes = connTest.getReachableNodes();
			reachableNodes.addAll(connectedNodes);
			
			this.pushNodesToTest(connectedNodes);
			this.run();

			/*
			 * Prune the set to all nodes we just learned about
			 */
			Set<Contact> harvestNodes = this.getHarvestedContacts();
			harvestNodes.removeAll(allKnownNodes);
			toProbe.clear();
			toProbe.addAll(this.getHarvestedContacts());
			allKnownNodes.addAll(toProbe);
			
			/*
			 * Do logging
			 */
			this.expLogger.info("total nodes known " + allKnownNodes.size() + " round " + (counter + 1));
			this.expLogger.info("Reachable nodes known " + allKnownNodes.size() + " round " + (counter));
		}
		
	}

	public static void main(String args[]) throws IOException, InterruptedException {

		HarvestExperiment self = new HarvestExperiment();
		ConnectionExperiment connTest = new ConnectionExperiment(false);
		self.manageMultiRoundCollection(connTest);
	}
}
