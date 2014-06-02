package experiment;

import java.util.Collection;
import java.util.concurrent.*;

import net.Node;

public class ExperimentContainer {
	
	private ConcurrentLinkedQueue<Node> incompleteWork;
	private Semaphore incompleteWorkCount;
	private ConcurrentLinkedQueue<Node> completedWork;
	private Semaphore completedWorkCount;
	
	public ExperimentContainer(){
		this.incompleteWork = new ConcurrentLinkedQueue<Node>();
		this.incompleteWorkCount = new Semaphore(0);
		this.completedWork = new ConcurrentLinkedQueue<Node>();
		this.completedWorkCount = new Semaphore(0);
	}
	
	public void nodeReadyToWork(Node incNode){
		this.incompleteWork.add(incNode);
		this.incompleteWorkCount.release();
	}
	
	public void nodesReadyToWork(Collection<Node> nodeCollection){
		for(Node tNode: nodeCollection){
			this.nodeReadyToWork(tNode);
		}
	}
	
	public Node fetchWork() throws InterruptedException{
		this.incompleteWorkCount.acquire();
		return this.incompleteWork.poll();
	}

	public void returnCompletedNode(Node incNode){
		this.completedWork.add(incNode);
		this.completedWorkCount.release();
	}
	
	public Node fetchCompleted() throws InterruptedException{
		this.completedWorkCount.acquire();
		return this.completedWork.poll();
	}
}
