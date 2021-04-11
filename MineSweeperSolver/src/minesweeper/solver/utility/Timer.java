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
	
	static public String humanReadable(long ms) {
		
		long milliseconds = ms % 1000;
		long rem = (ms - milliseconds) / 1000;
		long seconds = rem % 60;
		rem = (rem - seconds) / 60;
		long minutes = rem % 60;
		long hours = (rem - minutes) /60;
		
		String result; 
		if (hours > 0) {
			result = hours(hours) + " " + minutes(minutes);
		} else if (minutes > 0) {
			result = minutes(minutes) + " " + seconds(seconds);
		} else if (seconds > 0){
			result = seconds(seconds);
		} else {
			result = "< 1 second";
		}
		
		return result;
	}

	static private String hours(long hours) {
		if (hours == 1) {
			return "1 hour";
		} else { 
			return hours + " hours";
		}
	}
	
	static private String minutes(long minutes) {
		
		if (minutes == 0) {
			return "";
		} else if (minutes == 1) {
			return "1 minute";
		} else { 
			return minutes + " minutes";
		}
	}
	
	static private String seconds(long seconds) {
		
		if (seconds == 0) {
			return "";
		} else if (seconds == 1) {
			return "1 second";
		} else { 
			return seconds + " seconds";
		}
	}
	
	@Override
	public String toString() {
		return label + " " + FORMAT.format(getDuration()) + " milliseconds";
	}
	
}
