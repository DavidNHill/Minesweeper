package minesweeper.solver.utility;

public class Logger {
	
	/**
	 * Nothing gets logged
	 */
	public final static Logger NO_LOGGING = new Logger(Level.NONE);
	
	public enum Level {
		DEBUG(1),
		INFO(2),
		WARN(3),
		ERROR(4),
		NONE(99);
		
		private int value;
		private Level(int value) {
			this.value = value;
		}
	}
	
	private final String logName;
	private final Level logLevel;
	private final String prefix;
	
	public Logger(Level level) {
		this(level, "");
	}

	public Logger(Level level, String logName) {
		this.logLevel = level;
		this.logName = logName;
		if (this.logName.isEmpty()) {
			this.prefix = this.logLevel + " ";
		} else {
			this.prefix = this.logLevel + " " + this.logName + " ";
		}

	}
	
	public void log(Level level, String format, Object... parms) {
		
		if (level.value < logLevel.value) {
			return;
		}
		
		String output;
		try {
			output = String.format(format, parms);
			
		} catch (Exception e) {  // if it goes wrong show the unformated information
			StringBuilder sb = new StringBuilder();
			sb.append(format);
			sb.append(" Parms:");
			for (Object parm: parms) {
				sb.append("[");
				sb.append(parm);
				sb.append("]");
			}
			output = sb.toString();
		}
		
		System.out.println(prefix + output);
		
	}
	
}
