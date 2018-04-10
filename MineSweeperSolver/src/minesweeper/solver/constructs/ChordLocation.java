package minesweeper.solver.constructs;

import java.util.Comparator;

import minesweeper.gamestate.Location;

public class ChordLocation extends Location {

	final int benefit;
	
	public ChordLocation(int x, int y, int benefit) {
		super(x, y);

		this.benefit = benefit;
		
	}
	
	
	public int getBenefit() {
		return this.benefit;
	}
	
	static public final Comparator<ChordLocation> SORT_BY_BENEFIT_DESC  = new Comparator<ChordLocation>() {
		@Override
		public int compare(ChordLocation o1, ChordLocation o2) {
			return o2.benefit - o1.benefit;
		}
	};
	
	
}
