package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.ChordLocation;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class EfficiencyHelper {
	
	private static final BigDecimal MINE_THRESHOLD = BigDecimal.valueOf(1);   // probability of mine to consider
	private static final int RISK_ADVANTAGE = 2;   // <= benefit - cost
	private static final BigDecimal ONE_ADVANTAGE_THRESHOLD = BigDecimal.valueOf(0.85);   // accept mine probability when benefit - cost = 1
	
	private static final BigDecimal CLEAR_ZERO_VALUE = BigDecimal.valueOf(1.0);   // clear a possible zero if chance if >= this value regardless of any other consideration
	private static final BigDecimal IGNORE_ZERO_THRESHOLD = BigDecimal.valueOf(0.375);   // ignore a zero when the chance it happens is less than this
	private static final BigDecimal SAFE_3BV_CLICK_THRESHOLD = BigDecimal.valueOf(1);   // a 3BV safe tile with safety greater or equal than this is included as a click possibility
	
	
	private static final BigDecimal NFE_BLAST_PENALTY = BigDecimal.valueOf(0.75);
	
	private static final boolean ALLOW_ZERO_NET_GAIN_CHORD = true;
	private static final boolean ALLOW_ZERO_NET_GAIN_PRE_CHORD = true;
	
	private BoardState board;
	private WitnessWeb wholeEdge;
	private List<Action> actions;
	private ProbabilityEngineModel pe;
	//private Set<Location> risky3BVRevealed;
	
	public EfficiencyHelper(BoardState board, WitnessWeb wholeEdge, List<Action> actions, ProbabilityEngineModel pe)  {
		
		this.board = board;
		this.actions = actions;
		this.wholeEdge = wholeEdge;
		this.pe = pe;
		
		//this.risky3BVRevealed = new HashSet<>();
	}

	
	public List<Action> process() {
		
		List<Action> result = new ArrayList<>();
		List<ChordLocation> chordLocations = new ArrayList<>();
		
        //
        // identify all the tiles which are next to a known mine
        //
		
    	Map<Location, BigDecimal> zeroProbs = new HashMap<>();
    	
    	// locations next to a mine can't be zero
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {
				if (this.board.isConfirmedMine(i, j)) {
					for (Location adjTile: board.getAdjacentSquaresIterable(this.board.getLocation(i, j))) {
						if (board.isUnrevealed(adjTile)) {
							zeroProbs.put(adjTile, BigDecimal.ZERO);  // tiles adjacent to a mine have zero probability of being a '0'
						}
					}
				}
			}
		}                  
		
		// look for tiles satisfied by known mines and work out the benefit of placing the mines and then chording
		for (Location loc: board.getAllLivingWitnesses()) {
			
			// if the witness is satisfied how many clicks will it take to clear the area by chording
			if (board.getWitnessValue(loc) == board.countAdjacentConfirmedFlags(loc)) {
				
				List<Location> mines = new ArrayList<>();
				
                // how many hidden tiles are next to the mine(s) we would have flagged, the more the better
                // this favours flags with many neighbours over flags buried against cleared tiles.
                Set<Location> hiddenMineNeighbours = new HashSet<>();  
                for (Location adjMine: board.getAdjacentSquaresIterable(loc)) {

                    if (!board.isConfirmedMine(adjMine)) {
                        continue;
                    }
                    
                    // if the flag isn't on the board we need to add it
                    if (!board.isFlagOnBoard(adjMine)) {
                        mines.add(adjMine);
                    }

                    for (Location adjTile:  board.getAdjacentSquaresIterable(adjMine)) {
                        if (board.isUnrevealed(adjTile)) {
                            hiddenMineNeighbours.add(adjTile);
                        }
                    }                       
                }
				
				int cost = board.getWitnessValue(loc) - board.countAdjacentFlagsOnBoard(loc);  // flags still to be placed
				int benefit = board.countAdjacentUnrevealed(loc);   
				
				if (board.getWitnessValue(loc) != 0) {  // if the witness isn't a zero then add the cost of chording - zero can only really happen in the analyser
                    cost++;
                }
				
				if (benefit >= cost) {
					board.getLogger().log(Level.INFO, "Chord %s has reward %d and tiles adjacent to new flags %d", loc, (benefit - cost), hiddenMineNeighbours.size());
					chordLocations.add(new ChordLocation(loc.x, loc.y, benefit, cost, hiddenMineNeighbours.size(), BigDecimal.ONE, mines));
				}
			}

		}

		final BigDecimal oneAdvantageTest = BigDecimal.ONE.subtract(ONE_ADVANTAGE_THRESHOLD);
		
		// also consider tiles which are possibly mines and their benefit
		for (CandidateLocation cl: pe.getProbableMines(MINE_THRESHOLD)) {

            for (Location adjTile:  board.getAdjacentSquaresIterable(cl)) {
                if (board.isRevealed(adjTile) && board.getWitnessValue(adjTile) - board.countAdjacentConfirmedFlags(adjTile) == 1) { // if the adjacent tile needs 1 more tile
                	
    				int cost = board.getWitnessValue(adjTile) - board.countAdjacentFlagsOnBoard(adjTile) + 1;    // placing the flag and chording
    				int benefit = board.countAdjacentUnrevealed(adjTile) - 1; // the probable mine isn't a benefit  
   				
    				if (benefit >= cost + RISK_ADVANTAGE || benefit > cost && cl.getProbability().compareTo(oneAdvantageTest) < 0) {
    					
                		List<Location> mines = new ArrayList<>();
                    	mines.add(cl);
       					Set<Location> hiddenMineNeighbours = new HashSet<>();
                        for (Location adjNewFlag:  board.getAdjacentSquaresIterable(cl)) {
                            if (board.isUnrevealed(adjNewFlag)) {
                                hiddenMineNeighbours.add(adjNewFlag);
                            }
                        }                       
       					
                        for (Location adjMine: board.getAdjacentSquaresIterable(adjTile)) {

                            if (!board.isConfirmedMine(adjMine)) {
                                continue;
                            }
                            
                            // if the flag isn't on the board we need to add it
                            if (!board.isFlagOnBoard(adjMine)) {
                                mines.add(adjMine);
                            }

                            for (Location adjNewFlag:  board.getAdjacentSquaresIterable(adjMine)) {
                                if (board.isUnrevealed(adjNewFlag)) {
                                    hiddenMineNeighbours.add(adjNewFlag);
                                }
                            }                       
                        }
                    	
    					board.getLogger().log(Level.INFO, "Placing possible mine %s and Chording %s has reward %d and tiles adjacent to new flags %d", cl, adjTile, (benefit - cost), hiddenMineNeighbours.size());
    					chordLocations.add(new ChordLocation(adjTile.x, adjTile.y, benefit, cost, hiddenMineNeighbours.size(), BigDecimal.ONE.subtract(cl.getProbability()), mines));
    				}                    	
                }
            }                       
 			
		}
		
		
		// sort the most beneficial chords to the top
		Collections.sort(chordLocations, ChordLocation.SORT_BY_BENEFIT_DESC);
		
		ChordLocation bestChord = null;
		BigDecimal bestNetBenefit = BigDecimal.ZERO;
		for (ChordLocation cl: chordLocations) {
			
			if (cl.getNetBenefit().signum() > 0  || EfficiencyHelper.ALLOW_ZERO_NET_GAIN_CHORD && cl.getNetBenefit().signum() == 0 && cl.getCost() > 0) {
				bestChord = cl;
				bestNetBenefit = cl.getNetBenefit();
			}
			
			break;
		}
		
		if (bestChord != null) {
			board.getLogger().log(Level.INFO, "Chord %s has best reward of %f", bestChord, bestChord.getNetBenefit());
		} else {
			board.getLogger().log(Level.INFO, "No Chord has net benefit >= 0");
		}
		
		/*
		for (ChordLocation cl: chordLocations) {
			
			for (Location l: board.getAdjacentSquaresIterable(cl)) {
				// flag not yet on board
				if (!processed[l.x][l.y] && board.isConfirmedFlag(l) && !board.isFlagOnBoard(l)) {
					result.add(new Action(l, Action.FLAG, MoveMethod.TRIVIAL, "Place flag", BigDecimal.ONE, 0));
				}
				// flag on board in error
				if (!processed[l.x][l.y] && !board.isConfirmedFlag(l) && board.isFlagOnBoard(l)) {
					result.add(new Action(l, Action.FLAG, MoveMethod.CORRECTION, "Remove flag", BigDecimal.ONE, 0));
				}
				processed[l.x][l.y] = true;
			}
			// now add the clear all
			result.add(new Action(cl, Action.CLEARALL, MoveMethod.TRIVIAL, "Clear All", BigDecimal.ONE, 1));	
				
			break;

		}
		*/
			
		List<Action> neutral3BV = new ArrayList<>();
		Action bestAction = null;
		BigDecimal highest = BigDecimal.ZERO;

		Action bestZero = null;
		BigInteger bestZeroSolutions = BigInteger.ZERO;
		
		List<Location> emptyList = Collections.emptyList();
		
		SolutionCounter currSolnCount = board.getSolver().validatePosition(wholeEdge, emptyList, null, Area.EMPTY_AREA);
		if (bestNetBenefit.signum() > 0) {
			highest = new BigDecimal(currSolnCount.getSolutionCount()).multiply(bestNetBenefit);
		}
		
		// find all the tiles next to a confirmed mine, or safe
		List<Location> candidates = new ArrayList<>();
		for (Action act: actions) {
			if (!zeroProbs.containsKey(act)) {
				candidates.add(act);
			}
		}
		candidates.addAll(zeroProbs.keySet());

		// look for click then chord if the right number turns up
		// or chord then chord if the right number turns up

		//for (Action act: actions) {
		for (Location loc: candidates) {
			
			BigDecimal testP = this.pe.getSafety(loc);
			if (testP.compareTo(SAFE_3BV_CLICK_THRESHOLD) < 0) {
				continue;
			}
			Action act = new Action(loc, Action.CLEAR, MoveMethod.TRIVIAL, "Clear", testP, 0);
			if (act.getAction() == Action.CLEAR) {
				
				// check to see if the tile (trivially) can't be next to a zero. i.e. 3BV safe
				boolean valid = true;
                for (Location adjTile: this.board.getAdjacentSquaresIterable(act)) {
                	if (this.board.isUnrevealed(adjTile)) {
                		if (!zeroProbs.containsKey(adjTile)) {
                			valid = false;
                			break;
                		}
                	}
                }
                if (valid) {
                	board.getLogger().log(Level.INFO, "Tile %s is 3BV safe because it can't be next to a zero", act);
                	neutral3BV.add(act);
                }
                
                
                // find all the chords adjacent to this clear 
                List<ChordLocation> adjChords = new ArrayList<>();
                
                for (ChordLocation cl: chordLocations) {
                    if (cl.getNetBenefit().signum() == 0 && !ALLOW_ZERO_NET_GAIN_PRE_CHORD) {
                        continue;
                    }

                    if (cl.isAdjacent(act)) {
                        adjChords.add(cl);
                    	// first adjacent chord, or better adj chord, or cheaper adj chord, or exposes more tiles 
                        //if (adjChord == null || adjChord.getNetBenefit().compareTo(cl.getNetBenefit()) < 0 || adjChord.getNetBenefit().compareTo(cl.getNetBenefit()) == 0  && adjChord.getCost() > cl.getCost() || 
                        //		adjChord.getNetBenefit().compareTo(cl.getNetBenefit()) == 0 && adjChord.getCost() == cl.getCost() && adjChord.getExposedTileCount() < cl.getExposedTileCount()) {
                        //    adjChord = cl;
                        //}
                    }
                }
                //if (adjChord == null) {
                //    //console.log("(" + act.x + "," + act.y + ") has no adjacent chord with net benefit > 0");
                //} else {
                //	board.getLogger().log(Level.INFO, "Tile %s has adjacent chord %s with net benefit %f", act, adjChord, adjChord.getNetBenefit());
                //}
				
				
				int adjMines = board.countAdjacentConfirmedFlags(act);
				int adjFlags = board.countAdjacentFlagsOnBoard(act);
				int hidden = board.countAdjacentUnrevealed(act);
				
				int chordCost;
                if (adjMines != 0) {  // if the value we want isn't zero we'll need to subtract the cost of doing the chording
                    chordCost = 1;
                } else {
                    chordCost = 0;
                }
				
				//BigDecimal chordReward = BigDecimal.valueOf(hidden - adjMines + adjFlags - chordCost);  // tiles adjacent - ones which are mines - mines which aren't flagged yet
				BigDecimal chordReward = ChordLocation.chordReward(hidden, adjMines - adjFlags + chordCost);
				
				if (chordReward.compareTo(bestNetBenefit) > 0) {
					
					SolutionCounter counter = board.getSolver().validateLocationUsingSolutionCounter(wholeEdge, act, adjMines, Area.EMPTY_AREA);
					
					// if no solutions for this click value then move to the next option
					//if (counter.getSolutionCount().signum() == 0) {
					//	continue;
					//}
					
					BigDecimal current = new BigDecimal(counter.getSolutionCount()).multiply(chordReward);
					
					BigDecimal prob = new BigDecimal(counter.getSolutionCount()).divide(new BigDecimal(currSolnCount.getSolutionCount()), 10, RoundingMode.HALF_UP);
					
					// realistic expectation
					BigDecimal expBenefit = current.divide(new BigDecimal(currSolnCount.getSolutionCount()), 10, RoundingMode.HALF_UP);
					
					board.getLogger().log(Level.INFO, "considering Clear (" + act.x + "," + act.y + ") with value " + adjMines + " and reward " + chordReward + " ( H=" + hidden + " M=" + adjMines + " F=" + adjFlags + " Chord=" + chordCost
                            + " Prob=" + prob + "), expected benefit " + expBenefit);
					
                    // if we have found an 100% certain zero then just click it.
                    if (adjMines == 0) {
                    	if (prob.compareTo(IGNORE_ZERO_THRESHOLD) < 0) {  // don't click a potential zero with a low chance of success
                    		continue;
                    		
                    	} else if (counter.getSolutionCount().equals(currSolnCount.getSolutionCount())) {
                           	board.getLogger().log(Level.INFO, "Tile %s is a certain zero no need for further analysis", act);
                        	bestZero = act;
                        	bestZeroSolutions = currSolnCount.getSolutionCount();
                        	bestAction = null;
                            bestChord = null;
                            break;

                    	} else if (counter.getSolutionCount().compareTo(bestZeroSolutions) > 0) { 
                    		bestZero = act;
                    		bestZeroSolutions = counter.getSolutionCount();
                    	}
                    } 
					
                    // realistic expectation
                    BigDecimal clickChordNetBenefit = chordReward.multiply(new BigDecimal(counter.getSolutionCount())); // expected benefit from clicking the tile then chording it
                    
                    // optimistic expectation
                    //BigDecimal clickChordNetBenefit = BigDecimal.valueOf(reward).multiply(new BigDecimal(currSolnCount.getSolutionCount())); // expected benefit from clicking the tile then chording it
                    
                    //if (adjMines == 0) {
                    //	adjChord = null;
                    //	board.getLogger().log(Level.INFO, "Not considering Chord Chord combo because we'd be chording into a zero");
                    //}
                    
                    current = clickChordNetBenefit;  // expected benefit == p*benefit
                    ChordLocation adjChord = null;
                    
                    for (ChordLocation cl: adjChords) {
                    	board.getLogger().log(Level.INFO, "Tile %s has adjacent chord %s with net benefit %f", act, cl, cl.getNetBenefit());
                    	
                        BigDecimal tempCurrent = chordChordCombo(cl, act, counter.getSolutionCount(), currSolnCount.getSolutionCount());
                        
                        // chose the better option or if the same choose the chord/chord option over the click/chord
                        if (current.compareTo(tempCurrent) < 0 || current.compareTo(tempCurrent) == 0 && adjChord == null) {  // keep track of the best chord / chord combo
                            current = tempCurrent;
                            adjChord = cl;
                        }
                    }
                    
                    /*
                    // if it is a chord/chord combo
                    if (adjChord != null) {
                        current = chordChordCombo(adjChord, act, counter.getSolutionCount(), currSolnCount.getSolutionCount());
                        
                        if (current.compareTo(clickChordNetBenefit) < 0) {  // if click chord is better then discard the adjacent chord
                            current = clickChordNetBenefit;
                            adjChord = null;
                        }

                    } else {  // or a clear/chord combo
                        current = clickChordNetBenefit;  // expected benefit == p*benefit
                    }
                    */
                    
					if (current.compareTo(highest) > 0) {
                        highest = current;
                        if (adjChord != null) {  // if there is an adjacent chord then use this to clear the tile
                            bestChord = adjChord;
                            bestAction = null;
                        } else {
                            bestChord = null;
                            bestAction = act;
                        }
					}					
					
				} else {
					board.getLogger().log(Level.INFO, "Not considering Tile %s", act);
				}
				
			}
			
		}		
		
		final BigInteger zeroThreshold = new BigDecimal(currSolnCount.getSolutionCount()).multiply(CLEAR_ZERO_VALUE).toBigInteger();
		if (bestZero != null && bestZeroSolutions.compareTo(zeroThreshold) >= 0) {  // only want zeros which are 100% safe, the others get judged as a best action.
			result.add(bestZero);
			
		} else if (bestAction != null) {
			result.add(bestAction);
		
		} else if (bestChord != null) {
            result.clear();
            
            // add the required flags if they aren't already there
            for (Location adjMine: bestChord.getMines()) {
            	if (!board.isFlagOnBoard(adjMine)) {
                	result.add(new Action(adjMine, Action.FLAG, MoveMethod.TRIVIAL, "Place flag", BigDecimal.ONE, 0));
            	}
            }
            
            //for (Location adjTile: board.getAdjacentSquaresIterable(bestChord)) {
            //    if (board.isConfirmedFlag(adjTile) && !board.isFlagOnBoard(adjTile)) {
            //       result.add(new Action(adjTile, Action.FLAG, MoveMethod.TRIVIAL, "Place flag", BigDecimal.ONE, 0));
            //    }
            //}

            // Add the chord action
            result.add(new Action(bestChord, Action.CLEARALL, MoveMethod.TRIVIAL, "Clear All", bestChord.getScale(), 1));
        }
		
		if (!result.isEmpty()) {  // found a good move
			return result;
		
		} else if (!neutral3BV.isEmpty()) {  // return the first 3BV neutral action
			result.add(neutral3BV.get(0));
			return result;
		
		} else if (result.isEmpty()) {  // return the first action
			result.add(actions.get(0));
			return result;
		
		} else {
			return result;
		}
		

	}
	
	// the ChordLocation of the tile to chord, the Tile to be chorded afterwards if the value comes up good, the number of solutions where this occurs
    // and the total number of solutions
    // this method works out the net benefit of this play
    private BigDecimal chordChordCombo(ChordLocation chord1, Location chord2Tile, BigInteger occurs, BigInteger total) {

        // now check each tile around the tile to be chorded 2nd and see how many mines to flag and tiles will be cleared
        int alreadyCounted = 0;
        int needsFlag = 0;
        int clearable = 0;
        int chordClick = 0;
        for (Location adjTile: board.getAdjacentSquaresIterable(chord2Tile)) {

            if (board.isConfirmedMine(adjTile)) {
                chordClick = 1;
            }

            // if adjacent to chord1
            if (chord1.isAdjacent(adjTile)) {
                alreadyCounted++;
            } else if (board.isConfirmedMine(adjTile) && !board.isFlagOnBoard(adjTile)) {
                needsFlag++;
            } else if (board.isUnrevealed(adjTile)) {
                clearable++;
            }
        }

        BigDecimal failedBenefit = chord1.getNetBenefit(); 
        //BigDecimal secondBenefit;
        //if (chordClick == 0) {   // chording into a zero is waste of the tiles adjacent to the zero
        //	secondBenefit = BigDecimal.valueOf(1);
        //} else {
        //	secondBenefit = ChordLocation.chordReward(clearable, needsFlag + chordClick);
        //}
        	
        BigDecimal secondBenefit = ChordLocation.chordReward(clearable, needsFlag + chordClick);

        // realistic expectation
        BigDecimal score = failedBenefit.multiply(new BigDecimal(total)).add(secondBenefit.multiply(new BigDecimal(occurs)));

        // optimistic expectation
        //BigDecimal score = failedBenefit.multiply(new BigDecimal(total)).add( BigDecimal.valueOf(secondBenefit).multiply(new BigDecimal(total)));
        
        BigDecimal expBen = score.divide(new BigDecimal(total), Solver.DP, RoundingMode.HALF_DOWN);
        
        board.getLogger().log(Level.INFO, "Chord %s followed by Chord %s: Chord 1: benefit %f, Chord2: H=%d, to F=%d, Chord=%d, Benefit=%f ==> expected benefit %f"
        		, chord1, chord2Tile, chord1.getNetBenefit(), clearable, needsFlag, chordClick, secondBenefit, expBen);
        
        return score;

    }
	
    
    /**
     * A No-flag efficiency algorithm
     */
    public List<Action> processNF() {

    	List<Action> result = new ArrayList<>();
    	
    	//Set<Location> notZeros = new HashSet<>();
    	
    	Map<Location, BigDecimal> zeroProbs = new HashMap<>();
    	
    	// locations next to a mine can't be zero
		for (int i=0; i < board.getGameWidth() - 1; i++) {
			for (int j=0; j < board.getGameHeight() - 1; j++) {
				if (this.board.isConfirmedMine(i, j)) {
					for (Location adjTile: board.getAdjacentSquaresIterable(this.board.getLocation(i, j))) {
						if (board.isUnrevealed(adjTile)) {
							zeroProbs.put(adjTile, BigDecimal.ZERO);  // tiles adjacent to a mine have zero probability of being a '0'
						}
					}
					
					//notZeros.addAll(board.getAdjacentUnrevealedSquares(this.board.getLocation(i, j)));
				}
			}
		}                        
    	
		// calculate the current solution count
		List<Location> emptyList = Collections.emptyList();
		SolutionCounter currSolnCount = board.getSolver().validatePosition(wholeEdge, emptyList, null, Area.EMPTY_AREA);
		
		
		Set<Location> onEdgeSet = new HashSet<>(this.wholeEdge.getSquares());
		
		Set<Location> adjacentEdgeSet = new HashSet<>();
		
		BigDecimal zeroTileScore = null;
		Location zeroTile = null;
		
		 // do a more costly check for whether zero is possible, for those which haven't already be determined
		for (Location tile: this.wholeEdge.getSquares()) {
			
			if (!zeroProbs.containsKey(tile) && !this.board.isConfirmedMine(tile)) {
				SolutionCounter counter = board.getSolver().validateLocationUsingSolutionCounter(wholeEdge, tile, 0, Area.EMPTY_AREA);
				
				if (counter.getSolutionCount().signum() == 0) {  // no solution where this is a zero
					zeroProbs.put(tile, BigDecimal.ZERO);
				} else if (counter.getSolutionCount().compareTo(currSolnCount.getSolutionCount()) == 0) {
					board.getLogger().log(Level.INFO, "Tile %s is always zero", tile);
					result.add(new Action(tile, Action.CLEAR, MoveMethod.TRIVIAL, "Certain zero (1)", BigDecimal.ONE));
					break;
				} else {

					BigDecimal zeroProb = new BigDecimal(counter.getSolutionCount()).divide(new BigDecimal(currSolnCount.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					zeroProbs.put(tile, zeroProb);
					
					BigDecimal safety = this.pe.getSafety(tile);
					BigDecimal score = zeroProb.subtract(BigDecimal.ONE.subtract(safety).multiply(NFE_BLAST_PENALTY));
					
					if (zeroTile == null || zeroTileScore.compareTo(score) < 0) {
						zeroTile = tile;
						zeroTileScore = score;
					}
				}
			}

			// collect hidden tiles adjacent to the boundary and not on the boundary
			for (Location adjTile: this.board.getAdjacentSquaresIterable(tile)) {
				if (this.board.isUnrevealed(adjTile) && !onEdgeSet.contains(adjTile)) {
					adjacentEdgeSet.add(adjTile);
				}
			}
			
		}
    	
		if (!result.isEmpty()) {
			return result;
		}
		
		 // do a more costly check for whether zero is possible for actions not already considered, for those which haven't already be determined
		for (Action tile: this.actions) {
			
			if (tile.getAction() == Action.CLEAR && !zeroProbs.containsKey(tile)) {
				SolutionCounter counter = board.getSolver().validateLocationUsingSolutionCounter(wholeEdge, tile, 0, Area.EMPTY_AREA);
				
				if (counter.getSolutionCount().signum() == 0) {  // no solution where this is a zero
					zeroProbs.put(tile, BigDecimal.ZERO);
				} else if (counter.getSolutionCount().compareTo(currSolnCount.getSolutionCount()) == 0) {
					board.getLogger().log(Level.INFO, "Tile %s is always zero", tile);
					result.add(tile);
					break;
				} else {

					BigDecimal zeroProb = new BigDecimal(counter.getSolutionCount()).divide(new BigDecimal(currSolnCount.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					zeroProbs.put(tile, zeroProb);
					
					BigDecimal safety = this.pe.getSafety(tile);
					BigDecimal score = zeroProb.subtract(BigDecimal.ONE.subtract(safety).multiply(NFE_BLAST_PENALTY));
					
					if (zeroTile == null || zeroTileScore.compareTo(score) < 0) {
						zeroTile = tile;
						zeroTileScore = score;
					}
				}
			}

			// collect hidden tiles adjacent to the boundary and not on the boundary
			for (Location adjTile: this.board.getAdjacentSquaresIterable(tile)) {
				if (this.board.isUnrevealed(adjTile) && !onEdgeSet.contains(adjTile)) {
					adjacentEdgeSet.add(adjTile);
				}
			}
			
		}
		
		if (!result.isEmpty()) {
			return result;
		}
		
		BigDecimal offEdgeSafety = this.pe.getOffEdgeSafety();
		
		// see if tiles adjacent to the boundary can be zero
		for (Location tile: adjacentEdgeSet) {
			SolutionCounter counter = board.getSolver().validateLocationUsingSolutionCounter(wholeEdge, tile, 0, Area.EMPTY_AREA);
			
			if (counter.getSolutionCount().signum() == 0) {  // no solution where this is a zero
				zeroProbs.put(tile, BigDecimal.ZERO);
				
			} else if (counter.getSolutionCount().compareTo(currSolnCount.getSolutionCount()) == 0) {
				board.getLogger().log(Level.INFO, "Tile %s is always zero", tile);
				result.add(new Action(tile, Action.CLEAR, MoveMethod.TRIVIAL, "Certain zero (2)", BigDecimal.ONE));
				break;
			} else {

				BigDecimal zeroProb = new BigDecimal(counter.getSolutionCount()).divide(new BigDecimal(currSolnCount.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
				zeroProbs.put(tile, zeroProb);
				
				BigDecimal score = zeroProb.subtract(BigDecimal.ONE.subtract(offEdgeSafety).multiply(NFE_BLAST_PENALTY));
				
				if (zeroTile == null || zeroTileScore.compareTo(score) < 0) {
					zeroTile = tile;
					zeroTileScore = score;
				}
			}
		}

		if (!result.isEmpty()) {
			return result;
		}
		
		BigDecimal maxAllNotZeroProbability = BigDecimal.ZERO;
		Action bestAllNotZeroAction = null;
		
		// see if any safe tiles are also never next to a zero
		for (Action act: actions) {

			if (act.getAction() == Action.CLEAR) {
				
                // find the best chord adjacent to this clear if there is one
                //boolean valid = true;
                BigDecimal allNotZeroProbability = BigDecimal.ONE;
                // if all the adjacent tiles can't be zero then we are safe to clear this tile without wasting a 3BV
                for (Location adjTile: this.board.getAdjacentSquaresIterable(act)) {
                	if (this.board.isUnrevealed(adjTile)) {
                		if (zeroProbs.containsKey(adjTile)) {
                			allNotZeroProbability = allNotZeroProbability.multiply(BigDecimal.ONE.subtract(zeroProbs.get(adjTile)));
                		} else {
                			board.getLogger().log(Level.WARN, "Tile %s doesn't have a probability for being a zero", adjTile);
                		}
                		
                	}
                	
                }
                if (bestAllNotZeroAction == null || maxAllNotZeroProbability.compareTo(allNotZeroProbability) < 0) {
                	bestAllNotZeroAction = act;
                	maxAllNotZeroProbability = allNotZeroProbability;
                }
                
                if (allNotZeroProbability.compareTo(BigDecimal.ONE) == 0) {
                	board.getLogger().log(Level.INFO, "Tile %s is 3BV safe because it can't be next to a zero", act);
                	result.add(act);
                 }
			}
		}
		
		if (!result.isEmpty()) {
			return result;
		}		
		
       if (zeroTile != null) {

    	   BigDecimal prob = this.pe.getSafety(zeroTile);
            if (bestAllNotZeroAction != null) {
                if (maxAllNotZeroProbability.compareTo(zeroTileScore ) > 0 && zeroTileScore.compareTo(BigDecimal.ZERO) < 0) {
                    result.add(bestAllNotZeroAction);
                } else {
                    result.add(new Action(zeroTile, Action.CLEAR, MoveMethod.TRIVIAL, "best zero", prob));
                }
            } else {
            	 result.add(new Action(zeroTile, Action.CLEAR, MoveMethod.TRIVIAL, "best zero", prob));
            }
        } else {
            if (bestAllNotZeroAction != null) {
                result.add(bestAllNotZeroAction);
            }
        }
		
		// otherwise use the best tile looking for a zero
		//Action action = new Action(zeroTile, Action.CLEAR, MoveMethod.TRIVIAL, "best zero", this.pe.getProbability(zeroTile));
		//result.add(action);
		//board.getLogger().log(Level.INFO, "Action %s", action);
		
    	return result;
    }
    
    private class NfData {
    	
    	private BigDecimal safety = BigDecimal.ZERO;
    	private BigDecimal zeroProbability = BigDecimal.ZERO;
    	private boolean zeroPoison = false;
    	
    }
    
    
    //
    // Improved NF efficiency logic
    //
    public List<Action> processImprovedNF() {

		long startTime = System.currentTimeMillis();

        BigInteger tsc;
        if (this.pe == null) {
            tsc = BigInteger.ONE;
        } else {
            tsc = this.pe.getSolutionCount();
        }
        
        //final BigInteger solutionsCount = tsc;
        final BigDecimal solutionsCount = new BigDecimal(tsc);

        final int value = 0;

        // check that at least 1 tile can be a zero
        boolean openingNotPossible = true;

        // an array of probabilities for simple cases where the tile is surrounded by n-floating tiles and nothing else.
        BigDecimal[] simple = new BigDecimal[9];
        //Arrays.fill(simple, -1);
        
        NfData[][] data = new NfData[board.getGameWidth()][board.getGameHeight()];
        
        Set<Location> risky3BVRevealed = board.get3BVRiskyTiles(wholeEdge.getOriginalWitnesses());
        
    	// locations next to a mine can't be zero
		for (int i=0; i < board.getGameWidth(); i++) {
			for (int j=0; j < board.getGameHeight(); j++) {
				
            	Location tile = this.board.getLocation(i, j);
            	
				NfData tileData = new NfData();
				data[i][j] = tileData;
				
	            // no need to analyse a revealed tile
	            if (board.isRevealed(tile)) {
	                continue;
	            }

	            // store this tiles safety
	            tileData.safety = this.pe.getSafety(tile);
	            
				// no need to analyse a bomb
				if (board.isConfirmedMine(tile) || tileData.safety.signum() == 0) {
					continue;
				}
				
	            // if the number of mines adjacent is > 0 then this can't be a zero
	            int adjMines = this.board.countAdjacentConfirmedFlags(tile);
	            if (adjMines > 0) {
	                continue;
	            }
	            
	            final int floating = this.evaluateTileForValue(tile);
	            if (floating != -1 && simple[floating] != null) {
	                tileData.zeroProbability = simple[floating];

	            } else {
	            	
	    			SolutionCounter work = board.getSolver().validateLocationUsingSolutionCounter(wholeEdge, tile, 0, Area.EMPTY_AREA);
	    			
	                // if this is a valid board state
	                if (work.getSolutionCount().signum() > 0) {
	                    BigDecimal valueProbability = new BigDecimal(work.getSolutionCount()).divide(solutionsCount, 10, RoundingMode.HALF_UP);
	                    
	                    tileData.zeroProbability = valueProbability;

	                    if (floating != -1) {
	                        simple[floating] = valueProbability;
	                    }

	                } else {

	                    //console.log(tile.asText() + " can't be a '" + value + "'");
	                }
	            }
	            
	            // a zero is possible
	            if (tileData.zeroProbability.signum() > 0) {
	                openingNotPossible = false;
	            }


	            // if the tile is adjacent to a risky revealed tile then this tile is zero% poison.
	            // i.e. it opens a zero next to a 3BV risky tile, or creates another 3BV risky tile.
	            if (tileData.zeroProbability.signum() != 0 && tileData.zeroProbability.compareTo(BigDecimal.ONE) != 0) {
	                for (Location adjTile: this.board.getAdjacentSquaresIterable(tile)) {
	                    if (risky3BVRevealed.contains(adjTile)) {
	                    	board.getLogger().log(Level.INFO, "Tile %s is 3BV poison", tile);
	                        tileData.zeroPoison = true;
	                        break;
	                    }	                	
	                }
	            }


			}
		}                        
		
		board.getLogger().log(Level.INFO, "Evaluating Zero probabilities took " + (System.currentTimeMillis() - startTime) + " milliseconds");

        if (openingNotPossible) {
        	board.getLogger().log(Level.INFO, "No opening is possible, so reverting to best play");
            //return this.removeFlagging(this.actions);
            return Collections.emptyList();
        }

        
        // how much the zero % is inflated to make a zero more attractive
        final BigDecimal ZERO_WEIGHT = new BigDecimal("0.6");

        // the amount a poison zero is allowed to contibute to the 3bv safety contribution
        final BigDecimal POISON_RATIO = new BigDecimal("0.75");

        // find the tile with highest 3BV safety
        List<Location> safe3BV = new ArrayList<>();
        BigDecimal best3BVSafety = BigDecimal.ZERO;
        //BigDecimal bestZeroProbability = BigDecimal.ZERO;
        Location bestTile = null;
        NfData bestTileData = null;

        BigDecimal bestScore = BigDecimal.ZERO;

    	// locations next to a mine can't be zero
		for (int i=0; i < board.getGameWidth(); i++) {
			for (int j=0; j < board.getGameHeight(); j++) {
				
            	Location tile = this.board.getLocation(i, j);
            	
				// no need to analyse a bomb
				if (board.isConfirmedMine(tile)) {
					continue;
				}
				
	            // no need to analyse a revealed tile
	            if (board.isRevealed(tile)) {
	                continue;
	            }

				NfData tileData = data[i][j];
				
	            // don't consider dead tiles unless 100% safe
	            if (tileData.safety.compareTo(BigDecimal.ONE) != 0 && this.pe.getDeadLocations().contains(tile)) {
	                continue;
	            }
				
	            // 3BV safety can never be larger than normal safety + the zero bonus
	            if (tileData.safety.add(tileData.zeroProbability.multiply(ZERO_WEIGHT)).compareTo(best3BVSafety) < 0) {
	                continue;
	            }
	            
	            //const adjTiles = this.board.getAdjacent(tile);
	            BigDecimal safety3BV = tileData.safety;
	            
	            if (tileData.zeroProbability.signum() > 0) {
		            for (Location adjTile1: this.board.getAdjacentSquaresIterable(tile)) {
		            	
		            	NfData adjTile1Data = data[adjTile1.x][adjTile1.y];
		            	
		                // is covered and not 100% safe. Can't be a mine or we would have Zero % > 0
	                    if (!this.board.isRevealed(adjTile1) && adjTile1Data.safety.compareTo(BigDecimal.ONE) != 0) {
	                    	
	                        for (Location adjTile2: this.board.getAdjacentSquaresIterable(tile)) {
	                            if (!adjTile2.equals(adjTile1) && !adjTile2.isAdjacent(adjTile1)) {
	                                BigDecimal zeroProb;
	                                NfData adjTile2Data = data[adjTile2.x][adjTile2.y];
	                                if (adjTile2Data == null) {
	                                	board.getLogger().log(Level.ERROR, "%s has no 3BV Zero data", tile);
	                                	zeroProb = BigDecimal.ZERO;
	                                } else if (adjTile2Data.zeroPoison) {
	                                    zeroProb = adjTile2Data.zeroProbability.multiply(POISON_RATIO);
	                                } else {
	                                    zeroProb = adjTile2Data.zeroProbability;
	                                }
	                                safety3BV = safety3BV.subtract(zeroProb.multiply(BigDecimal.ONE.subtract(adjTile1Data.safety)));  // safety3bv - (zeroProb* (1- adjTile1.safety))
	                            } else {
	                                //console.log("Ignore " + adjTile1.asText() + " and " + adjTile2.asText());
	                            }
	                        }
	                    }
		            	
		            }
	            
	            } else {  // can't be a zero, how 3bv safe is it?
	                for (Location adjTile1: this.board.getAdjacentSquaresIterable(tile)) {

		            	NfData adjTile1Data = data[adjTile1.x][adjTile1.y];
		            	
	                    // is covered and not a mine
	                    if (!this.board.isRevealed(adjTile1) && adjTile1Data.safety.signum() != 0) {
	                        BigDecimal zeroProb;
	                        if (adjTile1Data.zeroPoison) {
	                            zeroProb = adjTile1Data.zeroProbability.multiply(POISON_RATIO);
	                        } else {
	                            zeroProb = adjTile1Data.zeroProbability;
	                        }

	                        safety3BV = safety3BV.subtract(zeroProb);
	                    }
	                }
	            }

	            // store all the 100% safe 3BV
	            //if (safety3BV == 1 && !tile.zeroPoison) {
	            if (safety3BV.compareTo(BigDecimal.ONE) == 0) {
	                safe3BV.add(tile);
	            }
	            	
	            
	            BigDecimal score = safety3BV.add(tileData.zeroProbability.multiply(ZERO_WEIGHT));
	            boolean better = false;
	            if (bestTile == null || bestTileData.zeroPoison && !tileData.zeroPoison) {
	                better = true;

	            } else if (!bestTileData.zeroPoison && tileData.zeroPoison) {
	                better = false;

	            } else if (score.compareTo(bestScore) > 0 || (score.compareTo(bestScore) == 0 && tileData.zeroProbability.compareTo(bestTileData.zeroProbability) > 0)) {
	                better = true;

	            } else {
	                better = false;
	            }

	            if (better) {
	                //this.writeToConsole(tile.asText() + " has estimated 3BV safety of " + safety3BV + " and Zero probability " + tile.zeroProbability + ", score=" + score);
	                board.getLogger().log(Level.INFO, "%s has estimated 3BV safety of %f and Zero probability %f, score=" + score, tile, safety3BV, tileData.zeroProbability, score);
	                bestTile = tile;
	                best3BVSafety = safety3BV;
	                bestTileData = tileData;
	                //bestZeroProbability = tile.zeroProbability;
	                bestScore = score;
	                board.getLogger().log(Level.INFO, "%s is now the recommended tile", tile);
	            }

			}
		}                        
        
        if (safe3BV.size() > 0) {
            List<Action> result = new ArrayList<>();
            board.getLogger().log(Level.INFO, "Found " + safe3BV.size() + " 3BV safe tiles");
            for (Location tile: safe3BV) {
                result.add(new Action(tile, Action.CLEAR, MoveMethod.TRIVIAL, "3BV Safe", BigDecimal.ONE));
            }

            return result;

        } else if (bestTile != null) {
            List<Action> result = new ArrayList<>();
            result.add(new Action(bestTile, Action.CLEAR, MoveMethod.TRIVIAL, "Best NF Guess", bestTileData.safety));  // send the best tile
            return result;
            
        } else {
            //return this.removeFlagging(this.actions);  // return the actions
            return Collections.emptyList();
        }

    }

    // Count how many adjacent tiles are 'floating tiles'. 
    // If any adjacent tiles are not floating and not mines or safe then return -1
    private int evaluateTileForValue(Location tile) {

    	//Location tile = this.board.getLocation(x, y);
    	
        if (this.wholeEdge.isOnWeb(tile)) {
            //console.log(tile.asText() + " on edge=" + tile.isOnEdge() + " = " + tile.onEdge);
            return -1;
        }

        int floating = 0;

        for (Location adjTile: this.board.getAdjacentSquaresIterable(tile)) {

			// no need to analyse a bomb
			if (board.isConfirmedMine(adjTile)) {
				continue;
			}
			
            // if 100% safe
            if (this.pe.getSafety(adjTile).compareTo(BigDecimal.ONE) == 0) {
                continue;
            }

            if (this.wholeEdge.isOnWeb(adjTile)) {
                //console.log(tile.asText() + " on edge=" + tile.isOnEdge() + " = " + tile.onEdge);
                floating++;
                continue;
            }
            
            //console.log(tile.asText() + " not mine, not safe, not floating");
            return -1;

        }

        //console.log(tile.asText() + " floating=" + floating);
        return floating;
    }
    
    
    // Only allow clears to be passed back
    private List<Action> removeFlagging(List<Action> actions) {

    	List<Action> result = new ArrayList<Action>();
    	
        for (Action action: actions) {
            if (action.getAction() == Action.CLEAR) {
                result.add(action);
            }
        }

        return result;

    }
    
}
