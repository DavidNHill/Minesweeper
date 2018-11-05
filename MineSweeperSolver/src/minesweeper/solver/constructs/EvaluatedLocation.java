package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.util.Comparator;

import minesweeper.gamestate.MoveMethod;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class EvaluatedLocation extends Location {
	
	private final BigDecimal clearProbability;
	private final BigDecimal progressProbability;
	private String description = "";
	private BigDecimal expectedClears;
	private final int fixedClears;  //number of tiles which are clears regardless of what value is revealed
	

	public EvaluatedLocation(int x, int y, BigDecimal clearProbability, BigDecimal progressProbability, BigDecimal expectedClears, int fixedClears) {
		super(x,y);
		
		this.clearProbability = clearProbability;
		this.progressProbability = progressProbability;
		this.expectedClears = expectedClears;
		this.fixedClears = fixedClears;
		
	}
	
	public void merge(EvaluatedLocation link) {
		
		expectedClears = this.expectedClears.add(link.expectedClears);
		
	}
	
	public BigDecimal getProbability() {
		return this.clearProbability;
	}
	
	
	public Action buildAction(MoveMethod method) {
		
        String comment = description;
        
        return new Action(this, Action.CLEAR, method, comment, clearProbability);		
		
	}
	
	@Override
	public String display() {
		
		return super.display() + " Fixed clears is " + fixedClears + " expected clears is " + expectedClears.toPlainString() + ", progress probability is " + progressProbability;
		
	}
	
	/**
	 * This sorts by ...
	 */
	static public final Comparator<EvaluatedLocation> SORT_BY_EXPECTED_CLEARS  = new Comparator<EvaluatedLocation>() {
		@Override
		public int compare(EvaluatedLocation o1, EvaluatedLocation o2) {
			

			int c = 0;
			
			if (o1.fixedClears == 0 && o2.fixedClears > 0) {
				c = 1;
			} else if (o1.fixedClears > 0 && o2.fixedClears == 0) {
				c = -1;
			}
			
			//c = -(o1.fixedClears - o2.fixedClears);
			//c = -o1.progressProbability.compareTo(o2.progressProbability);  // highest probability of making progress first
			if (c == 0) {
				c = -o1.progressProbability.compareTo(o2.progressProbability);  // highest probability of making progress first
				//c = -o1.expectedClears.compareTo(o2.expectedClears);  // then highest expected number of clears
			}

			if (c == 0) {
				c = -o1.expectedClears.compareTo(o2.expectedClears);  // then highest expected number of clears
			}
			return c;
		
		}
	};
	
	
}
