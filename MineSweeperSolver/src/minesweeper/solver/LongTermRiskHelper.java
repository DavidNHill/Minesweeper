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

	private final static BigDecimal APPROX_THRESHOLD = new BigDecimal("0.01");
	
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
	private BigInteger[][] influence5050s;
	private BigInteger[][] influenceEnablers;
	private Location pseudo;
	
	final List<Location> mines = new ArrayList<>();
	final List<Location> notMines = new ArrayList<>();
	
	public LongTermRiskHelper(BoardState board, WitnessWeb wholeEdge, ProbabilityEngineModel pe)  {
		
		this.board = board;
		this.wholeEdge = wholeEdge;
		this.currentPe = pe;
		
		influence5050s = new BigInteger[board.getGameWidth()][board.getGameHeight()];
		influenceEnablers = new BigInteger[board.getGameWidth()][board.getGameHeight()];
		
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
		
		final int minesLeft = board.getMines() - board.getConfirmedMineCount();
		
		BigInteger influence = BigInteger.ZERO;
		
		Location tile1, tile2, tile3;
		
		// 2-tile 50/50
		tile1 = board.getLocation(tile.x - 1, tile.y);
		
		BigInteger influence1 = BigInteger.ZERO;
		influence1 = maxNotNull(BigInteger.ZERO, getHorizontal(tile, 4, 4, minesLeft));
		influence1 = maxNotNull(influence1, getHorizontal(tile1, 4, 4, minesLeft));

		influence = influence.add(influence1);
		
		tile2 = board.getLocation(tile.x, tile.y - 1);
		
		BigInteger influence2 = BigInteger.ZERO;
		influence2 = maxNotNull(influence2, getVertical(tile, 4, 4, minesLeft));
		influence2 = maxNotNull(influence2, getVertical(tile2, 4, 4, minesLeft));
		
		influence = influence.add(influence2);
		
		// 4-tile 50/50
		tile3 = board.getLocation(tile.x - 1, tile.y - 1);
	
		BigInteger influence4 = BigInteger.ZERO;
		influence4 = maxNotNull(influence4, getBoxInfluence(tile, 5, 5, minesLeft));
		influence4 = maxNotNull(influence4, getBoxInfluence(tile1, 5, 5, minesLeft));
		influence4 = maxNotNull(influence4, getBoxInfluence(tile2, 5, 5, minesLeft));
		influence4 = maxNotNull(influence4, getBoxInfluence(tile3, 5, 5, minesLeft));

		influence = influence.add(influence4);
		
		board.getLogger().log(Level.INFO, "Tile %s base influence tally %d", tile, influence);
		
		// enablers also get influence as playing there also removes the 50/50 risk, so consider that as well as the 50/50
		if (influenceEnablers[tile.x][tile.y] != null) {
			influence = influence.add(influenceEnablers[tile.x][tile.y]);
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
		
		final int minMissingMines = 2;
		final int maxMissingMines = 2;
		
		board.getLogger().log(Level.INFO, "Checking for 2-tile 50/50 influence");
    	
	   	final int minesLeft = board.getMines() - board.getConfirmedMineCount();

    	// horizontal 2x1
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight(); j++) {

				Location tile1 = board.getLocation(i, j);
				Location tile2 = board.getLocation(i + 1, j);
				
				Result result = getHorizontal(tile1, minMissingMines, maxMissingMines, minesLeft);
		
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

				Location tile1 = board.getLocation(i, j);
				Location tile2 = board.getLocation(i, j + 1);
				
				Result result = getVertical(tile1, minMissingMines, maxMissingMines, minesLeft);
				
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

	private Result getHorizontal(final Location subject, final int minMissingMines, final int maxMissingMines, final int minesLeft) {
		
		if (subject == null) {
			return null;
		}
		
		int i = subject.x;
		int j = subject.y;
		
		if (i < 0 || i + 1 >= board.getGameWidth()) {  // need 1 extra space to the right
			return null;
		}
		
		// need 2 hidden tiles
		if (!isHidden(i, j) || !isHidden(i + 1, j)) {
			return null;
		}
		
		List<Location> missingMines = getMissingMines(board.getLocation(i-1, j-1), board.getLocation(i-1, j), board.getLocation(i-1, j+1),
				board.getLocation(i+2, j-1), board.getLocation(i+2, j), board.getLocation(i+2, j+1));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (missingMines == null || missingMines.size() + 1 > maxMissingMines || missingMines.size() + 1 > minesLeft) {
			return null;
		}
		
		Location tile1 = subject;
		Location tile2 = board.getLocation(i + 1, j);

		BigDecimal approxChance = calculateApproxChanceOf5050(missingMines, tile1);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s - approx chance %f", tile1, tile2, approxChance);
		
		// if the estimate chance is too low then don't consider it
		if (missingMines.size() + 1 > minMissingMines && approxChance.compareTo(APPROX_THRESHOLD) < 0) {
			return null;
		}
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(missingMines);
		mines.add(tile1);
		notMines.add(tile2);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		return new Result(counter.getSolutionCount(), missingMines);
		
	}
	
	private Result getVertical(final Location subject, final int minMissingMines, final int maxMissingMines, final int minesLeft) {
		
		if (subject == null) {
			return null;
		}
		
		int i = subject.x;
		int j = subject.y;
		
		if (j < 0 || j + 1 >= board.getGameHeight()) { // need 1 extra space below
			return null;
		}
		
		// need 2 hidden tiles
		if (!isHidden(i, j) || !isHidden(i, j + 1)) {
			return null;
		}
		
		List<Location> missingMines = getMissingMines(board.getLocation(i-1, j-1), board.getLocation(i, j - 1), board.getLocation(i + 1, j - 1),
				board.getLocation(i - 1, j + 2), board.getLocation(i, j + 2), board.getLocation(i + 1, j + 2));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (missingMines == null || missingMines.size() + 1 > maxMissingMines || missingMines.size() + 1 > minesLeft) {
			return null;
		}
		
		Location tile1 = board.getLocation(i, j);
		Location tile2 = board.getLocation(i, j + 1);
		
		BigDecimal approxChance = calculateApproxChanceOf5050(missingMines, tile1);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s - approx chance %f", tile1, tile2, approxChance);
		
		// if the estimate chance is too low then don't consider it
		if (missingMines.size() + 1 > minMissingMines && approxChance.compareTo(APPROX_THRESHOLD) < 0) {
			return null;
		}
		
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
		
		final int minMissingMines = 2;
		final int maxMissingMines = 2;
		
	   	int minesLeft = board.getMines() - board.getConfirmedMineCount();
		
		board.getLogger().log(Level.INFO, "Checking for 2-tile 50/50 influence: Mines left %d", minesLeft);
	   	
    	// box 2x2 
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {

				Location tile1 = board.getLocation(i, j);
				Location tile2 = board.getLocation(i, j + 1);
				Location tile3 = board.getLocation(i + 1, j);
				Location tile4 = board.getLocation(i + 1, j + 1);
				
				Result result = getBoxInfluence(tile1, minMissingMines, maxMissingMines, minesLeft);
				
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
	
	private Result getBoxInfluence(final Location subject, final int minMissingMines, final int maxMissingMines, final int minesLeft) {
		
		if (subject == null) {
			return null;
		}
		
		int i = subject.x;
		int j = subject.y;
		
		if (j < 0 || j + 1 >= board.getGameHeight() || i < 0 || i + 1 >= board.getGameWidth()) { // need 1 extra space to the right and below
			return null;
		}
		
		// need 4 hidden tiles
		if (!isHidden(i, j) || !isHidden(i, j + 1) || !isHidden(i + 1, j) || !isHidden(i + 1, j + 1)) {
			return null;
		}
		
		List<Location> missingMines = getMissingMines(board.getLocation(i - 1, j - 1), board.getLocation(i + 2, j - 1), board.getLocation(i - 1, j + 2), board.getLocation(i + 2, j + 2));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (missingMines == null || missingMines.size() + 2 > maxMissingMines || missingMines.size() + 2 > minesLeft) {
			return null;
		}
		
		Location tile1 = board.getLocation(i, j);
		Location tile2 = board.getLocation(i, j + 1);
		Location tile3 = board.getLocation(i + 1, j);
		Location tile4 = board.getLocation(i + 1, j + 1);
		
		BigDecimal approxChance = calculateApproxChanceOf5050(missingMines, tile1, tile4);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s %s %s - approx chance %f", tile1, tile2, tile3, tile4, approxChance);
		
		// if the estimate chance is too low then don't consider it
		if (missingMines.size() + 2 > minMissingMines && approxChance.compareTo(APPROX_THRESHOLD) < 0) {
			return null;
		}
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(missingMines);
		mines.add(tile1);
		mines.add(tile4);
		notMines.add(tile2);
		notMines.add(tile3);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		board.getLogger().log(Level.INFO, "Candidate 50/50 - %s %s %s %s influence %d", tile1, tile2, tile3, tile4, counter.getSolutionCount());
		
		return new Result(counter.getSolutionCount(), missingMines);
		
	}
	
	private BigDecimal calculateApproxChanceOf5050(List<Location> missingMines, Location... other) {
		
		BigDecimal result = BigDecimal.ONE;
		
		for (Location tile: missingMines) {
			result = result.multiply(BigDecimal.ONE.subtract(this.currentPe.getProbability(tile)));
		}
		
		for (Location tile: other) {
			result = result.multiply(BigDecimal.ONE.subtract(this.currentPe.getProbability(tile)));
		}
		
		return result;
		
	}
	
	private BigInteger addNotNull(BigInteger influence, Result result) {
		
		if (result == null) {
			return influence;
		} else {
			return function(influence, result.influence);
		}
	
	}
	
	private BigInteger maxNotNull(BigInteger influence, Result result) {
		
		if (result == null) {
			return influence;
		} else {
			return influence.max(result.influence);
		}
	
	}
	
	private void addInfluence(BigInteger influence, List<Location> enablers, Location... tiles) {
		
		List<Location> pseudos = new ArrayList<>();
		
		// the tiles which enable a 50/50 but aren't in it also get an influence

		if (enablers != null) {
			
			//BigInteger influence2 = influence.multiply(BigInteger.valueOf(2)).divide(BigInteger.valueOf(3));
			
			for (Location loc: enablers) {
				
				// store the influence
				if (influenceEnablers[loc.x][loc.y] == null) {
					influenceEnablers[loc.x][loc.y] = influence;
				} else {
					influenceEnablers[loc.x][loc.y] = function(influenceEnablers[loc.x][loc.y],influence);
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
			//if (influence.compareTo(mineTally) == 0 && pseudo == null) {
			//	if (!currentPe.getDeadLocations().contains(loc)) {  // don't accept dead tiles
			//		//board.getLogger().log(Level.INFO, "Tile %s is a 50/50, or safe", loc);
			//		pseudo = loc;
			//	}
			//}
			if (influence.compareTo(mineTally) == 0 && pseudo == null) {
				if (!currentPe.getDeadLocations().contains(loc)) {  // don't accept dead tiles
					//board.getLogger().log(Level.INFO, "Tile %s is a 50/50, or safe", loc);
					pseudos.add(loc);
				}
			}
			// store the influence
			if (influence5050s[loc.x][loc.y] == null) {
				influence5050s[loc.x][loc.y] = influence;
			} else {
				//influences[loc.x][loc.y] = influences[loc.x][loc.y].max(influence);
				influence5050s[loc.x][loc.y] = function(influence5050s[loc.x][loc.y],influence);
			}

		}
	
		if (pseudos.size() == 3) {
			pickPseudo(pseudos);
		} else if (!pseudos.isEmpty()) {
			pseudo = pseudos.get(0);
		}
		
		
	}
	
	private void pickPseudo(List<Location> locations) {
		
		int maxX = 0;
		int maxY = 0;
		
		for (Location loc: locations) {
			maxX = Math.max(maxX, loc.x);
			maxY = Math.max(maxY, loc.y);
		}
		
		int maxX1 = maxX - 1;
		int maxY1 = maxY - 1;
		
		int found = 0;
		// see if this diagonal exists in the pseudo candidates
		for (Location loc: locations) {
			if (loc.x == maxX && loc.y == maxY || loc.x == maxX1 && loc.y == maxY1) {
				found++;
			}
		}

		// if the 2 diagonals exist then choose the pseudo from those, other wise choose the pseudo from the other diagonal
		if (found == 2) {
			pseudo = board.getLocation(maxX, maxY);
		} else {
			pseudo = board.getLocation(maxX - 1, maxY);
		}
		
	}
	
	
	
	/**
	 * Get how many solutions have common 50/50s at this location
	 */
	/*
	public BigInteger get5050Influence(Location loc) {
		
		BigInteger result = BigInteger.ZERO;

		if (influence5050s[loc.x][loc.y] != null) {
			result = result.add(influence5050s[loc.x][loc.y]);
		}
		
		if (influenceEnablers[loc.x][loc.y] != null) {
			result = result.add(influenceEnablers[loc.x][loc.y]);
		}
		
		return result;
	}
	*/
	
	/**
	 * Return all the locations with 50/50 influence
	 */
	public List<Location> getInfluencedTiles(BigDecimal threshold) {
		
		BigInteger cuttoffTally = threshold.multiply(new BigDecimal(currentPe.getSolutionCount())).toBigInteger();
		
		List<Location> result = new ArrayList<>();
		
		for (int i=0; i < board.getGameWidth(); i++) {
			for (int j=0; j < board.getGameHeight(); j++) {
				
				BigInteger influence = BigInteger.ZERO;

				if (influence5050s[i][j] != null) {
					influence = influence.add(influence5050s[i][j]);
				}
				
				if (influenceEnablers[i][j] != null) {
					influence = influence.add(influenceEnablers[i][j]);
				}
				
				
				if (influence.signum() !=0 ) {	  // if we are influenced by 50/50s

					Location loc = board.getLocation(i,j);
	
					if (!currentPe.getDeadLocations().contains(loc)) {  // and not dead
						
						Box b = currentPe.getBox(loc);
						BigInteger mineTally;
						if (b == null) {
							mineTally = currentPe.getOffEdgeTally();
						} else {
							mineTally = b.getTally();
						}			
	
						BigInteger safetyTally = currentPe.getSolutionCount().subtract(mineTally).add(influence);

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
	
	// should we add or use max?  Make a single place to change.
	private BigInteger function(BigInteger a, BigInteger b) {
		return a.add(b);
	}
	
	// given a list of tiles return those which are on the board but not a mine
	// if any of the tiles are revealed then return null
	private List<Location> getMissingMines(Location... tiles) {
		
		List<Location> result = new ArrayList<>();
		
		for (Location loc: tiles) {
			
			// if out of range don't return the location
			if (loc == null) {
				continue;
			}
			
			// if the tile is revealed then we can't form a 50/50 here
			if (board.isRevealed(loc)) {
				return null;
			}
			
			// if the location is already a mine then don't return the location
	    	if (board.isConfirmedMine(loc) || isMineInPe(loc.x, loc.y)) {
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
    	
    	if (board.isConfirmedMine(x, y) || isMineInPe(x, y)) {
    		return false;
    	} else {
    		return true;
    	}
    	
    }
	
    // not a certain mine or revealed
    private boolean isHidden(int x, int y) {
    	
    	if (board.isConfirmedMine(x, y)) {
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
