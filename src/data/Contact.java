package data;

import java.io.Serializable;
import java.net.*;

import net.Node;

public class Contact implements Serializable{

	private InetAddress myIp;
	private int myPort;
	private long myTimeStamp;
	private boolean lastSeenDirect;

	public Contact(InetAddress ip, int port, long timeStamp, boolean direct) {
		this.myIp = ip;
		this.myPort = port;
		this.myTimeStamp = timeStamp;
		this.lastSeenDirect = direct;
	}
	
	public Contact(InetAddress ip, int port){
		this.myIp = ip;
		this.myPort = port;
		this.myTimeStamp = 0;
		this.lastSeenDirect = false;
	}
	
	public Node getNodeForContact(){
		return new Node(this);
	}

	public void updateTimeStamp(long ts, boolean direct){
		this.myTimeStamp = ts;
		this.lastSeenDirect = direct;
	}
	
	public InetAddress getIp() {
		return myIp;
	}
	
	public boolean isIPv6(){
		return this.myIp instanceof Inet6Address;
	}

	public int getPort() {
		return myPort;
	}

	public long getLastSeen() {
		return myTimeStamp;
	}

	public boolean isLastSeenDirect() {
		return lastSeenDirect;
	}
	
	public void setLastSeenDirect(boolean value){
		this.lastSeenDirect = value;
	}
	
	public int hashCode(){
		return this.myIp.hashCode() + this.myPort;
	}
	
	public boolean equals(Object rhs){
		Contact rhsContact = (Contact)rhs;
		return this.myIp.equals(rhsContact.myIp) && this.myPort == rhsContact.myPort;
	}
	
	public String toString(){
		return this.myIp.toString() + ":" + this.myPort;
	}
	
	public String getLoggingString(){
		return this.toString() + "," + this.myTimeStamp + "," + this.lastSeenDirect;
	}
}
