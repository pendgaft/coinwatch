package experiment;

import java.util.*;

public class WallingExperimentContainer<T, E> extends ExperimentContainer<T, E> {

	/*
	 * These variables do not need to be thread safe so long as only ONE thread
	 * is ADDING work to the queue (multiple worker threads can still access as
	 * per usual).
	 */
	private int workInQueue;
	private boolean okToDrain;
	private Queue<E> completedWorkCollection;

	public WallingExperimentContainer() {
		super();
		this.workInQueue = 0;
		this.okToDrain = false;
	}

	public void nodeReadyToWork(T incNode) {
		super.nodeReadyToWork(incNode);
		this.workInQueue++;
	}

	public void nodesReadyToWork(Collection<T> nodeCollection) {
		super.nodesReadyToWork(nodeCollection);
		this.workInQueue += nodeCollection.size();
	}

	public E fetchCompleted() throws InterruptedException {
		if (!this.okToDrain) {
			/*
			 * Poll the parent until we have all the work we placed into the
			 * queue
			 */
			while (this.completedWorkCollection.size() < this.workInQueue) {
				this.completedWorkCollection.add(super.fetchCompleted());
			}
			this.okToDrain = true;
			this.workInQueue = 0;
		}

		E workToReturn = this.completedWorkCollection.poll();
		if (this.completedWorkCollection.size() == 0) {
			this.okToDrain = false;
		}
		return workToReturn;
	}

}
