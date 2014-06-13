package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

import data.Contact;

public class ZmapThread implements Runnable {

	private Socket conn;
	private ObjectInputStream in;
	private ObjectOutputStream out;

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

			// TODO write to file

			// TODO invoke zmap

			// TODO read file into data structure

			// XXX testing
			returnSet.add(new Contact(InetAddress.getByName("5.6.7.8"), 124, 19, false));

			System.out.println("Starting set write of " + returnSet.size() + " elements");
			this.out.writeObject(returnSet);
			this.conn.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
}
