package zmap;

import java.io.*;
import java.net.*;
import java.util.*;

import logging.LogHelper;
import net.Constants;

import data.Contact;

public class ZmapSelf {

	private Runtime myRuntime;
	private static final String FILE_DIR = "/home/schuch/zmap/logs/";

	public ZmapSelf() {
		this.myRuntime = Runtime.getRuntime();
	}

	// TODO use sim logger
	public Set<Contact> checkAddresses(Set<Contact> testSet) throws IOException {
		/*
		 * Prune out all IPv6 and non 8333 port contacts
		 */
		Set<Contact> removeSet = new HashSet<Contact>();
		for (Contact tContact : testSet) {
			if (tContact.getIp() instanceof Inet6Address || tContact.getPort() != Constants.DEFAULT_PORT) {
				removeSet.add(tContact);
			}
		}
		testSet.removeAll(removeSet);
		System.out.println("Finished set prune with " + testSet.size() + " elements");

		/*
		 * Write hosts to a file for zmap to white list
		 */
		String whiteFileName = ZmapSelf.FILE_DIR + System.currentTimeMillis();
		BufferedWriter whiteListFile = new BufferedWriter(new FileWriter(whiteFileName));
		for (Contact tContact : testSet) {
			whiteListFile.write(tContact.getIp().toString().split("/")[1] + "/32\n");
		}
		whiteListFile.close();

		/*
		 * Fork and wait on zmap
		 */
		System.out.println("Strating zmap");
		long start = System.currentTimeMillis();
		// TODO calibrate bandwidth vs accuracy for bobafett
		Process childProcess = this.myRuntime.exec("zmap -w " + whiteFileName + " -o " + whiteFileName
				+ "-out -S 160.94.77.70 -P 5 -c 30 -i eth0 -p 8333 -B 100K");
		try {
			childProcess.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Zmap finished in " + LogHelper.formatMili(System.currentTimeMillis() - start));

		/*
		 * Read the results of the scan, load into set
		 */
		Set<Contact> returnSet = new HashSet<Contact>();
		BufferedReader inBuffer = new BufferedReader(new FileReader(whiteFileName + "-out"));
		// eat the first line as it is just the word "saddr"
		inBuffer.readLine();
		while (inBuffer.ready()) {
			String readStr = inBuffer.readLine().trim();
			if (readStr.length() > 0) {
				returnSet.add(new Contact(InetAddress.getByName(readStr), Constants.DEFAULT_PORT, 0, false));
			}
		}
		inBuffer.close();

		/*
		 * Clean up the temp files
		 */
		File whiteFile = new File(whiteFileName);
		whiteFile.delete();
		whiteFile = new File(whiteFileName + "-out");
		whiteFile.delete();

		return returnSet;
	}

}
