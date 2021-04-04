package minesweeper.solver.utility;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Timer {

	private final static NumberFormat FORMAT = new DecimalFormat("###0.000");
	
	private long start;
	private boolean running = false;
	private long duration = 0;
	private final String label;
	
	public Timer(String label) {
		
		this.label = label;
		
	}
	
	public Timer start() {
		if (running) {  // if already started then ignore
			return this;
		}
		start = System.nanoTime();
		running = true;
		return this;
	}
	
	public Timer stop() {
		if (!running) { // if not running then ignore
			return this;
		}
		running = false;
		duration = duration + System.nanoTime() - start;
		return this;
	}
	
	/**
	 * Return the duration in milliseconds
	 * @return
	 */
	public double getDuration() {
		
		long result;
		
		if (running) {
			result = duration + System.nanoTime() - start;
		} else {
			result = duration;
		}
		
		double milli = (double) result / 1000000d;
		
		return milli;
	}
	
	@Override
	public String toString() {
		return label + " " + FORMAT.format(getDuration()) + " milliseconds";
	}
	
}
