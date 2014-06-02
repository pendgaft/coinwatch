package message;

import java.net.*;
import java.io.*;

import util.ByteOps;
import net.Constants;

public class Version extends CommonMessage {

	private int versionNumber;
	private String userAgent;
	private int lastBlockNumber;
	
	
	public static final int DEFAULT_VERSION_NUMBER = 70002;
	public static final byte[] DEFAULT_SERVICE_BYTES = ByteOps.hexStringToByteArray("0100000000000000");
	
	
	public Version(InetAddress dest, int destPort, InetAddress src, int srcPort){
		super("version");
		this.buildPayload(dest, destPort, src, srcPort);
	}
	
	public Version(InetAddress dest) throws UnknownHostException{
		super("version");
		this.buildPayload(dest, Constants.DEFAULT_PORT, InetAddress.getLocalHost(), Constants.DEFAULT_PORT);
	}
	
	private void buildPayload(InetAddress dest, int destPort, InetAddress src, int srcPort){
		byte[] payload = new byte[4 + 8 + 8 + 26 + 26 + 8 + 1 + 4 + 1];
		
		//version
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianInt(Version.DEFAULT_VERSION_NUMBER), payload, 0);
		
		//service bytes
		ByteOps.appendBytes(Version.DEFAULT_SERVICE_BYTES, payload, 4);
		
		//time stamp
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianLong(System.currentTimeMillis()), payload, 4 + 8);
		
		//dest address, supress ts in version message
		ByteOps.appendBytes(CommonStructures.netAddress(dest, destPort, false), payload, 4 + 8 + 8);
		
		//source address, supress ts in version message
		ByteOps.appendBytes(CommonStructures.netAddress(src, srcPort, false), payload, 4 + 8 + 8 + 26);
		
		//TODO actually generate a nonce
		//nonce
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianLong(2), payload, 4 + 8 + 8 + 26 + 26);
		
		//TODO actually populate user agent in future -- packet size will need to change
		//user agent
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("00"), payload, 4 + 8 + 8 + 26 + 26 + 8);
		
		//TODO maybe populate this guy to someting "sane"-ish
		//last block received
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianInt(0), payload, 4 + 8 + 8 + 26 + 26 + 8 + 1);
		
		//relay flag
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("00"), payload, 4 + 8 + 8 + 26 + 26 + 8 + 1 + 4);
		
		this.setPayload(payload);
	}
}
