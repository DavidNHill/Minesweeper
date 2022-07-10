package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import minesweeper.gamestate.MoveMethod;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class EvaluatedLocation extends Location {
	
	private final BigDecimal safeProbability;
	private BigDecimal weight;
	private final BigDecimal maxValueProgress;
	private String description = "";
	private BigDecimal expectedClears;
	private final int fixedClears;  //number of tiles which are clears regardless of what value is revealed
	private List<Box> emptyBoxes;
	private boolean pruned = false;
	private boolean deferGuessing = false;

	public EvaluatedLocation(int x, int y, BigDecimal safeProbability, BigDecimal weight, BigDecimal expectedClears, int fixedClears, 
			List<Box> emptyBoxes, BigDecimal maxValueProgress) {
		super(x,y);
		
		this.safeProbability = safeProbability;
		this.weight = weight.setScale(8, RoundingMode.UP); // give a slight bump up, so those coming later have to be actually better
		this.expectedClears = expectedClears;
		this.fixedClears = fixedClears;
		this.maxValueProgress = maxValueProgress;
		this.emptyBoxes = emptyBoxes;
	
	}
	
	public BigDecimal getProbability() {
		return this.safeProbability;
	}
	
	public BigDecimal getWeighting() {
		return this.weight;
	}
	
	public BigDecimal getMaxValueProgress() {
		return maxValueProgress;
	}
	
	public List<Box> getEmptyBoxes() {
		return emptyBoxes;
	}
	
	
	public Action buildAction(MoveMethod method) {
		
        String comment = description;
        
        return new Action(this, Action.CLEAR, method, comment, safeProbability);		
		
	}
	
	public void setPruned() {
		this.pruned = true;
	}
	
	public void setDeferGuessing(boolean deferGuessing) {
		this.deferGuessing = deferGuessing;
	}
	
	public boolean isDeferGuessing() {
		return this.deferGuessing;
	}
	
	@Override
	public String toString() {
		
		String prunedString;
		if (this.pruned) {
			prunedString = "  ** Pruned";
		} else {
			prunedString = "";
		}
		
		return super.toString() + " Fixed clears is " + fixedClears + " expected clears is " + expectedClears.toPlainString() 
		+ ", final weight is " + weight + ", maximum tile value prob is " + maxValueProgress + ", defer guessing " + deferGuessing + prunedString;
		
	}
	
	/**
	 * This sorts by ...
	 */
	static public final Comparator<EvaluatedLocation> SORT_BY_WEIGHT  = new Comparator<EvaluatedLocation>() {
		@Override
		public int compare(EvaluatedLocation o1, EvaluatedLocation o2) {
			

			int c = 0;
			
			if (c == 0) {
				if (o1.deferGuessing && !o2.deferGuessing) {
					c = 1;
				} else if (!o1.deferGuessing && o2.deferGuessing) {
					c = -1;
				} 
			}
			
			if (c == 0) {
				c = -o1.weight.compareTo(o2.weight);  // tile with the highest weighting
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
			
			c = -o1.safeProbability.compareTo(o2.safeProbability);  // safest tiles

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
	
	
}
