package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class LongTermRiskHelperOld {

	private boolean[][] PATTERNS = new boolean[][] {{true, false, false, true}, {false, true, true, false}};  // 2 ways to make a 50/50 in a box
	
	private class Risk5050 {
		
		private Location poi;
		private List<Location> area;
		private List<Location> livingArea = new ArrayList<>();
		
		private Risk5050(Location poi, Location... locs) {
			this.poi = poi;
			this.area = Arrays.asList(locs);
			
			for (Location loc: locs) {
				if (!deadLocations.contains(loc)) {
					livingArea.add(loc);
				}
			}
			
		}
		
	}
	
	
	private final static BigDecimal HALF = new BigDecimal("0.5");
	
	private final BoardState board;
	private final WitnessWeb wholeEdge;
	private final ProbabilityEngineModel currentPe;
	private final Area deadLocations;
	private final List<Location> fifty;
	
	private BigDecimal currentLongTermSafety;
	
	private Risk5050 worstBox5050;
	private BigDecimal box5050Safety = BigDecimal.ONE;
	
	private BigDecimal twoTileSafety;
	
	private List<Risk5050> risk5050s = new ArrayList<>();
	
	public LongTermRiskHelperOld(BoardState board, WitnessWeb wholeEdge, ProbabilityEngineModel pe)  {
		
		this.board = board;
		this.wholeEdge = wholeEdge;
		this.currentPe = pe;
		this.deadLocations = pe.getDeadLocations();
		this.fifty = currentPe.getFiftyPercenters();
		
		// sort into location order
		fifty.sort(null);
		
	}
	
	public void findRisks() {
		
		checkFor2Tile5050();
		
		//checkForBox5050();
		
		this.currentLongTermSafety = this.box5050Safety.multiply(this.twoTileSafety);
		
	}

	public void checkFor2Tile5050() {
		
		BigDecimal longTermSafety = BigDecimal.ONE;
		
		for (int i=0; i < fifty.size(); i++) {
			
			Location tile1 = fifty.get(i);
			Location tile2 = null;
			
			Location info = null;
			Risk5050 risk = null;
			
			for (int j=i+1; j < fifty.size(); j++) {
				tile2 = fifty.get(j);
				
				// tile2 is below tile1
				if (tile1.x == tile2.x && tile1.y == tile2.y - 1) {
					info = checkVerticalInfo(tile1, tile2);
					
					if (info == null)  { // try extending it
						
						Location tile3 = getFifty(tile2.x, tile2.y + 2);
						Location tile4 = getFifty(tile2.x, tile2.y + 3);

						if (tile3 != null && tile4 != null) {
							info = checkVerticalInfo(tile1, tile4);
							if (info != null) {
								risk = new Risk5050(info, tile1, tile2, tile3, tile4);
							}
						}

					} else {
						risk = new Risk5050(info, tile1, tile2);
					}

					break;
				}
				
				// tile 2 is right of tile1
				if (tile1.x == tile2.x - 1 && tile1.y == tile2.y) {
					info = checkHorizontalInfo(tile1, tile2);
					
					if (info == null) { // try extending it
						
						Location tile3 = getFifty(tile2.x + 2, tile2.y);
						Location tile4 = getFifty(tile2.x + 3, tile2.y);
						
						if (tile3 != null && tile4 != null) {
							info = checkHorizontalInfo(tile1, tile4);
							if (info != null) {
								risk = new Risk5050(info, tile1, tile2, tile3, tile4);
							}
						}
					
					}  else {
						risk = new Risk5050(info, tile1, tile2);
					}
					
					break;
				}
				
			}
			
			// if the 2 fifties form a pair with only 1 remaining source of information
			if (risk != null) {
				risk5050s.add(risk);  // store the positions of interest
				
				BigDecimal safety = BigDecimal.ONE.subtract(BigDecimal.ONE.subtract(currentPe.getSafety(info)).multiply(HALF));
				board.getLogger().log(Level.INFO, "Seed %d - %s %s has 1 remaining source of information - tile %s  %f", board.getSolver().getGame().getSeed(), tile1, tile2, info, safety);
				longTermSafety = longTermSafety.multiply(safety);
			}
			
		}
		
		if (longTermSafety.compareTo(BigDecimal.ONE) != 0) {
			board.getLogger().log(Level.INFO, "Seed %d - Total long term safety %f", board.getSolver().getGame().getSeed(), longTermSafety);
		}

		
		this.twoTileSafety = longTermSafety;
		
	}
	
	private boolean isFifty(int x, int y) {
		
		return (getFifty(x, y) != null);
		
	}
	
	private Location getFifty(int x, int y) {
		
		for (Location loc: fifty) {
			if (loc.x == x && loc.y == y) {
				return loc;
			}
		}
		
		return null;
		
	}
	
	public List<Location> get5050Breakers() {
		List<Location> breakers = new ArrayList<>();
		
		if (board.getSolver().preferences.considerLongTermSafety()) {
			for (Risk5050 risk: risk5050s) {
				breakers.addAll(risk.livingArea);
				breakers.add(risk.poi);
			}
		}

		
		return breakers;
	}
	
	public BigDecimal getLongTermSafety() {
		return this.currentLongTermSafety;
	}
	
	public BigDecimal getLongTermSafety(Location candidate, ProbabilityEngineModel pe) {
		
		BigDecimal longTermSafety = null;
		
		if (board.getSolver().preferences.considerLongTermSafety()) {
			
			// if there is a possible box 50/50 then see if we are breaking it, otherwise use that as the start safety
			if (worstBox5050 != null) {
				
				if (worstBox5050.poi.equals(candidate) || pe.getSafety(worstBox5050.poi).compareTo(BigDecimal.ONE) == 0) {
					//board.getLogger().log(Level.INFO, "%s has broken 50/50", candidate);
					longTermSafety =  BigDecimal.ONE;					
				} else {
					for (Location loc: worstBox5050.area) {
						if (loc.equals(candidate)) {
							//board.getLogger().log(Level.INFO, "%s has broken 50/50", candidate);
							longTermSafety =  BigDecimal.ONE;
							break;
						}
					}					
				}
				

				if (longTermSafety == null) {
					longTermSafety = this.box5050Safety;
				}
				
			} else {
				longTermSafety = BigDecimal.ONE;
			}
			
			
			for (Risk5050 risk: this.risk5050s) {
				BigDecimal safety = null;
				
				// is the candidate part of the 50/50 - if so it is being broken
				for (Location loc: risk.area) {
					if (loc.equals(candidate)) {
						safety =  BigDecimal.ONE;
						break;
					}
				}
				
				if (safety == null) {
					if (risk.poi.equals(candidate)) {  
						safety = BigDecimal.ONE;
					} else {
						safety = BigDecimal.ONE.subtract(BigDecimal.ONE.subtract(pe.getSafety(risk.poi)).multiply(HALF));
					}				
				}
				
				longTermSafety = longTermSafety.multiply(safety);
			}			
		} else {
			longTermSafety = BigDecimal.ONE;
		}
		
		return longTermSafety;
		
	}
	
	
	// returns the location of the 1 tile which can still provide information, or null
	private Location checkVerticalInfo(Location tile1, Location tile2) {
		
		Location info = null; 
		
		final int top = tile1.y - 1;
		final int bottom = tile2.y + 1;
		
		final int left = tile1.x - 1;
		
		if (isPotentialInfo(left, top)) {
	    	if (board.isRevealed(left, top)) {
	    		return null;
	    	} else {
	    		info = new Location(left, top);
	    	}
		}
		
		if (isPotentialInfo(left + 1, top)) {
			if (board.isRevealed(left + 1, top)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left + 1, top);
		}		
		
		if (isPotentialInfo(left + 2, top)) {
			if (board.isRevealed(left + 2, top)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left + 2, top);
		}		
		
		if (isPotentialInfo(left, bottom)) {
			if (board.isRevealed(left, bottom)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left, bottom);
		}		
		
		if (isPotentialInfo(left + 1, bottom)) {
			if (board.isRevealed(left + 1, bottom)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left + 1, bottom);
		}		
		
		if (isPotentialInfo(left + 2, bottom)) {
			if (board.isRevealed(left + 2, bottom)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left + 2, bottom);
		}		
		
		return info;
		
	}
	
	// returns the location of the 1 tile which can still provide information, or null
	private Location checkHorizontalInfo(Location tile1, Location tile2) {
		
		Location info = null; 
		
		final int top = tile1.y - 1;
		
		final int left = tile1.x - 1;
		final int right = tile2.x + 1;
		
		if (isPotentialInfo(left, top)) {
	    	if (board.isRevealed(left, top)) {
	    		return null;
	    	} else {
	    		info = new Location(left, top);
	    	}
		}
		
		if (isPotentialInfo(left, top + 1)) {
			if (board.isRevealed(left, top + 1)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left, top + 1);
		}		
		
		if (isPotentialInfo(left, top + 2)) {
			if (board.isRevealed(left, top + 2)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left, top + 2);
		}		
		
		if (isPotentialInfo(right, top)) {
			if (board.isRevealed(right, top)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(right, top);
		}		
		
		if (isPotentialInfo(right, top + 1)) {
			if (board.isRevealed(right, top + 1)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(right, top + 1);
		}		
		
		if (isPotentialInfo(right, top + 2)) {
			if (board.isRevealed(right, top + 2)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(right, top + 2);
		}		
		
		return info;
		
	}
	
	private void checkForBox5050() {
		
    	// box 2x2
		Location[] tiles = new Location[4];
		
		BigDecimal maxProbability = BigDecimal.ZERO;
		Risk5050 worst5050 = null;
		
		List<Location> mines = new ArrayList<>();
		List<Location> noMines = new ArrayList<>();
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {
				
				// need 4 hidden tiles
				if (!board.isUnrevealed(i, j) || !board.isUnrevealed(i, j + 1) || !board.isUnrevealed(i + 1, j) || !board.isUnrevealed(i + 1, j + 1)) {
					continue;
				}
				
				tiles[0] = new Location(i, j);
				Location info = checkBoxInfo(tiles[0]);
				
				// need the corners to be flags or off the board
				if (info == null) {
					continue;  // this skips the rest of the logic below this in the for-loop 
				}

				tiles[1] = new Location(i + 1, j);
				tiles[2] = new Location(i, j + 1);
				tiles[3] = new Location(i + 1, j + 1);
				
				BigInteger solutions = BigInteger.ZERO;
				
				for (int k = 0; k < PATTERNS.length; k++) {
					
					mines.clear();
					noMines.clear();
					
					mines.add(info);  // the missing mine
					
					// allocate each position as a mine or noMine
					for (int l = 0; l < 4; l++) {
						if (PATTERNS[k][l]) {
							mines.add(tiles[l]);
						} else {
							noMines.add(tiles[l]);
						}
					}
					
					// see if the position is valid
					SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, noMines, Area.EMPTY_AREA);

					// if it is then mark each mine tile as risky
					if (counter.getSolutionCount().signum() != 0) {
						board.getLogger().log(Level.DEBUG, "Pattern %d is valid with %d solutions", k, counter.getSolutionCount());
						solutions = solutions.add(counter.getSolutionCount());
						
					} else {
						board.getLogger().log(Level.DEBUG, "Pattern %d is not valid", k);
					}
				}
				
				BigDecimal probability = new BigDecimal(solutions).divide(new BigDecimal(this.currentPe.getSolutionCount()), 6, RoundingMode.HALF_UP);
				
				board.getLogger().log(Level.INFO, "%s %s %s %s is box 50/50 %f of the time", tiles[0], tiles[1], tiles[2], tiles[3], probability);
				
				if (probability.compareTo(maxProbability) > 0) {
					maxProbability = probability;
					worst5050 = new Risk5050(info, tiles[0], tiles[1], tiles[2], tiles[3]);
					board.getLogger().log(Level.INFO, "%s %s %s %s is box 50/50 is new worst 50/50", tiles[0], tiles[1], tiles[2], tiles[3]);
				}
				
			}
		}
		
		this.worstBox5050 = worst5050;
		this.box5050Safety = BigDecimal.ONE.subtract(maxProbability.multiply(HALF));
		
	}
	
	// returns the location of the 1 tile which can still provide information for a 2x2 box, or null
	private Location checkBoxInfo(Location tileTopLeft) {
		
		Location info = null; 
		
		final int top = tileTopLeft.y - 1;
		final int left = tileTopLeft.x - 1;
		
		if (isPotentialInfo(left, top)) {
	    	if (board.isRevealed(left, top)) {
	    		return null;
	    	} else {
	    		info = new Location(left, top);
	    	}
		}
		
		if (isPotentialInfo(left, top - 3)) {
			if (board.isRevealed(left, top - 3)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left, top - 3);
		}		
		
		if (isPotentialInfo(left + 3, top)) {
			if (board.isRevealed(left + 3, top)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left + 3, top);
		}		
		
		if (isPotentialInfo(left + 3, top + 3)) {
			if (board.isRevealed(left + 3, top + 3)) {  // info is certain
				return null;
			} else {
				if (info != null) {  // more than 1 tile giving possible info
					return null;
				}
			}
			info = new Location(left + 3, top + 3);
		}		
		
		return info;
		
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
	
	private boolean isMineInPe(int x, int y) {
		for (Location loc: this.currentPe.getMines()) {
			if (loc.x == x && loc.y == y) {
				return true;
			}
		}
		return false;
	}
}
