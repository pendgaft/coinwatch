package data;

import net.Node;

public class NodeBooleanPair {

	private Node myNode;
	private boolean boolValue;
	
	
	public NodeBooleanPair(Node node, boolean bool){
		this.myNode = node;
		this.boolValue = bool;
	}
	
	public Node getNode(){
		return this.myNode;
	}
	
	public boolean getBoolean(){
		return this.boolValue;
	}
}
