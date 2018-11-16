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
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.solver.constructs.LinkedLocation;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class EvaluateLocations {

	private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_PROGRESS_PROBABILITY;  // this works well
	//private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_LINKED_EXPECTED_CLEARS;   // trying this
	//private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_FIXED_CLEARS_PROGRESS;      // trying this
	
	
	private final static int[][] OFFSETS = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

	
	private final BoardState boardState;
	private final WitnessWeb wholeEdge;
	private final ProbabilityEngine pe;
	private final Solver solver;

	private List<EvaluatedLocation> evaluated = new ArrayList<>();

	public EvaluateLocations(Solver solver, BoardState boardState, WitnessWeb wholeEdge, ProbabilityEngine pe) {

		this.boardState = boardState;
		this.wholeEdge = wholeEdge;
		this.pe = pe;
		this.solver = solver;

	}

	/**
	 * Look for off edge positions which are good for breaking open new areas
	 */
	public void addOffEgdeCandidates(List<Location> allUnrevealedSquares) {

		Set<Location> tileOfInterest = new HashSet<>();

		// look for potential super locations
		for (Location tile: wholeEdge.getOriginalWitnesses()) {

			//boardState.display(tile.display() + " is an original witness");

			for (int[] offset: OFFSETS) {

				int x1 = tile.x + offset[0];
				int y1 = tile.y + offset[1];
				if ( x1 >= 0 && x1 < boardState.getGameWidth() && y1 >= 0 && y1 < boardState.getGameHeight()) {

					Location loc = new Location(x1, y1);
					if (boardState.isUnrevealed(loc) && !wholeEdge.isOnWeb(loc)) {   // if the location is un-revealed and not on the edge
						//boardState.display(loc.display() + " is of interest");
						tileOfInterest.add(loc);
					}

				}
			}
		}

		evaluateLocations(tileOfInterest);

		// look for potential off edge squares with not many neighbours and calculate their probability of having no more flags around them
		for (Location tile: allUnrevealedSquares) {

			int adjMines = boardState.countAdjacentConfirmedFlags(tile);
			int adjUnrevealed = boardState.countAdjacentUnrevealed(tile);

			if ( adjUnrevealed > 1 && adjUnrevealed < 4 && !wholeEdge.isOnWeb(tile) && !tileOfInterest.contains(tile)) {

				//SolutionCounter counter = solver.validateLocationUsingSolutionCounter(tile, adjMines, wholeEdge.getOriginalWitnesses());
				SolutionCounter counter = solver.validateLocationUsingSolutionCounter(tile, adjMines);

				BigInteger sol = counter.getSolutionCount();

				if (sol.signum() != 0) {

					BigDecimal probThisTile = pe.getProbability(tile);   // probability of this tile being clear
					BigDecimal prob = new BigDecimal(sol).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP); // probability of this tile's value being 'adjMines'

    				boardState.display("Off edge good guess: " + tile.display() + " with value " + adjMines + " has " + counter.getClearCount() + " clears with probability " + prob.toPlainString());

    				BigDecimal expectedClears = BigDecimal.valueOf(counter.getClearCount()).multiply(prob); // expect tiles cleared if we play here

    				//BigDecimal progressProb = prob.divide(probThisTile, Solver.DP, RoundingMode.HALF_UP); // probability given we survive


    				
    				EvaluatedLocation evalTile = new EvaluatedLocation(tile.x, tile.y, probThisTile, prob, expectedClears, 0, isCorner(tile));

    				evaluated.add(evalTile);

				} else {
					boardState.display(tile.display() + " with value " + adjMines + " has probability zero");
				}


			}

		}		


	}

	/**
	 * Evaluate a set of tiles to see the expected number of clears it will provide
	 */
	public void evaluateLocations(Collection<? extends Location> tiles) {

		for (Location tile: tiles) {
			evaluateLocation(tile);
		}

	}

	/**
	 * Evaluate a tile to see the expected number of clears it will provide
	 */
	public void evaluateLocation(Location tile) {

		EvaluatedLocation evalTile = doEvaluateTile(tile);
		//EvaluatedLocation evalTile = doFullEvaluateTile(tile);

		if (evalTile != null) {
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

		for (int i = minMines; i < maxMines + 1; i++) {
			//int clears = solver.validateLocationUsingLocalCheck(tile, i);
			int clears = 1;
			if (clears > 0) {

				SolutionCounter counter = solver.validateLocationUsingSolutionCounter(tile, i);
				BigInteger sol = counter.getSolutionCount();
				clears = counter.getClearCount();

				if (sol.signum() != 0 && clears > linkedTiles) {
				//if (sol.signum() != 0) {

					BigDecimal prob = new BigDecimal(sol).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					boardState.display(tile.display() + " with value " + i + " has " + clears + " clears with probability " + prob.toPlainString());

					// expected clears is the sum of the number of mines cleared * the probability of clearing them
					expectedClears = expectedClears.add(BigDecimal.valueOf(clears - linkedTiles).multiply(prob));    

					progressProb = progressProb.add(prob);
				} else {
					if (sol.signum() == 0) {
						boardState.display(tile.display() + " with value " + i + " with probability zero");
						if (i == minMines && i == maxMines) {  // if we are only checking one value and it has no chance then try one more
							maxMines++;
						}
					} else {
						boardState.display(tile.display() + " with value " + i + " only has linked clears");
					}
					
				}

			} else {
				boardState.display(tile.display() + " with value " + i + " fails local check");
			}
		}

		if (linkedTiles > 0) {
			progressProb = probThisTile;
		}



		//if (expectedClears.compareTo(BigDecimal.ZERO) > 0) {
			result = new EvaluatedLocation(tile.x, tile.y, probThisTile, progressProb, expectedClears, linkedTiles, isCorner(tile));
			
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

		for (int i = minMines; i <= maxMines; i++) {

			SolutionCounter counter = solver.validateLocationUsingSolutionCounter(tile, i);

			BigInteger sol = counter.getSolutionCount();
			int clears = counter.getClearCount();

			if (sol.signum() != 0 && clears > linkedTiles) {

				BigDecimal prob = new BigDecimal(sol).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
				boardState.display(tile.display() + " with value " + i + " has " + clears + " clears with probability " + prob.toPlainString());

				// expected clears is the sum of the number of mines cleared * the probability of clearing them
				expectedClears = expectedClears.add(BigDecimal.valueOf(clears - linkedTiles).multiply(prob));   

				if (clears != 0) {
					progressProb = progressProb.add(prob);
				}

			} else {
				if (sol.signum() == 0) {
					boardState.display(tile.display() + " with value " + i + " with probability zero");
				} else {
					boardState.display(tile.display() + " with value " + i + " only has linked clears");
				}
			}

		}

		if (linkedTiles > 0) {
			progressProb = probThisTile;
		}

		//if (expectedClears.compareTo(BigDecimal.ZERO) > 0) {
		result = new EvaluatedLocation(tile.x, tile.y, probThisTile, progressProb, expectedClears, linkedTiles, isCorner(tile));
		//}


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

		evaluated.sort(SORT_ORDER);

		EvaluatedLocation evalLoc = evaluated.get(0);

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
	

}
