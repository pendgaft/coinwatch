package zmap;

import java.io.*;
import java.net.*;

public class ZmapServer implements Runnable {

	private ServerSocket incSocket;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		ZmapServer self = new ZmapServer();
		self.run();
	}

	public ZmapServer() throws IOException {
		this.incSocket = new ServerSocket(ZmapSupplicant.DEFAULT_ZMAP_PORT);
	}

	public void run() {
		try {
			while (true) {
				Socket incConn = incSocket.accept();
				System.out.println("Recieved connection from: " + incConn.getInetAddress());
				ZmapThread child = new ZmapThread(incConn);
				Thread tThread = new Thread(child);
				tThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			this.incSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
