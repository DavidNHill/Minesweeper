package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import minesweeper.structure.Location;

public class ChordLocation extends Location {

	private final int benefit;
	private final int cost;
	private final int exposedTiles;
	private final BigDecimal netBenefit;
	private final BigDecimal scale;
	private final List<Location> mines;
	
	public ChordLocation(int x, int y, int benefit, int cost, int exposedTiles, BigDecimal scale, List<Location> mines) {
		super(x, y);

		this.benefit = benefit;
		this.cost = cost;
		this.exposedTiles = exposedTiles;
		 
		this.netBenefit = chordReward(benefit, cost).multiply(scale);
		//this.netBenefit = chordReward(benefit, cost);
		
		this.scale = scale;  // probability of being a mine
		this.mines = mines;
	}
	
	public int getBenefit() {
		return this.benefit;
	}
	
	public int getCost() {
		return this.cost;
	}
	
	public BigDecimal getNetBenefit() {
		return this.netBenefit;
	}
	
	public int getExposedTileCount() {
		return this.exposedTiles;
	}
	
	public List<Location> getMines() {
		return this.mines;
	}
	
	public BigDecimal getScale() {
		return this.scale;
	}
	
	static public final BigDecimal chordReward(int benefit, int cost) {
		
		BigDecimal netBenefit;    // benefit as a ratio of the cost
		
		/*
		if (cost == 0) {
			netBenefit = BigDecimal.valueOf(benefit);    
		} else {
			netBenefit = BigDecimal.valueOf(benefit - cost).divide(BigDecimal.valueOf(cost), 10, RoundingMode.HALF_UP);    // benefit as a ratio of the cost
		}
		*/
		
		/*
		if (cost == 0) {
			netBenefit = BigDecimal.valueOf(benefit);    
		} else {
			netBenefit = BigDecimal.valueOf(benefit).divide(BigDecimal.valueOf(cost), 10, RoundingMode.HALF_UP);    // benefit as a ratio of the cost
		}
		*/
		
		netBenefit = BigDecimal.valueOf(benefit - cost);   // absolute benefit without regard for the cost
		
		return netBenefit;
		
	}
	
	static public final Comparator<ChordLocation> SORT_BY_BENEFIT_DESC  = new Comparator<ChordLocation>() {
		@Override
		public int compare(ChordLocation o1, ChordLocation o2) {
			
			int c = o2.netBenefit.compareTo(o1.netBenefit);
			
			if (c==0) {
				c = o2.exposedTiles - o1.exposedTiles;
			}
			
			if (c==0) {
				c = o2.scale.compareTo(o1.scale);
			}
			
			return c;
			
			//if (o2.netBenefit == o1.netBenefit) {
			//	return o2.exposedTiles - o1.exposedTiles;
			//} else {
			//	return o2.netBenefit.compareTo(o1.netBenefit);
			//}

		}
	};
	
	
}
