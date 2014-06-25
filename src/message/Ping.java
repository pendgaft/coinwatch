package message;

import net.Constants;

public class Ping extends CommonMessage {

	private byte[] nonce;

	public Ping(byte[] data) {
		super(Constants.PING_CMD);
		this.nonce = data;
	}

	public Ping() {
		super(Constants.PING_CMD);
		byte[] nonce = new byte[8];
		Constants.NON_SEC_RNG.nextBytes(nonce);
		this.setPayload(this.nonce);
	}

	public Pong buildPong() {
		return new Pong(this.nonce);
	}
}
