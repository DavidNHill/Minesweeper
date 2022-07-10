package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.Solver;
import minesweeper.structure.Location;

public class InformationLocation extends Location {

	public final static BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	
	public class ByValue {
		
		public final int value;
		public final int clears;
		public final BigDecimal probability;
		
		private ByValue(int value, int clears, BigDecimal prob) {
			this.value = value;
			this.clears = clears;
			this.probability = prob;
		}
		
	}
	
	
	private BigDecimal prob;
	private BigDecimal expectedClears;
	private BigDecimal progressProbability;
	private BigDecimal weighting;
	private BigDecimal expectedSolutionSpaceReduction;
	private BigDecimal mTanzerRatio;
	private BigDecimal secondarySafety;
	
	private BigDecimal longTermSafety;
	//private BigDecimal poweredRatio;
	
	
	private List<ByValue> byValues;
	
	public InformationLocation(int x, int y) {
		super(x, y);
	}

	public void calculate() {
		
		BigDecimal expClears = BigDecimal.ZERO;
		BigDecimal progProb = BigDecimal.ZERO;
		BigDecimal essr = BigDecimal.ZERO;
		BigDecimal powerClears = BigDecimal.ZERO;
		
		
		if (byValues == null) {
			return;
		}
		
		for (ByValue bv: byValues) {
			
			//essr = essr.add(bv.probability.multiply(BigDecimal.ONE.subtract(bv.probability)));  // sum of p(1-p)
			essr = essr.add(bv.probability.multiply(bv.probability));  // sum of p^2
			
			if (bv.clears != 0) {
				progProb = progProb.add(bv.probability);
				expClears = expClears.add(bv.probability.multiply(BigDecimal.valueOf(bv.clears)));
				//powerClears = powerClears.add(bv.probability.multiply(new BigDecimal(Math.pow(1.15d, bv.clears))));
			}
			
		}
		
		this.expectedClears = expClears;
		this.progressProbability = progProb;
		this.expectedSolutionSpaceReduction = essr;
		
		BigDecimal bonus = BigDecimal.ONE.add(progressProbability.multiply(Solver.PROGRESS_VALUE));
		
		this.weighting = this.prob.multiply(bonus);
		
		if (this.prob.compareTo(BigDecimal.ONE) != 0) {
			this.mTanzerRatio = this.progressProbability.divide(BigDecimal.ONE.subtract(prob), 4, RoundingMode.HALF_DOWN);
		}

		//if (this.prob.compareTo(BigDecimal.ONE) != 0) {
		//	this.poweredRatio = powerClears.divide(BigDecimal.ONE.subtract(prob), 4, RoundingMode.HALF_DOWN);
		//}
		
	}
	
	public BigDecimal getProbability() {
		return this.prob;
	}

	public void setProbability(BigDecimal prob) {
		this.prob = prob;
	}
	
	public void setByValue(int value, int clears, BigDecimal prob) {
		if (byValues == null) {
			byValues = new ArrayList<>();
		}
		
		byValues.add(new ByValue(value, clears, prob));
		
	}
	
	public void setSecondarySafety(BigDecimal safety2) {
		this.secondarySafety = safety2;
	}
	
	public void setLongTermSafety(BigDecimal lts) {
		this.longTermSafety = lts;
	}
	
	public BigDecimal getSecondarySafety() {
		return this.secondarySafety;
	}
	
	public BigDecimal getLongTermSafety() {
		return this.longTermSafety;
	}
	
	public List<ByValue> getByValueData() {
		return this.byValues;
	}

	public BigDecimal getExpectedClears() {
		return expectedClears;
	}

	public BigDecimal getProgressProbability() {
		return progressProbability;
	}

	public BigDecimal getWeighting() {
		return weighting;
	}
	
	public BigDecimal getExpectedSolutionSpaceReduction() {
		return this.expectedSolutionSpaceReduction;
	}
	
	public BigDecimal getMTanzerRatio() {
		return this.mTanzerRatio;
	}
	
	//public BigDecimal getPoweredRatio() {
	//	return this.poweredRatio;
	//}
}
