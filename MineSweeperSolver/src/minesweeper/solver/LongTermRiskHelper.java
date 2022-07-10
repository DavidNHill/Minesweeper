package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class LongTermRiskHelper {

	private class Result {
		private final BigInteger influence;
		private final List<Location> enablers;   // these are mines which needed to form the outer part of the 50/50
		
		private Result(BigInteger influence, List<Location> enablers) {
			this.influence = influence;
			this.enablers = enablers;
		}
	}
	
	private final BoardState board;
	private final WitnessWeb wholeEdge;
	private final ProbabilityEngineModel currentPe;
	
	// the number of solutions that come from 50/50s for each tile 
	// this is only an approximation based on the most common types of 50/50
	private BigInteger[][] influences;
	private Location pseudo;
	
	final List<Location> mines = new ArrayList<>();
	final List<Location> notMines = new ArrayList<>();
	
	public LongTermRiskHelper(BoardState board, WitnessWeb wholeEdge, ProbabilityEngineModel pe)  {
		
		this.board = board;
		this.wholeEdge = wholeEdge;
		this.currentPe = pe;
		
		influences = new BigInteger[board.getGameWidth()][board.getGameHeight()];
		
	}
	
	/**
	 * Scan whole board looking for tiles heavily influenced by 50/50s
	 */
	public Location findInfluence() {
		
		checkFor2Tile5050();
		
		checkForBox5050();
		
		if (pseudo != null) {
			board.getLogger().log(Level.INFO, "Tile %s is a 50/50, or safe", pseudo);
		}
		
		return pseudo;
		
	}

	/**
	 * Get the 50/50 influence for a particular tile
	 */
	public BigInteger findInfluence(Location tile) {
		
		final int minesLeft = board.getMines() - board.getConfirmedFlagCount();
		
		BigInteger influence = BigInteger.ZERO;
		
		Location tile1, tile2, tile3;
		
		// 2-tile 50/50
		tile1 = new Location(tile.x - 1, tile.y);
		
		influence = addNotNull(BigInteger.ZERO, getHorizontal(tile, 4, minesLeft));
		influence = addNotNull(influence, getHorizontal(tile1, 4, minesLeft));
		
		tile2 = new Location(tile.x, tile.y - 1);
		influence = addNotNull(influence, getVertical(tile, 4, minesLeft));
		influence = addNotNull(influence, getVertical(tile2, 4, minesLeft));
		
		// 4-tile 50/50
		tile3 =  new Location(tile.x - 1, tile.y - 1);
		influence = addNotNull(influence, getBoxInfluence(tile, 5, minesLeft));
		influence = addNotNull(influence, getBoxInfluence(tile1, 5, minesLeft));
		influence = addNotNull(influence, getBoxInfluence(tile2, 5, minesLeft));
		influence = addNotNull(influence, getBoxInfluence(tile3, 5, minesLeft));
		
		// enablers also get influence, so consider that as well as the 50/50
		if (influences[tile.x][tile.y] != null) {
			influence = influence.max(influences[tile.x][tile.y]);
		}
		
		BigInteger maxInfluence;
		Box box = currentPe.getBox(tile);
		if (box == null) {
			maxInfluence = currentPe.getOffEdgeTally();
		} else {
			maxInfluence = box.getTally();
		}
		
		// 50/50 influence P(50/50)/2 can't be larger than P(mine) or P(safe)
		BigInteger other = currentPe.getSolutionCount().subtract(maxInfluence);
		maxInfluence = maxInfluence.min(other);
		
		influence = influence.min(maxInfluence);
		
		return influence;
		
	}
	
	private void checkFor2Tile5050() {
		
		final int maxMissingMines = 2;
		
		board.getLogger().log(Level.INFO, "Checking for 2-tile 50/50 influence");
    	
	   	final int minesLeft = board.getMines() - board.getConfirmedFlagCount();

    	// horizontal 2x1
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight(); j++) {

				Location tile1 = new Location(i, j);
				Location tile2 = new Location(i + 1, j);
				
				Result result = getHorizontal(tile1, maxMissingMines, minesLeft);
		
				if (result != null) {
					BigInteger influenceTally = addNotNull(BigInteger.ZERO,  result);
					BigDecimal influence = new BigDecimal(influenceTally).divide(new BigDecimal(currentPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					board.getLogger().log(Level.INFO, "%s and %s have horiontal 2-tile 50/50 influence %f", tile1, tile2, influence);
					
					addInfluence(influenceTally, result.enablers, tile1, tile2);
					if (pseudo != null) {  // if we've found a pseudo then we can stop here
						return;
					}
				}
				

				
			}
		}                        
		
    	// vertical 2x1
		for (int i=0; i < board.getGameWidth(); i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {

				Location tile1 = new Location(i, j);
				Location tile2 = new Location(i, j + 1);
				
				Result result = getVertical(tile1, maxMissingMines, minesLeft);
				
				if (result != null) {
					
					BigInteger influenceTally = addNotNull(BigInteger.ZERO, result);
					BigDecimal influence = new BigDecimal(influenceTally).divide(new BigDecimal(currentPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					board.getLogger().log(Level.INFO, "%s and %s have vertical 2-tile 50/50 influence %f", tile1, tile2, influence);
					
					addInfluence(influenceTally, result.enablers, tile1, tile2);
					if (pseudo != null) {  // if we've found a pseudo then we can stop here
						return;
					}
				}
	
			}
		}                        
	}

	private Result getHorizontal(final Location subject, final int maxMissingMines, final int minesLeft) {
		
		int i = subject.x;
		int j = subject.y;
		
		if (i < 0 || i + 1 >= board.getGameWidth()) {
			return null;
		}
		
		// need 2 hidden tiles
		if (!isHidden(i, j) || !isHidden(i + 1, j)) {
			return null;
		}
		
		List<Location> missingMines = getMissingMines(new Location(i-1, j-1), new Location(i-1, j), new Location(i-1, j+1),
				new Location(i+2, j-1), new Location(i+2, j), new Location(i+2, j+1));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (missingMines == null || missingMines.size() + 1 > maxMissingMines || missingMines.size() + 1 > minesLeft) {
			return null;
		}
		
		Location tile1 = subject;
		Location tile2 = new Location(i + 1, j);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s", tile1, tile2);
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(missingMines);
		mines.add(tile1);
		notMines.add(tile2);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		return new Result(counter.getSolutionCount(), missingMines);
		
	}
	
	private Result getVertical(final Location subject, final int maxMissingMines, final int minesLeft) {
		
		int i = subject.x;
		int j = subject.y;
		
		if (j < 0 || j + 1 >= board.getGameHeight()) {
			return null;
		}
		
		// need 2 hidden tiles
		if (!isHidden(i, j) || !isHidden(i, j + 1)) {
			return null;
		}
		
		List<Location> missingMines = getMissingMines(new Location(i-1, j-1), new Location(i, j - 1), new Location(i + 1, j - 1),
				new Location(i - 1, j + 2), new Location(i, j + 2), new Location(i + 1, j + 2));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (missingMines == null || missingMines.size() + 1 > maxMissingMines || missingMines.size() + 1 > minesLeft) {
			return null;
		}
		
		Location tile1 = new Location(i, j);
		Location tile2 = new Location(i, j + 1);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s", tile1, tile2);
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(missingMines);
		mines.add(tile1);
		notMines.add(tile2);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		return new Result(counter.getSolutionCount(), missingMines);
		
	}

	private void checkForBox5050() {
		
		final int maxMissingMines = 2;
		
	   	int minesLeft = board.getMines() - board.getConfirmedFlagCount();
		
		board.getLogger().log(Level.INFO, "Checking for 2-tile 50/50 influence: Mines left %d", minesLeft);
	   	
    	// box 2x2 
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {

				Location tile1 = new Location(i, j);
				Location tile2 = new Location(i, j + 1);
				Location tile3 = new Location(i + 1, j);
				Location tile4 = new Location(i + 1, j + 1);
				
				Result result = getBoxInfluence(tile1, maxMissingMines, minesLeft);
				
				if (result != null) {
					
					BigInteger influenceTally = addNotNull(BigInteger.ZERO, result);
					
					BigDecimal influence = new BigDecimal(influenceTally).divide(new BigDecimal(currentPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					board.getLogger().log(Level.INFO, "%s %s %s %s have box 4-tile 50/50 influence %f", tile1, tile2, tile3, tile4, influence);
					
					addInfluence(influenceTally, result.enablers, tile1, tile2, tile3, tile4);
					if (pseudo != null) {  // if we've found a pseudo then we can stop here
						return;
					}
				}
				
			}
		}                        
		
	}
	
	private Result getBoxInfluence(final Location subject, final int maxMissingMines, final int minesLeft) {
		
		int i = subject.x;
		int j = subject.y;
		
		if (j < 0 || j + 1 >= board.getGameHeight() || i < 0 || i + 1 >= board.getGameWidth()) {
			return null;
		}
		
		// need 4 hidden tiles
		if (!isHidden(i, j) || !isHidden(i, j + 1) || !isHidden(i + 1, j) || !isHidden(i + 1, j + 1)) {
			return null;
		}
		
		List<Location> missingMines = getMissingMines(new Location(i - 1, j - 1), new Location(i + 2, j - 1), new Location(i - 1, j + 2), new Location(i + 2, j + 2));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (missingMines == null || missingMines.size() + 2 > maxMissingMines || missingMines.size() + 2 > minesLeft) {
			return null;
		}
		
		Location tile1 = new Location(i, j);
		Location tile2 = new Location(i, j + 1);
		Location tile3 = new Location(i + 1, j);
		Location tile4 = new Location(i + 1, j + 1);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s %s %s", tile1, tile2, tile3, tile4);
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(missingMines);
		mines.add(tile1);
		mines.add(tile4);
		notMines.add(tile2);
		notMines.add(tile3);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		return new Result(counter.getSolutionCount(), missingMines);
		
	}
	
	private BigInteger addNotNull(BigInteger influence, Result result) {
		
		if (result == null) {
			return influence;
		} else {
			return influence.add(result.influence);
		}
	
	}
	
	private void addInfluence(BigInteger influence, List<Location> enablers, Location... tiles) {
		
		// the tiles which enable a 50/50 but aren't in it also get an influence
		if (enablers != null) {
			for (Location loc: enablers) {
				
				// store the influence
				if (influences[loc.x][loc.y] == null) {
					influences[loc.x][loc.y] = influence;
				} else {
					influences[loc.x][loc.y] = influences[loc.x][loc.y].add(influence);
				}
				
			}
		}
		
		for (Location loc: tiles) {
			
			Box b = currentPe.getBox(loc);
			BigInteger mineTally;
			if (b == null) {
				mineTally = currentPe.getOffEdgeTally();
			} else {
				mineTally = b.getTally();
			}			
			// If the mine influence covers the whole of the mine tally then it is a pseudo-5050
			if (influence.compareTo(mineTally) == 0 && pseudo == null) {
				if (!currentPe.getDeadLocations().contains(loc)) {  // don't accept dead tiles
					pseudo = loc;
				}
			}
			
			// store the influence
			if (influences[loc.x][loc.y] == null) {
				influences[loc.x][loc.y] = influence;
			} else {
				//influences[loc.x][loc.y] = influences[loc.x][loc.y].max(influence);
				influences[loc.x][loc.y] = influences[loc.x][loc.y].add(influence);
			}

		}
	
	}
	
	/**
	 * Get how many solutions have common 50/50s at this location
	 */
	public BigInteger get5050Influence(Location loc) {
		
		if (influences[loc.x][loc.y] == null) {
			return BigInteger.ZERO;
		} else {
			return influences[loc.x][loc.y];
		}
		
	}

	/**
	 * Return all the locations with 50/50 influence
	 */
	public List<Location> getInfluencedTiles(BigDecimal threshold) {
		
		BigInteger cuttoffTally = threshold.multiply(new BigDecimal(currentPe.getSolutionCount())).toBigInteger();
		
		List<Location> result = new ArrayList<>();
		
		for (int i=0; i < board.getGameWidth(); i++) {
			for (int j=0; j < board.getGameHeight(); j++) {
				
				if (influences[i][j] != null) {	  // if we are influenced by 50/50s

					Location loc = new Location(i,j);
	
					if (!currentPe.getDeadLocations().contains(loc)) {  // and not dead
						
						Box b = currentPe.getBox(loc);
						BigInteger mineTally;
						if (b == null) {
							mineTally = currentPe.getOffEdgeTally();
						} else {
							mineTally = b.getTally();
						}			
	
						BigInteger safetyTally = currentPe.getSolutionCount().subtract(mineTally).add(influences[i][j]);

						if (safetyTally.compareTo(cuttoffTally) > 0) {
							board.getLogger().log(Level.INFO, "Tile %s has influence %d cutoff %d", loc, safetyTally, cuttoffTally);
							result.add(loc);
						}

					}
				}
		
			}
		}
				
		return result;
	}
	
	// given a list of tiles return those which are on the board but not a mine
	// if any of the tiles are revealed then return null
	private List<Location> getMissingMines(Location... tiles) {
		
		List<Location> result = new ArrayList<>();
		
		for (Location loc: tiles) {
			
			// if out of range don't return the location
			if (loc.x >= board.getGameWidth() || loc.x < 0 || loc.y < 0 || loc.y >= board.getGameHeight()) {
				continue;
			}
			
			// if the tile is revealed then we can't form a 50/50 here
			if (board.isRevealed(loc)) {
				return null;
			}
			
			// if the location is already a mine then don't return the location
	    	if (board.isConfirmedFlag(loc) || isMineInPe(loc.x, loc.y)) {
	    		continue;
	    	}
	    	
	    	result.add(loc);
		}
		
		return result;
	}
	

	
    // returns whether there information to be had at this location; i.e. on the board and either unrevealed or revealed
    private boolean isPotentialInfo(int x, int y) {
    	
    	if (x < 0 || x >= board.getGameWidth() || y < 0 || y >= board.getGameHeight()) {
    		return false;
    	}
    	
    	if (board.isConfirmedFlag(x, y) || isMineInPe(x, y)) {
    		return false;
    	} else {
    		return true;
    	}
    	
    }
	
    // not a certain mine or revealed
    private boolean isHidden(int x, int y) {
    	
    	if (board.isConfirmedFlag(x, y)) {
    		return false;
    	}
    	
    	if (board.isRevealed(x, y)) {
    		return false;
    	}
    	
    	//if (isMineInPe(x, y)) {
    	//	return false;
    	//}
    	
    	return true;
    	
    }
    
    
	private boolean isMineInPe(int x, int y) {
		for (Location loc: this.currentPe.getMines()) {
			if (loc.x == x && loc.y == y) {
				return true;
			}
		}
		return false;
	}
}
