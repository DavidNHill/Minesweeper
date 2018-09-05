package minesweeper.solver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;
import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.BoardStateCache.AdjacentSquares;
import minesweeper.solver.BoardStateCache.Cache;
import minesweeper.solver.constructs.ChordLocation;

public class BoardState {

	private final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
	private final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};


	/*
	// iterator for adjacent squares
	protected class AdjacentSquares implements Iterable<Location> {

		private Location loc;
		//private Location nextLoc;
		//private int index = 0;
		private final int size;
		private List<Location> locations;

		private AdjacentSquares(Location l) {
			this(l, 1);
		}
		
		AdjacentSquares(Location l, int size) {
			this.loc = l;
			this.size = size;
			
			if (size == 1) {
				locations = new ArrayList<>(8);
				for (int i=0; i < DX.length; i++) {
					if (loc.x + DX[i] >= 0 && loc.x + DX[i] < myGame.getx() && loc.y + DY[i] >= 0 && loc.y + DY[i] < myGame.gety()) {
						locations.add(new Location(loc.x + DX[i], loc.y + DY[i]));
					}		
					
				}
			} else {
				int startX = Math.max(0, loc.x - this.size);
				int endX = Math.min(myGame.getx() - 1, loc.x + this.size);
				
				int startY = Math.max(0, loc.y - this.size);
				int endY = Math.min(myGame.gety() - 1, loc.y + this.size);
				
				locations = new ArrayList<>((this.size * 2 - 1) * (this.size * 2 - 1));
				for (int i=startX; i <= endX; i++) {
					for (int j=startY; j <= endY; j++) {
						if (i == loc.x && j == loc.y) {
							// don't send back the central location
						} else {
							locations.add(new Location(i,j));
						}
						
					}
				}				
			}

		
		}

		@Override
		public Iterator<Location> iterator() {
			return locations.iterator();
			//return new AdjacentSquaresIterator(this);
		}

	}
	*/

	/*
	private class AdjacentSquaresIterator implements Iterator<Location> {
		
		private AdjacentSquares source;
		private Location nextLoc;
		private int index = 0;
		
		private AdjacentSquaresIterator(AdjacentSquares source) {
			this.source = source;

			nextLoc = findNext();
		}
		
		@Override
		public boolean hasNext() {
			return (nextLoc != null);
		}

		@Override
		public Location next() {

			Location result = nextLoc;
			nextLoc = findNext();

			return result;
		}

		private Location findNext() {

			Location next;
			if (index < source.locations.size()) {
				 next = source.locations.get(index);
			} else {
				next = null;
			}
			
			index++;
			
			return next;

		}
		
	}
	*/
	
	private AdjacentSquares[][] adjacentLocations1;
	private AdjacentSquares[][] adjacentLocations2;

	private int[][] board;
	private boolean[][] revealed;
	private boolean[][] flagConfirmed;
	private boolean[][] flagOnBoard;
	
	//private int[][] clearAll;

	private int[][] adjFlagsConfirmed;
	private int[][] adjFlagsOnBoard;
	//private int[][] adjRevealed;
	private int[][] adjUnrevealed;

	// this holds the actions made against each location and a list of actions generated this turn
	private Action[][] action;
	private List<Action> actionList = new ArrayList<Action>();

	
	private List<ChordLocation> chordLocations = new ArrayList<>();
	//private Location clearAllLocation;
	//private int clearAllValue;
	//private List<Action> clearAllList = new ArrayList<>();
	
	private GameStateModel myGame;
	private Solver solver;

	private int totalFlags = 0;
	private int totalFlagsConfirmed = 0;
	private int numOfHidden = 0;

	private int[] unplayedMoves;

	public BoardState(Solver solver) {

		this.solver = solver;
		this.myGame = solver.getGame();

		flagConfirmed = new boolean[myGame.getx()][myGame.gety()];
		adjFlagsConfirmed =  new int[myGame.getx()][myGame.gety()];
		adjUnrevealed = new int[myGame.getx()][myGame.gety()];
		revealed = new boolean[myGame.getx()][myGame.gety()];
		board = new int[myGame.getx()][myGame.gety()];
		//clearAll = new int[myGame.getx()][myGame.gety()];

		action = new Action[myGame.getx()][myGame.gety()];

		// look up the adjacent squares details
		Cache cache = BoardStateCache.getInstance().getAdjacentSquares1(myGame.getx(), myGame.gety());
		adjacentLocations1 = cache.adjacentLocations1;
		adjacentLocations2 = cache.adjacentLocations2;
		

		final int bottom = myGame.gety() - 1;
		final int right =  myGame.getx() - 1;

		//  set up how many adjacent locations there are to each square - they are all unrevealed to start with
		for (int x=0; x < myGame.getx(); x++) {
			for (int y=0; y < myGame.gety(); y++) {

				int adjacent = 8;
				// corners
				if (x == 0 && y == 0 || x == 0 && y == bottom || x == right && y == 0 || x == right && y == bottom) {
					adjacent = 3; 
					// the edge
				} else if (x == 0 || y == 0 || x == right || y == bottom){
					adjacent = 5;
				}

				adjUnrevealed[x][y] = adjacent;
				
				//adjacentLocations1[x][y] = new AdjacentSquares(new Location(x,y), 1);
				//adjacentLocations2[x][y] = new AdjacentSquares(new Location(x,y), 2);

			}
		}


	}

	public void process() {

		flagOnBoard = new boolean[myGame.getx()][myGame.gety()];
		adjFlagsOnBoard = new int[myGame.getx()][myGame.gety()];
		totalFlags = 0;
		totalFlagsConfirmed = 0;
		numOfHidden = 0;

		//clearAllLocation = null;
		//clearAllValue = 1;
		//clearAllList.clear();
		
		// clear down the moves we collected last turn
		actionList.clear();

		// load up what we can see on the board
		for (int i=0; i < myGame.getx(); i++) {
			for (int j=0; j < myGame.gety(); j++) {
				int info = myGame.query(new Location(i, j));

				Action act = action[i][j];

				// if the move isn't a certainty then don't bother with it. The opening book is a certainty on the first move, but isn't really if the player plays somewhere else.
				if (act != null && (!act.isCertainty() || act.getMoveMethod() == MoveMethod.BOOK)) {
					action[i][j] = null;
					act = null;
				}

				if (info != GameStateModel.HIDDEN) {
					if (info == GameStateModel.FLAG) {
						totalFlags++;
						flagOnBoard[i][j] = true;
						
						// inform its neighbours they have a flag on the board
						for (int k=0; k < DX.length; k++) {
							if (i + DX[k] >= 0 && i + DX[k] < myGame.getx() && j + DY[k] >= 0 && j + DY[k] < myGame.gety()) {
								adjFlagsOnBoard[i + DX[k]][j + DY[k]]++;
							}
						}    							

						if (flagConfirmed[i][j]) {    // flag found by solver
							totalFlagsConfirmed++;
						} else {
							numOfHidden++;           // flag on the board but we can't confirm it
						}

						// if the board is a flag, but we are 100% sure its a clear then remove the flag
						// then clear the square
						if (act != null && act.getAction() == Action.CLEAR && act.isCertainty()) {
							actionList.add(new Action(act, Action.FLAG, MoveMethod.CORRECTION, "Remove flag", BigDecimal.ONE, 0));
							actionList.add(act);
						}

					} else {

						// if this is a new unrevealed location then set it up and inform it's neighbours they have one less unrevealed adjacent location
						if (!revealed[i][j]) {

							//display("Location (" + i + "," + j + ") is revealed");
							
							revealed[i][j] = true;
							board[i][j] = info;            

							for (int k=0; k < DX.length; k++) {
								if (i + DX[k] >= 0 && i + DX[k] < myGame.getx() && j + DY[k] >= 0 && j + DY[k] < myGame.gety()) {
									adjUnrevealed[i + DX[k]][j + DY[k]]--;
								}
							}    	

						}

					}
				} else {
					if (solver.isFlagFree() && flagConfirmed[i][j]) {  // if we are playing flags free then all confirmed flags are consider to be on the board
						//flagOnBoard[i][j] = true;
						totalFlagsConfirmed++;   
						totalFlags++;
					} else {
						numOfHidden++;
					}

					// if we have an action again this location which we are 100% sure about then do it
					if (act != null && act.isCertainty()) {
						if (solver.isFlagFree() && act.getAction() == Action.FLAG) {
							// unless the we are playing flag free and it's a flag
						} else {
							actionList.add(act);
						}

					}
				}

			}
		}

		// this sorts the moves by when they were discovered
		Collections.sort(actionList, Action.SORT_BY_MOVE_NUMBER);

		unplayedMoves = new int[MoveMethod.values().length];

		// accumulate how many unplayed moves there are by method
		for (Action a: actionList) {
			unplayedMoves[a.getMoveMethod().ordinal()]++;
		}

		display("Moves left to play = " + actionList.size());
		for (int i=0; i < unplayedMoves.length; i++) {
			if (unplayedMoves[i] != 0) {
				display("   " + MoveMethod.values()[i] + " has " + unplayedMoves[i] + " moves unplayed");
			}
		}


	}

	protected int getGameWidth() {
		return this.myGame.getx();
	}

	protected int getGameHeight() {
		return this.myGame.gety();
	}

	protected int getMines() {
		return this.myGame.getMines();
	}

	
	public void setAction(Action a) {
		setAction(a, true);
	}

	/**
	 * Register the action against the location(x,y);
	 * Optionally add the action to the list of actions to play this turn
	 */
	public void setAction(Action a, boolean addToList) {

		if (action[a.x][a.y] != null) {
			return;
		}

		action[a.x][a.y] = a;

		if (solver.isFlagFree() && a.getAction() == Action.FLAG) {
			// if it is flag free and we are placing a flag then don't do it
		} else if (isFlagOnBoard(a) && a.getAction() == Action.FLAG) {
			// if the flag is already on the board then nothing to do
		} else if (isFlagOnBoard(a) && a.getAction() == Action.CLEAR) {
			// if a flag is blocking the clear move then remove the flag first
			actionList.add(new Action(a, Action.FLAG, MoveMethod.CORRECTION, "Remove flag", BigDecimal.ONE, 0));
			actionList.add(a);			
		} else {
			actionList.add(a);
		}


	}

	protected boolean alreadyActioned(Location l) {
		return alreadyActioned(l.x, l.y);
	}

	protected boolean alreadyActioned(int x, int y) {
		return (action[x][y] != null);
	}


	protected List<Action> getActions() {
		return this.actionList;
	}

	protected List<Action> getActions1() {

		List<Action> actions = new ArrayList<>();
		
		boolean[][] processed = new boolean[myGame.getx()][myGame.gety()];
		
		// sort the most beneficial chords to the top
		Collections.sort(chordLocations, ChordLocation.SORT_BY_BENEFIT_DESC);
		
		List<ChordLocation> toDelete = new ArrayList<>();
		
		for (ChordLocation cl: chordLocations) {
			
			int benefit = 0;
			int cost = 0;
			
			for (Location l: getAdjacentSquaresIterable(cl)) {
				// flag not yet on board
				if (!processed[l.x][l.y] && isConfirmedFlag(l) && !isFlagOnBoard(l)) {
					cost++;
				}
				if (!processed[l.x][l.y] && isUnrevealed(l)) {
					benefit++;
				}
			}			
			
			
			if (benefit - cost > 1) {

				for (Location l: getAdjacentSquaresIterable(cl)) {
					// flag not yet on board
					if (!processed[l.x][l.y] && isConfirmedFlag(l) && !isFlagOnBoard(l)) {
						actions.add(new Action(l, Action.FLAG, MoveMethod.TRIVIAL, "Place flag", BigDecimal.ONE, 0));
					}
					// flag on board in error
					if (!processed[l.x][l.y] && !isConfirmedFlag(l) && isFlagOnBoard(l)) {
						actions.add(new Action(l, Action.FLAG, MoveMethod.CORRECTION, "Remove flag", BigDecimal.ONE, 0));
					}
					processed[l.x][l.y] = true;
				}
				// now add the clear all
				actions.add(new Action(cl, Action.CLEARALL, MoveMethod.TRIVIAL, "Clear All", BigDecimal.ONE, 1));	
				
			} else {
				//toDelete.add(cl);
			}
		}
		
		chordLocations.removeAll(toDelete);

		// now add the the actions that haven't been resolved by a chord play
		for (Action act: actionList) {
			if (!processed[act.x][act.y]) {
				processed[act.x][act.y] = true;
			}
			actions.add(act);
		}

		return actions;
	}
	
	/**
	 * Get the probability of a mine being in this square (based upon the actions still pending)
	 */
	protected BigDecimal getProbability(int x, int y) {
		
		for (Action act: actionList) {
			if (act.x == x && act.y== y) {
				if (act.getAction() == Action.FLAG) {
					return BigDecimal.ZERO;
				} else if (act.getAction() == Action.CLEAR) {
					return act.getBigProb();
				}
				
			}
		}		
		
		
		return null;
		
	}
	
	
	//protected int getActionsCount() {
	//	return this.actionList.size();
	//}

	/**
	 * From the given locations, generate all the revealed squares that can witness these locations
	 */
	protected List<Location> getWitnesses(List<? extends Location> square) {

		Set<Location> work = new HashSet<>(100);

		for (Location loc: square) {

			for (Location adj: this.getAdjacentSquaresIterable(loc)) {
				// determine the number of distinct witnesses
				if (isRevealed(adj)) {
					work.add(adj);
				}            		
			}            
		}

		return new ArrayList<Location>(work);

	}
	
	protected List<Location> getWitnesses1(List<? extends Location> square) {

		List<Location> work = new ArrayList<>(100);

		//int n = 0;
		for (Location loc: square) {

			for (Location adj: this.getAdjacentSquaresIterable(loc)) {
				// determine the number of distinct witnesses
				if (isRevealed(adj)) {

					boolean dup = false;
					for (Location l: work) {
						if (adj.equals(l)) {
							dup = true;
							break;                        		
						}
					}
					if (!dup) {
						work.add(adj);
					}

				}            		

			}            

		}

		return work;

	}

	// take a list of Locations and find all the un-revealed squares around them
	protected List<Location> getUnrevealedSquares(List<? extends Location> witnesses) {

		ArrayList<Location> work = new ArrayList<>();       

		for (Location loc: witnesses) {
			
			for (Location adj: this.getAdjacentSquaresIterable(loc)) {

				if (isUnrevealed(adj)) {

					boolean dup = false;
					for (Location l: work) {
						if (adj.equals(l)) {
							dup = true;
							break;                        		
						}
					}
					if (!dup) {
						work.add(adj);
					}

				}            		
			}
			
		}

		return work;
	}

	/**
	 * Return all the unrevealed Locations on the board
	 */
	protected List<Location> getAllUnrevealedSquares() {

		ArrayList<Location> work = new ArrayList<>(myGame.getx() * myGame.gety());       

		for (int i=0; i < myGame.getx(); i++) {
			for (int j=0; j < myGame.gety(); j++) {
				if (isUnrevealed(i,j)) {
					work.add(new Location(i,j));
				}
			}
		}                        

		return work;
	}

	/**
	 * Return a list of Unrevealed Locations adjacent to this one
	 */
	protected List<Location> getAdjacentUnrevealedSquares(Location loc) {

		return getAdjacentUnrevealedSquares(loc, 1);
	}
	
	
	/**
	 * Return a list of Unrevealed Locations adjacent to this one
	 */
	protected List<Location> getAdjacentUnrevealedSquares(Location loc, int size ) {

		ArrayList<Location> work = new ArrayList<>();       

		for (Location a: getAdjacentSquaresIterable(loc, size)) {
			if (isUnrevealed(a)) {
				work.add(a);
			}
		}

		return work;
	}

	/**
	 * This returns the adjacent squares to a depth of size away. So (l,2) returns the 24 squares 2 deep surrounding l.
	 * @param l
	 * @param size
	 * @return
	 */
	protected Iterable<Location> getAdjacentSquaresIterable(Location loc, int size) {
		if (size == 1) {
			return getAdjacentSquaresIterable(loc);
		} else {
			return adjacentLocations2[loc.x][loc.y];
		}
		
	}

	protected Iterable<Location> getAdjacentSquaresIterable(Location loc) {
		//if (adjacentLocations1[loc.x][loc.y] == null) {
		//	adjacentLocations1[loc.x][loc.y] = new AdjacentSquares(loc, 1);
		//}
		return adjacentLocations1[loc.x][loc.y];
		//return new AdjacentSquares(loc, 1);
	}


	/**
	 * return all the squares adjacent to this square
	 */
	/*
     protected List<Location> getAdjacentSquares(Location l) {

        ArrayList<Location> work = new ArrayList<>();       

        for (Location a: getAdjacentSquaresIterable(l)) {
    		work.add(a);
        }


        //int x = l.x;
        //int y = l.y;
        //for (int k=0; k < DX.length; k++) {
        //    if (x + DX[k] >= 0 && x + DX[k] < myGame.getx() && y + DY[k] >= 0 && y + DY[k] < myGame.gety()) {
        //    	work.add(new Location(x + DX[k], y + DY[k]));
        //    }
        //}

        return work;
    }
	 */

	/**
	 * Done as part of validating Locations, must be undone using clearWitness()
	 */
	protected void setWitnessValue(Location l, int value) {    	 
		board[l.x][l.y] = value;
		revealed[l.x][l.y] = true;
	}

	/**
	 * Done as part of validating Locations
	 */
	protected void clearWitness(Location l) {    	 
		board[l.x][l.y] = 0;
		revealed[l.x][l.y] = false;
	}

	/**
	 * Method to read our own array of reveal values.
	 */
	protected int getWitnessValue(Location l) {
		return getWitnessValue(l.x, l.y);
	}

	/**
	 * Method to read our own array of reveal values.
	 */
	protected int getWitnessValue(int x, int y) {
		if (isUnrevealed(x,y)) {
			throw new RuntimeException("Trying to get a witness value for an unrevealed square");
		}
		return board[x][y];
	}    

	/**
	 * indicates a flag is on the board, but the solver can't confirm it
	 */
	protected boolean isUnconfirmedFlag(Location l) {
		return isUnconfirmedFlag(l.x, l.y);
	}

	/**
	 * indicates a flag is on the board, but the solver can't confirm it
	 */
	protected boolean isUnconfirmedFlag(int x, int y) {
		return flagOnBoard[x][y] && !flagConfirmed[x][y];
	}        

	/**
	 * indicates a flag is on the board
	 */
	protected boolean isFlagOnBoard(Location l) {
		return isFlagOnBoard(l.x, l.y);
	}

	/**
	 * indicates a flag is on the board
	 */
	protected boolean isFlagOnBoard(int x, int y) {
		return flagOnBoard[x][y];
	}        


	protected void setFlagOnBoard(Location l) {
		setFlagOnBoard(l.x, l.y);
	}

	protected void setFlagOnBoard(int x, int y) {
		flagOnBoard[x][y] = true;
	}

	protected void setFlagConfirmed(Location loc) {

		if (isConfirmedFlag(loc)) {
			return;
		}

		totalFlagsConfirmed++;
		flagConfirmed[loc.x][loc.y] = true;

		// if the flag isn't already on the board then this is also another on the total of all flags
		if (!flagOnBoard[loc.x][loc.y]) {
			totalFlags++;
		}

		// let all the adjacent squares know they have one more flag next to them and one less unrevealed location
		for (Location a: getAdjacentSquaresIterable(loc)) {
			adjFlagsConfirmed[a.x][a.y]++;
			adjUnrevealed[a.x][a.y]--;
		}
		
	}

	/**
	 * Since Flag Free is a thing, we can't rely on the GameState to tell us where the flags are,
	 * so this replaces that.
	 */
	protected boolean isConfirmedFlag(Location l) {
		return isConfirmedFlag(l.x, l.y);
	}

	/**
	 * Since Flag Free is a thing, we can't rely on the GameState to tell us where the flags are,
	 * so this replaces that.
	 */
	protected boolean isConfirmedFlag(int x, int y) {
		return flagConfirmed[x][y];
	}    

	/**
	 * Returns whether the location is revealed
	 */
	protected boolean isRevealed(Location l) {
		return isRevealed(l.x, l.y);
	}    

	/**
	 * Returns whether the location is revealed
	 */
	protected boolean isRevealed(int x, int y) {
		return revealed[x][y];
	}    

	/**
	 * Returns whether the location is unrevealed (neither revealed nor a confirmed flag)
	 */
	protected boolean isUnrevealed(Location l) {
		return isUnrevealed(l.x, l.y);
	}    

	/**
	 * Returns whether the location is unrevealed (neither revealed nor a confirmed flag)
	 */
	protected boolean isUnrevealed(int x, int y) {
		return !flagConfirmed[x][y] && !revealed[x][y];
	}    

	/**
	 * count how many confirmed flags are adjacent to this square
	 */
	protected int countAdjacentConfirmedFlags(Location l) {
		return countAdjacentConfirmedFlags(l.x, l.y);
	}

	/**
	 * count how many confirmed flags are adjacent to this square
	 */
	protected int countAdjacentConfirmedFlags(int x, int y) {

		return adjFlagsConfirmed[x][y];

	}

	/**
	 * count how many flags are adjacent to this square on the board
	 */
	protected int countAdjacentFlagsOnBoard(Location l) {
		return countAdjacentFlagsOnBoard(l.x, l.y);
	}

	/**
	 * count how many flags are adjacent to this square on the board
	 */
	protected int countAdjacentFlagsOnBoard(int x, int y) {

		return adjFlagsOnBoard[x][y];

	}
	
	
	/**
	 * count how many confirmed and unconfirmed flags are adjacent to this square
	 */
	protected int countAllFlags(int x, int y) {

		int result = 0;

		for (int i=0; i < DX.length; i++) {
			if (x + DX[i] >= 0 && x + DX[i] < myGame.getx() && y + DY[i] >= 0 && y + DY[i] < myGame.gety()) {
				if (flagConfirmed[x + DX[i]][y + DY[i]] || flagOnBoard[x + DX[i]][y + DY[i]]) {
					result++;
				}
			}
		}

		return result;

	}

	/**
	 * count how many adjacent squares are neither flags nor revealed
	 */
	protected int countAdjacentUnrevealed(Location l) {

		return countAdjacentUnrevealed(l.x, l.y);

	}

	/**
	 * count how many adjacent squares are neither confirmed flags nor revealed
	 */
	protected int countAdjacentUnrevealed(int x, int y) {

		int result = 0;

		result = adjUnrevealed[x][y];

		return result;

	}    

	/**
	 * count how many squares are neither confirmed flags nor revealed
	 */    
	protected int getTotalUnrevealedCount() {

		int result = 0;

		result = numOfHidden;

		return result;

	}

	/**
	 * Returns the number of mines the solver knows the location of
	 */
	protected int getConfirmedFlagCount() {

		return totalFlagsConfirmed;

	}

	/**
	 * Number of flags on the board both confirmed and unconfirmed
	 */
	protected int getTotalFlagCount() {
		return this.totalFlags;
	}

	protected int getUnplayedMoves(MoveMethod method) {
		return unplayedMoves[method.ordinal()];
	}


	// check for flags which can be determined to be wrong
	protected boolean validateData() {

		for (int i=0; i < myGame.getx(); i++) {
			for (int j=0; j < myGame.gety(); j++) {

				// if there is an unconfirmed flag on the board but the solver
				// thinks it is clear then the flag is wrong
				if (isUnconfirmedFlag(i,j) && action[i][j] != null && action[i][j].getAction() == Action.CLEAR) {
					display("Flag in Error at " + i + " " + j + " confirmed CLEAR");
					return false;
				}

				if (isRevealed(i,j) && getWitnessValue(i,j) != 0) {
					int flags = countAllFlags(i,j);

					// if we have too many flags by a revealed square then a mistake has been made
					if (getWitnessValue(i,j) < flags) {
						display("Flag in Error at witness " + i + " " + j + " Overloads: Flags = " + flags);
						return false;
					}
				}

			}        

		}

		return true;

	}

	/**
	 * Inform the board state that this location might benefit from using a clear all move, returns true if the move is accepted as a clear all
	 * @param loc
	 */
	protected boolean setChordLocation(Location loc) {
		
		if (!solver.isPlayChords()) {
			return false;
		}
		
		
		boolean accepted = false;
		
		int benefit = countAdjacentUnrevealed(loc);
		int cost = getWitnessValue(loc) - countAdjacentFlagsOnBoard(loc);
		
		if (benefit - cost > 1) {
			
			accepted = true;

			chordLocations.add(new ChordLocation(loc.x, loc.y, benefit - cost));
			
			/*
			for (Location l: getAdjacentSquaresIterable(loc)) {
				// flag not yet on board
				if (isConfirmedFlag(l) && !isFlagOnBoard(l)) {
					clearAllList.add(new Action(l, Action.FLAG, Solver.OBVIOUS, "Place flag", BigDecimal.ONE, 0));
				}
				// flag on board in error
				if (!isConfirmedFlag(l) && isFlagOnBoard(l)) {
					clearAllList.add(new Action(l, Action.FLAG, Solver.OBVIOUS, "Remove flag", BigDecimal.ONE, 0));
				}
			}
			// now add the clear all
			clearAllList.add(new Action(loc, Action.CLEARALL, Solver.OBVIOUS, "Clear All", BigDecimal.ONE, 1));	
			*/
			
		}
		
		return accepted;
		
	}

	
	protected void display(String text) {
		solver.display(text);
	}


}
