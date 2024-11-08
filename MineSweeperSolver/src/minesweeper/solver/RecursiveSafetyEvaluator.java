package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.Solver.RunPeResult;
import minesweeper.solver.bulk.StaticCounter;
import minesweeper.solver.bulk.StaticCounter.SCType;
import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.solver.constructs.LocationValue;
import minesweeper.solver.utility.Logger;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class RecursiveSafetyEvaluator implements LocationEvaluator {

	private final static BigDecimal ESS_CONTRIBUTION = new BigDecimal("0.00");
	private final static BigDecimal EQUALITY_THRESHOLD = new BigDecimal("0.0001");
	
	private final static BigDecimal FIFTYFIFTY_SCALE = new BigDecimal("0.9");   // was 0.9
	private final static BigDecimal INCLUDE_THRESHOLD = new BigDecimal("0.9");   
	
	private final static BigDecimal HALF = new BigDecimal("0.5");
	
	private final static Comparator<EvaluatedLocation> SORT_ORDER = EvaluatedLocation.SORT_BY_WEIGHT; 
	
	private final static int[][] OFFSETS = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

	private final static int[][] OFFSETS_ALL = {{2,-2}, {2,-1}, {2,0}, {2,1}, {2,2}, {-2,-2}, {-2,-1}, {-2,0}, {-2,1}, {-2,2}, {-1,2}, {0,2}, {1,2}, {-1,-2}, {0,-2}, {1,-2}};
	
	private final BoardState boardState;
	private final WitnessWeb wholeEdge;
	private final ProbabilityEngineModel pe;
	private final Solver solver;
	private final BruteForceAnalysisModel incompleteBFA;
	//private final FiftyFiftyHelper fiftyFiftyHelper;
	
	private final Set<CandidateLocation> tileOfInterestOff = new HashSet<>();
	private final Set<CandidateLocation> tileOfInterestOn = new LinkedHashSet<>();
	//private final List<Location> tileOfInterestOn = new ArrayList<>();
	
	//private final LongTermRiskHelperOld ltrHelperOld;
	private final SpaceCounter spaceCounter;
	
	private final LongTermRiskHelper ltrHelper;
	
	private final BigDecimal progressContribution;
	private final static BigDecimal essrContribution = new BigDecimal("0.02");
	
	private List<EvaluatedLocation> evaluated = new ArrayList<>();
	private EvaluatedLocation best;
	private boolean certainProgress = false;
	
	// the sequence of guesses that gives the best outcome
	private Location[] bestPath = new Location[20];
	
	// the sequence of guesses that is the current path
	private LocationValue[] currentPath = new LocationValue[20];
	private Map<String, BigDecimal> cache = new HashMap<>();
	
	public RecursiveSafetyEvaluator(Solver solver, BoardState boardState, WitnessWeb wholeEdge, ProbabilityEngineModel pe, BruteForceAnalysisModel incompleteBFA, LongTermRiskHelper ltr) {

		this.boardState = boardState;
		this.wholeEdge = wholeEdge;
		this.pe = pe;
		this.solver = solver;
		this.incompleteBFA = incompleteBFA;
		this.progressContribution = solver.preferences.getProgressContribution();
		
		// find major 50/50 influence on the board - wip
		this.ltrHelper = ltr;
		
		this.spaceCounter = new SpaceCounter(boardState, 8);
		
	}

	/**
	 * Look for off edge positions which are good for breaking open new areas
	 */
	@Override
	public void evaluateOffEdgeCandidates(List<Location> allUnrevealedSquares) {

		tileOfInterestOff.addAll(doEvaluateOffEdgeCandidates(wholeEdge, allUnrevealedSquares));

	}

	private Set<CandidateLocation> doEvaluateOffEdgeCandidates(WitnessWeb web, List<Location> allUnrevealedSquares) {

		Set<CandidateLocation> result = new HashSet<>();
		
		// if there are only a small number of tiles off the edge then consider them all
		if (allUnrevealedSquares.size() - web.getSquares().size() < 30) {
			for (Location tile: allUnrevealedSquares) {
				if (!web.isOnWeb(tile)) {
					result.add(new CandidateLocation(tile.x, tile.y, pe.getOffEdgeProb(), 0, 0));
				}
			}	
			return result;
		}

		int[][] offsets;
		if (boardState.isHighDensity()) {
			offsets = OFFSETS_ALL;
		} else {
			offsets = OFFSETS;
		}
		
		// look for potential super locations
		for (Location tile: web.getOriginalWitnesses()) {

			//boardState.display(tile.display() + " is an original witness");

			for (int[] offset: offsets) {

				int x1 = tile.x + offset[0];
				int y1 = tile.y + offset[1];
				if ( x1 >= 0 && x1 < boardState.getGameWidth() && y1 >= 0 && y1 < boardState.getGameHeight()) {

					CandidateLocation loc = new CandidateLocation(x1, y1, pe.getOffEdgeProb(), boardState.countAdjacentUnrevealed(x1, y1), boardState.countAdjacentConfirmedFlags(x1, y1));
					if (boardState.isUnrevealed(loc) && !web.isOnWeb(loc)) {   // if the location is un-revealed and not on the edge
						//boardState.display(loc.display() + " is of interest");
						result.add(loc);
					}

				}
			}
		}

		// look for potential off edge squares with not many neighbours and calculate their probability of having no more flags around them
		for (Location tile: allUnrevealedSquares) {

			int adjMines = boardState.countAdjacentConfirmedFlags(tile);
			int adjUnrevealed = boardState.countAdjacentUnrevealed(tile);

			if ( adjUnrevealed > 1 && adjUnrevealed < 4 && !web.isOnWeb(tile) && !result.contains(tile)) {
				result.add(new CandidateLocation(tile.x, tile.y, pe.getOffEdgeProb(), boardState.countAdjacentUnrevealed(tile), boardState.countAdjacentConfirmedFlags(tile)));
			}
		}		
		
		return result;
		
	}

	
	@Override
	public void addLocations(Collection<CandidateLocation> tiles) {
		tileOfInterestOn.addAll(tiles);
		
	}
	
	/**
	 * Evaluate a set of tiles to find the best option
	 */
	@Override
	public void evaluateLocations() {

		//TODO need to add in the 50/50 influence somewhere
		/*
		BigDecimal threshold = pe.getBestSafety().multiply(Solver.PROB_ENGINE_HARD_TOLERENCE);
		for (Location loc: ltrHelper.getInfluencedTiles(threshold)) {
			if (!tileOfInterestOff.contains(loc)) {  // if we aren't in the other set then add it to this one
				tileOfInterestOn.add(loc);
			}
		}
		*/
		
		List<CandidateLocation> defered = new ArrayList<>();
		List<CandidateLocation> notDefered = new ArrayList<>();
		
		for (CandidateLocation tile: tileOfInterestOn) {
			if (this.spaceCounter.meetsThreshold(tile)) {
				notDefered.add(tile);
			} else {
				solver.logger.log(Level.INFO, "Tile %s does not meet space threshold, defering.", tile);
				defered.add(tile);
			}
		}

		for (CandidateLocation tile: tileOfInterestOff) {
			if (this.spaceCounter.meetsThreshold(tile)) {
				notDefered.add(tile);
			} else {
				solver.logger.log(Level.INFO, "Tile %s does not meet space threshold, defering.", tile);
				defered.add(tile);
			}
		}
		
		BigDecimal safety ;
		if (!notDefered.isEmpty()) {
			safety = calculateSafety(notDefered, wholeEdge, pe, 0);
		} else {
			safety = calculateSafety(defered, wholeEdge, pe, 0);
		}
		
	}

	/**
	 * Evaluate a tile to see the expected number of clears it will provide
	 */
	private void evaluateLocation(Location tile) {
		
		EvaluatedLocation evalTile = doFullEvaluateTile(tile);

		if (evalTile != null) {
			if (best == null || evalTile.getWeighting().compareTo(best.getWeighting()) > 0) {
				best = evalTile;
			}
			evaluated.add(evalTile);
		}
	}

	private EvaluatedLocation doFullEvaluateTile(Location tile) {
		
		// find how many common tiles 
		SolutionCounter counter1 = solver.validatePosition(wholeEdge, Collections.emptyList(), Arrays.asList(tile), pe.getDeadLocations());

		///int linkedTilesCount = 0;
		
		int linkedTilesCount = counter1.getLivingClearCount();
		
		boolean dominated = false;
		for (Box box: counter1.getEmptyBoxes()) {
			if (box.contains(tile)) {  // if the box contains the tile to be processed then ignore it
				
			} else {
				if (box.getSquares().size() > 1) {
					dominated = true;
					//linkedTilesCount = linkedTilesCount + box.getSquares().size();
				} else {
					//linkedTilesCount++;
				}
			}
		}
		
		solver.logger.log(Level.INFO, "%s has %d linked living tiles and dominated=%b", tile, linkedTilesCount, dominated);
		
		EvaluatedLocation result;
		
		
		if (dominated) {
			BigDecimal probThisTile = pe.getProbability(tile);  // this is both the safety, secondary safety and progress probability.
			
			BigDecimal bonus = BigDecimal.ONE.add(probThisTile.multiply(this.progressContribution));
			BigDecimal weight = probThisTile.multiply(bonus);
			
			BigDecimal expectedClears = BigDecimal.valueOf(counter1.getLivingClearCount());  // this isn't true, but better than nothing?
			
			result = new EvaluatedLocation(tile.x, tile.y, probThisTile , weight, expectedClears, 0, counter1.getEmptyBoxes(), probThisTile);
			
		} else {
			result = doFullEvaluateTile(tile, linkedTilesCount);
		}
		
		return result;
	}
	

	/**
	 * Evaluate this tile and return its EvaluatedLocation
	 */
	private EvaluatedLocation doFullEvaluateTile(Location tile, int linkedTilesCount) {

		long nanoStart = System.nanoTime();

		EvaluatedLocation result = null;

		List<Location> superset = boardState.getAdjacentUnrevealedSquares(tile);
		int minesGot = boardState.countAdjacentConfirmedFlags(tile);

		int minMines = minesGot;
		int maxMines = minesGot + superset.size();

		Box tileBox = pe.getBox(tile);
		
		BigInteger safetyTally;
		
		int tilesOnEdge;
		BigDecimal safetyThisTile;
		if (tileBox == null) {
			safetyThisTile = pe.getOffEdgeProb();
			tilesOnEdge = 1;
			safetyTally = pe.getSolutionCount().subtract(pe.getOffEdgeTally());  //number of solutions this tile is safe
			
		} else {
			safetyThisTile = tileBox.getSafety();
			tilesOnEdge = tileBox.getEdgeLength();
			safetyTally = pe.getSolutionCount().subtract(tileBox.getTally());  //number of solutions this tile is safe
		}
		
		BigDecimal fiftyFiftyInfluence;
		BigDecimal fiftyFiftyContribution;
		if (this.solver.preferences.considerLongTermSafety()) {
			BigInteger tally = ltrHelper.findInfluence(tile);
			BigDecimal bdTally = new BigDecimal(tally);
			
			BigDecimal modifiedTally = bdTally.multiply(FIFTYFIFTY_SCALE);
			fiftyFiftyInfluence = new BigDecimal(safetyTally).add(modifiedTally).divide(new BigDecimal(safetyTally), Solver.DP, RoundingMode.HALF_UP);
			fiftyFiftyContribution = new BigDecimal(tally).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
			
			/*
			try {
				fiftyFiftyInfluence = new BigDecimal(safetyTally).subtract(bdTally).divide(new BigDecimal(pe.getSolutionCount()).subtract(bdTally).subtract(bdTally), Solver.DP, RoundingMode.HALF_UP);
				fiftyFiftyInfluence = fiftyFiftyInfluence.divide(safetyThisTile, Solver.DP, RoundingMode.HALF_UP);
			} catch (Exception e) {
				fiftyFiftyInfluence = BigDecimal.valueOf(10);
			}
			*/
			
		} else {
			fiftyFiftyInfluence = BigDecimal.ONE;
			fiftyFiftyContribution = BigDecimal.ZERO;
		}
		
		// work out the expected number of clears if we clear here to start with (i.e. ourself + any linked clears)
		BigDecimal expectedClears = BigDecimal.ZERO;

		BigDecimal maxValueSafety = BigDecimal.ZERO;
		BigDecimal minValueSafety = BigDecimal.ONE;
		BigDecimal secondarySafety = BigDecimal.ZERO;
		BigDecimal progressProb = BigDecimal.ZERO;
		BigDecimal ess = BigDecimal.ONE.subtract(safetyThisTile); // expect solution space = p(mine) + sum[ P(n)*p(n) ]
		
		BigDecimal safetyThisTileLeft = safetyThisTile;
		
		Location singleSafestTile = null;
		boolean sameSingleSafestTile = true;
		
		BigDecimal create5050Chance = BigDecimal.ZERO;
		
		int validValues = 0;
		List<Box> commonClears = null;
		for (int i = minMines; i <= maxMines; i++) {

			// calculate the weight
			BigDecimal progressBonus = BigDecimal.ONE.add(progressProb.add(safetyThisTileLeft).multiply(this.progressContribution));
			BigDecimal essrBonus = BigDecimal.ONE.add(ESS_CONTRIBUTION);
			//BigDecimal weight = secondarySafety.add(safetyThisTileLeft).multiply(bonus).multiply(fiftyFiftyInfluence);
			BigDecimal weight = secondarySafety.add(safetyThisTileLeft.multiply(fiftyFiftyInfluence)).multiply(progressBonus).multiply(essrBonus);
			
			// if the remaining safe component for the tile can now never reach the best if 100% safe for all future values then abandon analysis
			if (best != null && weight.compareTo(best.getWeighting()) < 0) {
				result = new EvaluatedLocation(tile.x, tile.y, safetyThisTile, weight, expectedClears, 0, commonClears, maxValueSafety);
				result.setPruned();
				return result;
			}
			
			RunPeResult peResult = solver.runProbabilityEngine(wholeEdge, tile, i);
			ProbabilityEngineModel counter = peResult.pe;

			BigInteger sol = counter.getSolutionCount();
			int clears = counter.getLivingClearCount();

			
			// keep track of the maximum probability across all valid values
			if (sol.signum() != 0) {
				
				validValues++;
				
				if (commonClears == null) {
					commonClears = counter.getEmptyBoxes();
				} else {
					commonClears = mergeEmptyBoxes(commonClears, counter.getEmptyBoxes());
				}
				
				BigDecimal prob = new BigDecimal(sol).divide(new BigDecimal(pe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
				
				if (peResult.found5050) {
					create5050Chance = create5050Chance.add(prob);
				}
				
				maxValueSafety = maxValueSafety.max(prob);  // Safest outcome
				minValueSafety = minValueSafety.min(prob);  // least safe outcome
				
				ess = ess.add(prob.multiply(prob));  // sum of p^2

				// expected clears is the sum of the number of mines cleared * the probability of clearing them
				expectedClears = expectedClears.add(BigDecimal.valueOf(clears).multiply(prob));   
				
				
				BigDecimal nextMoveSafety = counter.getBlendedSafety();
				
				//BigDecimal lts = this.ltrHelperOld.getLongTermSafety(tile, counter);
				//BigDecimal lts = BigDecimal.ONE;
				
				solver.logger.log(Level.INFO, "%s with value %d has %d living clears with probability %f, secondary safety %f, 50/50 influence %f and %d tiles on edge", tile, i, clears, prob, nextMoveSafety, fiftyFiftyInfluence, tilesOnEdge);
				
				secondarySafety = secondarySafety.add(prob.multiply(nextMoveSafety).multiply(fiftyFiftyInfluence));
				
				if (clears > linkedTilesCount) {
					progressProb = progressProb.add(prob);
				}
				
                if (counter.getSingleSafestTile() == null) {  // no single safest tile, so they can't always be the same
                    sameSingleSafestTile = false;

                } else if (singleSafestTile == null) {  // the first single safest tile found
                    singleSafestTile = counter.getSingleSafestTile();

                } else if (!singleSafestTile.equals(counter.getSingleSafestTile())) {  // another single safest tile found, but it is different
                    sameSingleSafestTile = false;
                }
				
				// reduce the remaining safe probability
				safetyThisTileLeft = safetyThisTileLeft.subtract(prob);
					
			} else {
				solver.logger.log(Level.DEBUG, "%s with value %d is not valid", tile, i);
			}

		}
		
		if (commonClears != null && !commonClears.isEmpty()) {
			solver.logger.log(Level.DEBUG, "%s has certain progress if survive", tile);
			certainProgress = true;
		}
		
		// no measurable benefit in trying to prevent 50/50s being created
		/*
		BigDecimal create5050ChanceIfSurvive = create5050Chance.divide(safetyThisTile, Solver.DP, RoundingMode.HALF_UP);
		
		solver.logger.log(Level.INFO, "%s has a %f chance of creating a 50/50", tile, create5050ChanceIfSurvive);
		BigDecimal create5050Modifier;
		if (solver.preferences.isTestMode() && create5050ChanceIfSurvive.compareTo(BigDecimal.ONE) == 0) {
			create5050Modifier = BigDecimal.ONE.subtract(HALF.multiply(create5050ChanceIfSurvive));   // 1 - 0.5 * 5050 chance
		} else {
			create5050Modifier = BigDecimal.ONE;
		}
		*/
		
		// expected solution space reduction
		BigDecimal essr = BigDecimal.ONE.subtract(ess);
		BigDecimal essrBonus = BigDecimal.ONE.add(essr.multiply(ESS_CONTRIBUTION));
		
		// calculate the bonus for the progress
		BigDecimal progressBonus = BigDecimal.ONE.add(progressProb.multiply(this.progressContribution));
		
		//bonus = bonus.add(essr.multiply(this.essrContribution));
		
		BigDecimal weight = secondarySafety.multiply(progressBonus).multiply(essrBonus);
		//BigDecimal weight = minValueSafety.multiply(bonus);
		
		result = new EvaluatedLocation(tile.x, tile.y, safetyThisTile, weight, expectedClears, 0, commonClears, maxValueSafety);
		
		// if the tile is dead then relegate it to a deferred guess
		if (validValues == 1) {
			solver.logger.log(Level.INFO, "%s is discovered to be dead during secondary safety analysis, defering.", tile);
			result.setDeferGuessing(true);
		}
		
        if (sameSingleSafestTile) {
        	solver.logger.log(Level.INFO, "Tile %s is always the safest living tile after this guess", singleSafestTile);
        	if (pe.getProbability(singleSafestTile).compareTo(safetyThisTile) > 0) {
        		solver.logger.log(Level.INFO, "Tile %s is also safer, so dominates %s", singleSafestTile, tile);
        		result.setDominatingLocation(singleSafestTile);
        	}
        }
		
		//if (!this.spaceCounter.meetsThreshold(result)) {
		//	solver.logger.log(Level.INFO, "%s does not meet space threshold, defering.", tile);
		//	result.setDeferGuessing(true);
		//}

		long nanoEnd = System.nanoTime();

		solver.logger.log(Level.DEBUG, "Duration %d nano-seconds", (nanoEnd - nanoStart));

		return result;

	}

	
	/**
	 * recursively calculate a tile's safety to the required depth
	 */
	private BigDecimal calculateSafety(List<CandidateLocation> tiles, WitnessWeb currWeb, ProbabilityEngineModel currPe, int depth) {

		Collections.sort(tiles, CandidateLocation.SORT_BY_PROB_FREE_FLAG);
		
		LongTermRiskHelper ltr = new LongTermRiskHelper(boardState, currWeb, currPe);
		
		BigDecimal bestSafety = BigDecimal.ZERO;
		Location bestTile = null;
		
		// recursively get the safety of each tile
		for (Location tile: tiles) {
			
			Box tileBox = currPe.getBox(tile);
			
			BigInteger safetyTally;
			
			BigDecimal safetyThisTile;
			if (tileBox == null) {
				safetyThisTile = currPe.getOffEdgeProb();
				safetyTally = currPe.getSolutionCount().subtract(pe.getOffEdgeTally());  //number of solutions this tile is safe
				
			} else {
				safetyThisTile = tileBox.getSafety();
				safetyTally = currPe.getSolutionCount().subtract(tileBox.getTally());  //number of solutions this tile is safe
			}
			
			if (safetyTally.signum() == 0) {    // this means the tile is a mine? !! 
				continue;  // if this tile is a mine then skip to the next tile
			} 
			
			// consider 50/50 influence
			BigDecimal fiftyFiftyInfluence;
			if (this.solver.preferences.considerLongTermSafety()) {
				
				if (safetyTally.signum() == 0) {    // this means the tile is a mine? !! 
					fiftyFiftyInfluence = BigDecimal.ONE;
				} else {
					BigInteger tally = ltr.findInfluence(tile);
					BigDecimal bdTally = new BigDecimal(tally);
					
					BigDecimal modifiedTally = bdTally.multiply(FIFTYFIFTY_SCALE);
					fiftyFiftyInfluence = new BigDecimal(safetyTally).add(modifiedTally).divide(new BigDecimal(safetyTally), Solver.DP, RoundingMode.HALF_UP);
				}

				
			} else {
				fiftyFiftyInfluence = BigDecimal.ONE;
			}
			
			BigDecimal remainingMaxTileSafety = safetyThisTile;
			
			int minMines = boardState.countAdjacentConfirmedFlags(tile);
			int maxMines = minMines + boardState.countAdjacentUnrevealed(tile);

			// accumulated safety as we work through the tree for this tile
			BigDecimal totalSafety = BigDecimal.ZERO;

			
			int validValues = 0;
			boolean pruned = false;
			
			for (int value = minMines; value <= maxMines; value++) {

				currentPath[depth] = new LocationValue(tile, value);
				
				// Pruning logic, i.e. if all the remaining ties are 100% safe we still can't exceed the current best safety
				if (totalSafety.add(remainingMaxTileSafety.multiply(fiftyFiftyInfluence)).compareTo(bestSafety) <= 0) {
					pruned = true;
					break;
				}
				
				// make the move
				boardState.setWitnessValue(tile, value);

				// create a new list of witnesses
				List<Location> witnesses = new ArrayList<>(currWeb.getPrunedWitnesses().size() + 1);
				witnesses.addAll(currWeb.getPrunedWitnesses());
				witnesses.add(tile);

				Area witnessed = boardState.getUnrevealedArea(witnesses);
				
				WitnessWeb newWeb = new WitnessWeb(boardState, witnesses, witnessed.getLocations(), Logger.NO_LOGGING);

				int unrevealed = boardState.getTotalUnrevealedCount() - 1 - depth;  // this is reduced by the number of additional witnesses we've added

				int minesLeft = boardState.getMines() - boardState.getConfirmedMineCount();

				ProbabilityEngineModel newPe = new ProbabilityEngineFast(boardState, newWeb, unrevealed, minesLeft, Logger.NO_LOGGING);

				newPe.process();

				BigInteger sol = newPe.getSolutionCount();

				// keep track of the maximum probability across all valid values
				if (sol.signum() != 0) {

					validValues++;
					
					BigDecimal valueProb = new BigDecimal(sol).divide(new BigDecimal(currPe.getSolutionCount()), Solver.DP, RoundingMode.HALF_UP);
					
					BigDecimal recursiveSafety = null;

					// is the recursive safety cached?
					boolean inCache = false;
					if (depth == 1) {
						String key = pairToKey(this.currentPath);
						if (cache.containsKey(key)) {
							recursiveSafety = cache.get(key);
							StaticCounter.count(SCType.RECURSIVE_SAFETY_CACHE_HIT);
							inCache = true;
						}
					}					
					
					// if not calculate it
					if (!inCache) {
						int clears = newPe.getLivingClearCount();

						if (depth >= solver.preferences.getRecursiveSafetyDepth() - 1) {  // got to the edge of the recursion
							//recursiveSafety = newPe.getBestLivingSafety();
							recursiveSafety = newPe.getBlendedSafety();
						
						} else if (depth + clears > solver.preferences.getRecursiveSafetyDepth()) {  // got enough clears to reach the end of recursion + the blended tile
							recursiveSafety = BigDecimal.ONE;
							//recursiveSafety = newPe.getBlendedSafety();
						
						} else {
							
							List<CandidateLocation> bestCandidates = newPe.getBestCandidates(INCLUDE_THRESHOLD, true);
							
							// if the best guess is off edge then 
							if (newPe.isBestGuessOffEdge()) {
								bestCandidates.addAll(doEvaluateOffEdgeCandidates(newWeb, boardState.getAllUnrevealedSquares()));
							}
							
							if (bestCandidates.isEmpty()) {
								System.out.println("No Candidates");
							}
							
							recursiveSafety = calculateSafety(bestCandidates, newWeb, newPe, depth + 1);					
							
						}
					}

					// store the result in the cache
					if (depth == 1 && !inCache) {
						String key = pairToKey(this.currentPath);
						cache.put(key, recursiveSafety);
					}
					
					//solver.logger.log(Level.INFO, "%s with value %d has %d living clears with probability %f and secondary safety %f", tile, value, clears, prob, recursiveSafety);

					remainingMaxTileSafety = remainingMaxTileSafety.subtract(valueProb); // this is the maximum safety that can be got from the rest of the recursion
					
					totalSafety = totalSafety.add(valueProb.multiply(recursiveSafety).multiply(fiftyFiftyInfluence));

				} else {
					//solver.logger.log(Level.DEBUG, "%s with value %d is not valid", tile, value);
				}

				// undo the move
				boardState.clearWitness(tile);

			}

			// if this tile better than the current best?
			if (!pruned && totalSafety.compareTo(bestSafety) > 0) {
				bestSafety = totalSafety;
				bestTile = tile;
				this.bestPath[depth] = tile;   // keep track of the best path
				if (depth == 0) {
					solver.logger.log(Level.INFO, "BEST at depth %d: %s with recursive safety %f", depth, bestTile, bestSafety);
				}
				if (depth == 0) {
					solver.logger.log(Level.INFO, "--- Next Tile ----");
				}
					
			} else {
				if (depth == 0) {
					solver.logger.log(Level.INFO, "PRUNED at depth %d: %s with recursive safety %f ", depth, tile, totalSafety);
				}
			}
			
		}

		return bestSafety;

	}
 	
	private String pathToString(Location[] path) {
		
		String result = "";
		
		for (Location l: path) {
			if (l == null) {
				break;
			} else {
				result = result + l;
			}
		} 
		
		return result;
	}
	
	private String pairToKey(LocationValue[] path) {
		
		LocationValue lv1;
		LocationValue lv2;
		
		if (path[0].compareTo(path[1]) > 0) {
			lv1 = path[0];
			lv2 = path[1];
		} else {
			lv1 = path[1];
			lv2 = path[0];
		}
		
		String key = lv1.x + "#" + lv1.y + "#" + lv1.getValue() + "#" + lv2.x + "#" + lv2.y + "#" + lv2.getValue();
		
		return key;
	}
	
	public void showResults() {

		evaluated.sort(SORT_ORDER);

		solver.logger.log(Level.INFO, "--- evaluated locations ---");
		solver.logger.log(Level.INFO, "%s", bestPath);
		
		/*
		for (EvaluatedLocation el: evaluated) {
			solver.logger.log(Level.INFO, "%s", el);
		}
		*/
		
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
			if (eval.getProbability().subtract(move.getProbability()).compareTo(EQUALITY_THRESHOLD) > 0) {  // the alternative move is at least a bit safer than the current move
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
	
	@Override
	public Action[] bestMove() {

		Location best = this.bestPath[0];		
		
		if (best == null) {
			return new Action[0];
		}


		/*
		EvaluatedLocation evalLoc = evaluated.get(0);
		
		// see if this guess has a strictly better guess
		if (solver.preferences.isDoDomination()) {
			EvaluatedLocation alternative = findAlternativeMove(evalLoc);
			
			if (alternative != null) {
				solver.logger.log(Level.INFO, "Replacing %s ...", evalLoc);
				solver.logger.log(Level.INFO, "...  Simple Dominating %s", alternative);
				evalLoc = alternative;
			}			
		}
		
		if (evalLoc.getDominatingLocation() != null) {
			Location better = evalLoc.getDominatingLocation();
			EvaluatedLocation foundBetter = null;
			for (EvaluatedLocation evl: evaluated) {
				if (evl.equals(better)) {
					foundBetter = evl;
					break;
				}
			}
			if (foundBetter == null) {
				solver.logger.log(Level.INFO, "Unable to find %s in the Evaluated list", better);
			} else {
				evalLoc = foundBetter;
				solver.logger.log(Level.INFO, "Complex dominating tile: %s", evalLoc);
			}
			
		}
		
		// check whether the chosen move is dominated by a partially complete BFDA
		if (incompleteBFA != null) {
			Location better = incompleteBFA.checkForBetterMove(evalLoc);
			if (better != null) {
				EvaluatedLocation bfdaBetter = null;
				for (EvaluatedLocation evl: evaluated) {
					if (evl.equals(better)) {
						bfdaBetter = evl;
						break;
					}
				}
				if (bfdaBetter == null) {
					solver.logger.log(Level.INFO, "Unable to find %s in the Evaluated list", better);
				} else {
					evalLoc = bfdaBetter;
					solver.logger.log(Level.INFO, "Better in a partial BFDA: %s", evalLoc);
				}
			}
		}

		*/
		
		Action action = new Action(best, Action.CLEAR, MoveMethod.PROBABILITY_ENGINE, "", pe.getProbability(best));

		// let the boardState decide what to do with this action
		boardState.setAction(action);

		Action[] result = boardState.getActions().toArray(new Action[0]);

		//display("Best Guess: " + action.asString());

		return result;

	}

	@Override
	public List<EvaluatedLocation> getEvaluatedLocations() {
		return evaluated;
	}


}
