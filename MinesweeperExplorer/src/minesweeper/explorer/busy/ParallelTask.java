package minesweeper.explorer.busy;

import java.util.concurrent.CountDownLatch;


public abstract class ParallelTask<T> implements Runnable {
	
	private CountDownLatch executionCompleted = new CountDownLatch(1);
	private BusyController controller;

	@Override
	public void run() {
		doExecute();
		executionCompleted.countDown();
		controller.finished();
	}
	
	abstract public void doExecute();
	
	public void await() {
		try {
			executionCompleted.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		controller.finished();
		
	}

	abstract public T getResult();
	
	abstract public int getMaxProgress();
	abstract public int getProgress();
	
	protected void setController(BusyController controller) {
		this.controller = controller;
	}
	
}
