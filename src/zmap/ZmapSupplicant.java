package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

import data.Contact;

public class ZmapSupplicant {

	private Socket serverConnection;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	public static int DEFAULT_ZMAP_PORT = 12001;

	public ZmapSupplicant(String host) throws IOException {
		this.serverConnection = new Socket(host, ZmapSupplicant.DEFAULT_ZMAP_PORT);
	}

	@SuppressWarnings("unchecked")
	public Set<Contact> checkAddresses(Set<Contact> nodesToTest) throws IOException {
		this.out = new ObjectOutputStream(this.serverConnection.getOutputStream());
		this.in = new ObjectInputStream(this.serverConnection.getInputStream());

		Set<Contact> returnedSet = null;

		this.out.writeObject(nodesToTest);
		try {
			returnedSet = (Set<Contact>) this.in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		this.serverConnection.close();

		return returnedSet;
	}

	public static void main(String[] args) throws IOException {
		ZmapSupplicant self = new ZmapSupplicant("127.0.0.1");
		Set<Contact> testSet = new HashSet<Contact>();
		testSet.add(new Contact(InetAddress.getByName("9.9.9.9"), 1515, 0, false));
		testSet.add(new Contact(InetAddress.getByName("10.9.9.9"), 1515, 0, false));

		Set<Contact> retSet = self.checkAddresses(testSet);
		for (Contact tContact : retSet) {
			System.out.println(tContact.toString());
		}
	}

}
