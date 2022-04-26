package minesweeper.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class FiftyFiftyHelper {
	
	private boolean[][] PATTERNS = new boolean[][] {{true, true, true, true},   // four mines
		{true, true, true, false}, {true, false, true, true}, {false, true, true, true}, {true, true, false, true},   // 3 mines
		{true, false, true, false}, {false, true, false, true}, {true, true, false, false}, {false, false, true, true},   // 2 mines
		{false, true, false, false}, {false, false, false, true}, {true, false, false, false}, {false, false, true, false}  // 1 mine
	};
	
	private class Link {
		
		private Location tile1;
		private boolean closed1 = true;
		private Location tile2;
		private boolean closed2 = true;
		
		private boolean processed = false;
		
		// list of locations which could prevent us being an unavoidable 50/50
		private List<Location> trouble = new ArrayList<>();
		
	}
	
	private Set<Location> deferGuessing = new HashSet<>();
	
	private BoardState board;
	private WitnessWeb wholeEdge;
	private Area deadLocations;
	
	public FiftyFiftyHelper(BoardState board, WitnessWeb wholeEdge, Area deadLocations)  {
		
		this.board = board;
		this.wholeEdge = wholeEdge;
		this.deadLocations = deadLocations;
		
	}

	public Location findUnavoidable5050(List<Location> extraMines) {
		
		List<Link> links = new ArrayList<>();
		
		// also look for unavoidable guesses
		for (Witness witness: wholeEdge.getPrunedWitnesses()) {
			if (witness.getMines() == 1 && witness.getSquares().size() == 2) {
				
				// create a new link
				Link link = new Link();
				link.tile1 = witness.getSquares().get(0);
				link.tile2 = witness.getSquares().get(1);
				
				
				board.getLogger().log(Level.INFO, "Witness %s is a possible unavoidable guess witness for %s and %s", witness, link.tile1, link.tile2);
				boolean unavoidable = true;
				// if every monitoring tile also monitors all the other tiles then it can't provide any information
				for (Square tile: witness.getSquares()) {  
					for (Location adjTile: board.getAdjacentSquaresIterable(tile)) {
						
						// are we one of the tiles other tiles, if so then no need to check
						boolean toCheck = true;
						for (Square otherTile: witness.getSquares()) {
							if (otherTile.equals(adjTile)) {
								toCheck = false;
								break;
							}
						}
						
						// if we are monitoring and not a mine then see if we are also monitoring all the other mines
						if (toCheck && !board.isConfirmedFlag(adjTile) && !extraMines.contains(adjTile)) {  
							for (Square otherTile: witness.getSquares()) {
								if (!otherTile.equals(adjTile) && !adjTile.isAdjacent(otherTile)) {
									//board.display("Tile " + adjTile.display() + " is not monitoring " + otherTile.display());
									board.getLogger().log(Level.DEBUG, "Tile %S can receive exclusive information from %s", tile, adjTile);
									link.trouble.add(adjTile);
									if (tile.equals(link.tile1)) {
										link.closed1 = false;
									} else {
										link.closed2 = false;
									}
									unavoidable = false;
									//break check;
								}
							}
						}
					}
				}
				if (unavoidable) {
					Location guess =  board.getSolver().getLowest(witness.getSquares());
					board.getLogger().log(Level.INFO, "Tile %s is an unavoidable guess", guess);
					return guess;
				}
				
				links.add(link);
			}
		}
		
		List<Location> area5050 = new ArrayList<>();  // used to hold the expanding candidate 50/50
		
		// try and connect 2 or links together to form an unavoidable 50/50.  Closed at both ends.
		for (Link link: links) {
			if (!link.processed && (link.closed1 ^ link.closed2)) {  // this is the XOR operator, so 1 and only 1 of these is closed 

				Location openTile;
				int extensions = 0;
				if (!link.closed1) {
					openTile = link.tile1;
				} else {
					openTile = link.tile2;
				}

				area5050.clear();
				area5050.add(link.tile1);
				area5050.add(link.tile2);
				
				link.processed = true;
				
				boolean noMatch = false;
				while (openTile != null && !noMatch) {

					noMatch = true;
					for (Link extension: links) {
						if (!extension.processed) {

							if (extension.tile1.equals(openTile)) {
								extensions++;
								extension.processed = true;
								noMatch = false;

								// accumulate the trouble tiles as we progress;
								link.trouble.addAll(extension.trouble);
								area5050.add(extension.tile2);   // tile2 is the new tile
								
								if (extension.closed2) {
									if (extensions % 2 == 0 && noTrouble(link, area5050)) {
										board.getLogger().log(Level.INFO, "Tile %s is an unavoidable guess, with %d extensions", openTile, extensions);
										return board.getSolver().getLowest(area5050);		
									} else {
										board.getLogger().log(Level.INFO, "Tile %s is a closed extension with %d parts", openTile, (extensions + 1));
										deferGuessing.addAll(area5050);
										openTile = null;
									}		
								} else {  // found an open extension, now look for an extension for this
									openTile = extension.tile2;  
								}
								break;
							}
							if (extension.tile2.equals(openTile)) {
								extensions++;
								extension.processed = true;
								noMatch = false;

								// accumulate the trouble tiles as we progress;
								link.trouble.addAll(extension.trouble);
								area5050.add(extension.tile1);   // tile 1 is the new tile
								
								if (extension.closed1) {
									if (extensions % 2 == 0 && noTrouble(link, area5050)) {
										board.getLogger().log(Level.INFO, "Tile %s is an unavoidable guess, with %d extensions", openTile, extensions);
										return board.getSolver().getLowest(area5050);		
									} else {
										board.getLogger().log(Level.INFO, "Tile %s is a closed extension with %d parts", openTile, (extensions + 1));
										deferGuessing.addAll(area5050);
										openTile = null;
									}

								} else {  // found an open extension, now look for an extension for this
									openTile = extension.tile1;  
								}

								break;
							}

						}

					}

				}

			}
		}
		
		/*  This makes results worse, preumaby because some non-50/50s are getting through
		// try and find a circular unavoidable 50/50.  Not closed.
		for (Link link: links) {
			if (!link.processed && !link.closed1 &&  !link.closed2) {  // not processed and open at both ends

				Location openTile;
				Location startTile;
				int extensions = 0;
				startTile = link.tile1;
				openTile = link.tile2;

				area5050.clear();
				area5050.add(link.tile1);
				area5050.add(link.tile2);
				
				link.processed = true;
				
				boolean noMatch = false;
				while (openTile != null && !noMatch) {

					noMatch = true;
					for (Link extension: links) {
						if (!extension.processed && !extension.closed1 && !extension.closed2) {  // a circular 50/50 must have links open at both ends

							if (extension.tile1.equals(openTile)) {

								extension.processed = true;
								noMatch = false;

								// accumulate the trouble tiles as we progress;
								link.trouble.addAll(extension.trouble);
								area5050.add(extension.tile2);   // tile2 is the new tile
								
								if (extension.tile2.equals(startTile)) {
									if (extensions % 2 == 0 ) {  // && noTrouble(link, area5050)
										board.getLogger().log(Level.WARN, "Tile %s is an unavoidable circular 50/50 guess, with %d extensions", openTile, extensions);
										return board.getSolver().getLowest(area5050);		
									} else {
										board.getLogger().log(Level.INFO, "Tile %s is a circular extension with %d parts", openTile, (extensions + 1));
										deferGuessing.addAll(area5050);
										openTile = null;
									}		
								} else {  // not closed the loop, so keep going
									extensions++;
									openTile = extension.tile2;  
								}
								break;
							}
							if (extension.tile2.equals(openTile)) {
								extension.processed = true;
								noMatch = false;

								// accumulate the trouble tiles as we progress;
								link.trouble.addAll(extension.trouble);
								area5050.add(extension.tile1);   // tile 1 is the new tile
								
								if (extension.tile1.equals(startTile)) {
									if (extensions % 2 == 0 ) {  // && noTrouble(link, area5050)
										board.getLogger().log(Level.WARN, "Tile %s is an unavoidable circular 50/50 guess, with %d extensions", openTile, extensions);
										return board.getSolver().getLowest(area5050);		
									} else {
										board.getLogger().log(Level.INFO, "Tile %s is a circular extension with %d parts", openTile, (extensions + 1));
										deferGuessing.addAll(area5050);
										openTile = null;
									}

								} else {  // not closed the loop, so keep going
									extensions++;
									openTile = extension.tile1;  
								}

								break;
							}

						}

					}

				}

			}
		}
		*/
		
		board.getLogger().log(Level.INFO, "%d locations set to defered guessing", deferGuessing.size());
		return null;
		
	}
	
	private boolean noTrouble(Link link, List<Location> area) {
		
		// each trouble location must be adjacent to 2 tiles in the extended 50/50
		top: for (Location tile: link.trouble) {
			
			for (Location tile5050: area) {
				if (tile.equals(tile5050)) {
					continue top;    //if a trouble tile is part of the 50/50 it isn't trouble
  				}
			}
			
			
			int adjCount = 0;
			for (Location tile5050: area) {
				if (tile.isAdjacent(tile5050)) {
					adjCount++;
				}
			}
			if (adjCount % 2 != 0) {
				board.getLogger().log(Level.DEBUG, "Trouble Tile %s isn't adjacent to an even number of tiles in the extended candidate 50/50, adjacent %d of %d", tile, adjCount,  area.size());					
				return false;
			}
		}
		
		
		return true;
		
	}
	
	public boolean isDeferGuessing(Location l) {
		return deferGuessing.contains(l);
	}
	
	/**
	 * Looks for pseudo-50/50s (which may be real 50/50s since we don't check any further)
	 */
	public Location process() {
		
		board.getLogger().log(Level.INFO, "Starting search for 50/50s");
    	
    	int minesLeft = board.getMines() - board.getConfirmedFlagCount();
    	
    	// horizontal 2x1
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight(); j++) {

				// need 2 hidden tiles
				if (!board.isUnrevealed(i, j) || !board.isUnrevealed(i + 1, j)) {
					continue;
				}
				
				if (isPotentialInfo(i-1, j-1) || isPotentialInfo(i-1, j) || isPotentialInfo(i-1, j+1)
					|| isPotentialInfo(i+2, j-1) || isPotentialInfo(i+2, j) || isPotentialInfo(i+2, j+1)) {
					continue;  // this skips the rest of the logic below this in the for-loop 
				}
				
				Location tile1 = new Location(i, j);
				Location tile2 = new Location(i + 1, j);
				
				//display(tile1.display() + " and " + tile2.display() + " is candidate 50/50");
				
				if (minesLeft > 1) {
					// see if the 2 tiles can support 2 mines
					List<Location> mines = new ArrayList<>();
					mines.add(tile1);
					mines.add(tile2);
					SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, null, Area.EMPTY_AREA);
					if (counter.getSolutionCount().signum() == 0) {
						board.getLogger().log(Level.INFO, "%s and %s can't have 2 mines, guess immediately", tile1, tile2);
						return tile1;
					}
					
					
				} else {
					board.getLogger().log(Level.INFO, "%s and %s can't have 2 mines since not enough mines left in the game, guess immediately", tile1, tile2);
					return tile1;
				}

			}
		}                        
    	
    	// vertical 1x2
		for (int i=0; i < board.getGameWidth(); i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {
				
				// need 2 hidden tiles
				if (!board.isUnrevealed(i, j) || !board.isUnrevealed(i, j + 1)) {
					continue;
				}
				
				if (isPotentialInfo(i - 1, j - 1) || isPotentialInfo(i, j - 1) || isPotentialInfo(i + 1, j - 1)
					|| isPotentialInfo(i - 1, j + 2) || isPotentialInfo(i, j + 2) || isPotentialInfo(i + 1, j + 2)) {
					continue;  // this skips the rest of the logic below this in the for-loop 
				}
				
				Location tile1 = new Location(i, j);
				Location tile2 = new Location(i, j + 1);
				
				//display(tile1.display() + " and " + tile2.display() + " is candidate 50/50");
				
				if (minesLeft > 1) {
					// see if the 2 tiles can support 2 mines
					List<Location> mines = new ArrayList<>();
					mines.add(tile1);
					mines.add(tile2);
					SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, null, Area.EMPTY_AREA);
					if (counter.getSolutionCount().signum() == 0) {
						board.getLogger().log(Level.INFO, "%s and %s can't have 2 mines, guess immediately", tile1, tile2);
						return tile1;
					}
				} else {
					board.getLogger().log(Level.INFO, "%s and %s can't have 2 mines since not enought mines left in the game, guess immediately", tile1, tile2);
					return tile1;
				}
				
			}
		}            
		
		
		//if (!board.getSolver().preferences.isTestMode()) {
		//	return null;
		//}
		
    	// box 2x2
		Location[] tiles = new Location[4];
		
		List<Location> mines = new ArrayList<>();
		List<Location> noMines = new ArrayList<>();
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {
				
				// need 4 hidden tiles
				if (!board.isUnrevealed(i, j) || !board.isUnrevealed(i, j + 1) || !board.isUnrevealed(i + 1, j) || !board.isUnrevealed(i + 1, j + 1)) {
					continue;
				}
				
				// need the corners to be flags or off the board
				if (isPotentialInfo(i - 1, j - 1) || isPotentialInfo(i + 2, j - 1) || isPotentialInfo(i - 1, j + 2) || isPotentialInfo(i + 2, j + 2)) {
					continue;  // this skips the rest of the logic below this in the for-loop 
				}
				
				tiles[0] = new Location(i, j);
				tiles[1] = new Location(i + 1, j);
				tiles[2] = new Location(i, j + 1);
				tiles[3] = new Location(i + 1, j + 1);
				
				board.getLogger().log(Level.INFO, "%s %s %s %s is candidate box 50/50", tiles[0], tiles[1], tiles[2], tiles[3]);

				// keep track of which tiles are risky - once all 4 are then not a pseudo-50/50
				int riskyTiles = 0;
				boolean[] risky = new boolean[4];
				
				// check each tile is in the web and that at least one is living
				boolean okay = true;
				boolean allDead = true;
				for (int l = 0; l < 4; l++) {
					if (!this.deadLocations.contains(tiles[l])) {
						allDead = false;
					} else {
						riskyTiles++;
						risky[l] = true;  // since we'll never select a dead tile, consider them risky
					}
					if (!this.wholeEdge.isOnWeb(tiles[l])) {
						board.getLogger().log(Level.DEBUG, "%s has no witnesses, so nothing to check", tiles[l]);
						okay = false;
						break;
					}
				}
				if (!okay || allDead) {
					continue;
				}
				
				
				int start;
				if (minesLeft > 3) {
					start = 0;
				} else if (minesLeft == 3) {
					start = 1;
				} else if (minesLeft == 2) {
					start = 5;
				} else {
					start = 9;
				}

				for (int k = start; k < PATTERNS.length; k++) {
					
					mines.clear();
					noMines.clear();
					
					boolean run = false;
					// allocate each position as a mine or noMine
					for (int l = 0; l < 4; l++) {
						if (PATTERNS[k][l]) {
							mines.add(tiles[l]);
							if (!risky[l]) {
								run = true;
							}
						} else {
							noMines.add(tiles[l]);
						}
					}
					
					// only run if this pattern can discover something we don't already know
					if (!run) {
						board.getLogger().log(Level.DEBUG, "Pattern %d skipped", k);
						continue;
					}
					
					// see if the position is valid
					SolutionCounter counter = board.getSolver().validatePosition(wholeEdge, mines, noMines, Area.EMPTY_AREA);

					// if it is then mark each mine tile as risky
					if (counter.getSolutionCount().signum() != 0) {
						board.getLogger().log(Level.DEBUG, "Pattern %d is valid", k);
						for (int l = 0; l < 4; l++) {
							if (PATTERNS[k][l]) {
								if (!risky[l]) {
									risky[l] = true;
									riskyTiles++;
								}
							}
						}
						if (riskyTiles == 4) {
							break;
						}
					} else {
						board.getLogger().log(Level.DEBUG, "Pattern %d is not valid", k);
					}
				}
				
				// if not all 4 tiles are risky then send back one which isn't
				if (riskyTiles != 4) {
					for (int l = 0; l < 4; l++) {
						// if not risky and not dead then select it
						if (!risky[l] && !deadLocations.contains(tiles[l])) {
							board.getLogger().log(Level.INFO, "%s %s %s %s is pseudo 50/50 - " + tiles[l].toString() + " is not risky", tiles[0], tiles[1], tiles[2], tiles[3]);
							return tiles[l];
						}

					}					
				}
			}
		}                        
		
		
		return null;
		

	}
	
    // returns whether the tile is still valid even if it has no witnesses
    private boolean isExempt(Location l) {
    	
    	// if not test mode then no exemption
    	if (!board.getSolver().preferences.isTestMode()) {
    		return false;
    	}
    	
    	// if the tile is in a corner then it is exempt
    	if ((l.x == 0 || l.x == board.getGameWidth() - 1) && (l.y == 0 || l.y == board.getGameHeight() - 1)) {
    		return true;
    	}
    	
    	return false;
    	
    }
	
    // returns whether there information to be had at this location; i.e. on the board and either unrevealed or revealed
    private boolean isPotentialInfo(int x, int y) {
    	
    	if (x < 0 || x >= board.getGameWidth() || y < 0 || y >= board.getGameHeight()) {
    		return false;
    	}
    	
    	if (board.isConfirmedFlag(x, y)) {
    		return false;
    	} else {
    		return true;
    	}
    	
    }
	
	
}
