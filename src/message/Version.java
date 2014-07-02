package message;

import java.net.*;

import util.ByteOps;
import net.Constants;
import data.Contact;

public class Version extends CommonMessage {

	private long versionNumber;
	private String userAgent;
	private long lastBlockNumber;
	private Contact srcContact;

	public static final int DEFAULT_VERSION_NUMBER = 70002;
	public static final byte[] DEFAULT_SERVICE_BYTES = ByteOps.hexStringToByteArray("0100000000000000");

	public Version(InetAddress dest, int destPort) {
		super("version");
		this.buildPayload(dest, destPort, Constants.SrcIP, Constants.DEFAULT_PORT);
	}

	public Version(InetAddress dest) {
		super("version");

		this.buildPayload(dest, Constants.DEFAULT_PORT, Constants.SrcIP, Constants.DEFAULT_PORT);
	}

	public Version(byte[] incomingMessage) {
		super("version");

		// TODO implement parsing of a version packet
		this.versionNumber = CommonStructures.extractSmallEndianInt(ByteOps.subArray(incomingMessage, 4, 0));
		this.srcContact = CommonStructures.extractNoTSNetAddress(ByteOps.subArray(incomingMessage, 26, 4 + 8 + 8 + 26));
		byte[] uaBytes = ByteOps.subArray(incomingMessage, incomingMessage.length - (4 + 8 + 8 + 26 + 26 + 8), 4 + 8 + 8 + 26 + 26 + 8);
		int uaSize = CommonStructures.getVarStrSize(uaBytes);
		this.userAgent = CommonStructures.extractVarString(uaBytes);
		this.lastBlockNumber = CommonStructures.extractSmallEndianInt(ByteOps.subArray(incomingMessage, 4, 4 + 8 + 8 + 26 + 26 + uaSize));
	}

	private void buildPayload(InetAddress dest, int destPort, InetAddress src, int srcPort) {
		byte[] payload = new byte[4 + 8 + 8 + 26 + 26 + 8 + 1 + 4 + 1];

		// version
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianInt(Version.DEFAULT_VERSION_NUMBER), payload, 0);

		// service bytes
		ByteOps.appendBytes(Version.DEFAULT_SERVICE_BYTES, payload, 4);

		// time stamp
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianLong(System.currentTimeMillis()), payload, 4 + 8);

		// dest address, supress ts in version message
		ByteOps.appendBytes(CommonStructures.netAddress(dest, destPort, false), payload, 4 + 8 + 8);

		// source address, supress ts in version message
		ByteOps.appendBytes(CommonStructures.netAddress(src, srcPort, false), payload, 4 + 8 + 8 + 26);

		// nonce
		int nonce = Constants.NON_SEC_RNG.nextInt();
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianLong(nonce), payload, 4 + 8 + 8 + 26 + 26);

		/*
		 * XXX actually populate user agent in future -- packet size will need
		 * to change
		 */
		// user agent
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("00"), payload, 4 + 8 + 8 + 26 + 26 + 8);

		/*
		 * last block received -- nodes don't really seem to care that we claim
		 * we've never seen a block, we just look like a new client
		 */
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianInt(0), payload, 4 + 8 + 8 + 26 + 26 + 8 + 1);

		// relay flag
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("00"), payload, 4 + 8 + 8 + 26 + 26 + 8 + 1 + 4);

		this.setPayload(payload);
	}
	
	public Contact getSourceContact(){
		return this.srcContact;
	}
	
	public long getVersion(){
		return this.versionNumber;
	}
	
	public long getLastBlockSeen(){
		return this.lastBlockNumber;
	}
	
	public String getUserAgent(){
		return this.userAgent;
	}
}
