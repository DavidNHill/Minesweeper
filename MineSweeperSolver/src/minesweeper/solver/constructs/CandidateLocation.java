package minesweeper.solver.constructs;

import java.math.BigDecimal;
import java.util.Comparator;

import minesweeper.gamestate.MoveMethod;
import minesweeper.structure.Action;
import minesweeper.structure.Location;
public class CandidateLocation extends Location {
	
	private final BigDecimal prob;
	private String description = "";
	private final int adjSquares;
	private final int adjFlags;
	private final boolean dead;  // Whether the tile is dead
	private final boolean deferGuessing;  // Whether the tile is not a good idea to guess
	
	public CandidateLocation(int x, int y, BigDecimal prob, int adjSquares, int adjFlags) {
		this(x, y, prob, adjSquares, adjFlags, false, false);
		
	}

	public CandidateLocation(int x, int y, BigDecimal prob, int adjSquares, int adjFlags, boolean dead, boolean deferGuessing) {
		super(x,y);
		
		this.prob = prob;
		this.adjSquares = adjSquares;
		this.adjFlags = adjFlags;
		this.dead = dead;
		this.deferGuessing = deferGuessing;
		
	}
	
	public BigDecimal getProbability() {
		return this.prob;
	}
	
	public boolean isDead() {
		return this.dead;
	}
	
	public void setDescription(String desc) {
		this.description = desc;
	}

	public void appendDescription(String desc) {
		if (this.description != "") {
			this.description = this.description + " " + desc;
		} else {
			this.description = desc;
		}
		
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public boolean getDeferGuessing() {
		return this.deferGuessing;
	}
	
	public Action buildAction(MoveMethod method) {
		
        String comment = description;
        
        if (prob.compareTo(BigDecimal.ZERO) == 0) {  // the best move is to place a flag
        	return new Action(this, Action.FLAG, method, comment, BigDecimal.ONE);	
        } else {
        	return new Action(this, Action.CLEAR, method, comment, prob);	
        }

	}
	
	/**
	 * This sorts by highest probability of not being a mine then the number of adjacent flags, unrevealed squares and finally Location order
	 */
	static public final Comparator<CandidateLocation> SORT_BY_PROB_FLAG_FREE  = new Comparator<CandidateLocation>() {
		@Override
		public int compare(CandidateLocation o1, CandidateLocation o2) {
			
			int c = 0;
			
			c = -o1.prob.compareTo(o2.prob);  // highest probability first
			if (c == 0) {
				if (o1.deferGuessing && !o2.deferGuessing) {
					c = -1;
				} else if (!o1.deferGuessing && o2.deferGuessing) {
					c = 1;
				} else {
					c = -(o1.adjFlags - o2.adjFlags);  // highest number of flags 2nd
					if (c == 0) {
						c=  o1.adjSquares - o2.adjSquares;  // lowest adjacent free squares
						if (c == 0) {
							c = o1.sortOrder - o2.sortOrder;  // location order
						}
					}
				}
			}
			
			return c;
		
		}
	};
	
	/**
	 * This sorts by highest probability of not being a mine then the number unrevealed squares (lowest first), then of adjacent flags (highest first) ,and finally Location order
	 */
	static public final Comparator<CandidateLocation> SORT_BY_PROB_FREE_FLAG  = new Comparator<CandidateLocation>() {
		@Override
		public int compare(CandidateLocation o1, CandidateLocation o2) {
			
			int c = -o1.prob.compareTo(o2.prob);  // highest probability first
			if (c == 0) {
				int a1, a2;
				if (o1.adjSquares == 0) {
					a1 = 9;
				} else {
					a1 = o1.adjSquares;
				}
				if (o2.adjSquares == 0) {
					a2 = 9;
				} else {
					a2 = o2.adjSquares;
				}
				c=  a1 - a2;   // lowest adjacent free squares  (except zero treated as 9)
				if (c == 0) {
					c = -(o1.adjFlags - o2.adjFlags);  // highest number of flags 
					if (c == 0) {
						c = o1.sortOrder - o2.sortOrder;  // location order
					}
				}
			}
			
			return c;
		
		}
	};
	
}
