package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

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
	public void run() {
		try {
			System.out.println("Starting set read.");
			Set<Contact> trialSet = (Set<Contact>) this.in.readObject();
			System.out.println("Finished set read: " + trialSet.size() + " elements");
			Set<Contact> returnSet = new HashSet<Contact>();

			// XXX testing
			for (Contact tContact : trialSet) {
				System.out.println(tContact.toString());
			}
			
			/*
			 * Write hosts to a file for zmap to white list
			 */
			String whiteFileName = FILE_DIR + this.conn.getInetAddress() + this.conn.getPort();
			BufferedWriter whiteListFile = new BufferedWriter(new FileWriter(whiteFileName));
			for(Contact tContact: trialSet){
				if(tContact.getPort() == 8333){
					whiteListFile.write(tContact.getIp().toString().split("/")[1] + "\n");
				}
			}
			whiteListFile.close();

			//TODO zmap invoke
			System.out.println("Strating zmap");
			Runtime rt = Runtime.getRuntime();
			//Process childProcess = rt.exec("zmap");
			//childProcess.waitFor();
			System.out.println("Zmap finished");

			// TODO read file into data structure

			// XXX testing
			returnSet.add(new Contact(InetAddress.getByName("5.6.7.8"), 124, 19, false));

			System.out.println("Starting set write of " + returnSet.size() + " elements");
			this.out.writeObject(returnSet);
		} catch (IOException | InterruptedException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			this.conn.close();
		} catch (IOException e) {

		}
	}
}
