package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.solver.constructs.LinkedLocation;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class EvaluateLocations {

	//private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_PROGRESS_PROBABILITY;  // this works well
	private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_WEIGHT;   // trying this
	//private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_FIXED_CLEARS_PROGRESS;      // trying this
	
	private final static int[][] OFFSETS = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

	private final static int[][] OFFSETS_ALL = {{2,-2}, {2,-1}, {2,0}, {2,1}, {2,2}, {-2,-2}, {-2,-1}, {-2,0}, {-2,1}, {-2,2}, {-1,2}, {0,2}, {1,2}, {-1,-2}, {0,-2}, {1,-2}};
	
	private final BoardState boardState;
	private final WitnessWeb wholeEdge;
	private final ProbabilityEngineModel pe;
	private final Solver solver;

	private List<EvaluatedLocation> evaluated = new ArrayList<>();
	EvaluatedLocation best;

	public EvaluateLocations(Solver solver, BoardState boardState, WitnessWeb wholeEdge, ProbabilityEngineModel pe) {

		this.boardState = boardState;
		this.wholeEdge = wholeEdge;
		this.pe = pe;
		this.solver = solver;

	}

	/**
	 * Look for off edge positions which are good for breaking open new areas
	 */
	public void evaluateOffEdgeCandidates(List<Location> allUnrevealedSquares) {

		
		Set<CandidateLocation> tileOfInterest = new HashSet<>();
		
		//int minesLeft = boardState.getMines() - boardState.getConfirmedFlagCount();
		// || allUnrevealedSquares.size() - minesLeft < 6
		
		// if there are only a small number of tiles off the edge then consider them all
		if (allUnrevealedSquares.size() - wholeEdge.getSquares().size() < 30) {
			for (Location tile: allUnrevealedSquares) {
				if (!wholeEdge.isOnWeb(tile)) {
					tileOfInterest.add(new CandidateLocation(tile.x, tile.y, pe.getOffEdgeProb(), 0, 0));
				}
			}	
			evaluateLocations(tileOfInterest);
			return;
		}

		int[][] offsets;
		if (isHighDensity()) {
			offsets = OFFSETS_ALL;
		} else {
			offsets = OFFSETS;
		}
		
		// look for potential super locations
		for (Location tile: wholeEdge.getOriginalWitnesses()) {

			//boardState.display(tile.display() + " is an original witness");

			for (int[] offset: offsets) {

				int x1 = tile.x + offset[0];
				int y1 = tile.y + offset[1];
				if ( x1 >= 0 && x1 < boardState.getGameWidth() && y1 >= 0 && y1 < boardState.getGameHeight()) {

					CandidateLocation loc = new CandidateLocation(x1, y1, pe.getOffEdgeProb(), 0, 0);
					if (boardState.isUnrevealed(loc) && !wholeEdge.isOnWeb(loc)) {   // if the location is un-revealed and not on the edge
						//boardState.display(loc.display() + " is of interest");
						tileOfInterest.add(loc);
					}

				}
			}
		}



		// look for potential off edge squares with not many neighbours and calculate their probability of having no more flags around them
		for (Location tile: allUnrevealedSquares) {

			int adjMines = boardState.countAdjacentConfirmedFlags(tile);
			int adjUnrevealed = boardState.countAdjacentUnrevealed(tile);

			if ( adjUnrevealed > 1 && adjUnrevealed < 4 && !wholeEdge.isOnWeb(tile) && !tileOfInterest.contains(tile)) {

				tileOfInterest.add(new CandidateLocation(tile.x, tile.y, pe.getOffEdgeProb(), 0, 0));
				
			}

		}		

		evaluateLocations(tileOfInterest);
		
	}

	/**
	 * Evaluate a set of tiles to see the expected number of clears it will provide
	 */
	public void evaluateLocations(Collection<? extends CandidateLocation> tiles) {

		for (CandidateLocation tile: tiles) {
			evaluateLocation(tile);
		}

	}

	/**
	 * Evaluate a tile to see the expected number of clears it will provide
	 */
	public void evaluateLocation(CandidateLocation tile) {

		if (best != null) {
			if (tile.getProbability().multiply(Solver.PROGRESS_MULTIPLIER).compareTo(best.getWeighting()) <= 0) {
				boardState.display(tile.display() + " is ignored because it can not do better than the best");
				return;
			}
		}

		//EvaluatedLocation evalTile = doEvaluateTile(tile);
		EvaluatedLocation evalTile = doFullEvaluateTile(tile);


		if (evalTile != null) {
			if (best == null || evalTile.getWeighting().compareTo(best.getWeighting()) > 0) {
				best = evalTile;
			}
			evaluated.add(evalTile);
		}
	}

	/**
	 * Evaluate this tile and return its EvaluatedLocation
	 */
	private EvaluatedLocation doEvaluateTile(Location tile) {

		//long nanoStart = System.nanoTime();
		//boardState.display(tile.display() + " is of interest as a superset");

		EvaluatedLocation result = null;

		List<Location> superset = boardState.getAdjacentUnrevealedSquares(tile);
		int minesGot = boardState.countAdjacentConfirmedFlags(tile);

		//boardState.display("----");

		int minMines = minesGot;
		int hits = 0;

		for (Location loc: boardState.getAdjacentSquaresIterable(tile, 2)) {

			if (boardState.isRevealed(loc) && boardState.countAdjacentUnrevealed(loc) != 0) {   // if the location is revealed then see if we are a super set of it

				boolean supersetOkay = true;
				//boolean subSetIncludesMe = false; // does the subset contain the Tile we are considering
				for (Location adj: boardState.getAdjacentSquaresIterable(loc)) {
					if (boardState.isUnrevealed(adj)) {
						boolean found = false;

						if (adj.equals(tile)) {  // if the subset contains me that's okay
							found = true;
							//subSetIncludesMe = true;

						} else {   // otherwise check the superset
							for (Location test: superset) {
								if (adj.equals(test)) {
									found = true;
									break;
								}
							}							
						}
						if (!found) {
							supersetOkay = false;
							break;
						}							
					}
				}
				if (supersetOkay) {
					int minesNeeded = boardState.getWitnessValue(loc) - boardState.countAdjacentConfirmedFlags(loc);
					int value = minesNeeded + minesGot;
					//boardState.display(tile.display() + " is a superset of " + loc.display() + " value " + value);
					hits++;
					if (minMines < value) {
						minMines = value;
					}
				}

			}
		}		

		// if we aren't a superset square then just see what the chances that this square is already fully satisfied.
		if (hits == 0) {
			boardState.display(tile.display() + " is not a superset");
			hits = 1;
		} else {
			boardState.display(tile.display() + " is a superset " + hits + " times");
		}

		int maxMines = Math.min(minMines + hits - 1, minesGot + superset.size());
		
		BigDecimal probThisTile = pe.getProbability(tile);
		LinkedLocation linkedLocation = pe.getLinkedLocation(tile);
		int linkedTiles;
		if (linkedLocation != null) {
			linkedTiles = linkedLocation.getLinksCount();
		} else {
			linkedTiles = 0;
		}


		// work out the expected number of clears if we clear here to start with (i.e. ourself + any linked clears)
		//BigDecimal expectedClears = BigDecimal.valueOf(1 + linkedTiles).multiply(probThisTile); 
		//BigDecimal expectedClears = BigDecimal.ZERO; 
		BigDecimal expectedClears = probThisTile; 

		//boardState.display(tile.display() + " has " + linkedTiles + " linked tiles");

		BigDecimal progressProb = BigDecimal.ZERO;

		boolean found = false;
		
		for (int i = minMines; i < maxMines + 1; i++) {
			//int clears = solver.validateLocationUsingLocalCheck(tile, i);
			//if (clears > 0) {

				SolutionCounter counter = solver.validateLocationUsingSolutionCounter(wholeEdge, tile, i, pe.getDeadLocations());
				BigInteger sol = counter.getSolutionCount();
				int clears = counter.getClearCount();

				if (sol.signum() != 0 && clears > linkedTiles) {
				//if (sol.signum() != 0) {
					
					found = true;
					BigDecimal prob = new BigDecimal(sol).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					boardState.display(tile.display() + " with value " + i + " has " + clears + " clears with probability " + prob.toPlainString());

					// expected clears is the sum of the number of mines cleared * the probability of clearing them
					expectedClears = expectedClears.add(BigDecimal.valueOf(clears - linkedTiles).multiply(prob));    

					progressProb = progressProb.add(prob);
				} else {
					if (sol.signum() == 0) {
						boardState.display(tile.display() + " with value " + i + " with probability zero");
						if (!found && i == maxMines && maxMines != 8) {  // if we haven't found a possible match yet keep going
							maxMines++;
						}
					} else {
						found = true;
						boardState.display(tile.display() + " with value " + i + " only has linked clears");
					}
					
				}

			//} else {
			//	boardState.display(tile.display() + " with value " + i + " fails local check");
			//}
		}

		//if (linkedTiles > 0) {
		//	progressProb = probThisTile;
		//}



		//if (expectedClears.compareTo(BigDecimal.ZERO) > 0) {
			result = new EvaluatedLocation(tile.x, tile.y, probThisTile, progressProb, expectedClears, linkedTiles, null, BigDecimal.ZERO);
			
			if (linkedLocation != null) {
				boardState.display("Considering with " + linkedLocation.getLinkedLocations().size() + " linked locations");
				top: for (Location link: linkedLocation.getLinkedLocations()) {
					boardState.display("Linked with " + link.display());
					for (EvaluatedLocation e: evaluated) {
						if (e.equals(link)) {
							boardState.display("Found link in evaluated" + link.display());
							e.merge(result);
							result = null;
							break top;
						}
					}
				}			
			}
		//}

		//long nanoEnd = System.nanoTime();

		//boardState.display("Duration = " + (nanoEnd - nanoStart) + " nano-seconds");

		return result;

	}

	/**
	 * Evaluate this tile and return its EvaluatedLocation
	 */
	private EvaluatedLocation doFullEvaluateTile(Location tile) {

		long nanoStart = System.nanoTime();
		//boardState.display(tile.display() + " is of interest as a superset");

		EvaluatedLocation result = null;

		List<Location> superset = boardState.getAdjacentUnrevealedSquares(tile);
		int minesGot = boardState.countAdjacentConfirmedFlags(tile);

		//boardState.display("----");

		int minMines = minesGot;
		int maxMines = minesGot + superset.size();

		BigDecimal probThisTile = pe.getProbability(tile);

		// work out the expected number of clears if we clear here to start with (i.e. ourself + any linked clears)
		//BigDecimal expectedClears = BigDecimal.valueOf(1 + linkedTiles).multiply(probThisTile); 
		//BigDecimal expectedClears = BigDecimal.ZERO; 
		BigDecimal expectedClears = probThisTile; 

		BigDecimal maxValueProgress = BigDecimal.ZERO;
		BigDecimal progressProb = BigDecimal.ZERO;

		Area deadLocations = pe.getDeadLocations();
		
		List<Box> commonClears = null;
		for (int i = minMines; i <= maxMines; i++) {

			SolutionCounter counter = solver.validateLocationUsingSolutionCounter(wholeEdge, tile, i, deadLocations);

			BigInteger sol = counter.getSolutionCount();
			int clears = counter.getLivingClearCount();

			// keep track of the maximum probability across all valid values
			if (sol.signum() != 0) {
				
				if (commonClears == null) {
					commonClears = counter.getEmptyBoxes();
				} else {
					commonClears = mergeEmptyBoxes(commonClears, counter.getEmptyBoxes());
				}
				
				BigDecimal prob = new BigDecimal(sol).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
				
				maxValueProgress = maxValueProgress.max(prob);
				
				boardState.display(tile.display() + " with value " + i + " has " + clears + " living clears with probability " + prob.toPlainString());

				// expected clears is the sum of the number of mines cleared * the probability of clearing them
				expectedClears = expectedClears.add(BigDecimal.valueOf(clears).multiply(prob));   

				if (clears != 0) {
					progressProb = progressProb.add(prob);
				}
					
			} else {
				boardState.display(tile.display() + " with value " + i + " with probability zero");
			}

		}

		result = new EvaluatedLocation(tile.x, tile.y, probThisTile, progressProb, expectedClears, 0, commonClears, maxValueProgress);

		long nanoEnd = System.nanoTime();

		boardState.display("Duration = " + (nanoEnd - nanoStart) + " nano-seconds");

		return result;

	}

	public void showResults() {

		evaluated.sort(SORT_ORDER);

		boardState.display("--- evaluated locations ---");
		for (EvaluatedLocation el: evaluated) {
			boardState.display(el.display());
		}

	}

	private List<Box> mergeEmptyBoxes(List<Box> boxes1, List<Box> boxes2) {
		
		if (boxes1.size() == 0) {
			return boxes1;
		}
		
		if (boxes2.size() == 0) {
			return boxes2;
		}
		
		List<Box> result = new ArrayList<>();
		for (Box b1: boxes1) {
			for (Box b2: boxes2) {
				if (b1.equals(b2)) {
					result.add(b1);
					break;
				}
			}
		}
		
		return result;
	}
	
	
	// find a move which 1) is safer than the move given and 2) when move is safe ==> the alternative is safe
	private EvaluatedLocation findAlternativeMove(EvaluatedLocation move) {
		
		if (move.getEmptyBoxes() == null) {
			return null;
		}
		
		// if one of the common boxes contains a tile which has already been processed then the current tile is redundant
		for (EvaluatedLocation eval: evaluated) {
			if (eval.getProbability().subtract(move.getProbability()).compareTo(BigDecimal.valueOf(0.001d)) > 0) {  // the alternative move is at least a bit safer than the current move
				for (Box b: move.getEmptyBoxes()) {  // see if the move is in the list of empty boxes
					for (Location l: b.getSquares()) {
						if (l.equals(eval)) {
							return eval;
						}
					}
				}
			}
		}

		return null;
		
	}
	
	
	private boolean isCorner(Location tile) {
		if ((tile.x == 0 || tile.x == boardState.getGameWidth() - 1) && (tile.y == 0 || tile.y == boardState.getGameHeight() - 1)) {
			return true;
		} else {
			return false;
		}
	}
	
	public Action[] bestMove() {

		if (evaluated.isEmpty()) {
			return new Action[0];
		}

		// for high density board guess safety and then minimax probability of tile value
		if (isHighDensity()) {
			boardState.display("High density evaluation being used");
			evaluated.sort(EvaluatedLocation.SORT_BY_SAFETY_MINIMAX);
		} else {
			// other wise weigh safety and progress
			evaluated.sort(SORT_ORDER);
		}
		//evaluated.sort(SORT_ORDER);


		EvaluatedLocation evalLoc = evaluated.get(0);
		
		// see if this guess has a strictly better guess
		if (solver.preferences.isDoDomination()) {
			EvaluatedLocation alternative = findAlternativeMove(evalLoc);
			
			if (alternative != null) {
				boardState.display("Replacing " + evalLoc.display() + " with " + alternative.display());
				evalLoc = alternative;
			}			
		}


		Action action = new Action(evalLoc, Action.CLEAR, MoveMethod.PROBABILITY_ENGINE, "", evalLoc.getProbability());

		// let the boardState decide what to do with this action
		boardState.setAction(action);

		Action[] result = boardState.getActions().toArray(new Action[0]);

		//display("Best Guess: " + action.asString());

		return result;

	}

	public List<EvaluatedLocation> getEvaluatedLocations() {
		return evaluated;
	}
	
	private boolean isHighDensity() {
		
		int minesLeft = boardState.getMines() - boardState.getConfirmedFlagCount();
		int tilesLeft = boardState.getTotalUnrevealedCount();
		
		return (minesLeft * 5 > tilesLeft * 2) && Solver.CONSIDER_HIGH_DENSITY_STRATEGY;
	}

}
