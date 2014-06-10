package net;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import message.*;
import data.Contact;

//TODO better error handling
//TODO logger for exceptions, just so we can look at them and sanity check
public class Node {

	private Contact parent;

	private int connectionState;
	private Socket nodeSocket;
	private InputStream iStream;
	private OutputStream oStream;
	private Thread listenerThread;

	private HashSet<Contact> learnedContacts;

	private Semaphore transactionFlag;

	public Node(Contact parentContact) {
		this.parent = parentContact;

		this.connectionState = 0;
		this.iStream = null;
		this.oStream = null;
		this.listenerThread = null;

		this.learnedContacts = new HashSet<Contact>();

		this.transactionFlag = new Semaphore(0);
	}

	public int hashCode() {
		return this.parent.hashCode();
	}

	public boolean thinksConnected() {
		return this.connectionState == 15;
	}

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
			this.nodeSocket.connect(new InetSocketAddress(this.parent.getIp(), this.parent.getPort()),
					Constants.CONNECT_TIMEOUT);
			this.iStream = this.nodeSocket.getInputStream();
			this.oStream = this.nodeSocket.getOutputStream();
		} catch (SocketTimeoutException e) {
			return false;
		} catch (IOException e) {
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
		versionPacket = new Version(this.parent.getIp(), this.parent.getPort());

		/*
		 * The first part of the 4 way handshake is sending our version message,
		 * while technically not needed, do it for form anyway...
		 */
		try {
			this.oStream.write(versionPacket.getBytes());
			this.oStream.flush();
		} catch (IOException e1) {
			try {
				this.nodeSocket.close();
			} catch (IOException e2) {
				// Can be caught silently, we're already dying
			}
			return false;
		}
		this.connectionState += 1;

		try {
			this.transactionFlag.tryAcquire(2, Constants.TRANSACTION_TIMEOUT, Constants.TRANSACTION_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			try {
				this.nodeSocket.close();
			} catch (IOException e2) {
				// Can be caught silently, we're already dying
			}
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

	//XXX is there an issue with multiple places calling this at the same time?
	public void reportNodeIOError() {
		this.connectionState = 0;
		try {
			this.nodeSocket.close();
		} catch (IOException e) {
			// can die silently
		}
	}

	public void querryForNodes() {

		GetAddr outMsg = new GetAddr();
		try {
			this.oStream.write(outMsg.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			this.reportNodeIOError();
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
