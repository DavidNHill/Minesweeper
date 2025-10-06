package minesweeper.solver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.BoardStateCache.AdjacentSquares;
import minesweeper.solver.BoardStateCache.Cache;
import minesweeper.solver.utility.Logger;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class BoardState {

	private final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
	private final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

	//private AdjacentSquares[][] adjacentLocations1;
	//private AdjacentSquares[][] adjacentLocations2;

	private int[][] board;
	private boolean[][] revealed;
	private boolean[][] confirmedMine;
	private boolean[][] flagOnBoard;
	
	//private int[][] clearAll;

	private int[][] adjFlagsConfirmed;
	private int[][] adjFlagsOnBoard;
	private int[][] adjUnrevealed;

	// this holds the actions made against each location and a list of actions generated this turn
	private Action[][] action;
	private List<Action> actionList = new ArrayList<Action>();

	private final GameStateModel myGame;
	private final Solver solver;
	private final int height;
	private final int width;

	private int totalFlags = 0;
	private int confirmedMinesTotal = 0;
	private int numOfHidden = 0;

	private int[] unplayedMoves;

	private int testMoveBalance = 0;
	
	private Set<Location> livingWitnesses = new HashSet<>();
	
	private final Cache cache;
	
	public BoardState(Solver solver) {

		this.solver = solver;
		this.myGame = solver.getGame();
		this.width = myGame.getWidth();
		this.height = myGame.getHeight();

		confirmedMine = new boolean[myGame.getWidth()][myGame.getHeight()];
		adjFlagsConfirmed =  new int[myGame.getWidth()][myGame.getHeight()];
		adjUnrevealed = new int[myGame.getWidth()][myGame.getHeight()];
		revealed = new boolean[myGame.getWidth()][myGame.getHeight()];
		board = new int[myGame.getWidth()][myGame.getHeight()];
		
		flagOnBoard = new boolean[myGame.getWidth()][myGame.getHeight()];
		adjFlagsOnBoard = new int[myGame.getWidth()][myGame.getHeight()];
		
		action = new Action[myGame.getWidth()][myGame.getHeight()];

		// look up the adjacent squares details
		cache = BoardStateCache.getInstance().getAdjacentSquares1(myGame.getWidth(), myGame.getHeight());
		//adjacentLocations1 = cache.adjacentLocations1;
		//adjacentLocations2 = cache.adjacentLocations2;
		

		final int bottom = myGame.getHeight() - 1;
		final int right =  myGame.getWidth() - 1;

		//  set up how many adjacent locations there are to each square - they are all unrevealed to start with
		for (int x=0; x < width; x++) {
			for (int y=0; y < height; y++) {

				int adjacent = 8;
				// corners
				if (x == 0 && y == 0 || x == 0 && y == bottom || x == right && y == 0 || x == right && y == bottom) {
					adjacent = 3; 
					// the edge
				} else if (x == 0 || y == 0 || x == right || y == bottom){
					adjacent = 5;
				}

				adjUnrevealed[x][y] = adjacent;
				
			}
		}


	}

	public void process() {

		totalFlags = 0;
		confirmedMinesTotal = 0;
		numOfHidden = 0;

		// clear down this array, which is a lot faster then defining it fresh
		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {
				adjFlagsOnBoard[i][j] = 0;
			}
		}

		// clear down the moves we collected last turn
		actionList.clear();
		
		// load up what we can see on the board
		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {
				
				Location location = getLocation(i, j);
				
				flagOnBoard[i][j] = false; // until proven otherwise
				int info = myGame.query(location);

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
							if (i + DX[k] >= 0 && i + DX[k] < width && j + DY[k] >= 0 && j + DY[k] < height) {
								adjFlagsOnBoard[i + DX[k]][j + DY[k]]++;
							}
						}    							

						if (confirmedMine[i][j]) {    // mine found by solver
							confirmedMinesTotal++;
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

							livingWitnesses.add(location);  // add this to living witnesses
							//display("Location (" + i + "," + j + ") is revealed");
							
							revealed[i][j] = true;
							board[i][j] = info;            

							for (int k=0; k < DX.length; k++) {
								if (i + DX[k] >= 0 && i + DX[k] < width && j + DY[k] >= 0 && j + DY[k] < height) {
									adjUnrevealed[i + DX[k]][j + DY[k]]--;
								}
							}    	

						}

					}
				} else {
					if ((solver.getPlayStyle().flagless || solver.getPlayStyle().efficiency) && confirmedMine[i][j]) {  // if we are playing flags free then all confirmed mines are consider to be flagged
						confirmedMinesTotal++;   
						totalFlags++;
					} else {
						numOfHidden++;
					}

					// if we have an action against this location which we are 100% sure about then do it
					if (act != null && act.isCertainty()) {
						if ((solver.getPlayStyle().flagless || solver.getPlayStyle().efficiency) && act.getAction() == Action.FLAG) {
							// unless the we are playing flag free and it's a flag
						} else {
							actionList.add(act);
						}

					}
				}

			}
		}
		
		List<Location> toRemove = new ArrayList<>();
		for (Location wit: livingWitnesses) {
			if (this.countAdjacentUnrevealed(wit) == 0) {
				//display("Location " + wit.display() + " is now a dead witness");
				toRemove.add(wit);
			}
		}
		livingWitnesses.removeAll(toRemove);
		
		
		// this sorts the moves by when they were discovered
		Collections.sort(actionList, Action.SORT_BY_MOVE_NUMBER);

		unplayedMoves = new int[MoveMethod.values().length];

		// accumulate how many unplayed moves there are by method
		for (Action a: actionList) {
			unplayedMoves[a.getMoveMethod().ordinal()]++;
		}

		getLogger().log(Level.INFO, "Moves left to play is %d", actionList.size());
		for (int i=0; i < unplayedMoves.length; i++) {
			if (unplayedMoves[i] != 0) {
				getLogger().log(Level.INFO, "   %s has %d moves unplayed",MoveMethod.values()[i], unplayedMoves[i]);
			}
		}


	}

	protected int getGameWidth() {
		return width;
	}

	protected int getGameHeight() {
		return height;
	}

	protected int getMines() {
		return this.myGame.getMines();
	}

	
	//public void setAction(Action a) {
	//	setAction(a, true);
	//}

	/**
	 * Register the action against the location(x,y);
	 * Optionally add the action to the list of actions to play this turn
	 */
	public void setAction(Action a) {

		//display("Setting action at " + a.display());
		
		if (action[a.x][a.y] != null) {
			return;
		}

		action[a.x][a.y] = a;

		if (a.getAction() == Action.FLAG) {
			setMineFound(a);
		}
		
		if ((solver.getPlayStyle().flagless || solver.getPlayStyle().efficiency)  && a.getAction() == Action.FLAG) {
			// if it is flag free or efficiency and we have discovered a mine then don't flag it
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

	/**
	 * This will consider chords when returning the moves to play
	 */
	/*
	protected List<Action> getActionsWithChords() {

		// if we aren't using chords or none are available then skip all this expensive processing
		if (chordLocations.isEmpty() || !solver.isPlayChords()) {
			return getActions();
		}
		
		List<Action> actions = new ArrayList<>();
		boolean[][] processed = new boolean[myGame.getWidth()][myGame.getHeight()];
		
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
	*/
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
	 * Add a isolated dead tile
	 */
	//protected void addIsolatedDeadTile(Location loc) {
	//	isolatedDeadTiles.add(loc);
	//}
	
	//public int getIsolatedDeadTileCount() {
	//	return this.isolatedDeadTiles.size();
	//}
	
	/**
	 * Returns and removes the first Isolated Dead Tile in the set
	 */
	//public Location getIsolatedDeadTile() {
	//	for (Location loc: isolatedDeadTiles) {
	//		//isolatedDeadTiles.remove(loc);
	//		return loc;
	//	}
	//	return null;
	//}

	protected List<Location> getWitnesses(Collection<? extends Location> square) {
		return new ArrayList<Location>(getWitnessesArea(square).getLocations());
	}
	
	/**
	 * From the given locations, generate all the revealed squares that can witness these locations
	 */
	protected Area getWitnessesArea(Collection<? extends Location> square) {

		Set<Location> work = new HashSet<>(10);

		for (Location loc: square) {

			for (Location adj: this.getAdjacentSquaresIterable(loc)) {
				// determine the number of distinct witnesses
				if (isRevealed(adj)) {
					work.add(adj);
				}            		
			}            
		}

		return new Area(work);

	}
	
	/**
	 * From the given locations, generate an area containing all the un-revealed squares around them
	 */
	protected Area getUnrevealedArea(List<? extends Location> witnesses) {
		return new Area(getUnrevealedSquaresDo(witnesses));
	}
	
	/**
	 * From the given locations, generate all the un-revealed squares around them
	 */
	protected List<Location> getUnrevealedSquares(List<? extends Location> witnesses) {
		return new ArrayList<Location>(getUnrevealedSquaresDo(witnesses));
	}
	
	
	private Set<Location> getUnrevealedSquaresDo(List<? extends Location> witnesses) {

		Set<Location> work = new HashSet<>(witnesses.size() * 3);       

		for (Location loc: witnesses) {
			
			for (Location adj: this.getAdjacentSquaresIterable(loc)) {

				if (isUnrevealed(adj)) {
					work.add(adj);
				}            		
			}
			
		}

		return work;
	}
	
	Set<Location> get3BVRiskyTiles(List<? extends Location> witnesses) {

		Set<Location> work = new HashSet<>(witnesses.size() * 3);       

		for (Location loc: witnesses) {
			
			boolean adjacentToZero = false;
			for (Location adj: this.getAdjacentSquaresIterable(loc)) {

				if (isRevealed(adj) && getWitnessValue(adj) == 0 ) {
					adjacentToZero = true;
					break;
				}            		
			}
			
			if (!adjacentToZero) {
				work.add(loc);
			}            
		}

		return work;
	}
	
	/**
	 * Return all the unrevealed Locations on the board
	 */
	protected List<Location> getAllUnrevealedSquares() {

		ArrayList<Location> work = new ArrayList<>(width * height);       

		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {
				if (isUnrevealed(i,j)) {
					work.add(getLocation(i,j));
				}
			}
		}                        

		return work;
	}

	protected List<Location> getAllLivingWitnesses() {
		return new ArrayList<>(livingWitnesses);
	}
	
	
	/**
	 * Return a list of Unrevealed Locations adjacent to this one
	 */
	protected List<Location> getAdjacentUnrevealedSquares(Location loc) {

		return getAdjacentUnrevealedSquares(loc, 1);
	}

	/**
	 * Return an Area of un-revealed Locations adjacent to this one
	 */
	protected Area getAdjacentUnrevealedArea(Location loc) {

		return new Area(getAdjacentUnrevealedSquares(loc, 1));
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
	 * Return the Location for this position, avoids having to instantiate one
	 */
	protected Location getLocation(int x, int y) {
		if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
			return null;
		} else {
			return cache.getLocation(x, y);
		}

	}
	
	
	/**
	 * This returns the adjacent squares to a depth of size away. So (l,2) returns the 24 squares 2 deep surrounding l.
	 */
	protected Iterable<Location> getAdjacentSquaresIterable(Location loc, int size) {
		if (size == 1) {
			return getAdjacentSquaresIterable(loc);
		} else {
			return cache.adjacentLocations2[loc.x][loc.y];
		}
		
	}

	protected Iterable<Location> getAdjacentSquaresIterable(Location loc) {
		return cache.adjacentLocations1[loc.x][loc.y];
	}


	/**
	 * Done as part of validating Locations, must be undone using clearWitness()
	 */
	protected void setWitnessValue(Location l, int value) {    	 
		board[l.x][l.y] = value;
		revealed[l.x][l.y] = true;
		
		testMoveBalance++;
		
	}

	/**
	 * Done as part of validating Locations
	 */
	protected void clearWitness(Location l) {    	 
		board[l.x][l.y] = 0;
		revealed[l.x][l.y] = false;
		
		testMoveBalance--;
		
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
		return flagOnBoard[x][y] && !confirmedMine[x][y];
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

	protected void setMineFound(Location loc) {

		if (isConfirmedMine(loc)) {
			return;
		}

		confirmedMinesTotal++;
		confirmedMine[loc.x][loc.y] = true;

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

	protected void unsetMineFound(Location loc) {

		if (!isConfirmedMine(loc)) {
			return;
		}

		confirmedMinesTotal--;
		confirmedMine[loc.x][loc.y] = false;


		// if the flag isn't already on the board then this is also another on the total of all flags
		if (!flagOnBoard[loc.x][loc.y]) {
			totalFlags--;
		}

		// let all the adjacent squares know they have one less mine next to them and one more unrevealed location
		for (Location a: getAdjacentSquaresIterable(loc)) {
			adjFlagsConfirmed[a.x][a.y]--;
			adjUnrevealed[a.x][a.y]++;
		}
		
	}
	
	/**
	 * Since Flag Free is a thing, we can't rely on the GameState to tell us where the flags are,
	 * so this replaces that.
	 */
	protected boolean isConfirmedMine(Location l) {
		return isConfirmedMine(l.x, l.y);
	}

	/**
	 * Since Flag Free is a thing, we can't rely on the GameState to tell us where the flags are,
	 * so this replaces that.
	 */
	protected boolean isConfirmedMine(int x, int y) {
		return confirmedMine[x][y];
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
		return !confirmedMine[x][y] && !revealed[x][y];
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
			if (x + DX[i] >= 0 && x + DX[i] < width && y + DY[i] >= 0 && y + DY[i] < height) {
				if (confirmedMine[x + DX[i]][y + DY[i]] || flagOnBoard[x + DX[i]][y + DY[i]]) {
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

		return adjUnrevealed[x][y];

	}    

	/**
	 * count how many squares are neither confirmed flags nor revealed
	 */    
	protected int getTotalUnrevealedCount() {

		return numOfHidden;

	}

	/**
	 * Returns the number of mines the solver knows the location of
	 */
	protected int getConfirmedMineCount() {

		return confirmedMinesTotal;

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

		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {

				// if there is an unconfirmed flag on the board but the solver
				// thinks it is clear then the flag is wrong
				if (isUnconfirmedFlag(i,j) && action[i][j] != null && action[i][j].getAction() == Action.CLEAR) {
					getLogger().log(Level.INFO, "Flag in Error at (%d, %d) confirmed CLEAR", i, j);
					return false;
				}

				if (isRevealed(i,j) && getWitnessValue(i,j) != 0) {
					int flags = countAllFlags(i,j);

					// if we have too many flags by a revealed square then a mistake has been made
					if (getWitnessValue(i,j) < flags) {
						getLogger().log(Level.INFO, "Flag in Error at witness (%d, %d) Overloads: Flags %d" ,i, j, flags);
						return false;
					}
				}

			}        

		}

		return true;

	}

	/**
	 * Returns true if the board is consider high mine density
	 * @return
	 */
	public boolean isHighDensity() {
		
		int minesLeft = getMines() - getConfirmedMineCount();
		int tilesLeft = getTotalUnrevealedCount();
		
		return (minesLeft * 5 > tilesLeft * 2) && Solver.CONSIDER_HIGH_DENSITY_STRATEGY;
	}
	

	public int getTestMoveBalance() {
		return this.testMoveBalance;
	}
	
	protected Logger getLogger() {
		return solver.logger;
	}
	
	//protected void display(String text) {
	//	solver.display(text);
	//}

	protected Solver getSolver() {
		return solver;
	}

}
