/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.constructs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import minesweeper.solver.Solver;
import minesweeper.structure.Location;

/**
 *
 * @author David
 */
public class Witness extends Location {
    
    private final int minesToFind;   // the number of mines left to find

    private final int iterations;
    
    private int webNum = 0;

    private final List<Square> squares;
    
    private final List<Box> boxes = new ArrayList<>();
    
    private boolean processed = false;
 
    public Witness(Location loc, int mines, List<Square> adjSqu) {
    	super(loc.x, loc.y);
        
        this.minesToFind = mines;
        squares = adjSqu;

        this.iterations = Solver.combination(mines, squares.size()).intValue();
        
    }
    
    public List<Square> getSquares() {
        return this.squares;
    }
    
    public int getMines() {
        return minesToFind;
    }
    
    public void addSquare(Square squ) {

        squares.add(squ);
        
    }
    
    public void addBox(Box box) {
    	boxes.add(box);
    }
    
    public List<Box> getBoxes() {
    	return this.boxes;
    }
    
    public int getWebNum() {
        return webNum;
    }
    
	public boolean isProcessed() {
		return this.processed;
	}
	public void setProcessed(boolean processed) {
		this.processed = processed;
	}
	

    public void setWebNum(int webNum) {
        this.webNum = webNum;
    }    
    
    // if two witnesses have the same Squares around them they are equivalent
    public boolean equivalent(Witness wit) {

        // if the number of squares is different then they can't be equal
        if (squares.size() != wit.getSquares().size()) {
            return false;
        }
        
        // if the locations are too far apart they can't share the same squares
        if (Math.abs(wit.x - this.x) > 2 || Math.abs(wit.y - this.y) > 2) {
        	return false;
        }
        
        for (Square l1: squares) {
            boolean found = false; 
            for (Square l2: wit.getSquares()) {
                if (l2.equals(l1)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    public boolean overlap(Witness w) {
        
        // if the locations are too far apart they can't share any of the same squares
        if (Math.abs(w.x - this.x) > 2 || Math.abs(w.y - this.y) > 2) {
        	return false;
        }
    	
        boolean result = false;
        
        top: for (Square s: w.squares) {
            for (Square s1: this.squares) {
                if (s.equals(s1)) {
                    result = true;
                    break top;
                }
            }
        }
        
        return result;
        
    }
    
	/**
	 * This sorts by the number of iterations around this witness descending
	 */
	static public final Comparator<Witness> SORT_BY_ITERATIONS_DESC  = new Comparator<Witness>() {
		@Override
		public int compare(Witness o1, Witness o2) {
			
			return -(o1.iterations - o2.iterations);
		
		}
	};
    
}
