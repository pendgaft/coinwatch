package net;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import message.*;
import data.Contact;

//TODO logger for exceptions, just so we can look at them and sanity check
//TODO seems like we should NEVER recycle one of these objects (transactions for example), make them "one shot"
public class Node {

	/*
	 * Contact information for this peer
	 */
	private Contact parent;

	/*
	 * Connection state information
	 */
	private boolean outVersion;
	private boolean incVersion;
	private boolean incVerAck;

	/*
	 * Ping and pong information
	 */
	private String pingNonce;
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
	private long lastTSMovingMessage;

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

		this.pingNonce = null;
		this.pongNonce = null;
		this.currentErrorMsg = null;

		this.nodeSocket = null;
		this.iStream = null;
		this.oStream = null;
		this.listenerThread = null;

		this.remoteVersionMessage = null;
		this.lastTSMovingMessage = 0;
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
		this.lastTSMovingMessage = 0;
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
		if (!(rhs instanceof Node)) {
			return false;
		}

		Node rhsNode = (Node) rhs;
		return rhsNode.getContactObject().equals(this.getContactObject());
	}

	public boolean thinksConnected() {
		boolean result = false;
		synchronized (this) {
			result = this.outVersion && this.incVersion && this.incVerAck;
		}
		return result;
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

	private void sentTSAdvancingMessage() {
		long time = ((long) Math.floor(System.currentTimeMillis() / 1000));
		synchronized (this) {
			/*
			 * Check if the window would advance, don't forget that bitcoind
			 * will only advance once every 20 minutes
			 */
			if ((this.lastTSMovingMessage + 20 * 60) <= time) {
				this.lastTSMovingMessage = time;
			}
		}
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
			this.shutdownNode("I/O exception writing version packet");
			return false;
		}
		this.sentTSAdvancingMessage();
		synchronized (this) {
			this.outVersion = true;
		}

		boolean waitResult = false;
		try {
			waitResult = this.transactionFlag.tryAcquire(2, Constants.CONNECT_TIMEOUT, Constants.CONNECT_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			this.shutdownNode("Interupted waiting for version handshake.");
			return false;
		}
		this.endTransaction();

		/*
		 * evaluate if the try acquire succeeded
		 */
		if (Constants.STRICT_TIMEOUTS && !waitResult) {
			this.shutdownNode("Timed out waiting for version/verack handshake");
		}

		boolean con = this.thinksConnected();
		if (con) {
			this.parent.setLastSeenDirect(true);
		}
		return con;
	}

	public void initiatePing() {
		/*
		 * Build ping message, do a pinch of setup
		 */
		Ping outPing = new Ping();
		this.pingNonce = outPing.getNonceStr();
		synchronized (this) {
			this.pongNonce = null;
		}

		/*
		 * Write the ping message and this part is done, we don't block here, as
		 * later code will check with the testPing method after a "reasonable"
		 * amount of time
		 */
		try {
			this.oStream.write(outPing.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			this.shutdownNode("I/O error writing ping during connection test");
		}

		/*
		 * This action can move our timestamp forward at the remote host
		 */
		this.sentTSAdvancingMessage();
	}

	public boolean testPing() {

		boolean worked = false;

		if (this.pingNonce == null) {
			throw new RuntimeException("testPing called without a call to initiatePing!");
		}

		synchronized (this) {
			if (this.pongNonce != null) {
				worked = this.pongNonce.equals(this.pingNonce);
			}

			this.pingNonce = null;
			this.pongNonce = null;
		}

		if (!worked) {
			this.shutdownNode("bad ping attempt");
		}

		return worked;
	}

	public void recievedVersion(Version incVerMsg) {

		this.remoteVersionMessage = incVerMsg;

		VerAck verAckPacket = new VerAck();
		try {
			this.oStream.write(verAckPacket.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			this.shutdownNode("Error writing verack message.");
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
			 * We need to remove the old version of the contact objects first,
			 * adding in the new ones, this ensures timestamps are up to date
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
		synchronized (this) {
			this.pongNonce = incPong.getNonceStr();
		}
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
			//This one we can let slide I think
		}
		this.endTransaction();
	}

	public void clearContacts() {
		synchronized (this.learnedContacts) {
			this.learnedContacts.clear();
		}
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
