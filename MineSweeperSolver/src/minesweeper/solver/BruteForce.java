package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Monitor.AsynchMonitor;
import minesweeper.gamestate.Location;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.ProbabilityLocation;
import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.SuperLocation;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.constructs.ZeroLocation;
import minesweeper.solver.iterator.WitnessWebIterator;

public class BruteForce {

	private final static BigDecimal ZERO_THRESHOLD = new BigDecimal("0.25");
	
	
	private final WitnessWeb web;
	private final Solver solver;
	private final BoardState boardState;
	

	private final int mines;
	private final BigInteger max;

	private CrunchResult crunchResult;

	private boolean hasRun = false;
	private boolean certainClear = false;
	
	private final List<SuperLocation> zeroLocations = new ArrayList<>();
	
	private final List<CandidateLocation> results = new ArrayList<>();
	
	private final String scope;
	
	private BigInteger iterations;
	
	private BruteForceAnalysisModel bruteForceAnalysis;

	public BruteForce(Solver solver, BoardState boardState, WitnessWeb web, int mines, BigInteger max, String scope) {

		this.solver = solver;
		this.boardState = boardState;
		this.max = max;
		this.scope = scope;
		
		this.web = web;		
		
		this.mines = mines;

	}

	public void process() {

		boardState.display("Brute force on " + web.getSquares().size() + " Squares with " + mines + " mines");
		
		// if we have no mines to place then everything must be a clear
		if (mines == 0 ) {
			boardState.display("brute force but already found all the mines - clear all the remaining squares");
			for (Square squ: web.getSquares()) {
				results.add(new CandidateLocation(squ.x, squ.y, BigDecimal.ONE, boardState.countAdjacentUnrevealed(squ), boardState.countAdjacentConfirmedFlags(squ)));
			}
			iterations = BigInteger.ONE;
			hasRun = true;
			return;
		}


		// and crunch the result if we have something to check against
		if (web.getWitnesses().size() >= 0) {

			iterations = web.getIterations(mines);

			if (iterations.compareTo(max) <= 0) {
	
				//display("Brute Force about to process " + iterations + " iterations");
				WitnessWebIterator[] iterators = buildParallelIterators(mines, iterations);

				//if (iterations.compareTo(BigInteger.valueOf(1000000l)) <= 0) {
					//this.bruteForceAnalysis = new BruteForceAnalysis(solver, iterators[0].getLocations(), solver.preferences.BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS);
					this.bruteForceAnalysis = new BruteForceAnalysis(solver, iterators[0].getLocations(), solver.preferences.BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS, scope, solver.bfdaStartLocations());
				//}
				
			
				crunchResult  = crunchParallel(web.getSquares(), web.getWitnesses(), true, iterators);

				// if there are too many to process then don't bother 
				if (this.bruteForceAnalysis != null && this.bruteForceAnalysis.tooMany()) {
					this.bruteForceAnalysis = null;
				}
				
			
				int actIterations = 0;
				for (WitnessWebIterator i: iterators) {
					actIterations = actIterations + i.getIterations();
				}

				boardState.display("Expected iterations = " + iterations + " Actual iterations = " + actIterations);

				boardState.display("Found " + crunchResult.bigGoodCandidates + " candidate solutions in the " + scope);
				
				certainClear = findCertainClear(crunchResult);
				if (certainClear) {
					this.bruteForceAnalysis = null;
				}
				
				hasRun = true;
				
				//TODO zero additional mines calculater - do we want it?
				if (crunchResult.bigGoodCandidates.compareTo(BigInteger.ZERO) != 0) {
					BigInteger hwm = BigInteger.ZERO;
					int best = -1;
					for (int i=0; i < crunchResult.getSquare().size(); i++) {
						
						Location loc = crunchResult.getSquare().get(i);
						
						int adjacentMines = boardState.countAdjacentConfirmedFlags(loc); 
						
						BigDecimal prob = new BigDecimal(crunchResult.bigDistribution[i][adjacentMines]).divide(new BigDecimal(crunchResult.bigGoodCandidates), Solver.DP, RoundingMode.HALF_UP);
						
						if (prob.compareTo(ZERO_THRESHOLD) >= 0) {
							SuperLocation zl = new SuperLocation(loc.x, loc.y, boardState.countAdjacentUnrevealed(loc), adjacentMines);
							zl.setProbability(prob);
							zeroLocations.add(zl);						
						}
						if (crunchResult.bigDistribution[i][adjacentMines].compareTo(hwm) > 0) {
							hwm = crunchResult.bigDistribution[i][adjacentMines];
							best = i;
						}
					}
					
					if (best != -1) {
						BigDecimal prob = new BigDecimal(hwm).divide(new BigDecimal(crunchResult.bigGoodCandidates), Solver.DP, RoundingMode.HALF_UP);
						boardState.display("Location " + crunchResult.getSquare().get(best).display() + " is a 'zero additional mines' with probability " + prob);
					}
				}


			} else {
				boardState.display("Brute Force too large with " + iterations + " iterations");
			}

		} else {                             
			boardState.display("Brute Force not performed since there are no witnesses");
		}

	}

