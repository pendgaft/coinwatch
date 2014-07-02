package message;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import util.ByteOps;

public class CommonMessage {

	private static byte[] MAGIC = ByteOps.hexStringToByteArray("f9beb4d9");
	private static final int HEADERSIZE = 4 + 12 + 4 + 4;

	private String command;
	private byte[] payload;

	public CommonMessage(String messageCommand) {
		this.command = messageCommand;
		this.payload = null;
	}

	public CommonMessage(InputStream incomingMessage) throws IOException {
		byte[] header = new byte[CommonMessage.HEADERSIZE];
		int offset = 0;

		while (offset != CommonMessage.HEADERSIZE) {
			int currentRead = incomingMessage.read(header, offset, CommonMessage.HEADERSIZE - offset);
			offset += currentRead;
		}
		
		this.command = new String(ByteOps.subArray(header, 12, 4));
		this.command = this.command.trim().toLowerCase();
		//XXX can the payload size ever be larger than 2^31-1?
		int payloadSize = (int)CommonStructures.extractSmallEndianInt(ByteOps.subArray(header, 4, 4 + 12));
		
		this.payload = new byte[payloadSize];
		offset = 0;
		while(offset != payloadSize){
			int currentRead = incomingMessage.read(this.payload, offset, payloadSize - offset);
			offset += currentRead;
		}
		
		//System.out.println("Read: " + this.payload.length + " type " + this.command);
	}

	protected void setPayload(byte[] messagePayload) {
		this.payload = messagePayload;
	}
	
	public byte[] getPayload(){
		return this.payload;
	}

	public byte[] getBytes() {
		int payloadSize = -1;
		byte[] outBytes = null;

		if (this.payload == null) {
			payloadSize = 0;
		} else {
			payloadSize = this.payload.length;
		}
		outBytes = new byte[CommonMessage.HEADERSIZE + payloadSize];

		ByteOps.appendBytes(CommonMessage.MAGIC, outBytes, 0);
		ByteOps.appendBytes(this.command.getBytes(), outBytes, 4);
		ByteOps.appendBytes(CommonStructures.fixedSmallEndianInt(payloadSize), outBytes, 4 + 12);
		ByteOps.appendBytes(this.buildCheckSum(), outBytes, 4 + 12 + 4);

		if (this.payload != null) {
			ByteOps.appendBytes(this.payload, outBytes, 4 + 12 + 4 + 4);
		}

		return outBytes;
	}
	
	public String getCommand(){
		return this.command;
	}

	private byte[] buildCheckSum() {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		byte[] firstHash = null;
		if (this.payload == null) {
			firstHash = md.digest();
		} else {
			firstHash = md.digest(this.payload);
		}
		md.reset();
		byte[] secondHash = md.digest(firstHash);

		return ByteOps.subArray(secondHash, 4, 0);
	}



}
