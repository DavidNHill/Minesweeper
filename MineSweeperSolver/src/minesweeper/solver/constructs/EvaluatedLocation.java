package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.Solver;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class EvaluatedLocation extends Location {
	
	private final BigDecimal clearProbability;
	private BigDecimal progressProbability;
	private final BigDecimal maxValueProgress;
	private String description = "";
	private BigDecimal expectedClears;
	private final int fixedClears;  //number of tiles which are clears regardless of what value is revealed
	private List<Box> emptyBoxes;
	
	
	private BigDecimal weighting; 

	public EvaluatedLocation(int x, int y, BigDecimal clearProbability, BigDecimal progressProbability, BigDecimal expectedClears, int fixedClears, 
			List<Box> emptyBoxes, BigDecimal maxValueProgress) {
		super(x,y);
		
		this.clearProbability = clearProbability;
		//this.progressProbability = progressProbability.divide(clearProbability, Solver.DP, RoundingMode.HALF_UP);
		//this.progressProbability = progressProbability.multiply(clearProbability);
		this.progressProbability = progressProbability;
		this.expectedClears = expectedClears;
		this.fixedClears = fixedClears;
		this.maxValueProgress = maxValueProgress;
		this.emptyBoxes = emptyBoxes;
		
		calculateWeighting();
		
	}
	
	public void merge(EvaluatedLocation link) {
		
		expectedClears = this.expectedClears.add(link.expectedClears);
		
		BigDecimal pp1 = BigDecimal.ONE.subtract(this.progressProbability);
		BigDecimal pp2 = BigDecimal.ONE.subtract(link.progressProbability);
		
		this.progressProbability = BigDecimal.ONE.subtract(pp1.multiply(pp2));
		
		calculateWeighting();
		
	}
	
	private void calculateWeighting() {
		
		BigDecimal bonus = BigDecimal.ONE.add(progressProbability.multiply(Solver.PROGRESS_VALUE));
		
		this.weighting = this.clearProbability.multiply(bonus);
		
		//this.weighting = this.progressProbability.divide(BigDecimal.ONE.subtract(this.clearProbability), Solver.DP, RoundingMode.HALF_UP);
	}
	
	public void mergeOld(EvaluatedLocation link) {
		
		expectedClears = this.expectedClears.add(link.expectedClears);
		
	}
	
	public BigDecimal getProbability() {
		return this.clearProbability;
	}
	
	public BigDecimal getWeighting() {
		return this.weighting;
	}
	
	public BigDecimal getMaxValueProgress() {
		return maxValueProgress;
	}
	
	public List<Box> getEmptyBoxes() {
		return emptyBoxes;
	}
	
	
	public Action buildAction(MoveMethod method) {
		
        String comment = description;
        
        return new Action(this, Action.CLEAR, method, comment, clearProbability);		
		
	}
	
	@Override
	public String display() {
		
		return super.display() + " Fixed clears is " + fixedClears + " expected clears is " + expectedClears.toPlainString() 
		+ ", progress prob is " + progressProbability + ", final weight is " + weighting + ", maximum tile value prob is " + maxValueProgress;
		
	}
	
	/**
	 * This sorts by ...
	 */
	static public final Comparator<EvaluatedLocation> SORT_BY_WEIGHT  = new Comparator<EvaluatedLocation>() {
		@Override
		public int compare(EvaluatedLocation o1, EvaluatedLocation o2) {
			

			int c = 0;
			
			if (c == 0) {
				c = -o1.weighting.compareTo(o2.weighting);  // tile with the highest weighting
			}


			if (c == 0) {
				c = -o1.expectedClears.compareTo(o2.expectedClears);  // then highest expected number of clears
			}
			
			return c;
		
		}
	};
	
	static public final Comparator<EvaluatedLocation> SORT_BY_SAFETY_MINIMAX  = new Comparator<EvaluatedLocation>() {
		@Override
		public int compare(EvaluatedLocation o1, EvaluatedLocation o2) {
			

			int c = 0;
			
			c = -o1.clearProbability.compareTo(o2.clearProbability);  // safest tiles

			if (c == 0) {
				c = o1.maxValueProgress.compareTo(o2.maxValueProgress);  // then lowest max value ... the Minimax;
			}
		
			// go back to the weight option
			if (c == 0) {
				c = SORT_BY_WEIGHT.compare(o1, o2);
			}
			
			return c;
		
		}
	};
	
	/*
	static public final Comparator<EvaluatedLocation> SORT_BY_PROGRESS_PROBABILITY  = new Comparator<EvaluatedLocation>() {
		@Override
		public int compare(EvaluatedLocation o1, EvaluatedLocation o2) {
			

			int c = 0;
			
			c = -o1.progressProbability.compareTo(o2.progressProbability);  // highest probability of making progress first

			if (c == 0) {
				c = -o1.expectedClears.compareTo(o2.expectedClears);  // then highest expected number of clears
			}
			
			// avoid playing at a corner if tied
			if (c == 0) {
				if (o1.isCorner && !o2.isCorner) {
					c = 1;
				} else if (!o1.isCorner && o2.isCorner) {
					c = -1;
				}
			}
			return c;
		
		}
	};
	
	static public final Comparator<EvaluatedLocation> SORT_BY_LINKED_EXPECTED_CLEARS  = new Comparator<EvaluatedLocation>() {
		@Override
		public int compare(EvaluatedLocation o1, EvaluatedLocation o2) {
			
			int c = 0;
			
			if (o1.fixedClears == 0 && o2.fixedClears > 0) {
				c = 1;
			} else if (o1.fixedClears > 0 && o2.fixedClears == 0) {
				c = -1;
			}

			if (c == 0) {
				c = -o1.expectedClears.compareTo(o2.expectedClears);
			}
			
			
			return c;
		
		}
	};
	*/
	
}