	// break a witness web search into a number of non-overlapping iterators
	private WitnessWebIterator[] buildParallelIterators(int mines, BigInteger totalIterations) {

		boardState.display("Building parallel iterators");

		//WitnessWebIterator[] result1 = new WitnessWebIterator[1];
		//result1[0] = new WitnessWebIterator(web, mines);
		//return result1;

		boardState.display("Non independent iterations = " + web.getNonIndependentIterations(mines));


		// if there is only one cog then we can't lock it,so send back a single iterator
		if (web.getIndependentWitnesses().size() == 1 && web.getIndependentMines() >= mines || totalIterations.compareTo(Solver.PARALLEL_MINIMUM) < 0 || web.getWitnesses().size() == 0) {
			boardState.display("Only a single iterator will be used");
			WitnessWebIterator[] result = new WitnessWebIterator[1];
			result[0] = new WitnessWebIterator(web, mines);
			return result;
		}

		int witMines = web.getIndependentWitnesses().get(0).getMines();
		int squares = web.getIndependentWitnesses().get(0).getSquares().size();

		BigInteger bigIterations = Solver.combination(witMines, squares);

		int iter = bigIterations.intValue();

		boardState.display("The first cog has " + iter + " iterations, so parallel processing is possible");

		WitnessWebIterator[] result = new WitnessWebIterator[iter];

		for (int i=0; i < iter; i++) {
			result[i] = new WitnessWebIterator(web, mines, i);   // create a iterator with a lock first got at position i
		}



		return result;

	}

	// process the iterators in parallel
	private CrunchResult crunchParallel(List<Square> square, List<Witness> witness, boolean calculateDistribution, WitnessWebIterator... iterator) {

		boardState.display("At parallel crunch");

		Cruncher[] crunchers = new Cruncher[iterator.length];

		for (int i=0; i < iterator.length; i++) {
			crunchers[i] = new Cruncher(solver, iterator[i].getLocations(), witness, null, iterator[i], calculateDistribution, bruteForceAnalysis);
		}
		//Cruncher cruncher = new Cruncher(this, square, witness, hooks, iterator, calculateDistribution);

		AsynchMonitor monitor = new AsynchMonitor(crunchers);
		monitor.setMaxThreads(Solver.CORES);
		try {
			monitor.startAndWait();
		} catch (Exception ex) {
			System.out.println("Parallel processing caused an error!");
			ex.printStackTrace();
		}

		CrunchResult[] results = new CrunchResult[crunchers.length];
		for (int i=0; i < crunchers.length; i++) {
			results[i] = crunchers[i].getResult();
		}

		CrunchResult result = CrunchResult.bigMerge(results);

		return result;

	}

	private boolean findCertainClear(CrunchResult output) {


		// if there were no good candidates then there is nothing to check
		if (output.bigGoodCandidates.compareTo(BigInteger.ZERO) == 0) {
			return false;
		}

		// check the tally information to see if we have a square where a
		// mine is never present
		for (int i=0; i < output.bigTally.length; i++) {

			if (output.bigTally[i].compareTo(BigInteger.ZERO) == 0) {
				
				return true;

			}

		}        

		return false;

	}
	
	/*
	// do the tally check using the BigInteger values
	private List<CandidateLocation> checkBigTally(CrunchResult output) {

		List<CandidateLocation> result = new ArrayList<>();

		// if there were no good candidates then there is nothing to check
		if (output.bigGoodCandidates.compareTo(BigInteger.ZERO) == 0) {
			return result;
		}

		// check the tally information to see if we have a square where a
		// mine is always present or never present
		for (int i=0; i < output.bigTally.length; i++) {

			// if there is always a mine here then odds of clear is zero
			if (output.bigTally[i].compareTo(output.bigGoodCandidates) == 0) {

				int x = output.getSquare().get(i).x;
				int y = output.getSquare().get(i).y;
				results.add(new CandidateLocation(x, y, BigDecimal.ZERO, boardState.countAdjacentUnrevealed(x, y), boardState.countAdjacentConfirmedFlags(x, y)));

				// if never a mine then odds of clear is one
			} else if (output.bigTally[i].compareTo(BigInteger.ZERO) == 0) {

				int x = output.getSquare().get(i).x;
				int y = output.getSquare().get(i).y;
				results.add(new CandidateLocation(x, y, BigDecimal.ONE, boardState.countAdjacentUnrevealed(x, y), boardState.countAdjacentConfirmedFlags(x, y)));    
				
				certainClear = true;

			}

		}        

		return result;

	}
	*/
	
