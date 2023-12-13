package minesweeper.solver.bulk;

public class StaticCounter {

	public enum SCType {
		LONG_5050 (0, "Long 50/50")
		;
		
		final public String name;
		final private int index;
		
		private SCType(int index, String name) {
			this.index = index;
			this.name = name;
		}
	}
	
	private final static int[] counter = new int[SCType.values().length];
	
	private StaticCounter() {
	}

	final static public void count(SCType type) {
		counter[type.index]++;
	}

	
	final static public int report(SCType type) {
		return counter[type.index];
	}
	
	
}
