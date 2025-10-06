package minesweeper.solver;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class LongTermRiskHelper {

	private final static BigDecimal HALF = new BigDecimal("0.5");
	
	//private final static BigDecimal APPROX_THRESHOLD = new BigDecimal("0.01");
	private final static BigDecimal APPROX_THRESHOLD = new BigDecimal("0.00");
	private final static BigDecimal CORRECT_THRESHOLD = new BigDecimal("0.000");
	private final static BigDecimal FINAL_THRESHOLD = new BigDecimal("0.025"); //TODO was 0.025
	
	// a hotspot is considered risky if it's safety is less than this value
	private final static BigDecimal HOTSPOT_ENABLER_SAFETY = new BigDecimal("0.9");
	
	private class Result {
		private final BigInteger influence;
		private final List<Location> enablers;   // these are mines which needed to form the outer part of the 50/50
		
		private Result(BigInteger influence, List<Location> enablers) {
			this.influence = influence;
			this.enablers = enablers;
		}
	}
	
	private class MineDetails {
		private final List<Location> present;   // these are mines which needed to form the outer part of the 50/50 and are present
		private final List<Location> missing;   // these are mines which needed to form the outer part of the 50/50 but are missing
		
		private MineDetails(List<Location> present, List<Location> missing) {
			this.present = present;
			this.missing = missing;
		}
	}
	
	// a class to identify risk hot spots
	public class RiskHotspot {
		
		private final Location hotspot;
		private final BigDecimal ltrSafety;
		private final List<Location> exemptions;
		
		private RiskHotspot(Location hotspot, BigDecimal ltrSafety, Location... exemptions) {
			this.hotspot = hotspot;
			this.exemptions = Arrays.asList(exemptions);
			this.ltrSafety = ltrSafety;
		}
		
		public Location getHotpsot() {
			return this.hotspot;
		}
		
		public BigDecimal getLtrSafety() {
			return this.ltrSafety;
		}
		
		// exempt if you are the hotspot or part of the possible 50/50
		public boolean isExempt(Location loc) {
			
			if (loc.equals(this.hotspot)) {
				return true;
			}
			
			for (Location l: this.exemptions) {
				if (l.equals(loc)) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	// a class to identify risk hot spots
	public class Possible5050 {
		
		private final BigDecimal probability;  // chance a 50/50 forms here
		private final List<Location> exemptions = new ArrayList<>();
		
		private Possible5050(BigDecimal probability, List<Location> enablers, Location[] area) {
			this.exemptions.addAll(enablers);
			this.exemptions.addAll(Arrays.asList(area));
			
			this.probability = probability;
		}
		
		public BigDecimal get5050Probability() {
			return this.probability;
		}
		
		// exempt if you are an enabler or part of the possible 50/50
		public boolean isExempt(Location loc) {
			
			for (Location l: this.exemptions) {
				if (l.equals(loc)) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	private final BoardState board;
	private final WitnessWeb wholeEdge;
	private final ProbabilityEngineModel currentPe;
	
	// the number of solutions that come from 50/50s for each tile 
	// this is only an approximation based on the most common types of 50/50
	private BigInteger[][] influence5050s;
	private BigInteger[][] influenceEnablers;
	//private Location pseudo;
	private List<Location> pseudos = new ArrayList<>();
	
	private final List<Location> mines = new ArrayList<>();
	private final List<Location> notMines = new ArrayList<>();
	
	// found hotspots
	private final List<RiskHotspot> hotspots = new ArrayList<>();
	private boolean storeHotspots = true;
	private BigDecimal minHotspotSafety = BigDecimal.ZERO;
	
	// possible 50/50s 
	private final List<Possible5050> possible5050s = new ArrayList<>();
	
	
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
	public List<Location> findInfluence() {
		
		// if we aren't considering long term safety then we can skip all of this
		if (!this.board.getSolver().preferences.considerLongTermSafety()) {
			return pseudos;
		}
		
		checkFor2Tile5050();

		/// if we've found a pseudo no need to check these
		if (pseudos.isEmpty()) {
			checkForBox5050();
		}
		
		// after the full sweep don't record them during the individual tile 
		storeHotspots = false;
		
		// if none of the hot spots are very dangerous then ignore them
		if (this.minHotspotSafety.compareTo(BigDecimal.valueOf(0.6)) > 0) {
			hotspots.clear();
		}
		
		return pseudos;
		
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
		
		/*
		int maxMines;
		if (this.board.getSolver().preferences.isTestMode()) {
			maxMines = 3;
		} else {
			maxMines = 2;
		}
		*/
		
		final int minMissingMines = 2;
		final int maxMissingMines = 2; //TODO was 2
		
		
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
					BigDecimal influenceRatio = new BigDecimal(influenceTally).divide(new BigDecimal(currentPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					board.getLogger().log(Level.INFO, "%s and %s have horiontal 2-tile 50/50 influence %f", tile1, tile2, influenceRatio);
					
					if (influenceRatio.compareTo(CORRECT_THRESHOLD) < 0) {
						continue;
					}
					
					checkForPseudo(result, tile1, tile2);
					if (!pseudos.isEmpty()) {
						return;
					}
					
					addInfluence(influenceTally, influenceRatio, result, tile1, tile2);

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
					BigDecimal influenceRatio = new BigDecimal(influenceTally).divide(new BigDecimal(currentPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					board.getLogger().log(Level.INFO, "%s and %s have vertical 2-tile 50/50 influence %f", tile1, tile2, influenceRatio);
					
					if (influenceRatio.compareTo(CORRECT_THRESHOLD) < 0) {
						continue;
					}
					
					checkForPseudo(result, tile1, tile2);
					if (!pseudos.isEmpty()) {
						return;
					}
					
					addInfluence(influenceTally, influenceRatio, result, tile1, tile2);

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
		
		MineDetails md = getMissingMines(board.getLocation(i-1, j-1), board.getLocation(i-1, j), board.getLocation(i-1, j+1),
				board.getLocation(i+2, j-1), board.getLocation(i+2, j), board.getLocation(i+2, j+1));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (md == null || md.missing.size() + 1 > maxMissingMines || md.missing.size() + 1 > minesLeft) {
			return null;
		}
		
		
		Location tile1 = subject;
		Location tile2 = board.getLocation(i + 1, j);

		// 1 missing enabler and a 50% chance to be a mine
		if (md.missing.size() == 1  && this.storeHotspots) {
			Box box = this.currentPe.getBox(subject);
			if (box != null) {
				if (box.getTally().multiply(BigInteger.valueOf(2)).compareTo(this.currentPe.getSolutionCount()) == 0) {
					
					Location enabler = md.missing.get(0);
					BigDecimal safety = this.currentPe.getSafety(enabler);
					BigDecimal ltrSafety = HALF.add(safety.multiply(HALF));
					this.minHotspotSafety = this.minHotspotSafety.min(safety);
					
					if (safety.compareTo(HOTSPOT_ENABLER_SAFETY) < 0) {
						//savePosition("Hotspot");
						hotspots.add(new RiskHotspot(enabler, ltrSafety, tile1, tile2));
					}
				}
			}
		}
		
		BigDecimal approxChance = calculateApproxChanceOf5050(md.missing, tile1);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s - approx chance %f", tile1, tile2, approxChance);
		
		// if the estimate chance is too low then don't consider it
		if (md.missing.size() + 1 > minMissingMines && approxChance.compareTo(APPROX_THRESHOLD) < 0) {
			return null;
		}
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(md.missing);
		mines.add(tile1);
		notMines.add(tile2);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		return new Result(counter.getSolutionCount(), md.missing);
		
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
		
		MineDetails md = getMissingMines(board.getLocation(i-1, j-1), board.getLocation(i, j - 1), board.getLocation(i + 1, j - 1),
				board.getLocation(i - 1, j + 2), board.getLocation(i, j + 2), board.getLocation(i + 1, j + 2));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (md == null || md.missing.size() + 1 > maxMissingMines || md.missing.size() + 1 > minesLeft) {
			return null;
		}

		Location tile1 = subject;
		Location tile2 = board.getLocation(i, j + 1);
		
		// 1 missing enabler and a 50% chance to be a mine
		if (md.missing.size() == 1 && this.storeHotspots) {
			Box box = this.currentPe.getBox(subject);
			if (box != null) {
				
				if (box.getTally().multiply(BigInteger.valueOf(2)).compareTo(this.currentPe.getSolutionCount()) == 0) {
					
					Location enabler = md.missing.get(0);					
					BigDecimal safety = this.currentPe.getSafety(enabler);
					BigDecimal ltrSafety = HALF.add(safety.multiply(HALF));
					this.minHotspotSafety = this.minHotspotSafety.min(safety);
					
					if (safety.compareTo(HOTSPOT_ENABLER_SAFETY) < 0) {
						//savePosition("Hotspot");
						hotspots.add(new RiskHotspot(enabler, ltrSafety, tile1, tile2));
					}
				}
			}
		}
		
		BigDecimal approxChance = calculateApproxChanceOf5050(md.missing, tile1);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s - approx chance %f", tile1, tile2, approxChance);
		
		// if the estimate chance is too low then don't consider it
		if (md.missing.size() + 1 > minMissingMines && approxChance.compareTo(APPROX_THRESHOLD) < 0) {
			return null;
		}
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(md.missing);
		mines.add(tile1);
		notMines.add(tile2);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		return new Result(counter.getSolutionCount(), md.missing);
		
	}

	private void checkForBox5050() {
		
		final int minMissingMines = 2;
		final int maxMissingMines = 3;  //TODO was 3
		
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
					
					BigDecimal influenceRatio = new BigDecimal(influenceTally).divide(new BigDecimal(currentPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					board.getLogger().log(Level.INFO, "%s %s %s %s have box 4-tile 50/50 influence %f", tile1, tile2, tile3, tile4, influenceRatio);
					
					if (influenceRatio.compareTo(CORRECT_THRESHOLD) < 0) {
						continue;
					}
					
					checkForPseudo(result, tile1, tile2, tile3, tile4);
					if (!pseudos.isEmpty()) {
						
						/*
						if (result.enablers.size() > 1) {
							File saveFile = new File("C:\\Users\\david\\Documents\\Minesweeper\\Positions\\Saved", "Pos_" + board.getSolver().getGame().getSeed() + ".mine");
							try {
								System.out.println("Saving position in file " + saveFile.getAbsolutePath());
								board.getSolver().getGame().savePosition(saveFile, "Pseudo");
							} catch (Exception e) {
								System.out.println("Save position failed: " + e.getMessage());
							}
							
						}
						*/
						
						return;
					}
					
					addInfluence(influenceTally, influenceRatio, result, tile1, tile2, tile3, tile4);
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
		
		MineDetails md = getMissingMines(board.getLocation(i - 1, j - 1), board.getLocation(i + 2, j - 1), board.getLocation(i - 1, j + 2), board.getLocation(i + 2, j + 2));
		
		// only consider possible 50/50s with less than 3 missing mines or requires more mines then are left in the game (plus 1 to allow for the extra mine in the 50/50)
		if (md == null || md.missing.size() + 2 > maxMissingMines || md.missing.size() + 2 > minesLeft) {
			return null;
		}
		
		Location tile1 = board.getLocation(i, j);
		Location tile2 = board.getLocation(i, j + 1);
		Location tile3 = board.getLocation(i + 1, j);
		Location tile4 = board.getLocation(i + 1, j + 1);
		
		BigDecimal approxChance = calculateApproxChanceOf5050(md.missing, tile1, tile4);
		
		board.getLogger().log(Level.INFO, "Evaluating candidate 50/50 - %s %s %s %s - approx chance %f", tile1, tile2, tile3, tile4, approxChance);
		
		// if the estimate chance is too low then don't consider it
		if (md.missing.size() + 2 > minMissingMines && approxChance.compareTo(APPROX_THRESHOLD) < 0) {
			return null;
		}
		
		mines.clear();
		notMines.clear();
		
		// add the missing Mines and the mine required to form the 50/50
		mines.addAll(md.missing);
		mines.add(tile1);
		mines.add(tile4);
		notMines.add(tile2);
		notMines.add(tile3);
		SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, notMines, Area.EMPTY_AREA);
		
		board.getLogger().log(Level.INFO, "Candidate 50/50 - %s %s %s %s influence %d", tile1, tile2, tile3, tile4, counter.getSolutionCount());
		
		return new Result(counter.getSolutionCount(), md.missing);
		
	}
	
	private BigDecimal calculateApproxChanceOf5050(List<Location> missingMines, Location... other) {
		
		BigDecimal result = BigDecimal.ONE;
		
		for (Location tile: missingMines) {
			result = result.multiply(BigDecimal.ONE.subtract(this.currentPe.getSafety(tile)));
		}
		
		for (Location tile: other) {
			result = result.multiply(BigDecimal.ONE.subtract(this.currentPe.getSafety(tile)));
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
	
	//private void addInfluence(BigInteger influence, List<Location> enablers, Location... tiles) {
	private void addInfluence(BigInteger influenceTally, BigDecimal influenceRatio, Result result, Location... tiles) {
		

		// the tiles which enable a 50/50 but aren't in it also get an influence
		if (result.enablers != null && influenceRatio.compareTo(FINAL_THRESHOLD) > 0) {
			
			possible5050s.add(new Possible5050(influenceRatio, result.enablers, tiles));
			
			for (Location loc: result.enablers) {
				
				// store the influence
				if (influenceEnablers[loc.x][loc.y] == null) {
					influenceEnablers[loc.x][loc.y] = influenceTally;
				} else {
					influenceEnablers[loc.x][loc.y] = function(influenceEnablers[loc.x][loc.y],influenceTally);
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

			board.getLogger().log(Level.INFO, "Tile %s has mine tally %d of which %d are in a 50/50.", loc, mineTally, result.influence);
			
			if (influenceRatio.compareTo(FINAL_THRESHOLD) > 0) {
				// store the influence
				if (influence5050s[loc.x][loc.y] == null) {
					influence5050s[loc.x][loc.y] = influenceTally;
				} else {
					//influences[loc.x][loc.y] = influences[loc.x][loc.y].max(influence);
					influence5050s[loc.x][loc.y] = function(influence5050s[loc.x][loc.y],influenceTally);
				}
			}


		}
	
	}
	
	// see if any of the tiles in this possible 50/50 are a pseudo
	private void checkForPseudo(Result result, Location... tiles) {
		
		for (Location loc: tiles) {
			
			Box b = currentPe.getBox(loc);
			BigInteger mineTally;
			if (b == null) {
				mineTally = currentPe.getOffEdgeTally();
			} else {
				mineTally = b.getTally();
			}			

			board.getLogger().log(Level.INFO, "Tile %s has mine tally %d of which %d are in a 50/50.", loc, mineTally, result.influence);
			
			if (result.influence.compareTo(mineTally) == 0) {
				if (!currentPe.getDeadLocations().contains(loc)) {  // don't accept dead tiles
					board.getLogger().log(Level.INFO, "Tile %s is a 50/50, or safe", loc);
					pseudos.add(loc);
				}
			}
		}
	
	}
	
	/*
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
	
	// get the enabler locations which are likely to turn into 50/50s
	public List<RiskHotspot> getRiskHotspots() {
		return this.hotspots;
	}
	
	// get possible 50/50s
	public List<Possible5050> getPossible5050s() {
		return this.possible5050s;
	}
	
	// should we add or use max?  Make a single place to change.
	private BigInteger function(BigInteger a, BigInteger b) {
		
		return a.add(b);
	}
	
	// given a list of tiles return those which are on the board but not a mine
	// if any of the tiles are revealed then return null
	private MineDetails getMissingMines(Location... tiles) {
		
		List<Location> missing = new ArrayList<>();
		List<Location> present = new ArrayList<>();
		
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
	    		present.add(loc);
	    		continue;
	    	}
	    	
	    	missing.add(loc);
		}
		
		return new MineDetails(present, missing);
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
	
	private void savePosition(String text) {
		
		File saveFile = new File("C:\\Users\\david\\Documents\\Minesweeper\\Positions\\Saved", "Pos_" + board.getSolver().getGame().getSeed() + ".mine");
		if (saveFile.exists()) {
			return;
		}
		try {
			System.out.println("Saving position in file " + saveFile.getAbsolutePath());
			board.getSolver().getGame().savePosition(saveFile, text);
		} catch (Exception e) {
			System.out.println("Save position failed: " + e.getMessage());
		}
		
	}
}
