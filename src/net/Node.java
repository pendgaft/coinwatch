package net;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import message.*;
import data.Contact;

//TODO better error handling
public class Node {

	private InetAddress dest;
	private int destPort;

	private int connectionState;
	private Socket nodeSocket;
	private InputStream iStream;
	private OutputStream oStream;
	private Thread listenerThread;

	private HashSet<Contact> learnedContacts;

	private Semaphore transactionFlag;

	private static final int HARVEST_THRESH = 50;

	public Node(InetAddress destIP, int port) {
		this.dest = destIP;
		this.destPort = port;
		this.connectionState = 0;
		this.iStream = null;
		this.oStream = null;
		this.listenerThread = null;

		this.learnedContacts = new HashSet<Contact>();

		this.transactionFlag = new Semaphore(0);
	}

	public boolean thinksConnected() {
		return this.connectionState == 15;
	}

	// TODO better error handling please
	public boolean connect() {
		Version versionPacket;

		if (this.thinksConnected()) {
			return true;
		}

		/*
		 * Actually open the network connection
		 */
		try {
			this.nodeSocket = new Socket();
			this.nodeSocket.connect(new InetSocketAddress(this.dest, this.destPort), Constants.CONNECT_TIMEOUT);
			this.iStream = this.nodeSocket.getInputStream();
			this.oStream = this.nodeSocket.getOutputStream();
		} catch (SocketTimeoutException e) {
			System.out.println("Timeout connecting");
			return false;
		} catch (IOException e) {
			System.err.println("Socket connection error");
			return false;
		}

		/*
		 * Start up a listener
		 */
		IncomingParser listener = new IncomingParser(this, this.iStream);
		this.listenerThread = new Thread(listener);
		this.listenerThread.setDaemon(true);
		this.listenerThread.start();

		/*
		 * Build the version packet
		 */
		try {
			versionPacket = new Version(this.dest);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}

		/*
		 * The first part of the 4 way handshake is sending our version message,
		 * while technically not needed, do it for form anyway...
		 */
		try {
			this.oStream.write(versionPacket.getBytes());
			this.oStream.flush();
		} catch (IOException e1) {
			// TODO teardown
			e1.printStackTrace();
			return false;
		}
		this.connectionState += 1;

		try {
			this.transactionFlag.tryAcquire(2, Constants.TRANSACTION_TIMEOUT, Constants.TRANSACTION_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			System.out.println("Timeout trying to complete opening handshake.");
			return false;
		}

		return this.thinksConnected();
	}

	public void recievedVersion() {

		this.connectionState += 4;

		VerAck verAckPacket = new VerAck();
		try {
			this.oStream.write(verAckPacket.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.connectionState += 8;
		this.transactionFlag.release();
	}

	public void recievedVerAck() {
		this.connectionState += 2;
		this.transactionFlag.release();
	}

	public void recievedAddr(byte[] advPayload) {
		Addr incMessage = new Addr(advPayload);
		List<Contact> incContacts = incMessage.getLearnedContacts();
		this.learnedContacts.addAll(incContacts);
	}

	public void reportIOError() {
		// TODO handle error
	}

	public void querryForNodes() {

		GetAddr outMsg = new GetAddr();
		try {
			this.oStream.write(outMsg.getBytes());
		} catch (IOException e) {
			// TODO better error handling
			e.printStackTrace();
		}

		try {
			Thread.sleep(Constants.TRANSACTION_TIMEOUT);
		} catch (InterruptedException e) {
			return;
		}
	}

	public void clearContacts() {
		this.learnedContacts.clear();
	}

	public Set<Contact> getContacts() {
		return this.learnedContacts;
	}
}
