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

	/**
	 * Contact information for this peer
	 */
	private Contact parent;

	/**
	 * Connection state information
	 */
	private boolean outVersion;
	private boolean incVersion;
	private boolean incVerAck;

	private String pongNonce;
	private String currentErrorMsg;

	/**
	 * Network information
	 */
	private Socket nodeSocket;
	private InputStream iStream;
	private OutputStream oStream;
	private Thread listenerThread;

	private Version remoteVersionMessage;
	private HashSet<Contact> learnedContacts;

	/**
	 * Transaction variables
	 */
	private Semaphore transactionLine;
	private String currentTransaction;
	private Semaphore transactionFlag;

	public Node(Contact parentContact) {
		this.parent = parentContact;

		this.outVersion = false;
		this.incVersion = false;
		this.incVerAck = false;

		this.pongNonce = null;
		this.currentErrorMsg = null;

		this.nodeSocket = null;
		this.iStream = null;
		this.oStream = null;
		this.listenerThread = null;

		this.remoteVersionMessage = null;
		this.learnedContacts = new HashSet<Contact>();

		this.transactionFlag = new Semaphore(0);
		this.transactionLine = new Semaphore(1);
		this.currentTransaction = Constants.NONE_TX;
	}

	public Node(Socket incSocket) throws IOException {
		this.parent = null;

		this.outVersion = false;
		this.incVersion = false;
		this.incVerAck = false;

		this.currentErrorMsg = null;
		this.pongNonce = null;

		this.nodeSocket = incSocket;
		this.iStream = this.nodeSocket.getInputStream();
		this.oStream = this.nodeSocket.getOutputStream();

		this.listenerThread = null;

		this.remoteVersionMessage = null;
		this.learnedContacts = new HashSet<Contact>();

		this.transactionFlag = new Semaphore(0);
		this.transactionLine = new Semaphore(1);
		this.currentTransaction = Constants.NONE_TX;
	}

	private boolean initiateTransaction(String txName) {
		try {
			this.transactionLine.acquire();
		} catch (InterruptedException e) {
			return false;
		}
		synchronized (this) {
			this.transactionFlag.drainPermits();
			this.currentTransaction = txName;
		}

		return true;
	}

	private void endTransaction() {
		synchronized (this) {
			this.currentTransaction = Constants.NONE_TX;
			this.transactionFlag.drainPermits();
		}
		this.transactionLine.release();
	}

	private void releaseIfInTransaction(String txName) {
		boolean match = false;
		synchronized (this) {
			match = this.currentTransaction.equals(txName);
		}

		if (match) {
			this.transactionFlag.release();
		}
	}

	public int hashCode() {
		return this.parent.hashCode();
	}

	public boolean equals(Object rhs) {
		Node rhsNode = (Node) rhs;
		return rhsNode.getContactObject().equals(this.getContactObject());
	}

	public boolean thinksConnected() {
		return this.outVersion && this.incVersion && this.incVerAck;
	}

	private void resetConnectionStatus() {
		synchronized (this) {
			this.outVersion = false;
			this.incVersion = false;
			this.incVerAck = false;
		}
	}

	private void updateErrorStatus(String incError) {
		synchronized (this) {
			if (this.currentErrorMsg == null) {
				this.currentErrorMsg = incError;
			}
		}
	}

	public String getErrorMsg(boolean clear) {
		String errFetch = null;

		synchronized (this) {
			errFetch = this.currentErrorMsg;
			if (clear) {
				this.currentErrorMsg = null;
			}
		}

		return errFetch;
	}

	public boolean connect() {
		Version versionPacket;

		if (this.thinksConnected()) {
			return true;
		}
		this.initiateTransaction(Constants.CONNECT_TX);

		/*
		 * Actually open the network connection
		 */
		if (this.nodeSocket == null) {
			try {
				this.nodeSocket = new Socket();
				this.nodeSocket.connect(new InetSocketAddress(this.parent.getIp(), this.parent.getPort()),
						Constants.CONNECT_TIMEOUT);
				this.iStream = this.nodeSocket.getInputStream();
				this.oStream = this.nodeSocket.getOutputStream();
			} catch (SocketTimeoutException e) {
				this.updateErrorStatus("Socket timeout");
				return false;
			} catch (IOException e) {
				this.updateErrorStatus("I/O exception binding: " + e.getMessage());
				return false;
			}
		}

		// FIXME this needs to be actually populated, with data from the version
		// message
		if (this.parent == null) {
			// FIXME dirty hack for now, assumes default port (might not be
			// true)
			this.parent = new Contact(this.nodeSocket.getInetAddress(), Constants.DEFAULT_PORT);
		}

		/*
		 * Start up a listener
		 */
		IncomingParser listener = new IncomingParser(this, this.iStream);
		this.listenerThread = new Thread(listener);
		this.listenerThread.setDaemon(true);
		this.listenerThread.setName("Incoming Parser: " + this.nodeSocket.getInetAddress().toString());
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
			this.updateErrorStatus("I/O exception writing version packet");
			try {
				this.nodeSocket.close();
			} catch (IOException e2) {
				// Can be caught silently, we're already dying
			}
			return false;
		}
		this.parent.updateTimeStamp(((long) Math.floor(System.currentTimeMillis() / 1000)), false);
		synchronized (this) {
			this.outVersion = true;
		}

		boolean waitResult = false;
		try {
			waitResult = this.transactionFlag.tryAcquire(2, Constants.CONNECT_TIMEOUT, Constants.CONNECT_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			this.updateErrorStatus("Interupted waiting for version handshake.");
			try {
				this.nodeSocket.close();
			} catch (IOException e2) {
				// Can be caught silently, we're already dying
			}
			return false;
		}
		this.endTransaction();

		/*
		 * evaluate if the try acquire succeeded
		 */
		if (Constants.STRICT_TIMEOUTS && !waitResult) {
			this.updateErrorStatus("Timed out waiting for version/verack handshake");
			try {
				this.nodeSocket.close();
			} catch (IOException e2) {
				// Can be caught silently, we're already dying
			}
		}

		boolean con = this.thinksConnected();
		if (con) {
			this.parent.setLastSeenDirect(true);
		}
		return con;
	}

	public boolean testConnectionLiveness() {
		if (!this.thinksConnected()) {
			return false;
		}

		if (!this.initiateTransaction(Constants.PING_TX)) {
			return false;
		}

		Ping outPing = new Ping();
		String nonceToSee = outPing.getNonceStr();

		this.pongNonce = null;
		try {
			this.oStream.write(outPing.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			this.shutdownNode("I/O error writing ping during connection test");
			return false;
		}

		/*
		 * Block on response, we don't actually need to error here, as we'll
		 * error during the test beneath
		 */
		boolean waitSuccess = false;
		try {
			waitSuccess = this.transactionFlag.tryAcquire(Constants.TRANSACTION_TIMEOUT,
					Constants.TRANSACTION_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			/*
			 * Deal w/ silently, not a big deal honestly
			 */
		}
		this.endTransaction();

		if (Constants.STRICT_TIMEOUTS && !waitSuccess) {
			this.shutdownNode("Timeout waiting for ping reply");
			return false;
		} else {
			if (this.pongNonce != null) {
				boolean result = this.pongNonce.equals(nonceToSee);
				if (!result) {
					this.shutdownNode("bad ping reply");
				}
				return result;
			} else {
				this.shutdownNode("Ping reply still null (shouldn't get here...).");
				return false;
			}
		}
	}

	public void recievedVersion(Version incVerMsg) {

		this.remoteVersionMessage = incVerMsg;

		VerAck verAckPacket = new VerAck();
		try {
			this.oStream.write(verAckPacket.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		synchronized (this) {
			this.incVersion = true;
		}
		this.releaseIfInTransaction(Constants.CONNECT_TX);
	}

	public void recievedVerAck() {
		synchronized (this) {
			this.incVerAck = true;
		}
		this.releaseIfInTransaction(Constants.CONNECT_TX);
	}

	public void recievedAddr(byte[] advPayload) {
		Addr incMessage = new Addr(advPayload);
		List<Contact> incContacts = incMessage.getLearnedContacts();
		synchronized (this.learnedContacts) {
			/*
			 * We need to remove the old contact objects first, adding in the
			 * new ones
			 */
			this.learnedContacts.removeAll(incContacts);
			this.learnedContacts.addAll(incContacts);
		}

		if (incContacts.size() < 1000) {
			this.releaseIfInTransaction(Constants.GETADDR_TX);
		}
	}

	public void recievedPing(byte[] pingPayload) {
		Ping incPing = new Ping(pingPayload);
		Pong response = incPing.buildPong();

		try {
			this.oStream.write(response.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			this.shutdownNode("error writing pong");
		}
	}

	public void recievedPong(byte[] pongPayload) {
		Pong incPong = new Pong(pongPayload);
		this.pongNonce = incPong.getNonceStr();
		this.releaseIfInTransaction(Constants.PING_TX);
	}

	public void shutdownNode(String errorMessage) {
		this.updateErrorStatus(errorMessage);

		this.resetConnectionStatus();
		try {
			this.nodeSocket.close();
		} catch (IOException e) {
			// can die silently
		}
	}

	public void querryForNodes() {

		GetAddr outMsg = new GetAddr();

		this.initiateTransaction(Constants.GETADDR_TX);

		try {
			this.oStream.write(outMsg.getBytes());
		} catch (IOException e) {
			this.shutdownNode("error wrting get_addr message");
		}

		try {
			this.transactionFlag.tryAcquire(Constants.TRANSACTION_TIMEOUT, Constants.CONNECT_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			return;
		}
		this.endTransaction();
	}

	public void clearContacts() {
		this.learnedContacts.clear();
	}

	public Set<Contact> getContacts() {
		HashSet<Contact> retSet = new HashSet<Contact>();
		synchronized (this.learnedContacts) {
			retSet.addAll(this.learnedContacts);
		}
		return retSet;
	}

	public int getContactCount() {
		int size = 0;
		synchronized (this.learnedContacts) {
			size = this.learnedContacts.size();
		}

		return size;
	}

	public String getRemoteUserAgent() {
		if (this.remoteVersionMessage != null) {
			return this.remoteVersionMessage.getUserAgent();
		} else {
			return null;
		}
	}

	public long getLastBlockSeen() {
		if (this.remoteVersionMessage != null) {
			return this.remoteVersionMessage.getLastBlockSeen();
		} else {
			return 0;
		}
	}

	public Contact getContactObject() {
		return this.parent;
	}
}
