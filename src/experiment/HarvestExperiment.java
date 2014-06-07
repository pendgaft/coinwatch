package experiment;

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

		/*
		 * Build handlers
		 */
		FileHandler logHandler = null;
		try {
			logHandler = new FileHandler("logs/harvest.out");
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

		System.out.println("Took " + time + " seconds.");
		this.expLogger.info("Found nodes: " + this.harvestedContacts.size());
	}

	public static void main(String args[]) throws IOException, InterruptedException {
		BufferedWriter connLog = new BufferedWriter(new FileWriter("logs/conTest.log"));

		ConnectionExperiment connTest = new ConnectionExperiment(connLog);
		HarvestExperiment self = new HarvestExperiment();

		Set<Contact> targets = ConnectionExperiment.dnsBootStrap();
		connTest.pushNodesToTest(targets);
		connTest.run();
		Set<Node> connectedNodes = connTest.getReachableNodes();

		self.pushNodesToTest(connectedNodes);
		self.run();

		connTest.pushNodesToTest(self.getHarvestedContacts());
		connTest.run();
		connTest.shutdown();
	}
}
