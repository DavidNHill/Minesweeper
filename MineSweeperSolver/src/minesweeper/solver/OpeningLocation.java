package minesweeper.solver;

import java.util.ArrayList;
import java.util.List;

import minesweeper.structure.Location;

public class OpeningLocation extends Location {

	private List<OpeningLocation> children = new ArrayList<>();
	private final int condition;
	
	public OpeningLocation(int x, int y, int condition) {
		super(x, y);
		
		this.condition = condition;
		
	}
	
	public OpeningLocation(int x, int y) {
		super(x, y);
		this.condition = -1;
	}

	public OpeningLocation addChild(OpeningLocation child) {
		
		children.add(child);
		
		return this;
		
	}
	
	public OpeningLocation getNextMove(BoardState solver) {
		
		if (solver.isRevealed(this)) {
			for (OpeningLocation child: children) {
				if (child.getCondition() == -1 || child.getCondition() == solver.getWitnessValue(this)) {
					OpeningLocation nextMove = child.getNextMove(solver);
					if (nextMove != null) {
						return nextMove;
					}
				}
				
				
			}
		} else {
			if (solver.getAdjacentUnrevealedSquares(this).size() == countNeighbours(solver)) {
				return this;
			}
		}
		
		return null;
		
	}
	
	private int countNeighbours(BoardState solver) {
		final int right = solver.getGameWidth() - 1;
		final int bottom = solver.getGameHeight() - 1;
		
		int adjacent = 8;
		// corners
		if (this.x == 0 && this.y == 0 || this.x == 0 && this.y == bottom || this.x == right && this.y == 0 || this.x == right && this.y == bottom) {
			adjacent = 3; 
		// the edge
		} else if (this.x == 0 || this.y == 0 || this.x == right || this.y == bottom){
			adjacent = 5;
		}
		
		return adjacent;
	}
	
	
	private int getCondition() {
		return this.condition;
	}
	
}
