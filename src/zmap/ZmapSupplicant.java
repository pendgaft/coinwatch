package zmap;

import java.net.*;
import java.io.*;
import java.util.*;

import data.Contact;

public class ZmapSupplicant {

	private Socket serverConnection;
	public static int DEFAULT_ZMAP_PORT = 12001;
	public static String DEFAULT_HOME_IP = "209.98.139.69";

	public ZmapSupplicant() throws IOException {
		this.serverConnection = new Socket(ZmapSupplicant.DEFAULT_HOME_IP, ZmapSupplicant.DEFAULT_ZMAP_PORT);
	}

	@SuppressWarnings("unchecked")
	public Set<Contact> checkAddresses(Set<Contact> nodesToTest) throws IOException {
		Set<Contact> returnedSet = null;
		
		/*
		 * Write to the server and hang up
		 */
		ObjectOutputStream oOut = new ObjectOutputStream(this.serverConnection.getOutputStream());
		oOut.writeObject(nodesToTest);
		this.serverConnection.close();
		
		/*
		 * Listen for the call back
		 */
		ServerSocket incServer = new ServerSocket(ZmapSupplicant.DEFAULT_ZMAP_PORT);
		Socket incSocket = incServer.accept();
		ObjectInputStream oIn = new ObjectInputStream(incSocket.getInputStream());
		
		/*
		 * Read results
		 */
		try {
			returnedSet = (Set<Contact>)oIn.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		/*
		 * Close everything down
		 */
		incSocket.close();
		incServer.close();

		return returnedSet;
	}

}
