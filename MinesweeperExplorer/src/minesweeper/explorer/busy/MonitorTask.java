package minesweeper.explorer.busy;

import minesweeper.solver.utility.ProgressMonitor;

public class MonitorTask implements Runnable {
	
	private final ProgressMonitor progress;
	private final BusyController controller;
	private boolean running = true;
	
	public MonitorTask(ProgressMonitor progress, BusyController controller) {
		this.progress = progress;
		this.controller = controller;
	}

	@Override
	public void run() {
		
		while (running) {
			
			controller.update(progress);
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				running = false;
				break;
			}
		}

		System.out.println("Monitor task ending");
	}
	
	protected void stop() {
		running = false;
	}

	
}