	public  List<CandidateLocation> getBestSolutions(BigDecimal freshhold) {

		if (crunchResult == null) {
			return results;
		}
		
		if (!results.isEmpty()) {
			return results;
		}

		List<CandidateLocation> candidates = new ArrayList<>();

		boolean ignoreBad = true;
		if (crunchResult.getMaxCount() <= 1) {
			ignoreBad = false;
			solver.display("No candidates provide additional information");
		}
		
		
		// Calculate the probability of a mine being in the square and store in a list
		for (int i=0; i < crunchResult.bigTally.length; i++) {

			BigDecimal mine = new BigDecimal(crunchResult.bigTally[i]).divide(new BigDecimal(crunchResult.bigGoodCandidates), Solver.DP, RoundingMode.HALF_UP);
			BigDecimal notMine = BigDecimal.ONE.subtract(mine);

			Location l = crunchResult.getSquare().get(i);
			// ignore candidates that yield no info, unless none do or they are certainties
			if (crunchResult.getBigCount()[i]  > 1 || !ignoreBad || notMine.compareTo(BigDecimal.ZERO) == 0 || notMine.compareTo(BigDecimal.ONE) == 0) {
				candidates.add(new CandidateLocation(l.x, l.y, notMine, boardState.countAdjacentUnrevealed(l), boardState.countAdjacentConfirmedFlags(l), crunchResult.getBigCount()[i]));
			} else {
				solver.display(l.display() + " clear probability " + notMine + " discarded because it reveals no further information");
			}
			//candidates.add(new CandidateLocation(l.x, l.y, notMine, boardState.countAdjacentUnrevealed(l), boardState.countAdjacentConfirmedFlags(l)));

		}        

		// sort the candidates into descending order by probability
		Collections.sort(candidates, CandidateLocation.SORT_BY_PROB_FLAG_FREE);

		BigDecimal hwm = candidates.get(0).getProbability();
		
		BigDecimal tolerence;
		if (hwm.compareTo(BigDecimal.ONE) == 0) {
			tolerence = hwm;
		} else {
			tolerence = hwm.multiply(freshhold);
		}

		for (CandidateLocation cl: candidates) {
			if (cl.getProbability().compareTo(tolerence) >= 0) {
				results.add(cl);
			} else {
				break;
			}
		}

		boardState.display("Best Guess: " + candidates.size() + " candidates, " + results.size() + " passed tolerence at " + tolerence);

		return results;
	}

	public boolean hasRun() {
		return this.hasRun;
	}
	
	public boolean hasCertainClear() {
		return this.certainClear;
	}
	
	public CrunchResult getCrunchResult() {
		return this.crunchResult;
	}
	
	public BigInteger getSolutionCount() {
		if (crunchResult == null) {
			return BigInteger.ONE;
		}
		
		return crunchResult.bigGoodCandidates;
	}
	
	public BigInteger getIterations() {
		return this.iterations;
	}
	
	/**
	 * This is a list of locations and the probability that they don't have any undiscovered mines adjacent to them.
	 * The list is restricted to probabilities >= to the constant ZERO_THRESHOLD.
	 */
	public List<SuperLocation> getZeroLocations() {
		return this.zeroLocations;
	}
	
	public BruteForceAnalysisModel getBruteForceAnalysis() {
		return bruteForceAnalysis;
	}
	
	/**
	 * Set the probability for the probabilityLocation being satisfied
	 * @param list
	 */
	public <T extends ProbabilityLocation> List<T> setProbabilities(List<T> list) {
		
		if (!hasRun) {
			return list;
		}
		
		List<T> output = new ArrayList<>();
		
		for (ProbabilityLocation pl: list) {
			for (int i=0; i < crunchResult.getSquare().size(); i++) {
				if (crunchResult.getSquare().get(i).equals(pl)) {
					
					// get the values which are good for this location
					int[] adjFlagsRequired = pl.getAdjacentFlagsRequired();
					
					// count the number of solutions which have those values
					BigInteger count = BigInteger.ZERO;
					for (int j = 0; j < adjFlagsRequired.length; j++) {
						count = count.add(crunchResult.bigDistribution[i][adjFlagsRequired[j]]);
					}
					
					// work out the % chance of it happening or if zero chance discard the location
					if (count.compareTo(BigInteger.ZERO) != 0) {
						BigDecimal prob = new BigDecimal(count).divide(new BigDecimal(crunchResult.bigGoodCandidates), Solver.DP, RoundingMode.HALF_UP);
						pl.setProbability(prob);
						boardState.display(pl.display() + " has probability " + prob);
						output.add((T) pl);
					} else {
						boardState.display(pl.display() + " has probability zero and is being discarded");
					}
					break;
				}
			}
		}
		
		return output;

	}
	
	/**
	 * Returns the probability that this square is not a mine
	 * @param x
	 * @param y
	 * @return
	 */
	public BigDecimal getProbability(int x, int y) {
		
		Location l = new Location(x,y);
		
		if (crunchResult == null) {   // this can happen if there are no mines left to find, so everything is a clear
			for (Location loc: web.getSquares()) {   // if the mouse is hovering over one of the brute forced squares
				if (loc.equals(l)) {
					return BigDecimal.ONE;
				}
			}
			return BigDecimal.ZERO;
		}

		
		for (int i=0; i < crunchResult.getSquare().size(); i++) {
			if (crunchResult.getSquare().get(i).equals(l)) {
				BigDecimal prob = new BigDecimal(crunchResult.bigTally[i]).divide(new BigDecimal(crunchResult.bigGoodCandidates), Solver.DP, RoundingMode.HALF_UP);
				return BigDecimal.ONE.subtract(prob);
			}
		}
		
		return BigDecimal.ZERO;
		
	}
	
}
