package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
	
	private List<ByValue> byValues;
	
	public InformationLocation(int x, int y) {
		super(x, y);
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
	
	public List<ByValue> getByValueData() {
		return this.byValues;
	}
	
}
