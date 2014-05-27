package message;

import java.net.*;
import java.io.*;

import util.ByteOps;

public class Version extends CommonMessage {

	public static final byte[] SERVICE_BYTES = ByteOps.hexStringToByteArray("0100000000000000"); 
	
	public Version(InetAddress dest, int destPort, InetAddress src, int srcPort){
		super("version");
		
		byte[] payload = new byte[4 + 8 + 8 + 26 + 26 + 8 + 1 + 4 + 1];
		ByteOps.appendBytes(ByteOps.fixedSmallEndianInt(70002), payload, 0);
		ByteOps.appendBytes(Version.SERVICE_BYTES, payload, 4);
		ByteOps.appendBytes(ByteOps.fixedSmallEndianLong(System.currentTimeMillis()), payload, 4 + 8);
		ByteOps.appendBytes(CommonMessage.netAddress(dest, destPort, false), payload, 4 + 8 + 8);
		ByteOps.appendBytes(CommonMessage.netAddress(src, srcPort, false), payload, 4 + 8 + 8 + 26);
		
		//TODO actually generate a nonce
		ByteOps.appendBytes(ByteOps.fixedSmallEndianLong(2), payload, 4 + 8 + 8 + 26 + 26);
		
		//TODO actually populate user agent in future -- packet size will need to change
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("00"), payload, 4 + 8 + 8 + 26 + 26 + 8);
		
		//TODO maybe populate this guy to someting "sane"-ish
		ByteOps.appendBytes(ByteOps.fixedSmallEndianInt(0), payload, 4 + 8 + 8 + 26 + 26 + 8 + 1);
		
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("00"), payload, 4 + 8 + 8 + 26 + 26 + 8 + 1 + 4);
		
		this.setPayload(payload);
	}
	
	public static void main(String args[]) throws IOException{
		Version self = new Version(InetAddress.getByName("192.168.1.69"), 8333, InetAddress.getByName("192.168.1.69"), 8340);
		Socket tSock = new Socket(InetAddress.getByName("192.168.1.69"), 8333);
		java.io.OutputStream out = tSock.getOutputStream();
		out.write(self.getBytes());
		out.flush();
		
		while(true){
			
		}
	}
}
