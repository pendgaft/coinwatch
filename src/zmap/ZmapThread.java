package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

import net.Constants;

import data.Contact;

public class ZmapThread implements Runnable {

	private Socket conn;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	private static final String FILE_DIR = "/home/pendgaft/scratch/zmap/";

	public ZmapThread(Socket connection) throws IOException {
		this.conn = connection;
		this.in = new ObjectInputStream(this.conn.getInputStream());
		this.out = new ObjectOutputStream(this.conn.getOutputStream());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			System.out.println("Starting set read.");
			Set<Contact> trialSet = (Set<Contact>) this.in.readObject();
			System.out.println("Finished set read: " + trialSet.size() + " elements");
			Set<Contact> returnSet = new HashSet<Contact>();

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
			Process childProcess = rt.exec("zmap -w " + whiteFileName + " -o " + whiteFileName
					+ "-out -S 192.168.1.69 -P 5 -c 15 -i p34p1 -p 8333");
			childProcess.waitFor();
			System.out.println("Zmap finished");

			/*
			 * Read the results of the scan, load into set
			 */
			BufferedReader inBuffer = new BufferedReader(new FileReader(whiteFileName + "-out"));
			// eat the first line as it is just the word "saddr"
			inBuffer.readLine();
			while (inBuffer.ready()) {
				String readStr = inBuffer.readLine().trim();
				if(readStr.length() > 0){
					returnSet.add(new Contact(InetAddress.getByName(readStr), Constants.DEFAULT_PORT, 0 , false));
				}
			}
			inBuffer.close();

			System.out.println("Starting set write of " + returnSet.size() + " elements");
			this.out.writeObject(returnSet);
			
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

		try {
			this.conn.close();
		} catch (IOException e) {

		}
	}
}
