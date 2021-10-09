package minesweeper.solver.constructs;

import java.util.ArrayList;
import java.util.List;

import minesweeper.structure.Location;

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
	
	private boolean dominated = false;
	
	// defer guessing is set when the box is part of a enclosed edge where knowing the 
	// number of mines left will solve the edge
	private boolean deferGuessing = false;
	
	// this is used to indicate how many tiles in the box must not contain mine.
	private int emptyTiles = 0;
	
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
			
			// this has a small improvement on time, but (for some reason) has a small -ve impact on win rate.  Need to investigate more.
			// must be at least this many mines in the box.
			//int workMin = w.getMines() - (w.getSquares().size() -this.squares.size());  // mines left for this witness - tiles adjacent not in the box (where the mines could be)
			//if (workMin > minMines) {
			//	minMines = workMin;
			//}
			
			// this seems tohave no impact on time or win rate
			// if the witness only has 1 adjacent box - i.e. us then the minimum number of mines must also be set
			//if (w.getBoxes().size() == 1) {
			//	minMines = w.getMines();
			//}
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
	
	@Override
    public boolean equals(Object obj) {
		
		if (!(obj instanceof Box)) {
			return false;
		}
		
		Box box = (Box) obj;
		
		// to be equal they must have the same number of squares
		if (this.squares.size() != box.squares.size()) {
			return false;
		}
		
		for (Square sq1: this.squares) {
			boolean found = false;
			for (Square sq2: box.squares) {
				if (sq1.equals(sq2)) {
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
	
	public void setDominated() {
		this.dominated = true;
	}
	
	public boolean isDominated() {
		return this.dominated;
	}
	
	public void setDeferGuessing() {
		this.deferGuessing = true;
	}
	
	public boolean doDeferGuessing() {
		return this.deferGuessing;
	}
	
	public int getEmptyTiles() {
		return emptyTiles;
	}


	public void incrementEmptyTiles() {
		this.emptyTiles++;
		// can't have more mines than there are squares to put them
		if (this.maxMines > this.squares.size() - this.emptyTiles) {
			this.maxMines = this.squares.size() - this.emptyTiles;
			
			if (this.maxMines < this.minMines) {
				System.out.println("Illegal Mines: max " + maxMines + " min " + minMines);
			}
		}
	}
	
	public void display() {
		
		System.out.print("Box Witnesses: ");
		for (Witness w: adjWitnesses) {
			System.out.print(w.toString());
		}
		System.out.println("");
		
		System.out.print("Box Squares: ");
		for (Square squ: squares) {
			System.out.print(squ.toString());
		}
		System.out.println("");
		System.out.println("Mines: max " + maxMines + " min " + minMines);
		
	}



	
	
}
