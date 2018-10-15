package minesweeper.solver.constructs;

import java.math.BigDecimal;

import minesweeper.gamestate.Action;
import minesweeper.gamestate.Location;

abstract public class ProbabilityLocation extends Location {

	public final static BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	
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
			return "[unknown]";
		}		
	}

	public String getPercentageString() {
		
		if (probKnown) {
			return Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED)) + "%";
		} else {
			return "[unknown]";
		}		
	}
	
	
	
	abstract public int[] getAdjacentFlagsRequired(); 
	
}
