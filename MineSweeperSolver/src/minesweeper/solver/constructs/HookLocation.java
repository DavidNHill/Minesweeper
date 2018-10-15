package minesweeper.solver.constructs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HookLocation extends ProbabilityLocation {

	public final static int NO_DECISION = -1;
	public final static int REJECTED = -2;
	public final static int OKAY = -3;
	
	
	private int state = NO_DECISION;
	
	private int value = 0;  // this is the value the location has from the less obvious scan.

	private List<Integer> allValues = new ArrayList<>();
	
	private final int adjacentFlags;
	private final int adjHidden;  // number of unrevealed locations around this one
	
	public HookLocation(int x, int y, int adjacentFlags, int adjHidden) {
		super(x, y);

		this.adjacentFlags = adjacentFlags;
		this.adjHidden = adjHidden;
	}
	
	public void setValue(int value) {
		this.value = value + this.adjacentFlags;
		allValues.add(this.value);
	}
	
	public int getValue() {
		return this.value;
	}
	
	public int getExtraValue() {
		return this.value - this.adjacentFlags;
	}
	
	public int getSize() {
		return this.adjHidden;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public int getState() {
		return this.state;
	}
	
	/**
	 * Takes another hook if it doesn't have the same location then an error is thrown
	 * adds the 'value' of the hook to the value array.
	 * @param hook
	 */
	public void merge(HookLocation hook) {
		
		if (!hook.equals(this)) {
			throw new RuntimeException("Cannot merge Hooks for different locations");
		}
		
		for (int i: allValues) {
			if (i == hook.value) {
				return;  // already storing this value
			}
		}
		
		allValues.add(hook.value);
		
	}
	
	@Override
	public String display() {
		
		String values = "";
		for (int i=0; i < allValues.size(); i++) {
			values = values + allValues.get(i) + " ";
		}
		
		return "Hook: " + super.display() + " value(s) " + values + " size " + adjHidden;
	}

	@Override
	public int[] getAdjacentFlagsRequired() {
		
		int[] result = new int[this.allValues.size()];
		for (int i=0; i < allValues.size(); i++) {
			result[i] = allValues.get(i);
		}
		
		return result;
	}
	
	/**
	 * This sorts by the number of adjacent flags
	 */
	static public final Comparator<HookLocation> SORT_BY_ADJ_HIDDEN  = new Comparator<HookLocation>() {
		@Override
		public int compare(HookLocation o1, HookLocation o2) {
			
			int c = 0;
			
			c = o1.adjHidden - o2.adjHidden;
			
			return c;
		
		}
	};

}
