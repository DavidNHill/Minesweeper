package minesweeper.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minesweeper.solver.bulk.StaticCounter;
import minesweeper.solver.bulk.StaticCounter.SCType;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.utility.Logger;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class PseudoHelper {
	
	private class Link implements Comparable<Link>{
		
		private Witness witness;
		
		private Location tile1;
		private boolean closed1 = true;
		private boolean dead1 = false;
		
		private Location tile2;
		private boolean closed2 = true;
		private boolean dead2 = false;
		
		private boolean processed = false;
		private boolean pseudo = false;
		private boolean unavoidable = true;
		
		// list of locations which could provide information into the pseudo-50/50
		private List<Location> trouble = new ArrayList<>();

		@Override
		public int compareTo(Link o) {
			
			if (this.pseudo) {
				if (!o.pseudo) {
					return 1;
				}
			} else if (o.pseudo) {
				return -1;
			}
			
			return this.witness.getSquares().size() - o.witness.getSquares().size();
		}
		
	}
	
	private class Chain implements Comparable<Chain>{
		
		private List<Location> whole5050 = new ArrayList<>();
		private List<Location> living5050 = new ArrayList<>();
		
		// Tiles which make the pseudo
		private List<Location> pseudoTiles = new ArrayList<>();
		
		private Location openTile;
		
		private int pseudoCount = 0;
		private boolean pseudo = false;
		
		// list of locations which could provide information into the pseudo-50/50
		private List<Location> trouble = new ArrayList<>();

		@Override
		public int compareTo(Chain o) {
			if (this.pseudo) {
				if (!o.pseudo) {
					return 1;
				}
			} else if (o.pseudo) {
				return -1;
			}
			return 0;
		}
		
	}
	
	
	private Set<Location> deferGuessing = new HashSet<>();
	
	private final BoardState board;
	private final ProbabilityEngineModel pe;
	private final WitnessWeb wholeEdge;
	private final Area deadLocations;
	private final List<Location> extraMines;
	private final Logger logger;
	
	
	public PseudoHelper(BoardState board, ProbabilityEngineModel pe, WitnessWeb wholeEdge, Area deadLocations, List<Location> extraMines, Logger logger)  {
		
		this.board = board;
		this.pe = pe;
		this.wholeEdge = wholeEdge;
		this.deadLocations = deadLocations;
		this.extraMines = extraMines;
		this.logger = logger;
		
	}
	
	public List<Location> findPseudo5050() {
		
		List<Link> links = new ArrayList<>();
		
		// pseudo links are witnesses needing one more mine which aren't rooted to a wall or a line of mines
		// they can potentially join up two chains to form a pseudo-50/50
		List<Witness> pseudoLinks = new ArrayList<>();
		
		// also look for unavoidable guesses
		for (Witness witness: wholeEdge.getPrunedWitnesses()) {
			
			// find witnesses which only require one more mine
			if (witness.getMines() == 1) {
				
				// create a new link
				Link link = new Link();
				link.witness = witness;
				
				// if only 2 tiles left uncleared then these form the linking tiles
				if (witness.getSquares().size() == 2) {
					link.tile1 = witness.getSquares().get(0);
					link.tile2 = witness.getSquares().get(1);
					
					logger.log(Level.INFO, "Witness %s is a possible unavoidable guess witness for %s and %s", witness, link.tile1, link.tile2);
					assessLink(link);
					
					links.add(link);
					
				} else {  // otherwise 
					
					List<Link> rooted = findRootedLinks(witness);
					if (rooted.isEmpty()) {
						pseudoLinks.add(witness);
					}
					
					links.addAll(rooted);
				}

			}
		}
		
		// sort so non-pseudos at the top 
		links.sort(null);
		
		// if there is an unavoidable link then return that, prioritising real 50/50s over pseudo
		Link unavoidableLink = null;
		for (Link link: links) {
			if (link.unavoidable) {
				unavoidableLink = link;
				if (!link.pseudo) {
					break;
				} 
			}
		}
		if (unavoidableLink != null) {
			logger.log(Level.INFO, "Tiles %s and %s form an unavoidable guess", unavoidableLink.tile1, unavoidableLink.tile2);
			return preferLiving(Arrays.asList(unavoidableLink.tile1, unavoidableLink.tile2));			
		}
		
		
		List<Chain> chains = new ArrayList<>();
		
		// try and connect 2 or more links together to form an unavoidable 50/50.  Closed at both ends.
		for (Link link: links) {
			
			// start with a real link with one closed end
			if (!link.processed && (link.closed1 ^ link.closed2)) {  // this is the XOR operator, so 1 and only 1 of these is closed 

				Chain chain = new Chain();
				chain.whole5050.add(link.tile1);
				chain.whole5050.add(link.tile2);
				chain.trouble.addAll(link.trouble);
				
				if (link.pseudo) {
					chain.pseudo = true;
				}
				
				//Location openTile;
				int extensions = 0;
				if (!link.closed1) {
					chain.openTile = link.tile1;
				} else {
					chain.openTile = link.tile2;
				}

				if (!link.dead1) {
					chain.living5050.add(link.tile1);
					if (link.pseudo) {
						chain.pseudoTiles.add(link.tile1);
					}
				}
				if (!link.dead2) {
					chain.living5050.add(link.tile2);
					if (link.pseudo) {
						chain.pseudoTiles.add(link.tile2);
					}
				}				
				
				link.processed = true;
				
				boolean noMatch = false;
				while (chain.openTile != null && !noMatch) {

					noMatch = true;
					for (Link extension: links) {
						if (!extension.processed && !(chain.pseudo && extension.pseudo)) {  // can't add another pseudo link to an already pseudo chain

							if (extension.tile1.equals(chain.openTile)) {
								extensions++;
								extension.processed = true;
								noMatch = false;

								if (extension.pseudo) {
									chain.pseudo = true;
									
									if (!extension.dead1) {
										chain.pseudoTiles.add(extension.tile1);
									}
									if (!extension.dead2) {
										chain.pseudoTiles.add(extension.tile2);
									}									
								}
								
								// accumulate the trouble tiles as we progress;
								chain.trouble.addAll(extension.trouble);
								chain.whole5050.add(extension.tile2);

								if (!extension.dead2) {
									chain.living5050.add(extension.tile2);
								}										
								
								if (extension.closed2) {
									if (extensions % 2 == 0 && noTrouble(chain, chain.whole5050)) {
										logger.log(Level.INFO, "Tile %s is an unavoidable guess, with %d extensions", chain.openTile, extensions);
										
										List<Location> area5050 = chain.whole5050;
										if (area5050.get(0).y == area5050.get(area5050.size() - 1).y  && Math.abs(area5050.get(0).x - area5050.get(area5050.size() - 1).x) > 2
												|| area5050.get(0).x == area5050.get(area5050.size() - 1).x && Math.abs(area5050.get(0).y - area5050.get(area5050.size() - 1).y) > 2) {
											StaticCounter.count(SCType.LONG_5050);
										}

										// if the final extension is a pseudo then return only those tiles
										if (extension.pseudo) {
											//this.board.getSolver().savePosition("Pseudo");
											return preferLiving(Arrays.asList(extension.tile1, extension.tile2));
										} else {
											return chain.living5050;
										}

									} else {
										logger.log(Level.INFO, "Tile %s is a closed extension with %d parts", chain.openTile, (extensions + 1));
										deferGuessing.addAll(chain.whole5050);
										chain.openTile = null;
									}		
								} else {  // found an open extension, now look for an extension for this
									chain.openTile = extension.tile2;  
								}
								break;
							}
							
							if (extension.tile2.equals(chain.openTile)) {
								extensions++;
								extension.processed = true;
								noMatch = false;

								if (extension.pseudo) {
									chain.pseudo = true;
									
									if (!extension.dead1) {
										chain.pseudoTiles.add(extension.tile1);
									}
									if (!extension.dead2) {
										chain.pseudoTiles.add(extension.tile2);
									}							
								}
								
								// accumulate the trouble tiles as we progress;
								chain.trouble.addAll(extension.trouble);
								chain.whole5050.add(extension.tile1);   // tile 1 is the new tile
								
								if (!extension.dead1) {
									chain.living5050.add(extension.tile1);
								}									
								
								if (extension.closed1) {
									if (extensions % 2 == 0 && noTrouble(chain, chain.whole5050)) {
										logger.log(Level.INFO, "Tile %s is an unavoidable guess, with %d extensions", chain.openTile, extensions);
										
										List<Location> area5050 = chain.whole5050;										
										if (area5050.get(0).y == area5050.get(area5050.size() - 1).y  && Math.abs(area5050.get(0).x - area5050.get(area5050.size() - 1).x) > 2
												|| area5050.get(0).x == area5050.get(area5050.size() - 1).x && Math.abs(area5050.get(0).y - area5050.get(area5050.size() - 1).y) > 2) {
											StaticCounter.count(SCType.LONG_5050);
										}
										
										// if the final extension is a pseudo then return only those tiles
										if (extension.pseudo) {
											//this.board.getSolver().savePosition("Pseudo");
											return preferLiving(Arrays.asList(extension.tile1, extension.tile2));
										} else {
											return chain.living5050;
										}

									} else {
										logger.log(Level.INFO, "Tile %s is a closed extension with %d parts", chain.openTile, (extensions + 1));
										deferGuessing.addAll(chain.whole5050);
										chain.openTile = null;
									}

								} else {  // found an open extension, now look for an extension for this
									chain.openTile = extension.tile1;  
								}

								break;
							}

						}

					}

				}
				
				// incomplete chains can still be connected 
				if (noMatch) {
					chains.add(chain);
				}

			}
		}

		// can't find these complex pseudo-50/50s without the probability engine
		if (this.pe == null) {
			return null;
		}
		
		chains.sort(null);
		
		// can we join two chains together using a pseudo-link
		top: for (Witness witness: pseudoLinks) {
			
			Chain chain1 = null;
			Chain chain2 = null;
			
			BigInteger tally1 = null;
			BigInteger tally2 = null;
			
			search: for (Location tile: witness.getSquares()) {
				
				for (Chain chain: chains) {
					if (chain.openTile.equals(tile)) {
						if (chain1 == null) {
							chain1 = chain;
							tally1 = this.pe.getBox(tile).getTally();
							break;
						} else {
							chain2 = chain;
							tally2 = this.pe.getBox(tile).getTally();
							break search;
						}

					}
				}
			}

			if (chain1 != null && chain2 != null) {  
				
				// see if this link has any trouble
				Link linker = new Link();
				linker.tile1 = chain1.openTile;
				linker.tile2 = chain2.openTile;
				
				assessLink(linker);
				
				Chain combinedChain = new Chain();
				combinedChain.whole5050.addAll(chain1.whole5050);
				combinedChain.whole5050.addAll(chain2.whole5050);
				
				combinedChain.trouble.addAll(linker.trouble);
				combinedChain.trouble.addAll(chain1.trouble);
				combinedChain.trouble.addAll(chain2.trouble);
				
				if (combinedChain.whole5050.size() % 2 == 0 && noTrouble(combinedChain, combinedChain.whole5050)) {
					
					// If both chains are pseudos then make sure the chosen tile is the safest
					if (chain1.pseudo && chain2.pseudo) {
						BigInteger lowestTally = tally1.min(tally2);
						for (Location tile: combinedChain.whole5050) {
							if (this.pe.getBox(tile).getTally().compareTo(lowestTally) < 0) {
								continue top;  // if there is a safer tile then this potential pseudo is broken
							}
						}
						//this.board.getSolver().savePosition("triple Pseudo");
					}
					//if (combinedChain.whole5050.size() > 6) {
					//	this.board.getSolver().savePosition("Complex Pseudo");
					//}
					int c = tally1.compareTo(tally2);
					if (c == 0) {
						return preferLiving(Arrays.asList(chain1.openTile, chain2.openTile));
					} else if (c < 0) {
						return Arrays.asList(chain1.openTile);
					} else {
						return Arrays.asList(chain2.openTile);
					}
				}
				
			}
		}
		
		
		logger.log(Level.INFO, "%d locations set to defered guessing", deferGuessing.size());
		return null;
		
	}
	
	// find any links which can be formed from this witness which have at least one closed end
	private List<Link> findRootedLinks(Witness witness) {
		
		List<Link> links = new ArrayList<>();
		
		for (Location tile1: witness.getSquares()) {

			for (Location tile2: witness.getSquares()) {
				
				// immediately above
				if (tile2.x == tile1.x && tile2.y == tile1.y - 1) {
					// create a new link
					Link link = new Link();
					link.witness = witness;
					link.pseudo = true;
					
					link.tile1 = tile1;
					link.tile2 = tile2;
					
					assessLink(link);
					if (link.closed1 || link.closed2) {  // at least one end closed
						if (!link.dead1 || !link.dead2) {  // at least one tile living
							links.add(link);
						}
					}
				}
				
				// immediately right
				if (tile2.x == tile1.x + 1 && tile2.y == tile1.y) {
					// create a new link
					Link link = new Link();
					link.witness = witness;
					link.pseudo = true;
					
					link.tile1 = tile1;
					link.tile2 = tile2;
					
					assessLink(link);
					if (link.closed1 || link.closed2) {
						if (!link.dead1 || !link.dead2) {  // at least one tile living
							links.add(link);
						}
					}
				}
			}
		}
		
		
		return links;
		
	}
	
	// Check if this link can receive information from any adjacent tiles 
	// Check if the link has closed ends
	// Check if the tiles are dead
	// Check if the link by itself is a pseudo 50/50, i.e. closed at both ends
	private void assessLink(Link link) {
		
		// if every monitoring tile also monitors all the other tiles then it can't provide any information
		Location[] tiles = new Location[] {link.tile1, link.tile2};  
		
		for (Location tile: tiles) {  
			for (Location adjTile: board.getAdjacentSquaresIterable(tile)) {
				
				// are we one of the tiles other tiles, if so then no need to check
				boolean toCheck = true;
				for (Location otherTile: tiles) {
					if (otherTile.equals(adjTile)) {
						toCheck = false;
						break;
					}
				}
				
				// if we are monitoring and not a mine then see if we are also monitoring all the other mines
				if (toCheck && !board.isConfirmedMine(adjTile) && !extraMines.contains(adjTile)) {  
					for (Location otherTile: tiles) {
						if (!otherTile.equals(adjTile) && !adjTile.isAdjacent(otherTile)) {
							//board.display("Tile " + adjTile.display() + " is not monitoring " + otherTile.display());
							logger.log(Level.DEBUG, "Tile %S can receive exclusive information from %s", tile, adjTile);
							link.trouble.add(adjTile);
							if (tile.equals(link.tile1)) {
								link.closed1 = false;
							} else {
								link.closed2 = false;
							}
							link.unavoidable = false;
						}
					}
				}
			}
		}
		
		link.dead1 = isDead(link.tile1);
		link.dead2 = isDead(link.tile2);
		
	}
	
	
	private boolean noTrouble(Chain chain, List<Location> area) {
		
		// each trouble location must be adjacent to 2 tiles in the extended 50/50
		top: for (Location tile: chain.trouble) {
			
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
	
	private List<Location> preferLiving(List<Location> tiles) {
		
		List<Location> result = new ArrayList<>();
		
		for (Location tile: tiles) {
			if (!isDead(tile)) {
				result.add(tile);
			}
		}
		
		if (result.isEmpty()) {
			return tiles;
		} else {
			return result;
		}
		
	}
	
	private boolean isDead(Location tile) {
		return this.deadLocations.contains(tile);
	}
	
}
