package message;

import util.ByteOps;
import net.Constants;

public class Pong extends CommonMessage {

	private byte[] nonce;

	public Pong(byte[] data) {
		super(Constants.PONG_CMD);
		this.nonce = data;
	}
	
	public String getNonceStr(){
		return ByteOps.bytesToHex(this.nonce);
	}

}
