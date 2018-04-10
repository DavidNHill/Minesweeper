package minesweeper.solver.constructs;

import java.math.BigDecimal;

import minesweeper.gamestate.Location;

abstract public class ProbabilityLocation extends Location {

	private BigDecimal prob = BigDecimal.ZERO;
	private boolean probKnown = false;
	
	public ProbabilityLocation(int x, int y) {
		super(x, y);

	}

	public void setProbability(BigDecimal prob) {
		this.prob = prob;
		this.probKnown = true;
	}
	
	public BigDecimal getProbability() {
		return this.prob;
	}

	public boolean probablityKnown() {
		return probKnown;
	}
	
	public String getProbabilityString() {
		
		if (probKnown) {
			return prob.toPlainString();
		} else {
			return "unknown";
		}		
	}

	
	
	abstract public int[] getAdjacentFlagsRequired(); 
	
}
