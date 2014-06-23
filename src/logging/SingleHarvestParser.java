package logging;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import scijava.stats.*;

public class SingleHarvestParser {

	private String baseLogFile;
	private boolean parseComplete;

	private List<Collection<Double>> numberOfNodeQuerries;
	private List<Collection<Double>> returnedNodeCountChild;
	private List<Collection<Double>> returnedNodeCountParent;
	private int numberOfRounds;
	private int totalSeenNodes;
	private int totalReachableNodes;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("usage: java logging.SingleHarvestParser <log files...>");
		}
		for (String tFileName : args) {
			SingleHarvestParser self = new SingleHarvestParser(tFileName);
			self.run();
		}
	}

	public SingleHarvestParser(String fileName) {
		this.baseLogFile = fileName;
		this.parseComplete = false;

		this.numberOfNodeQuerries = new ArrayList<Collection<Double>>();
		this.returnedNodeCountChild = new ArrayList<Collection<Double>>();
		this.returnedNodeCountParent = new ArrayList<Collection<Double>>();

		this.numberOfRounds = 0;
		this.totalSeenNodes = 0;
		this.totalReachableNodes = 0;
	}

	public void run() {

		try {
			BufferedReader inBuffer = new BufferedReader(new FileReader(this.baseLogFile));

			List<Double> querryList = null;
			List<Double> returnedChildList = null;
			List<Double> returnedParentList = null;

			while (inBuffer.ready()) {
				String line = inBuffer.readLine().trim();

				/*
				 * Round flag
				 */
				Matcher roundMatch = RegexCollection.roundPattern.matcher(line);
				if (roundMatch.find()) {
					if (this.numberOfRounds != 0) {
						this.numberOfNodeQuerries.add(querryList);
						this.returnedNodeCountChild.add(returnedChildList);
						this.returnedNodeCountParent.add(returnedParentList);
					}
					querryList = new ArrayList<Double>();
					returnedChildList = new ArrayList<Double>();
					returnedParentList = new ArrayList<Double>();
					this.numberOfRounds++;
					continue;
				}

				/*
				 * Parse the number of nodes returned from each individual node
				 */
				Matcher indTotalPattern = RegexCollection.harvestTotalWorkerThreadPattern.matcher(line);
				if (indTotalPattern.find()) {
					double result = Double.parseDouble(indTotalPattern.group(1));
					returnedChildList.add(result);
					continue;
				}
				
				Matcher indParentTotalMatch = RegexCollection.harvestTotalParentThreadPattern.matcher(line);
				if(indParentTotalMatch.find()){
					double result = Double.parseDouble(indParentTotalMatch.group(1));
					returnedParentList.add(result);
					continue;
				}

				/*
				 * Parse the number of rounds it takes to fully harvest
				 */
				Matcher indRoundsTakenPattern = RegexCollection.harvestIterationIndividualPattern.matcher(line);
				if (indRoundsTakenPattern.find()) {
					double result = Double.parseDouble(indRoundsTakenPattern.group(1));
					querryList.add(result);
					continue;
				}

				/*
				 * Parse total nodes seen
				 */
				Matcher totalNodesMatch = RegexCollection.totalNodesKnownPattern.matcher(line);
				if (totalNodesMatch.find()) {
					int result = Integer.parseInt(totalNodesMatch.group(1));
					this.totalReachableNodes = result;
					continue;
				}

				/*
				 * Parse total reachable nodes seen
				 */
				Matcher reachableNodesMatch = RegexCollection.reachableNodesKnownPattern.matcher(line);
				if (reachableNodesMatch.find()) {
					int result = Integer.parseInt(reachableNodesMatch.group(1));
					this.totalReachableNodes = result;
					continue;
				}
			}

			/*
			 * Don't forget to save the last list, as we won't see a final round
			 * flag
			 */
			this.numberOfNodeQuerries.add(querryList);
			this.returnedNodeCountChild.add(returnedChildList);
			this.returnedNodeCountParent.add(returnedParentList);

			inBuffer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		List<Double> totalQuerriesList = new ArrayList<Double>();
		for (Collection<Double> tCollection : this.numberOfNodeQuerries) {
			totalQuerriesList.addAll(tCollection);
		}
		List<Double> totalNodesChildFound = new ArrayList<Double>();
		for (Collection<Double> tCollection : this.returnedNodeCountChild) {
			totalNodesChildFound.addAll(tCollection);
		}
		List<Double> totalNodesParentFound = new ArrayList<Double>();
		for(Collection<Double> tCollection: this.returnedNodeCountParent){
			totalNodesParentFound.addAll(tCollection);
		}
		try {
			CDF.printCDFs(this.numberOfNodeQuerries, this.baseLogFile + "-perRoundQuerries");
			CDF.printCDFs(this.returnedNodeCountChild, this.baseLogFile + "-perRoundResponsesChild");
			CDF.printCDFs(this.returnedNodeCountParent, this.baseLogFile + "-perRoundResponsesParent");
			CDF.printCDF(totalQuerriesList, this.baseLogFile + "-querriesCDF");
			CDF.printCDF(totalNodesChildFound, this.baseLogFile + "-nodesChildCDF");
			CDF.printCDF(totalNodesParentFound, this.baseLogFile + "-nodesParentCDF");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.parseComplete = true;
	}

}
