package net;

import java.io.*;

import message.CommonMessage;

public class IncomingParser implements Runnable {

	private Node parent;
	private InputStream iStream;

	public IncomingParser(Node owningNode, InputStream input) {
		this.parent = owningNode;
		this.iStream = input;
	}

	public void run() {
		try {
			while (true) {
				CommonMessage incMessage = new CommonMessage(this.iStream);
				String cmdStr = incMessage.getCommand();
				
				if(cmdStr.equals(Constants.VERSION_CMD)){
					//System.out.println("handing version");
					this.parent.recievedVersion();
				}else if(cmdStr.equals(Constants.VERACK_CMD)){
					//System.out.println("handing version ack");
					this.parent.recievedVerAck();
				} else if(cmdStr.equals(Constants.ADDR_CMD)){
					this.parent.recievedAddr(incMessage.getPayload());
				}
				else{
					//TODO handle bad command
				}
			}
		} catch (IOException e) {
			this.parent.reportIOError();
		}
	}

}
