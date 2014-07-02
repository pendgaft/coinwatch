package experiment;

import java.util.Collection;
import java.util.concurrent.*;

//TODO move to data package
public class ExperimentContainer<T, E> {
	
	private ConcurrentLinkedQueue<T> incompleteWork;
	private Semaphore incompleteWorkCount;
	private ConcurrentLinkedQueue<E> completedWork;
	private Semaphore completedWorkCount;
	
	public ExperimentContainer(){
		this.incompleteWork = new ConcurrentLinkedQueue<T>();
		this.incompleteWorkCount = new Semaphore(0);
		this.completedWork = new ConcurrentLinkedQueue<E>();
		this.completedWorkCount = new Semaphore(0);
	}
	
	public void nodeReadyToWork(T incNode){
		this.incompleteWork.add(incNode);
		this.incompleteWorkCount.release();
	}
	
	public void nodesReadyToWork(Collection<T> nodeCollection){
		for(T tNode: nodeCollection){
			this.nodeReadyToWork(tNode);
		}
	}
	
	public T fetchWork() throws InterruptedException{
		this.incompleteWorkCount.acquire();
		return this.incompleteWork.poll();
	}

	public void returnCompletedNode(E incNode){
		this.completedWork.add(incNode);
		this.completedWorkCount.release();
	}
	
	public E fetchCompleted() throws InterruptedException{
		this.completedWorkCount.acquire();
		return this.completedWork.poll();
	}
}
