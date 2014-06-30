package message;

import util.ByteOps;
import net.Constants;

public class Ping extends CommonMessage {

	private byte[] nonce;

	public Ping(byte[] data) {
		super(Constants.PING_CMD);
		this.nonce = data;
	}

	public Ping() {
		super(Constants.PING_CMD);
		this.nonce = new byte[8];
		Constants.NON_SEC_RNG.nextBytes(this.nonce);
		this.setPayload(this.nonce);
	}
	
	public String getNonceStr(){
		return ByteOps.bytesToHex(this.nonce);
	}

	public Pong buildPong() {
		return new Pong(this.nonce);
	}
}
