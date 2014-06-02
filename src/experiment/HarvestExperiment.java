package experiment;

import java.util.*;
import java.io.*;

import net.Node;
import data.Contact;
import experiment.threading.HarvestThread;

public class HarvestExperiment {

	private Set<Node> nodesToTest;
	private ExperimentContainer holdingContainer;
	
	private static final int THREAD_COUNT = 10;
	public static final int MIN_GAIN = 50;
	
	public HarvestExperiment(){
		
		this.nodesToTest = new HashSet<Node>();
		
		/*
		 * Build experiment container and threads, start them up
		 */
		this.holdingContainer = new ExperimentContainer();
		for(int counter = 0; counter < HarvestExperiment.THREAD_COUNT; counter++){
			Thread tThread = new Thread(new HarvestThread(this.holdingContainer));
			tThread.setDaemon(true);
			tThread.start();
		}
	}
	
	public void pushNodesToTest(Set<Node> targets){
		this.nodesToTest.clear();
		this.nodesToTest.addAll(targets);
	}
	
	public void run() throws InterruptedException{
	
		long startTime = System.currentTimeMillis();
		
		for(Node tNode: this.nodesToTest){
			this.holdingContainer.nodeReadyToWork(tNode);
		}
		
		Set<Contact> harvestedContacts = new HashSet<Contact>();
		for(int counter = 0; counter < this.nodesToTest.size(); counter++){
			Node finishedNode = this.holdingContainer.fetchCompleted();
			harvestedContacts.addAll(finishedNode.getContacts());
		}
		
		long time = (System.currentTimeMillis() - startTime) / 1000;
		
		System.out.println("*******");
		System.out.println("Took " + time + " seconds.");
		System.out.println("Found nodes: " + harvestedContacts.size());
	}
	
	public static void main(String args[]) throws IOException, InterruptedException{
		BufferedWriter connLog = new BufferedWriter(new FileWriter("logs/conTest.log"));
		
		ConnectionExperiment connTest = new ConnectionExperiment(connLog);
		HarvestExperiment self = new HarvestExperiment();
		
		Set<Contact> targets = ConnectionExperiment.dnsBootStrap();
		connTest.pushNodesToTest(targets);
		connTest.run();
		Set<Node> connectedNodes = connTest.getReachableNodes();
		connTest.shutdown();
		
		self.pushNodesToTest(connectedNodes);
		self.run();
	}
}
