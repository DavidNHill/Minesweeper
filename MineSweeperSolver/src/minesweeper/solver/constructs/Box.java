package minesweeper.solver.constructs;

import java.util.ArrayList;
import java.util.List;

import minesweeper.gamestate.Location;

/**
 * A Box contains all the squares that share the same witnesses. A consequence of this is that they
 * must have the same probability of being a mine.
 * @author David
 *
 */

public class Box {

	final private List<Witness> adjWitnesses;
	final private List<Square> squares = new ArrayList<>();
	
	private int maxMines;
	private int minMines;
	final private int uid;  // this is a sequential count given to each box as it is created; 0, 1, 2, etc
	
	private boolean processed = false;
	
	public Box(Square square, int uid) {
		
		this.uid = uid;
		
		adjWitnesses = square.getWitnesses();
		squares.add(square);
		
		// connect this box to all the adjacent witnesses
		for (Witness w: adjWitnesses) {
			w.addBox(this);
		}
		
	}
	
	
	/**
	 * Once all the squares have been added we can do some calculations
	 */
	public void calculate(int minesLeft) {
		
		maxMines = Math.min(squares.size(), minesLeft);  // can't have more mines then there are squares to put them in or mines left to discover
		minMines = 0;
		
		for (Witness w: adjWitnesses) {
			// can't have more mines than the lowest constraint
			if (w.getMines() < maxMines) {
				maxMines = w.getMines();
			}
		}
		
	}
	
	

	/**
	 * A Square fits the Box if they have the same witnesses
	 * @param square
	 * @return
	 */
	public boolean fitsBox(Square square) {
		
		// they can't share the same witnesses if they have different number of them
		if (adjWitnesses.size() != square.getWitnesses().size()) {
			return false;
		}
		
		//check that each witness of the candidate square is also a witness for the box
		for (Witness w: square.getWitnesses()) {
			boolean found = false;
			for (Witness boxWitness: adjWitnesses) {
				if (w.equals(boxWitness)) {
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
	
	public boolean contains(Location l) {
		
		for (Square squ: squares) {
			if (squ.equals(l)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	/**
	 * Add this square to the box
	 * @param square
	 */
	public void addSquare(Square square) {
		squares.add(square);
	}
	
	/**
	 * Get witness that are adjacent to this box
	 */
	public List<Witness> getWitnesses() {
		return this.adjWitnesses;
	}
	
	/**
	 * Get squares that are in this box
	 */
	public List<Square> getSquares() {
		return this.squares;
	}
	
	public boolean isProcessed() {
		return this.processed;
	}
	public void setProcessed(boolean processed) {
		this.processed = processed;
	}
	public int getUID() {
		return this.uid;
	}
	
	public int getMaxMines() {
		return this.maxMines;
	}
	public int getMinMines() {
		return this.minMines;
	}
	
	public void display() {
		
		System.out.print("Box Witnesses: ");
		for (Witness w: adjWitnesses) {
			System.out.print(w.display());
		}
		System.out.println("");
		
		System.out.print("Box Squares: ");
		for (Square squ: squares) {
			System.out.print(squ.display());
		}
		System.out.println("");
		System.out.println("Mines: max " + maxMines + " min " + minMines);
		
	}
	
	
}
