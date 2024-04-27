package minesweeper.solver.constructs;

import minesweeper.structure.Location;

public class LocationValue extends Location {


	private final int value;
	
	public LocationValue(Location loc, int value) {
		super(loc.x, loc.y);
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
	
}
