package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

import logging.LogHelper;

import net.Constants;

import data.Contact;

public class ZmapThread implements Runnable {

	private Socket conn;
	private InetAddress requesterIP;

	private static final String FILE_DIR = "/home/schuch/zmap/logs/";

	public ZmapThread(Socket connection) throws IOException {
		this.conn = connection;
		this.requesterIP = this.conn.getInetAddress();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			/*
			 * Read the target IPs from the peer
			 */
			System.out.println("Starting set read.");
			ObjectInputStream oIn = new ObjectInputStream(this.conn.getInputStream());
			Set<Contact> trialSet = (Set<Contact>) oIn.readObject();
			this.conn.close();
			System.out.println("Done with read of " + trialSet.size() + " items.");

			/*
			 * Prune out all IPv6 and non 8333 port contacts
			 */
			Set<Contact> removeSet = new HashSet<Contact>();
			for (Contact tContact : trialSet) {
				if (tContact.getIp() instanceof Inet6Address || tContact.getPort() != Constants.DEFAULT_PORT) {
					removeSet.add(tContact);
				}
			}
			trialSet.removeAll(removeSet);
			System.out.println("Finished set prune with " + trialSet.size() + " elements");
			
			/*
			 * Write hosts to a file for zmap to white list
			 */
			String whiteFileName = FILE_DIR + this.conn.getInetAddress() + this.conn.getPort();
			BufferedWriter whiteListFile = new BufferedWriter(new FileWriter(whiteFileName));
			for (Contact tContact : trialSet) {
				if (tContact.getPort() == Constants.DEFAULT_PORT) {
					whiteListFile.write(tContact.getIp().toString().split("/")[1] + "/32\n");
				}
			}
			whiteListFile.close();

			/*
			 * Fork and wait on zmap
			 */
			System.out.println("Strating zmap");
			Runtime rt = Runtime.getRuntime();
			long start = System.currentTimeMillis();
			Process childProcess = rt.exec("zmap -w " + whiteFileName + " -o " + whiteFileName
					+ "-out -S 160.94.77.70 -P 5 -c 30 -i eth0 -p 8333 -B 200K -q");
			childProcess.waitFor();
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
			 * Write the output to the client after phoning home
			 */
			System.out.println("Starting set write of " + returnSet.size() + " elements");
			Socket clientCon = new Socket(this.requesterIP, ZmapSupplicant.DEFAULT_ZMAP_PORT);
			ObjectOutputStream oOut = new ObjectOutputStream(clientCon.getOutputStream());
			oOut.writeObject(returnSet);
			System.out.println("Done with write.");
			clientCon.close();
			
			/*
			 * Clean up the temp files
			 */
			File whiteFile = new File(whiteFileName);
			whiteFile.delete();
			whiteFile = new File(whiteFileName + "-out");
			whiteFile.delete();
		} catch (IOException | InterruptedException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
