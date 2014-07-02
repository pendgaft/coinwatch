package passiveMonitor;

import java.net.*;

import data.Contact;
import net.*;

public class PassiveTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws UnknownHostException, InterruptedException{
		// TODO Auto-generated method stub

		Constants.initConstants();
		Node testNode = new Node(new Contact(InetAddress.getByName("127.0.0.1"), Constants.DEFAULT_PORT));
		if(testNode.connect()){
			System.out.println("Conn good!");
			Thread.sleep(30000);
		}else{
			System.out.println("Conn bad.");
		}
	}
}
