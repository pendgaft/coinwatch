package logging;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.InetAddress;

import net.Constants;

import data.Contact;
import scijava.stats.CDF;
import scijava.stats.BasicStats;

public class IntersectParser {

	private HashMap<Contact, Set<Contact>> advancingNodesByReporting;
	private HashMap<Contact, Set<Contact>> advancingInverted;

	public IntersectParser(String logFile) throws IOException {
		this.advancingNodesByReporting = new HashMap<Contact, Set<Contact>>();
		this.advancingInverted = new HashMap<Contact, Set<Contact>>();
		this.parseContactKnownByMap(logFile);
		this.buildInverted();
	}

	private void parseContactKnownByMap(String logFile) throws IOException {
		BufferedReader inBuff = new BufferedReader(new FileReader(logFile));
		Pattern parentPattern = Pattern.compile("Contact [^/]*/(.+):(\\d+),(\\d+),([^,]+)");
		Pattern knowsPattern = Pattern.compile("[^/]*/(.+):(\\d+),(\\d+)");

		Contact parentContact = null;
		while (inBuff.ready()) {
			String line = inBuff.readLine().trim();

			if (line.length() == 0) {
				parentContact = null;
				continue;
			}

			Matcher parentMatch = parentPattern.matcher(line);
			if (parentMatch.find()) {
				parentContact = new Contact(InetAddress.getByName(parentMatch.group(1)), Integer.parseInt(parentMatch
						.group(2)), Long.parseLong(parentMatch.group(3)), Boolean.parseBoolean(parentMatch.group(4)));
				this.advancingNodesByReporting.put(parentContact, new HashSet<Contact>());
				continue;
			}

			Matcher knowsMatch = knowsPattern.matcher(line);
			if (knowsMatch.find()) {
				Contact tContact = new Contact(InetAddress.getByName(knowsMatch.group(1)), Integer.parseInt(knowsMatch
						.group(2)), Long.parseLong(knowsMatch.group(3)), false);
				this.advancingNodesByReporting.get(parentContact).add(tContact);
				continue;
			}
		}

		inBuff.close();
	}

	private void buildInverted() {
		for (Contact firstCon : this.advancingNodesByReporting.keySet()) {
			Set<Contact> secondSet = this.advancingNodesByReporting.get(firstCon);

			for (Contact secondCon : secondSet) {
				if (!this.advancingInverted.containsKey(secondCon)) {
					this.advancingInverted.put(secondCon, new HashSet<Contact>());
				}
				this.advancingInverted.get(secondCon).add(firstCon);
			}
		}
	}

	public void writeNumberCDF(HashMap<Contact, Set<Contact>> map, String outFile) throws IOException {
		List<Double> sizesList = new ArrayList<Double>(this.advancingNodesByReporting.size());
		for (Contact tCon : map.keySet()) {
			sizesList.add((double) map.get(tCon).size());
		}
		CDF.printCDF(sizesList, outFile);
	}

	public void writeNumberCDFSeen(HashMap<Contact, Set<Contact>> map, String outFile, boolean seen) throws IOException {
		List<Double> sizesList = new ArrayList<Double>(this.advancingNodesByReporting.size());
		for (Contact tCon : map.keySet()) {
			if (tCon.isLastSeenDirect() == seen) {
				sizesList.add((double) map.get(tCon).size());
			}
		}
		CDF.printCDF(sizesList, outFile);
	}

	public void getTimeStampStats() {
		List<Double> tsList = new LinkedList<Double>();

		for (Contact tCon : this.advancingNodesByReporting.keySet()) {
			for(Contact tAdvancing: this.advancingNodesByReporting.get(tCon)){
				tsList.add((double)tAdvancing.getLastSeen());
			}
		}

		double mean = BasicStats.meanOfDoubles(tsList);
		double stdDev = BasicStats.stdDevOfDoubles(tsList);
		double median = BasicStats.medianOfDoubles(tsList);

		double max = Collections.max(tsList);
		double min = Collections.min(tsList);

		System.out.println("Min/Max: " + min + ", " + max);
		System.out.println("Med/Mean/StdDev " + median + ", " + mean + ", " + stdDev);
	}
	
	private static void parseTimeSkew() throws IOException{
		BufferedReader inBuff = new BufferedReader(new FileReader(Constants.LOG_DIR + "timeSkewTest.csv"));
		
		Pattern nullPattern = Pattern.compile("[^/]*/(.+):(\\d+),(\\d+),([^,]+),null");
		Pattern dataPattern = Pattern.compile("[^/]*/.+:\\d+,(\\d+),[^,]+,(\\d+)");
		
		int notSeenCount = 0;
		List<Double> delta = new LinkedList<Double>();
		
		while(inBuff.ready()){
			String line = inBuff.readLine().trim();
			if(line.length() == 0){
				continue;
			}
			
			Matcher nullMatch = nullPattern.matcher(line);
			if(nullMatch.find()){
				notSeenCount++;
				continue;
			}
			
			Matcher dataMatch = dataPattern.matcher(line);
			if(dataMatch.find()){
				delta.add(Double.parseDouble(dataMatch.group(2)) - Double.parseDouble(dataMatch.group(1)));
			}
		}
		
		inBuff.close();
		
		System.out.println("No record of us: " + notSeenCount + ", record of us: " + delta.size());
		CDF.printCDF(delta, Constants.LOG_DIR + "skewDeltaCDF.csv");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
//		IntersectParser self = new IntersectParser("logs/activeOut.txt");
//		System.out.println("learned from " + self.advancingNodesByReporting.size() + " active nodes "
//				+ self.advancingInverted.size());
//		self.getTimeStampStats();
//		self.writeNumberCDF(self.advancingInverted, "logs/active-fullCDF.csv");
		IntersectParser.parseTimeSkew();
	}

}
