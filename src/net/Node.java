package net;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import message.*;
import data.Contact;

//TODO better error handling
//TODO logger for exceptions, just so we can look at them and sanity check
public class Node {

	private Contact parent;

	private int connectionState;
	private String pongNonce;
	private NodeErrorCode currentErrorNo;

	private Socket nodeSocket;
	private InputStream iStream;
	private OutputStream oStream;
	private Thread listenerThread;

	private Version remoteVersionMessage;
	private HashSet<Contact> learnedContacts;

	private Semaphore transactionFlag;

	public enum NodeErrorCode {
		CONN_TIMEOUT, HANDSHAKE_TIMEOUT, NONE, MISC_IO, OTHERSIDE_CLOSE, INCOMING_FAIL, REJECT
	}

	public Node(Contact parentContact) {
		this.parent = parentContact;

		this.connectionState = 0;
		this.pongNonce = null;
		this.currentErrorNo = NodeErrorCode.NONE;

		this.nodeSocket = null;
		this.iStream = null;
		this.oStream = null;
		this.listenerThread = null;

		this.remoteVersionMessage = null;
		this.learnedContacts = new HashSet<Contact>();

		this.transactionFlag = new Semaphore(0);
	}

	public Node(Socket incSocket) throws IOException {
		this.parent = null;

		this.connectionState = 0;
		this.currentErrorNo = NodeErrorCode.NONE;

		this.nodeSocket = incSocket;
		this.iStream = this.nodeSocket.getInputStream();
		this.oStream = this.nodeSocket.getOutputStream();

		this.listenerThread = null;

		this.remoteVersionMessage = null;
		this.learnedContacts = new HashSet<Contact>();
		this.transactionFlag = new Semaphore(0);
	}

	public int hashCode() {
		return this.parent.hashCode();
	}

	public boolean thinksConnected() {
		return this.connectionState == 15;
	}

	public NodeErrorCode getErronNo() {
		return this.currentErrorNo;
	}

	public boolean connect() {
		Version versionPacket;

		if (this.thinksConnected()) {
			return true;
		}

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
				this.currentErrorNo = NodeErrorCode.CONN_TIMEOUT;
				return false;
			} catch (IOException e) {
				this.currentErrorNo = NodeErrorCode.MISC_IO;
				return false;
			}
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
			this.currentErrorNo = NodeErrorCode.MISC_IO;
			return false;
		}
		this.connectionState += 1;

		try {
			this.transactionFlag.tryAcquire(2, Constants.CONNECT_TIMEOUT, Constants.CONNECT_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			try {
				this.nodeSocket.close();
			} catch (IOException e2) {
				// Can be caught silently, we're already dying
			}
			this.currentErrorNo = NodeErrorCode.HANDSHAKE_TIMEOUT;
			return false;
		}

		if (this.parent == null) {
			// TODO populate based on the version packet
		}

		return this.thinksConnected();
	}

	public boolean testConnectionLiveness() {
		Ping outPing = new Ping();
		String nonceToSee = outPing.getNonceStr();

		this.pongNonce = null;
		try {
			this.oStream.write(outPing.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			this.currentErrorNo = NodeErrorCode.MISC_IO;
			return false;
		}

		this.transactionFlag.drainPermits();
		try {
			this.transactionFlag.tryAcquire(Constants.TRANSACTION_TIMEOUT, Constants.TRANSACTION_TIMEOUT_UNIT);
		} catch (InterruptedException e) {
			/*
			 * Deal w/ silently, not a big deal honestly
			 */
		}

		if (this.pongNonce != null) {
			return this.pongNonce.equals(nonceToSee);
		} else {
			return false;
		}
	}

	public void recievedVersion(Version incVerMsg) {

		this.connectionState += 4;
		this.remoteVersionMessage = incVerMsg;

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
		synchronized (this.learnedContacts) {
			this.learnedContacts.addAll(incContacts);
		}
	}

	public void recievedPing(byte[] pingPayload) {
		Ping incPing = new Ping(pingPayload);
		Pong response = incPing.buildPong();

		try {
			this.oStream.write(response.getBytes());
			this.oStream.flush();
		} catch (IOException e) {
			this.currentErrorNo = NodeErrorCode.MISC_IO;
		}
	}

	public void recievedPong(byte[] pongPayload) {
		Pong incPong = new Pong(pongPayload);
		this.pongNonce = incPong.getNonceStr();
		this.transactionFlag.release();
	}

	// XXX is there an issue with multiple places calling this at the same time?
	public void shutdownNode(NodeErrorCode errno) {
		if (errno != null) {
			this.currentErrorNo = errno;
		}

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
			this.shutdownNode(NodeErrorCode.MISC_IO);
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

	public Set<Contact> getContacts(boolean reset) {
		HashSet<Contact> retSet = new HashSet<Contact>();
		synchronized (this.learnedContacts) {
			retSet.addAll(this.learnedContacts);
			if (reset) {
				this.learnedContacts.clear();
			}
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

	public int getLastBlockSeen() {
		if (this.remoteVersionMessage != null) {
			return this.remoteVersionMessage.getLastBlockSeen();
		} else {
			return 0;
		}
	}
}
