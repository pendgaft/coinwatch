package message;

import net.Constants;
import data.Contact;

import java.util.*;
import java.net.*;

import util.ByteOps;

public class Addr extends CommonMessage{
	
	private int numberOfRecords;
	private List<Contact> addresses;
	
	public Addr(){
		super(Constants.ADDR_CMD);
		
		this.numberOfRecords = 0;
		this.addresses = new ArrayList<Contact>();
	}
	
	public Addr(byte[] data){
		super(Constants.ADDR_CMD);
		
		this.numberOfRecords = (int)CommonStructures.extractVarInt(data);
		int recordSizeOffset = CommonStructures.getVarIntSize(data);
		
		this.addresses = new ArrayList<Contact>(this.numberOfRecords);
		byte[] addressBytes = ByteOps.subArray(data, data.length - recordSizeOffset, recordSizeOffset);
		if(addressBytes.length != this.numberOfRecords * 30){
			System.err.println("Invalid data block size for Addr: " + this.numberOfRecords + " " + addressBytes.length);
		}else{
			this.extractAddresses(addressBytes);
		}
	}
	
	private void extractAddresses(byte[] data){
		for(int counter = 0; counter < this.numberOfRecords; counter++){
			byte[] dataSlice = ByteOps.subArray(data, 30, 30 * counter);
			this.addresses.add(CommonStructures.extractNetAddress(dataSlice));
		}
	}

	public List<Contact> getLearnedContacts(){
		return this.addresses;
	}
}
