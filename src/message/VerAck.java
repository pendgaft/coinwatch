package message;

import util.ByteOps;

public class VerAck extends CommonMessage{

	public VerAck(){
		super("verack");
	}
	
	public static void main(String args[]){
		VerAck self = new VerAck();
		byte[] theBytes = self.getBytes();
		System.out.println(ByteOps.bytesToHex(theBytes));
	}
}
