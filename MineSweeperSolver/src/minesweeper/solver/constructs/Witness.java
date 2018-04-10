/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.constructs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import minesweeper.gamestate.Location;
import minesweeper.solver.Solver;

/**
 *
 * @author David
 */
public class Witness extends Location {
    
    private final int mines;   // the number of mines left to find

    private final int iterations;
    
    private int webNum = 0;

    private final List<Square> squares = new ArrayList<>();
    
    private final List<Box> boxes = new ArrayList<>();
    
    private boolean processed = false;
 
    public Witness(Location loc, int mines, List<Square> adjSqu) {
    	super(loc.x, loc.y);
        
        this.mines = mines;
        squares.addAll(adjSqu);

        this.iterations = Solver.combination(mines, squares.size()).intValue();
        
    }
    
    public List<Square> getSquares() {
        return this.squares;
    }
    
    public int getMines() {
        return mines;
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
