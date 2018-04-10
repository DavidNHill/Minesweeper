package minesweeper.solver.constructs;

import java.math.BigDecimal;

public class ZeroLocation extends ProbabilityLocation {

	public ZeroLocation(int x, int y, BigDecimal prob) {
		super(x, y);
		
		this.setProbability(prob);
	}
	
	@Override
	public int[] getAdjacentFlagsRequired() {
		return null;
	}

}
