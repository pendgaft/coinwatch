package message;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import util.ByteOps;

public abstract class CommonMessage {

	
	private static byte[] MAGIC = ByteOps.hexStringToByteArray("f9beb4d9");
	
	private String command;
	private byte[] payload;
	
	
	public CommonMessage(String messageCommand){
		this.command = messageCommand;
		this.payload = null;
	}
	
	protected void setPayload(byte[] messagePayload){
		this.payload = messagePayload;
	}
	
	public byte[] getBytes(){
		int payloadSize = -1;
		byte[] outBytes = null;
		
		if(this.payload == null){
			payloadSize = 0;
		}else{
			payloadSize = this.payload.length;
		}
		outBytes = new byte[4 + 12 + 4 + 4 + payloadSize];
		
		ByteOps.appendBytes(CommonMessage.MAGIC, outBytes, 0);
		ByteOps.appendBytes(this.command.getBytes(), outBytes, 4);
		ByteOps.appendBytes(ByteOps.fixedSmallEndianInt(payloadSize), outBytes, 4 + 12);
		ByteOps.appendBytes(this.buildCheckSum(), outBytes, 4 + 12 + 4);
		
		if(this.payload != null){
			ByteOps.appendBytes(this.payload, outBytes, 4 + 12 + 4 + 4);
		}
		
		return outBytes;
	}
	
	private byte[] buildCheckSum(){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		byte[] firstHash = null;
		if(this.payload == null){
			firstHash = md.digest();
		}else{
			firstHash = md.digest(this.payload);
		}
		md.reset();
		byte[] secondHash = md.digest(firstHash);
		
		return ByteOps.subArray(secondHash, 4, 0);
	}


	public static byte[] netAddress(InetAddress addy, int port, boolean includeTS){
		byte[] outBytes = null;
		int tsOffset = 0;
		
		if(includeTS){
			outBytes = new byte[30];
			//TODO impl
			//ByteOps.appendBytes(data, outBytes, 0);
			tsOffset = 4;
		}else{
			outBytes= new byte[26];
			tsOffset = 0;
		}
		
		ByteOps.appendBytes(Version.SERVICE_BYTES, outBytes, tsOffset);
		ByteOps.appendBytes(ByteOps.hexStringToByteArray("ffff"), outBytes, tsOffset + 8 + 10);
		ByteOps.appendBytes(addy.getAddress(), outBytes, tsOffset + 8 + 12);
		ByteOps.appendBytes(ByteOps.networkShort(port), outBytes, tsOffset + 8 + 16);
		
		return outBytes;
	}
	
	
}
