package net;

import java.net.*;
import java.io.*;

import passiveMonitor.PassiveListener;

public class ConnectionListener implements Runnable {

	private PassiveListener parent;
	private ServerSocket mySock;

	public ConnectionListener(PassiveListener myParent) throws IOException {
		this.parent = myParent;
		this.mySock = new ServerSocket(Constants.DEFAULT_PORT);
	}

	@Override
	public void run() {

		try {
			while (true) {
				Socket incConn = this.mySock.accept();
				this.parent.reportNewNode(new Node(incConn));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
