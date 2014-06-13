package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

import data.Contact;
import experiment.ConnectionExperiment;

public class ZmapSupplicant {

	private Socket serverConnection;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	public static int DEFAULT_ZMAP_PORT = 12001;
	public static String DEFAULT_HOME_IP = "209.98.139.69";

	public ZmapSupplicant() throws IOException {
		this.serverConnection = new Socket(ZmapSupplicant.DEFAULT_HOME_IP, ZmapSupplicant.DEFAULT_ZMAP_PORT);
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

}