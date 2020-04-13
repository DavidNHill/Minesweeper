package minesweeper.solver.utility;

public class ProgressMonitor {
	
	private String step;
	private int maxProgress;
	private int progress;
	
	public void SetMaxProgress(String step, int max) {
		this.step = step;
		this.maxProgress = max;
		this.progress = 0;
	}

	public int getMaxProgress() {
		return this.maxProgress;
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
	}
	
	public int getProgress() {
		return this.progress;
	}
	
}
