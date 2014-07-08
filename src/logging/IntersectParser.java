package logging;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.InetAddress;

import data.Contact;
import scijava.stats.CDF;
import scijava.stats.BasicStats;

public class IntersectParser {

	private HashMap<Contact, Set<Contact>> contactKnownBy;
	private HashMap<Contact, Set<Contact>> inverted;

	public IntersectParser(String logFile) throws IOException {
		this.contactKnownBy = new HashMap<Contact, Set<Contact>>();
		this.inverted = new HashMap<Contact, Set<Contact>>();
		this.parseContactKnownByMap(logFile);
		this.buildInverted();
	}

	private void parseContactKnownByMap(String logFile) throws IOException {
		BufferedReader inBuff = new BufferedReader(new FileReader(logFile));
		Pattern parentPattern = Pattern.compile("Contact [^/]*/(.+):(\\d+),(\\d+),([^,]+)");
		Pattern knowsPattern = Pattern.compile("[^/]*/(.+):(\\d+)");

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
				this.contactKnownBy.put(parentContact, new HashSet<Contact>());
				continue;
			}

			Matcher knowsMatch = knowsPattern.matcher(line);
			if (knowsMatch.find()) {
				Contact tContact = new Contact(InetAddress.getByName(knowsMatch.group(1)), Integer.parseInt(knowsMatch
						.group(2)));
				this.contactKnownBy.get(parentContact).add(tContact);
				continue;
			}
		}

		inBuff.close();
	}

	private void buildInverted() {
		for (Contact firstCon : this.contactKnownBy.keySet()) {
			Set<Contact> secondSet = this.contactKnownBy.get(firstCon);

			for (Contact secondCon : secondSet) {
				if (!this.inverted.containsKey(secondCon)) {
					this.inverted.put(secondCon, new HashSet<Contact>());
				}
				this.inverted.get(secondCon).add(firstCon);
			}
		}
	}

	public void writeNumberCDF(HashMap<Contact, Set<Contact>> map, String outFile) throws IOException {
		List<Double> sizesList = new ArrayList<Double>(this.contactKnownBy.size());
		for (Contact tCon : map.keySet()) {
			sizesList.add((double) map.get(tCon).size());
		}
		CDF.printCDF(sizesList, outFile);
	}

	public void writeNumberCDFSeen(HashMap<Contact, Set<Contact>> map, String outFile, boolean seen) throws IOException {
		List<Double> sizesList = new ArrayList<Double>(this.contactKnownBy.size());
		for (Contact tCon : map.keySet()) {
			if (tCon.isLastSeenDirect() == seen) {
				sizesList.add((double) map.get(tCon).size());
			}
		}
		CDF.printCDF(sizesList, outFile);
	}

	public void getTimeStampStats() {
		List<Double> tsList = new ArrayList<Double>(this.contactKnownBy.size());

		for (Contact tCon : this.contactKnownBy.keySet()) {
			if (tCon.getLastSeen() != 0 && (!tCon.isLastSeenDirect())) {
				tsList.add((double) tCon.getLastSeen());
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

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		IntersectParser self = new IntersectParser("logs/intOut.txt");
		System.out.println("learned from " + self.contactKnownBy.size() + " active nodes " + self.inverted.size());
		self.getTimeStampStats();
		self.writeNumberCDF(self.inverted, "logs/active-fullCDF.csv");
		self.writeNumberCDFSeen(self.inverted, "logs/active-directCDF.csv", true);
		self.writeNumberCDFSeen(self.inverted, "logs/active-nonConnected.csv", false);
	}

}
