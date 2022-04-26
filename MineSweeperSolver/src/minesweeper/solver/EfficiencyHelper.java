package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.ChordLocation;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class EfficiencyHelper {
	
	private BigDecimal  MINE_THRESHOLD = BigDecimal.valueOf(1.0);   // probability of mine
	
	private BoardState board;
	private WitnessWeb wholeEdge;
	private List<Action> actions;
	private ProbabilityEngineModel pe;
	
	public EfficiencyHelper(BoardState board, WitnessWeb wholeEdge, List<Action> actions, ProbabilityEngineModel pe)  {
		
		this.board = board;
		this.actions = actions;
		this.wholeEdge = wholeEdge;
		this.pe = pe;
		
	}

	
	public List<Action> process() {
		
		//if (actions.size() < 2) {
		//	return actions;
		//}
		
		List<Action> result = new ArrayList<>();
		List<ChordLocation> chordLocations = new ArrayList<>();

		
		// look for tiles satisfied by known mines and work out the benefit of placing the mines and then chording
		for (Location loc: board.getAllLivingWitnesses()) {
			
			// if the witness is satisfied how many clicks will it take to clear the area by chording
			if (board.getWitnessValue(loc) == board.countAdjacentConfirmedFlags(loc)) {
				
				List<Location> mines = new ArrayList<>();
				
                // how many hidden tiles are next to the mine(s) we would have flagged, the more the better
                // this favours flags with many neighbours over flags buried against cleared tiles.
                Set<Location> hiddenMineNeighbours = new HashSet<>();  
                for (Location adjMine: board.getAdjacentSquaresIterable(loc)) {

                    if (!board.isConfirmedFlag(adjMine)) {
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

		// also consider tiles which are possibly mines and their benefit
		for (CandidateLocation cl: pe.getProbableMines(MINE_THRESHOLD)) {

            for (Location adjTile:  board.getAdjacentSquaresIterable(cl)) {
                if (board.isRevealed(adjTile) && board.getWitnessValue(adjTile) - board.countAdjacentConfirmedFlags(adjTile) == 1) { // if the adjacent tile needs 1 more tile
                	
    				int cost = board.getWitnessValue(adjTile) - board.countAdjacentFlagsOnBoard(adjTile) + 1;    // placing the flag and chording
    				int benefit = board.countAdjacentUnrevealed(adjTile) - 1; // the probable mine isn't a benefit  
   				
    				if (benefit > cost) {
    					
                		List<Location> mines = new ArrayList<>();
                    	mines.add(cl);
       					Set<Location> hiddenMineNeighbours = new HashSet<>();
                        for (Location adjNewFlag:  board.getAdjacentSquaresIterable(cl)) {
                            if (board.isUnrevealed(adjNewFlag)) {
                                hiddenMineNeighbours.add(adjNewFlag);
                            }
                        }                       
       					
                        for (Location adjMine: board.getAdjacentSquaresIterable(adjTile)) {

                            if (!board.isConfirmedFlag(adjMine)) {
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
			
			if (cl.getNetBenefit().signum() > 0  || cl.getNetBenefit().signum() == 0 && cl.getCost() > 0) {
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
			
		Action bestAction = null;
		BigDecimal highest = BigDecimal.ZERO;
		//int currentReward = 0;
		
		List<Location> emptyList = Collections.emptyList();
		
		SolutionCounter currSolnCount = board.getSolver().validatePosition(wholeEdge, emptyList, null, Area.EMPTY_AREA);
		if (bestNetBenefit.signum() > 0) {
			highest = new BigDecimal(currSolnCount.getSolutionCount()).multiply(bestNetBenefit);
		}
		
		
		for (Action act: actions) {

			if (act.getAction() == Action.CLEAR) {
				
                // find the best chord adjacent to this clear if there is one
                ChordLocation adjChord = null;
                for (ChordLocation cl: chordLocations) {
                    //if (cl.netBenefit == 0 && !ALLOW_ZERO_NET_GAIN_PRE_CHORD) {
                    //    continue;
                    //}

                    if (cl.isAdjacent(act)) {
                        // first adjacent chord, or better adj chord, or cheaper adj chord, or exposes more tiles 
                        if (adjChord == null || adjChord.getNetBenefit().compareTo(cl.getNetBenefit()) < 0 || adjChord.getNetBenefit().compareTo(cl.getNetBenefit()) == 0  && adjChord.getCost() > cl.getCost() || 
                        		adjChord.getNetBenefit().compareTo(cl.getNetBenefit()) == 0 && adjChord.getCost() == cl.getCost() && adjChord.getExposedTileCount() < cl.getExposedTileCount()) {
                            adjChord = cl;
                        }
                    }
                }
                if (adjChord == null) {
                    //console.log("(" + act.x + "," + act.y + ") has no adjacent chord with net benefit > 0");
                } else {
                	board.getLogger().log(Level.INFO, "Tile %s has adjacent chord %s with net benefit %f", act, adjChord, adjChord.getNetBenefit());
                 }
				
				
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
					
					BigDecimal current = new BigDecimal(counter.getSolutionCount()).multiply(chordReward);
					
					BigDecimal prob = new BigDecimal(counter.getSolutionCount()).divide(new BigDecimal(currSolnCount.getSolutionCount()), 10, RoundingMode.HALF_UP);
					
					// realistic expectation
					BigDecimal expBenefit = current.divide(new BigDecimal(currSolnCount.getSolutionCount()), 10, RoundingMode.HALF_UP);
					
					board.getLogger().log(Level.INFO, "considering Clear (" + act.x + "," + act.y + ") with value " + adjMines + " and reward " + chordReward + " ( H=" + hidden + " M=" + adjMines + " F=" + adjFlags + " Chord=" + chordCost
                            + " Prob=" + prob + "), expected benefit " + expBenefit);
					
                    // if we have found an 100% certain zero then just click it.
                    if (adjMines == 0 && counter.getSolutionCount().equals(currSolnCount.getSolutionCount())) {
                    	board.getLogger().log(Level.INFO, "Tile %s is a certain zero no need for further analysis", act);
                        bestAction = act;
                        break;
                    }					
					
                    // realistic expectation
                    BigDecimal clickChordNetBenefit = chordReward.multiply(new BigDecimal(counter.getSolutionCount())); // expected benefit from clicking the tile then chording it
                    
                    // optimistic expectation
                    //BigDecimal clickChordNetBenefit = BigDecimal.valueOf(reward).multiply(new BigDecimal(currSolnCount.getSolutionCount())); // expected benefit from clicking the tile then chording it
                    
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
		
		if (bestAction != null) {
			result.add(bestAction);
		}

        if (bestChord != null) {
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
		
		if (result.isEmpty()) {  // return the first action
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

        BigDecimal failedBenefit = chord1.getNetBenefit();
 
        //var chord1Tile = chord1.tile;

        // now check each tile around the tile to be chorded 2nd and see how many mines to flag and tiles will be cleared
        int alreadyCounted = 0;
        int needsFlag = 0;
        int clearable = 0;
        int chordClick = 0;
        for (Location adjTile: board.getAdjacentSquaresIterable(chord2Tile)) {

            if (board.isConfirmedFlag(adjTile)) {
                chordClick = 1;
            }

            // if adjacent to chord1
            if (chord1.isAdjacent(adjTile)) {
                alreadyCounted++;
            } else if (board.isConfirmedFlag(adjTile) && !board.isFlagOnBoard(adjTile)) {
                needsFlag++;
            } else if (board.isUnrevealed(adjTile)) {
                clearable++;
            }
        }

        //int secondBenefit = clearable - needsFlag - chordClick;  // tiles cleared - flags placed - the chord click (which isn't needed if a zero is expected)
        BigDecimal secondBenefit = ChordLocation.chordReward(clearable, needsFlag + chordClick);

        // realistic expectation
        BigDecimal score = failedBenefit.multiply(new BigDecimal(total)).add(secondBenefit.multiply(new BigDecimal(occurs)));

        // optimistic expectation
        //BigDecimal score = failedBenefit.multiply(new BigDecimal(total)).add( BigDecimal.valueOf(secondBenefit).multiply(new BigDecimal(total)));
        
        /*
        var expected = failedBenefit + divideBigInt(occurs, total, 6) * secondBenefit;

        console.log("Chord " + chord1Tile.asText() + " followed by Chord " + chord2Tile.asText() + ": Chord 1: benefit " + chord1.netBenefit + ", Chord2: H=" + clearable + ", to F=" + needsFlag + ", Chord=" + chordClick
            + ", Benefit=" + secondBenefit + " ==> expected benefit " + expected);
		*/
        
        return score;

    }
	
}
