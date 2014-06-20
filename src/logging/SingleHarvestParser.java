package logging;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import scijava.stats.*;

public class SingleHarvestParser {

	private String baseLogFile;
	private boolean parseComplete;

	private List<Collection<Double>> numberOfNodeQuerries;
	private List<Collection<Double>> returnedNodeCounts;
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
		this.returnedNodeCounts = new ArrayList<Collection<Double>>();

		this.numberOfRounds = 0;
		this.totalSeenNodes = 0;
		this.totalReachableNodes = 0;
	}

	public void run() {

		try {
			BufferedReader inBuffer = new BufferedReader(new FileReader(this.baseLogFile));

			List<Double> querryList = null;
			List<Double> returnedList = null;

			while (inBuffer.ready()) {
				String line = inBuffer.readLine().trim();

				/*
				 * Round flag
				 */
				Matcher roundMatch = RegexCollection.roundPattern.matcher(line);
				if (roundMatch.find()) {
					if (this.numberOfRounds != 0) {
						this.numberOfNodeQuerries.add(querryList);
						this.returnedNodeCounts.add(returnedList);
					}
					querryList = new ArrayList<Double>();
					returnedList = new ArrayList<Double>();
					this.numberOfRounds++;
					continue;
				}

				/*
				 * Parse the number of nodes returned from each individual node
				 */
				Matcher indTotalPattern = RegexCollection.harvestTotalIndividualPattern.matcher(line);
				if (indTotalPattern.find()) {
					double result = Double.parseDouble(indTotalPattern.group(1));
					returnedList.add(result);
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

			inBuffer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		List<Double> totalQuerriesList = new ArrayList<Double>();
		for (Collection<Double> tCollection : this.numberOfNodeQuerries) {
			totalQuerriesList.addAll(tCollection);
		}
		List<Double> totalNodesFoundList = new ArrayList<Double>();
		for (Collection<Double> tCollection : this.returnedNodeCounts) {
			totalNodesFoundList.addAll(tCollection);
		}
		try {
			CDF.printCDFs(this.numberOfNodeQuerries, this.baseLogFile + "-perRoundQuerries");
			CDF.printCDFs(this.returnedNodeCounts, this.baseLogFile + "-perRoundResponses");
			CDF.printCDF(totalQuerriesList, this.baseLogFile + "-querriesCDF");
			CDF.printCDF(totalNodesFoundList, this.baseLogFile + "-nodesCDF");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
