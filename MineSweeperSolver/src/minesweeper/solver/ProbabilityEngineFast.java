package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.LinkedLocation;
import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.utility.Logger;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

/**
 * This class uses a non iterative approach to calculating probabilities for each {@link minesweeper.solver.constructs.Box Box}. It is driven by a {@link minesweeper.solver.WitnessWeb witness web}.
 * 
 * @author David
 *
 */
public class ProbabilityEngineFast extends ProbabilityEngineModel {

	private int[][] SMALL_COMBINATIONS = new int[][] {{1}, {1,1}, {1,2,1}, {1,3,3,1}, {1,4,6,4,1}, {1,5,10,10,5,1}, {1,6,15,20,15,6,1}, {1,7,21,35,35,21,7,1}, {1,8,28,56,70,56,28,8,1}};
	
	private static final boolean CHECK_FOR_DEAD_LOCATIONS = true;
	
	private class MergeSorter implements Comparator<ProbabilityLine> {

		int[] checks;
		
		private MergeSorter() {
			checks = new int[0];
		}
		
		
		private MergeSorter(List<Box> boxes) {
			
			checks = new int[boxes.size()];
			
			for (int i=0; i < boxes.size(); i++) {
				checks[i] = boxes.get(i).getUID();
			}
			
		}
		 
		
		@Override
		public int compare(ProbabilityLine p1, ProbabilityLine p2) {
			
			int c = p1.mineCount - p2.mineCount;
			
			if (c != 0) {
				return c;
			}
			
			for (int i=0; i < checks.length; i++) {
				int index = checks[i];
				
				//BigInteger c1 = p1.mineBoxCount[index].divide(p1.solutionCount);
				//BigInteger c2 = p2.mineBoxCount[index].divide(p2.solutionCount);
				//
				//c = c1.compareTo(c2);
				
				c = p1.allocatedMines[index] - p2.allocatedMines[index];
				
				if (c != 0) {
					return c;
				}
				
			}

			return 0;
		}
		
	}
	
	// used to hold a viable solution 
	private class ProbabilityLine implements Comparable<ProbabilityLine> {
		private int mineCount = 0;
		private BigInteger solutionCount = BigInteger.ZERO;
		private BigInteger[] mineBoxCount  = new BigInteger[boxCount];
		
		private int[] allocatedMines  = new int[boxCount];   // this is the number of mines originally allocate to a box
		
		private BigInteger[] hashCount  = new BigInteger[boxCount];
		private BigInteger hash = new BigInteger(30, new Random());
		
		{
			for (int i=0; i < mineBoxCount.length; i++) {
				mineBoxCount[i] = BigInteger.ZERO;
				hashCount[i] = BigInteger.ZERO;
			}
		}
		
		private ProbabilityLine() {
			this(BigInteger.ZERO);
		}
		
		private ProbabilityLine(BigInteger solutionCount) {
			this.solutionCount = solutionCount;
		}
		
		
		@Override
		// sort by the number of mines in the solution
		public int compareTo(ProbabilityLine o) {
			return this.mineCount - o.mineCount;
		}
	}
	
	// used to hold what we need to analyse next
	private class NextWitness {
		
		private Witness witness;
		private List<Box> newBoxes = new ArrayList<>();
		private List<Box> oldBoxes = new ArrayList<>();
		
		private NextWitness(Witness w) {
			
			this.witness = w;
			
			for (Box b: w.getBoxes()) {
				if (b.isProcessed()) {
					oldBoxes.add(b);
				} else {
					newBoxes.add(b);
				}
			}

		}
	
	}
	
	// information about the boxes surrounding a dead candidate
	private class DeadCandidate {
		
		private Location candidate;
		private Box myBox;
		private boolean isAlive = false;
		private List<Box> goodBoxes = new ArrayList<>();
		private List<Box> badBoxes = new ArrayList<>();
		
		private boolean firstCheck = true;
		private int total;
	
	}
	
	private long duration;
	
	private List<ProbabilityLine> workingProbs = new ArrayList<>(); // as we work through an independent set of witnesses probabilities are held here
 	private List<ProbabilityLine> heldProbs = new ArrayList<>();  
	
	//private BigDecimal[] boxProb;
	private BigInteger[] hashTally;
	private boolean offEdgeBest = true;
	private BigDecimal offEdgeSafety;
	private BigInteger offEdgeTally;
	private BigDecimal bestProbability;
	//private BigDecimal cutoffProbability;

	//when set to true indicates that the box has been part of this analysis
	private boolean[] mask;           
	
	private List<LinkedLocation> linkedLocations = new ArrayList<>();
	private List<LinkedLocation> contraLinkedLocations = new ArrayList<>();
	//private List<Location> dominatedTiles = new ArrayList<>();
	private List<Location> mines = new ArrayList<>();  // certain mines we have found 
	
	// list of locations which are potentially dead
	private List<DeadCandidate> deadCandidates = new ArrayList<>();
	
	// Edges which can be processed independently converted to Cruncher class, ready to be processed
	private List<BruteForce> isolatedEdges = new ArrayList<>();
	
	final private BoardState boardState;
	private final Logger logger;
	final private WitnessWeb web;
	final private int boxCount;
	final private List<Witness> witnesses;
	final private List<Box> boxes;
	final private int minesLeft;                 // number of mines undiscovered in the game
	final private int squaresLeft;               // number of squares undiscovered in the game and off the web
	private Area deadLocations;
	private boolean allDead = true;
	
	private int independentGroups = 0;
	private int recursions = 0;
	private boolean canDoDeadTileAnalysis;
	
	private BigInteger finalSolutionsCount;
	private int clearCount;
	private int livingClearCount;
	final private List<Box> emptyBoxes = new ArrayList<>();
	
	// these are the limits that can be on the edge
	final private int minTotalMines;
	final private int maxTotalMines;
	
	//final private Set<Integer> mineCounts = new HashSet<>();
	final private Map<Integer, BigInteger> mineCounts = new HashMap<>();
	
	public ProbabilityEngineFast(BoardState boardState, WitnessWeb web, int squaresLeft, int minesLeft) {
		this(boardState, web, squaresLeft, minesLeft, boardState.getLogger());
		
	}
	public ProbabilityEngineFast(BoardState boardState, WitnessWeb web, int squaresLeft, int minesLeft, Logger logger) {
		
		this.boardState = boardState;
		this.logger = logger;
		this.web = web;
		this.minesLeft = minesLeft;
		this.squaresLeft = squaresLeft - web.getSquares().size();
		this.deadLocations = Area.EMPTY_AREA;
		
		this.minTotalMines = Math.max(0, minesLeft - this.squaresLeft);  //we can't use so few mines that we can't fit the remainder elsewhere on the board
		this.maxTotalMines = minesLeft;    // we can't use more mines than are left in the game
		
		//solver.display("Total mines " + minTotalMines + " to " + maxTotalMines);
		
		web.generateBoxes();
		
		this.witnesses = web.getPrunedWitnesses();
		this.boxes = web.getBoxes();
		
		this.boxCount = boxes.size();
		
		//this.boxProb = new BigDecimal[boxCount];
		this.hashTally = new BigInteger[boxCount];
		
		for (Witness w: witnesses) {
			w.setProcessed(false);
		}
		
		for (Box b: boxes) {
			b.setProcessed(false);
		}
		
	}

	// run the probability engine
	public void process() {
		
		long startTime = System.currentTimeMillis();
		
		if (CHECK_FOR_DEAD_LOCATIONS) {
			determineCandidateDeadLocations();			
		}

		// if we compress the probability lines before the edge is completely processed we can't use the data to look for dead tiles
		canDoDeadTileAnalysis = true;
		
		// create an initial solution of no mines anywhere
		heldProbs.add(new ProbabilityLine(BigInteger.ONE));
		
		// add an empty probability line to get us started
		workingProbs.add(new ProbabilityLine(BigInteger.ONE));
		
		// create an empty mask - indicating no boxes have been processed
		mask = new boolean[boxCount];           
		
		NextWitness witness = findFirstWitness();
		
		while (witness != null) {
			
			// mark the new boxes as processed - which they will be soon
			for (Box b: witness.newBoxes) {
				mask[b.getUID()] = true;
			}
			
			//System.out.println("Processing " + witness.witness.getLocation().display());
			
			workingProbs = mergeProbabilities(witness);
			
			witness = findNextWitness(witness);
			
		}
		
		
		calculateBoxProbabilities();
		
		/*
		for (Box b: boxes) {
			solver.display("Box " + b.getUID() + " has probability " + boxProb[b.getUID()]);
		}
		*/
		
		duration = System.currentTimeMillis() - startTime;
	}
	
	private List<ProbabilityLine> crunchByMineCount(List<ProbabilityLine> target, MergeSorter sorter) {
		
		if (target.isEmpty()) {
			return target;
		}
		
		// sort the solutions by number of mines
		Collections.sort(target, sorter);
		
		List<ProbabilityLine> result = new ArrayList<>();
		
		ProbabilityLine current = null;
		
		
		for (ProbabilityLine pl: target) {

			/*
			String show = pl.mineCount + " : " + pl.solutionCount + " : ";
			for (int i=0; i < pl.mineBoxCount.length; i++) {
				show = show + pl.mineBoxCount[i] + " ";
			}
			boardState.display(show);
			*/
			
			if (current == null) {
				current = pl;
			} else if (sorter.compare(current, pl) != 0) {
				result.add(current);
				current = pl;
			} else {
				//boardState.display("Combining");
				combineProbabilities(current, pl);
			}
			
		}

		result.add(current);

		logger.log(Level.DEBUG, "%d Probability Lines compressed to %d", target.size(), result.size()); 
			
		return result;
		
	}

	
	// calculate how many ways this solution can be generated and roll them into one
	private void combineProbabilities(ProbabilityLine npl, ProbabilityLine pl) {
		
		/*
		BigInteger solutions = BigInteger.ONE;
		for (int i = 0; i < pl.mineBoxCount.length; i++) {
			solutions = solutions.multiply(BigInteger.valueOf(SMALL_COMBINATIONS[boxes.get(i).getSquares().size()][pl.mineBoxCount[i].intValue()]));
		}

		npl.solutionCount = npl.solutionCount.add(solutions);
		*/
		npl.solutionCount = npl.solutionCount.add(pl.solutionCount);
		npl.hash = npl.hash.add(pl.hash);
		
		for (int i = 0; i < pl.mineBoxCount.length; i++) {
			if (mask[i]) {  // if this box has been involved in this solution - if we don't do this the hash gets corrupted by boxes = 0 mines because they weren't part of this edge
	 			//npl.mineBoxCount[i] = npl.mineBoxCount[i].add(pl.mineBoxCount[i].multiply(solutions));
	 			npl.mineBoxCount[i] = npl.mineBoxCount[i].add(pl.mineBoxCount[i]);
	 			npl.hashCount[i] = npl.hashCount[i].add(pl.hashCount[i]);
	 			
				//if (pl.mineBoxCount[i].signum() == 0) {
				//	npl.hashCount[i] = npl.hashCount[i].subtract(pl.hash);   // treat no mines as -1 rather than zero
				//} else {
				//	npl.hashCount[i] = npl.hashCount[i].add(pl.mineBoxCount[i].multiply(pl.hash));
				//}				
			}
		}
		
		
		
	}
	
	// this combines newly generated probabilities with ones we have already stored from other independent sets of witnesses
	private void storeProbabilities() {
		
		List<ProbabilityLine> result = new ArrayList<>(); 
		
		//if (workingProbs.isEmpty()) {
		//	solver.display("working probabilites list is empty!!");
		//	return;
		//} 
		
		//if (CHECK_FOR_DEAD_LOCATIONS) {
		//	checkCandidateDeadLocations();			
		//}
	
		// crunch the new ones down to one line per mine count
		//List<ProbabilityLine> crunched = crunchByMineCount(workingProbs);

		List<ProbabilityLine> crunched = workingProbs;
		
		if (crunched.size() == 1) { // if the size is one then the number of mines in the area is fixed
			checkEdgeIsIsolated(true);
		} else {
			/*
			boolean deferedGuessing = true;
			for (ProbabilityLine pl: crunched) {
				if (!pl.solutionCount.equals(BigInteger.ONE)) {
					deferedGuessing = false;
					break;
				}
			}
			if (deferedGuessing && checkEdgeIsIsolated(false)) {
				logger.log(Level.INFO, "Seed %s Defered guess found", boardState.getSolver().getGame().getSeed());
				for (int i=0; i < mask.length; i++) {
					if (mask[i]) {
						this.boxes.get(i).setDeferGuessing();
					}
				}
			}
			*/
		}

		
		//solver.display("New data has " + crunched.size() + " entries");
		
		for (ProbabilityLine pl: crunched) {
			
			for (ProbabilityLine epl: heldProbs) {

				if (pl.mineCount + epl.mineCount <= maxTotalMines) {
					
					ProbabilityLine npl = new ProbabilityLine(pl.solutionCount.multiply(epl.solutionCount));
					npl.mineCount = pl.mineCount + epl.mineCount;
					npl.hash = epl.hash.add(pl.hash);
					
					//npl.solutionCount = pl.solutionCount.multiply(epl.solutionCount);
					
					for (int i=0; i < npl.mineBoxCount.length; i++) {
						
						BigInteger w1 = pl.mineBoxCount[i].multiply(epl.solutionCount);
						BigInteger w2 = epl.mineBoxCount[i].multiply(pl.solutionCount);
						npl.mineBoxCount[i] = w1.add(w2);
						
						npl.hashCount[i] = epl.hashCount[i].add(pl.hashCount[i]);

					}
					result.add(npl);
					
				}
				
			}
			
		}
	
		// sort into mine order 
		Collections.sort(result);
		
		heldProbs.clear();
		
		// if result is empty this is an impossible position
		if (result.isEmpty()) {
			return;
		}
		
		// and combine them into a single probability line for each mine count
		int mc = result.get(0).mineCount;
		ProbabilityLine npl = new ProbabilityLine();
		npl.mineCount = mc;
		
		for (ProbabilityLine pl: result) {
			if (pl.mineCount != mc) {
				heldProbs.add(npl);
				mc = pl.mineCount;
				npl = new ProbabilityLine();
				npl.mineCount = mc;
			}
			npl.solutionCount = npl.solutionCount.add(pl.solutionCount);
			npl.hash = npl.hash.add(pl.hash);
			
			for (int i = 0; i < pl.mineBoxCount.length; i++) {
				npl.mineBoxCount[i] = npl.mineBoxCount[i].add(pl.mineBoxCount[i]);
				
				npl.hashCount[i] = npl.hashCount[i].add(pl.hashCount[i]);
			}
		}

		heldProbs.add(npl);

		/*
		for (Box b: boxes) {
			System.out.print(b.getSquares().size() + " ");
		}
		System.out.println("");
		for (ProbabilityLine pl: heldProbs) {
			System.out.print("Mines = " + pl.mineCount + " solutions = " + pl.solutionCount + " boxes: ");
			for (int i=0; i < pl.mineBoxCount.length; i++) {
				System.out.print(" " + pl.mineBoxCount[i]);
			}
			System.out.println("");
		}
		*/
		
		
	}
	
	// here we expand the localised solution to one across the whole board and
	// sum them together to create a definitive probability for each box
	private void calculateBoxProbabilities() {
		
		//BigInteger[] tally = new BigInteger[boxCount];
		for (int i=0; i < hashTally.length; i++) {
			//tally[i] = BigInteger.ZERO;
			hashTally[i] =  BigInteger.ZERO;
		}

		// total game tally
		BigInteger totalTally = BigInteger.ZERO;
		
		// outside a box tally
		BigInteger outsideTally = BigInteger.ZERO;
		
		// calculate how many mines 
		for (ProbabilityLine pl: heldProbs) {
			
			if (pl.mineCount >= minTotalMines) {    // if the mine count for this solution is less than the minimum it can't be valid
				
				if (mineCounts.put(pl.mineCount, pl.solutionCount) != null) {
					logger.log(Level.ERROR, "Duplicate mines in probability Engine (merging probability lines not working?)");
				}
				
				BigInteger mult = Solver.combination(minesLeft - pl.mineCount, squaresLeft);  //# of ways the rest of the board can be formed
				
				outsideTally = outsideTally.add(mult.multiply(BigInteger.valueOf(minesLeft - pl.mineCount)).multiply(pl.solutionCount));
				
				// this is all the possible ways the mines can be placed across the whole game
				totalTally = totalTally.add(mult.multiply(pl.solutionCount));
				
				for (Box b: this.boxes) {
					BigInteger contribution = mult.multiply(pl.mineBoxCount[b.getUID()]).divide(BigInteger.valueOf(b.getSquares().size()));
					
					// the 50/50 component doesn't count
					//if (pl.solutionCount.compareTo(BigInteger.valueOf(2)) == 0 && pl.mineBoxCount[b.getUID()].compareTo(BigInteger.ONE) == 0 && b.getSquares().size() == 1) {  
					//	contribution = BigInteger.ZERO;
					//}
					
					BigInteger tally = b.getTally().add(contribution);
					b.setTally(tally);

				}
				
				for (int i=0; i < hashTally.length; i++) {
					//tally[i] = tally[i].add(mult.multiply(pl.mineBoxCount[i]).divide(BigInteger.valueOf(boxes.get(i).getSquares().size())));
					hashTally[i] = hashTally[i].add(pl.hashCount[i]);
				}				
			}

		}		
		
		logger.log(Level.INFO, "Total Candidate solutions found %d", totalTally);
		
		for (Box b: this.boxes) {
			if (totalTally.signum() != 0) {
				if (b.getTally().compareTo(totalTally) == 0) {  // a mine
					b.setSafety(BigDecimal.ZERO);
					for (Square squ: b.getSquares()) {  // add the squares in the box to the list of mines
						mines.add(squ);
						deadLocations = deadLocations.remove(squ);  // a definite mine can't be dead
					}					
				} else if (b.getTally().signum() == 0) {  // safe
					b.setSafety(BigDecimal.ONE);
					allDead = false;
					//for (Square squ: b.getSquares()) {
					//	deadLocations = deadLocations.remove(squ);  // a safe tile can't be dead
					//}					
				} else {
					b.setSafety(BigDecimal.ONE.subtract(new BigDecimal(b.getTally()).divide(new BigDecimal(totalTally), Solver.DP, RoundingMode.HALF_UP)));
				}
				
			} else {
				b.setSafety(BigDecimal.ZERO);
			}
		}
	
		// avoid divide by zero
		if (squaresLeft != 0 && totalTally.signum() != 0) {
			offEdgeTally = outsideTally.divide(BigInteger.valueOf(squaresLeft));
			offEdgeSafety = BigDecimal.ONE.subtract(new BigDecimal(outsideTally).divide(new BigDecimal(totalTally), Solver.DP, RoundingMode.HALF_UP).divide(new BigDecimal(squaresLeft), Solver.DP, RoundingMode.HALF_UP));
		} else {
			offEdgeSafety = BigDecimal.ZERO;
			offEdgeTally = BigInteger.ZERO;
		}
	
		finalSolutionsCount = totalTally;

		// determine how many clear squares there are
		if (totalTally.signum() > 0) {
			
			for (Box b: this.boxes) {
				if (b.getTally().signum() == 0) {
					 clearCount = clearCount + b.getSquares().size();
					 
					 for (Square sq: b.getSquares()) {
						 if (!deadLocations.contains(sq)) {
							 livingClearCount++;
						 }
					 }
					 
					 if (b.getSquares().size() > 0) {
						 emptyBoxes.add(b);
					 }
				}
			}				
			
		}
		
		// see if we can find a guess which is better than outside the boxes
		BigDecimal hwm = offEdgeSafety;
		
		offEdgeBest = true;
		
		for (Box b: boxes) {
			boolean living = false;
			for (Square squ: b.getSquares()) {
				if (!deadLocations.contains(squ)) {
					living = true;
					break;
				}
			}
			//BigDecimal prob = boxProb[b.getUID()];
			BigDecimal prob = b.getSafety();
			
			if (living && prob.signum() != 0) {  // if living and not a mine
	 			allDead = false;
			}
			
			if (living || prob.compareTo(BigDecimal.ONE) == 0) {   // if living or 100% safe then consider this probability
				
				if (hwm.compareTo(prob) <= 0) {
					offEdgeBest = false;
					hwm = prob;
				}				
			}
		}

		bestProbability = hwm;
		
		//solver.display("probability off web is " + outsideProb);
		
		
	}
	
	
	private void addLinkedLocation(List<LinkedLocation> list, Box box, Box linkTo) {
		
		top:for (Square s: box.getSquares()) {
			
			for (LinkedLocation ll: list) {
				if (s.equals(ll)) {
					ll.incrementLinks(linkTo.getSquares());
					continue top;
				}
			}		
			
			list.add(new LinkedLocation(s.x, s.y, linkTo.getSquares()));
		}
		
	}
	
	private List<ProbabilityLine> mergeProbabilities(NextWitness nw) {
		
		List<ProbabilityLine> newProbs = new ArrayList<>();
		
		for (ProbabilityLine pl: workingProbs) {
			
			int missingMines = nw.witness.getMines() - countPlacedMines(pl, nw);
			
			if (missingMines < 0) {
				// too many mines placed around this witness previously, so this probability can't be valid
			} else if (missingMines == 0) {
				newProbs.add(pl);   // witness already exactly satisfied, so nothing to do
			} else if (nw.newBoxes.isEmpty()) {
				// nowhere to put the new mines, so this probability can't be valid
			} else {
				newProbs.addAll(distributeMissingMines(pl, nw, missingMines, 0));
			}
	
		}
		
		// flag the last set of details as processed
		nw.witness.setProcessed(true);
		for (Box b: nw.newBoxes) {
			b.setProcessed(true);
		}
		
		//boardState.display("Processed witness " + nw.witness.display());
		
		//if we haven't compressed yet and we are still a small edge then don't compress
		if (newProbs.size() < 100 && canDoDeadTileAnalysis) {
			return newProbs;
		}
		
		// about to compress the line
		canDoDeadTileAnalysis = false;
		
		List<Box> boundaryBoxes = new ArrayList<>();
		for (Box box: boxes) {
			boolean notProcessed = false;
			boolean processed = false;
			for (Witness wit: box.getWitnesses()) {
				if (wit.isProcessed()) {
					processed = true;
				} else {
					notProcessed = true;
				}
				if (processed && notProcessed) {
					//boardState.display("partially processed box " + box.getUID());
					boundaryBoxes.add(box);
					break;
				}
			}
		}
		//boardState.display("Boxes partially processed " + boundaryBoxes.size());
		
		MergeSorter sorter = new MergeSorter(boundaryBoxes);
		
		newProbs = crunchByMineCount(newProbs, sorter);

		return newProbs;
		
	}
	
	// this is used to recursively place the missing Mines into the available boxes for the probability line
	private List<ProbabilityLine> distributeMissingMines(ProbabilityLine pl, NextWitness nw, int missingMines, int index) {
		
		recursions++;
		if (recursions % 10000 == 0) {
			logger.log(Level.WARN, "Probability Engine recursion exceeding %d iterations", recursions);
		}
		
		List<ProbabilityLine> result = new ArrayList<>();
		
		// if there is only one box left to put the missing mines we have reach this end of this branch of recursion
		if (nw.newBoxes.size() - index == 1) {
			// if there are too many for this box then the probability can't be valid
			if (nw.newBoxes.get(index).getMaxMines() < missingMines) {
				return result;
			}
			// if there are too few for this box then the probability can't be valid
			if (nw.newBoxes.get(index).getMinMines() > missingMines) {
				return result;
			}
			// if there are too many for this game then the probability can't be valid
			if (pl.mineCount + missingMines > maxTotalMines) {
				return result;
			}			
			
			// otherwise place the mines in the probability line
			
			//pl.mineBoxCount[nw.newBoxes.get(index).getUID()] = BigInteger.valueOf(missingMines).multiply(pl.solutionCount);
			//pl.mineCount = pl.mineCount + missingMines;
			//result.add(pl);
			
			result.add(extendProbabilityLine(pl, nw.newBoxes.get(index), missingMines, true));
			return result;
		}
		
		
		// this is the recursion
		int maxToPlace = Math.min(nw.newBoxes.get(index).getMaxMines(), missingMines);
		
		for (int i=nw.newBoxes.get(index).getMinMines(); i <= maxToPlace; i++) {
			ProbabilityLine npl = extendProbabilityLine(pl, nw.newBoxes.get(index), i, false);
			
			result.addAll(distributeMissingMines(npl, nw, missingMines - i, index + 1));
		}
		
		return result;
		
	}
	
	// create a new probability line by taking the old and adding the mines to the new Box
	private ProbabilityLine extendProbabilityLine(ProbabilityLine pl, Box newBox, int mines, boolean reuseLine) {
		
		int combination = SMALL_COMBINATIONS[newBox.getSquares().size()][mines];
		
		ProbabilityLine result;
		if (combination == 1 && reuseLine) {
			result = pl;
			result.mineCount = result.mineCount + mines;
		} else {
			BigInteger newSolutionCount = pl.solutionCount.multiply(BigInteger.valueOf(combination));
			
			result = new ProbabilityLine(newSolutionCount);
			
			result.mineCount = pl.mineCount + mines;
			result.hash = pl.hash;
			
			// copy the hash values
			System.arraycopy(pl.hashCount, 0, result.hashCount, 0, pl.mineBoxCount.length);
			
			// copy the probability array
			if (combination == 1) {
				System.arraycopy(pl.mineBoxCount, 0, result.mineBoxCount, 0, pl.mineBoxCount.length);
			} else {
				BigInteger multiplier = BigInteger.valueOf(combination);
				for (int i=0; i < pl.mineBoxCount.length; i++) {
					result.mineBoxCount[i] = pl.mineBoxCount[i].multiply(multiplier);
				}
			}
			
			result.allocatedMines = pl.allocatedMines.clone();
		}


		result.mineBoxCount[newBox.getUID()] = BigInteger.valueOf(mines).multiply(result.solutionCount);
		result.allocatedMines[newBox.getUID()] = mines;
		
		if (mines == 0) {
			result.hashCount[newBox.getUID()] = result.hash.negate();   // treat no mines as -1 rather than zero
		} else {
			result.hashCount[newBox.getUID()] = BigInteger.valueOf(mines).multiply(result.hash);
		}				
		
		return result;
	}
	
	/*
	// counts the number of mines already placed
	private int countPlacedMines(ProbabilityLine pl, NextWitness nw) {
		
		int result = 0;
		
		for (Box b: nw.oldBoxes) {
			result = result + pl.mineBoxCount[b.getUID()].intValue();
		}
		
		return result;
	}
	*/
	
	// counts the number of mines already placed
	private int countPlacedMines(ProbabilityLine pl, NextWitness nw) {
		
		BigInteger result = BigInteger.ZERO;
		
		for (Box b: nw.oldBoxes) {
			result = result.add(pl.mineBoxCount[b.getUID()]);
		}
		
		BigInteger[] divide = result.divideAndRemainder(pl.solutionCount);
		if (divide[1].signum() != 0) {
			logger.log(Level.WARN, "Min Box Count divide has non-zero remainder &d", divide[1]);
		}
		
		
		return divide[0].intValue();
	}
	
	// return any witness which hasn't been processed
	private NextWitness findFirstWitness() {
		
		for (Witness w: witnesses) {
			if (!w.isProcessed()) {
				return new NextWitness(w);
			}
		}
		
		// if we are here all witness have been processed
		return null;

	}
	
	
	// look for the next witness to process
	private NextWitness findNextWitness(NextWitness prevWitness) {
		
		int bestTodo = 99999;
		Witness bestWitness = null;
		
		// and find a witness which is on the boundary of what has already been processed
		for (Box b: boxes) {
			if (b.isProcessed()) {
				for (Witness w: b.getWitnesses()) {
					if (!w.isProcessed()) {
						int todo = 0;
						for (Box b1: w.getBoxes()) {
							if (!b1.isProcessed()) {
								todo++;
							}
						}
						if (todo == 0) {
							return new NextWitness(w);
						} else if (todo < bestTodo) {
							bestTodo = todo;
							bestWitness = w;
						}
					}
				}
			}
		}
		
		if (bestWitness != null) {
			return new NextWitness(bestWitness);
		}
		
		// if we are down here then there is no witness which is on the boundary, so we have processed a complete set of independent witnesses 
		
		independentGroups++;
		
		// since we have calculated all the mines in an independent set of witnesses we can crunch them down and store them for later
		
		// before we crunch everything down check for dead tiles
		if (CHECK_FOR_DEAD_LOCATIONS) {
			checkCandidateDeadLocations(canDoDeadTileAnalysis);
			//checkEdgeIsDead();
		}

		// if we haven't compressed yet then do it now
		if (canDoDeadTileAnalysis) {
			MergeSorter sorter = new MergeSorter();
			workingProbs = crunchByMineCount(workingProbs, sorter);
		} else {
			canDoDeadTileAnalysis = true;
		}

		/*
		// display the probability lines
		for (ProbabilityLine pl: workingProbs) {
			
			String display = "Mines=" + pl.mineCount + " Weight=" + pl.solutionCount;
			for (int i=0; i < pl.allocatedMines.length; i++) {
				//display = display + " "  + boxes.get(i).getSquares().size() + "(" + pl.allocatedMines[i] + ") ";
				
				String show;
				if (this.mask[i]) {
					show = pl.mineBoxCount[i].toString();
				} else {
					show = "-";
				}
				
				display = display + " "  + boxes.get(i).getSquares().size() + "(" + show + ") ";
				
			}

			boardState.getLogger().log(Level.INFO, display);

		}		
		*/
		
		// get an unprocessed witness
		NextWitness nw =  findFirstWitness();
		
		storeProbabilities();
		
		// reset the working array so we can start building up one for the new set of witnesses
		workingProbs.clear();
		workingProbs.add(new ProbabilityLine(BigInteger.ONE));
		
		
		// count how many tiles where on this edge
		int totalTiles = 0;
		for (int i=0; i < mask.length; i++) {
			if (mask[i]) {
				Box b = this.boxes.get(i);
				totalTiles = totalTiles + b.getSquares().size();
			}
		}
		// and store that information in the box
		for (int i=0; i < mask.length; i++) {
			if (mask[i]) {
				Box b = this.boxes.get(i);
				b.setEdgeLength(totalTiles);
			}
		}		
		
		// reset the mask indicating that no boxes have been processed 
		mask = new boolean[boxCount]; 

		// return the next witness to process
		return nw;
		
	}
	
	public BigDecimal getProbability(Location l) {
		
		for (Box b: boxes) {
			if (b.contains(l)) {
				//return boxProb[b.getUID()];
				return b.getSafety();
			}
		}
		
		return offEdgeSafety;
	}
	
	/**
	 * The probability of a mine being in a square not considered by this process
	 */
	protected BigDecimal getOffEdgeProb() {
		return offEdgeSafety;
	}
	
	/**
	 * The number of solutions where an off edge tile is a mine
	 */
	protected BigInteger getOffEdgeTally() {
		return offEdgeTally;
	}
	
	/**
	 * The probability of the safest witnessed tile
	 * @return
	 */
	protected BigDecimal getBestOnEdgeProb() {
		return bestProbability;
	}
	
	
	protected boolean isBestGuessOffEdge() {
		return this.offEdgeBest;
	}
	
	/**
	 * true if a 100% certain move has been found
	 * @return
	 */
	protected boolean foundCertainty() {
		return (bestProbability.compareTo(BigDecimal.ONE) == 0);
	}
	
	
	@Override
	protected List<CandidateLocation> getBestCandidates(BigDecimal freshhold, boolean excludeDead) {
		
		List<CandidateLocation> best = new ArrayList<>();
		
		//solver.display("Squares left " + this.squaresLeft + " squares analysed " + web.getSquares().size());
		
		// if the outside probability is the best then return an empty list
		BigDecimal test;
		//if (offEdgeBest) {
		//	solver.display("Best probability is off the edge " + bestProbability + " but will look for options on the edge only slightly worse");
		//	//test = bestProbability.multiply(Solver.EDGE_TOLERENCE);
		//	test = bestProbability.multiply(freshhold);
		//} else 
		
		if (bestProbability.compareTo(BigDecimal.ONE) == 0){  // if we have a probability of one then don't allow lesser probs to get a look in
			test = bestProbability;
		} else {
			test = bestProbability.multiply(freshhold);
		}

		logger.log(Level.INFO, "Best probability is %f, cutoff freshhold is %f", bestProbability, test);
		
		/*
		for (int i=0; i < boxProb.length; i++) {
			if (boxProb[i].compareTo(test) >= 0 ) {
				for (Square squ: boxes.get(i).getSquares()) {
					boolean isDead = deadLocations.contains(squ);
					if (!isDead || !excludeDead || boxProb[i].compareTo(BigDecimal.ONE) == 0) {  // if not a dead location or 100% safe then use it
						best.add(new CandidateLocation(squ.x, squ.y, boxProb[i], boardState.countAdjacentUnrevealed(squ), boardState.countAdjacentConfirmedFlags(squ), isDead, this.boxes.get(i).doDeferGuessing()));
					} else {
						logger.log(Level.INFO, "Candidate Location %s is ignored because it is dead", squ);
					}
				}
			}
		}
		*/
		
		for (Box b: this.boxes) {
			if (b.getSafety().compareTo(test) >= 0 ) {
				for (Square squ: b.getSquares()) {
					boolean isDead = deadLocations.contains(squ);
					if (!isDead || !excludeDead || b.getSafety().compareTo(BigDecimal.ONE) == 0) {  // if not a dead location or 100% safe then use it
						best.add(new CandidateLocation(squ.x, squ.y, b.getSafety(), boardState.countAdjacentUnrevealed(squ), boardState.countAdjacentConfirmedFlags(squ), isDead, b.doDeferGuessing()));
					} else {
						logger.log(Level.INFO, "Candidate Location %s is ignored because it is dead", squ);
					}
				}
			}
		}
		
		// sort in to best order
		best.sort(CandidateLocation.SORT_BY_PROB_FLAG_FREE);
		
		return best;
		
	}
	
	@Override
	protected BigDecimal getBestNotDeadSafety() {
		
		// see if we can do better than off edge
		BigDecimal safest = this.offEdgeSafety;
		
		for (Box b: this.boxes) {
			if (b.getSafety().compareTo(safest) > 0 ) {
				for (Square squ: b.getSquares()) {
					boolean isDead = deadLocations.contains(squ);
					if (!isDead) {  // if not a dead location then use it
						safest = b.getSafety();
					} else {
						//logger.log(Level.INFO, "Candidate Location %s is ignored because it is dead", squ);
					}
				}
			}
		}
		
	
		return safest;
		
	}
	
	/**
	 * Return locations which are exactly 50% chance of being a mine
	 */
	@Override
	protected List<Location> getFiftyPercenters() {
		
		List<Location> picks = new ArrayList<>();
		
		BigDecimal test = new BigDecimal("0.5");
		
		for (Box b: this.boxes) {
			if (b.getSafety().compareTo(test) == 0 ) {
				for (Square squ: b.getSquares()) {
					picks.add(squ);
				}
			}
		}
		
		return picks;		
		
	}
	
	@Override
	/**
	 * Return locations which are within the threshold of being a mine
	 */
	protected List<CandidateLocation> getProbableMines(BigDecimal freshhold) {
		
		List<CandidateLocation> best = new ArrayList<>();
		
		// if the outside probability is the best then return an empty list
		BigDecimal test = BigDecimal.ONE.subtract(freshhold);
		
		/*
		for (int i=0; i < boxProb.length; i++) {
			if (boxProb[i].signum() != 0 && boxProb[i].compareTo(test) <= 0 ) {
				for (Square squ: boxes.get(i).getSquares()) {
					boolean isDead = deadLocations.contains(squ);
					best.add(new CandidateLocation(squ.x, squ.y, boxProb[i], boardState.countAdjacentUnrevealed(squ), boardState.countAdjacentConfirmedFlags(squ), isDead, this.boxes.get(i).doDeferGuessing()));
				}
			}
		}
		*/
		
		for (Box b: this.boxes) {
			if (b.getSafety().signum() != 0 && b.getSafety().compareTo(test) <= 0 ) {
				for (Square squ: b.getSquares()) {
					boolean isDead = deadLocations.contains(squ);
					best.add(new CandidateLocation(squ.x, squ.y, b.getSafety(), boardState.countAdjacentUnrevealed(squ), boardState.countAdjacentConfirmedFlags(squ), isDead, b.doDeferGuessing()));
				}
			}
		}		
		
		// sort in to best order
		best.sort(CandidateLocation.SORT_BY_PROB_FLAG_FREE);

		logger.log(Level.INFO, "There are %d tiles with a chance of being a mine >= %f", best.size(), freshhold);
		
		return best;
		
	}
	
	
	private void checkCandidateDeadLocations(boolean checkPossible) {
		
		boolean completeScan;
		if (squaresLeft == 0) {
			completeScan = true;   // this indicates that every box has been considered in one sweep (only 1 independent edge)
			for (int i=0; i < mask.length; i++) {
				if (!mask[i]) {
					completeScan = false;
					break;
				}
			}
			if (completeScan) {
				logger.log(Level.DEBUG, "This is a complete scan");
			} else {
				logger.log(Level.DEBUG, "This is not a complete scan");
			}			
		} else {
			completeScan = false;
			logger.log(Level.DEBUG, "This is not a complete scan because there are squares off the edge");
		}

		
		for (DeadCandidate dc: deadCandidates) {
			
			if (dc.isAlive) {  // if this location isn't dead then no need to check any more
				continue;
			}
			
			// only do the check if all the boxes have been analysed in this probability iteration
			int boxesInScope = 0;
			for (Box b: dc.goodBoxes) {
				if (mask[b.getUID()]) {
					boxesInScope++;
				}
			}
			for (Box b: dc.badBoxes) {
				if (mask[b.getUID()]) {
					boxesInScope++;
				}
			}
			if (boxesInScope == 0) {
				continue;
			} else if (boxesInScope != dc.goodBoxes.size() + dc.badBoxes.size()) {
				logger.log(Level.DEBUG, "Tile %s has some boxes in scope and some out of scope so assumed alive", dc.candidate);
				dc.isAlive = true;
				continue;
			}
			
			//if we can't do the check because the edge has been compressed mid process then assume alive
			if (!checkPossible) {
				logger.log(Level.DEBUG, "Tile %s was on compressed edge so assumed alive", dc.candidate);
				dc.isAlive = true;
				continue;
			}
			
			boolean okay = true;
			int mineCount = 0;
			line: for (ProbabilityLine pl: workingProbs) {
				
				// ignore probability lines where the candidate is a mine
				//if (pl.mineBoxCount[dc.myBox.getUID()].compareTo(BigInteger.valueOf(dc.myBox.getSquares().size())) == 0) {
				if (pl.allocatedMines[dc.myBox.getUID()] == dc.myBox.getSquares().size()) {
					logger.log(Level.DEBUG, "Location %s is a mine on this Probability line %d", dc.candidate, pl.allocatedMines[dc.myBox.getUID()]);
					mineCount++;
					continue line;
				} else {
					logger.log(Level.DEBUG, "Location %s is not a mine on this Probability line %d", dc.candidate, pl.allocatedMines[dc.myBox.getUID()]);
				}
				
				if (completeScan && pl.mineCount != minesLeft) {
					continue;
				}				
				
				// all the bad boxes must be zero
				for (Box b: dc.badBoxes) {
					
					BigInteger requiredMines;
                    //int requiredMines;
                    if (b.getUID() == dc.myBox.getUID()) {
                        requiredMines = BigInteger.valueOf(b.getSquares().size() - 1).multiply(pl.solutionCount);
                    } else {
                        requiredMines = BigInteger.valueOf(b.getSquares().size()).multiply(pl.solutionCount);
                    }
					
					if (pl.mineBoxCount[b.getUID()].signum() != 0 && pl.mineBoxCount[b.getUID()].compareTo(requiredMines) != 0) {
						logger.log(Level.DEBUG, "Tile %s is not dead because a bad box is neither empty nor full of mines", dc.candidate);
						okay = false;
						break line;
					}
				}
				
				//BigInteger tally = BigInteger.ZERO;
				int tally = 0;
				// the number of mines in the good boxes must always be the same
				for (Box b: dc.goodBoxes) {
					//tally = tally.add(pl.mineBoxCount[b.getUID()]);
					tally = tally + pl.allocatedMines[b.getUID()];
				}
				//boardState.display("Location " + dc.candidate.display() + " has mine tally " + tally);
				if (dc.firstCheck) {
					dc.total = tally;
					dc.firstCheck = false;
				} else {
					if (dc.total != tally) {
						logger.log(Level.DEBUG, "Tile %s is not dead because the sum of mines in good boxes is not constant. Was %d now %d. Mines in probability line %d", dc.candidate, dc.total, tally, pl.mineCount);
						okay = false;
						break;
					}
				}
			}
			
			// if a check failed or this tile is a mine for every solution then it is alive
			if (!okay || mineCount == this.workingProbs.size()) {
				dc.isAlive = true;
			} else {
				// add the dead locations we found 
				deadLocations = deadLocations.add(dc.candidate);
				//logger.log(Level.INFO, "%s is dead", dc.candidate);
			}
			
		}
		
	}

	/*
	// an edge is dead if every tile on the edge is dead
	private boolean checkEdgeIsDead() {
		
		// For each box on this edge check each of the tiles in it, if any are alive then the edge is alive
        for (int i = 0; i < this.mask.length; i++) {
            if (this.mask[i]) {   
            	for (Square tile: boxes.get(i).getSquares()) {
            		if (!deadLocations.contains(tile)) {
            			return false;
            		}
            	}
            }
        }

        boardState.display("The following area is dead - all guesses in the area are equal");
        for (int i = 0; i < this.mask.length; i++) {
            if (this.mask[i]) {   
            	for (Square tile: boxes.get(i).getSquares()) {
            		boardState.display("Location " + tile.display());
            		boardState.addIsolatedDeadTile(tile);
            	}
            }
        }        
		
		return true;
	}
	*/
	
	// an edge is isolated if every tile on it is completely surrounded by boxes also on the same edge 
	// (we have already established this area has a fixed number of mines)
	private boolean checkEdgeIsIsolated(boolean equalMines) {
		
		Set<Location> edgeTiles = new HashSet<>();
		Set<Location> edgeWitnesses = new HashSet<>();
		
		boolean everything = true;
		
		// load each tile on this edge into a set
        for (int i = 0; i < this.mask.length; i++) {
            if (this.mask[i]) {  
            	edgeTiles.addAll(boxes.get(i).getSquares());
            	edgeWitnesses.addAll(boxes.get(i).getWitnesses());
             } else {
            	everything = false;
            }
        }

        // if this edge is everything then it isn't an isolated edge  - this is wrong because it doesn't allow for off edge tiles
        //if (everything) {
        //	logger.log(Level.DEBUG, "Not enclosed because the edge is everything");
        //	return false;
        //}
        
        
		// check whether every tile adjacent to the tiles on the edge is itself on the edge
        for (int i = 0; i < this.mask.length; i++) {
            if (this.mask[i]) {   
            	for (Square tile: boxes.get(i).getSquares()) {
            		if (!edgeTiles.containsAll(boardState.getAdjacentUnrevealedSquares(tile))) {
            			logger.log(Level.DEBUG, "Not enclosed because a tile's adjacent tiles isn't on the edge: %s", tile);
            			return false;
            		}
            	}
            }
        }

        if (equalMines) {
        	logger.log(Level.INFO, "Enclosed Edge with equal mines found");
        } else {
        	logger.log(Level.INFO, "Enclosed Edge with unequal mines found");
        	return true;
        }

        // an enclosed edge with a known number of mines can be solved independently from the main board
        
        List<Location> tiles = new ArrayList<>(edgeTiles);
        List<Location> witnesses = new ArrayList<>(edgeWitnesses);
        int mines = workingProbs.get(0).mineCount;
        
        // build a web of the isolated edge and use it to build a brute force
        WitnessWeb isolatedEdge = new WitnessWeb(boardState, witnesses, tiles);
        BruteForce bruteForce = new BruteForce(boardState.getSolver(), boardState, isolatedEdge, mines, boardState.getSolver().preferences.getBruteForceMaxIterations(), 
        		boardState.getSolver().preferences.getBruteForceMaxSolutions(), "Isolated Edge");
        
        isolatedEdges.add(bruteForce);
        
		return true;
	}
	
	private void determineCandidateDeadLocations() {
		
		// for each square on the edge
		for (Square loc: web.getSquares()) {
			
			List<Box> boxes = getAdjacentBoxes(loc);
			
			if (boxes == null) {  // this happens when the square isn't fully surrounded by boxes
				continue;
			}
			
			DeadCandidate dc = new DeadCandidate();
			dc.candidate = loc;
			dc.myBox = getBox(loc);
			
			for (Box box: boxes) {
				
				boolean good = true;
				for (Square square: box.getSquares()) {
					if (!square.isAdjacent(loc) && !square.equals(loc)) {
						good = false;
						break;
					}
				}
				if (good) {
					dc.goodBoxes.add(box); //  a good adjacent box is where all its Tiles are adjacent to the candidate
				} else {
					dc.badBoxes.add(box);  // otherwise it is a bad box
				}
				
			}
			
			// if the tile has no boxes adjacent to it then it is already dead (i.e. surrounded by mines and witnesses only)
			//if (boardState.getSolver().isTestMode() && dc.goodBoxes.isEmpty() && dc.badBoxes.isEmpty()) {
			if (dc.goodBoxes.isEmpty() && dc.badBoxes.isEmpty()) {
				// add the dead locations we found 
				//Set<Location> newDead = new HashSet<>();
				//newDead.add(dc.candidate);
				deadLocations = deadLocations.add(dc.candidate);
				//logger.log(Level.INFO, "Tile %s is dead since it is isolated", dc.candidate);
			} else {
				deadCandidates.add(dc);
			}

		}

		for (DeadCandidate dc: deadCandidates) {
			logger.log(Level.DEBUG, "%s is candidate dead with %d good boxes and %d bad boxes", dc.candidate, dc.goodBoxes.size(), dc.badBoxes.size());
		}
		
	}
	
	@Override
	Box getBox(Location l) {
		
		for (Box b: boxes) {
			if (b.contains(l)) {
				return b;
			}
		}
		
		return null;
	}
	
	private List<Box> getAdjacentBoxes(Location loc) {
		
		List<Box> result = new ArrayList<>();
		
		//int sizeOfBoxes = 0;
		
		// get each adjacent location
		for (Location adjLoc: boardState.getAdjacentUnrevealedSquares(loc)) {
			
			// find the box it is in
			boolean boxFound = false;
			for (Box box: web.getBoxes()) {
				if (box.contains(adjLoc)) {
					boxFound = true;
					// is the box already included?
					boolean found = false;
					for (Box oldBox: result) {
						if (box.getUID() == oldBox.getUID()) {
							found = true;
							break;
						}
					}
					// if not add it
					if (!found) {
						result.add(box);
						//sizeOfBoxes = box.getSquares().size();
					}
				}
			}
			
			// if a box can't be found for the adjacent square then the location can't be dead
			if (!boxFound) {
				return null;
			}
			
		}		
		
		// if the area in the boxes does not agree with the area around the target location then the boxes overspill the area
		//if (boardState.countAdjacentUnrevealed(loc) != sizeOfBoxes) {
		//	return null;
		//}
		
		
		return result;
		
	}
	
	
	/**
	 * The number of ways the mines can be placed in the game position
	 * @return
	 */
	protected BigInteger getSolutionCount() {
		return finalSolutionsCount;
	}
	
	/**
	 * The number of locations which are definitely clears
	 * @return
	 */
	protected int getClearCount() {
		return clearCount;
	}
	
	/**
	 * The number of locations which are definitely clears and also living
	 * @return
	 */
	@Override
	protected int getLivingClearCount() {
		return livingClearCount;
	}
	
	/**
	 * The boxes which contain no mines
	 * @return
	 */
	@Override
	protected List<Box> getEmptyBoxes() {
		return emptyBoxes;
	}
	
	/**
	 * The duration to do the processing in milliseconds
	 * @return
	 */
	protected long getDuration() {
		return this.duration;
	}
	
	/**
	 * How many independent groups we encountered in the processing
	 * @return
	 */
	protected long getIndependentGroups() {
		return this.independentGroups;
	}
	
	protected List<LinkedLocation> getLinkedLocations() {
		return this.linkedLocations;
	}
	
	// get the dead locations provided to the probability engine with any new ones dicovered
	protected Area getDeadLocations() {
		return this.deadLocations;
	}
	
	// Are all the tiles on the perimeter dead
	protected boolean allDead() {
		return this.allDead;
	}
	
	// returns the number of additional mines surrounding this location
	protected int getDeadValueDelta(Location l) {
		
		for (DeadCandidate dc: deadCandidates) {
			if (dc.candidate.equals(l)) {
				if (!dc.isAlive) {
					return dc.total;
				} else {
					logger.log(Level.WARN, "Request for Tile %s Dead value delta but it isn't dead!", l);
					return 0;
				}
			}
		}
		
		logger.log(Level.WARN, "Request for Tile %s Dead value delta but isn't dead", l);
		return 0;
	}
	
	protected List<BruteForce> getIsolatedEdges() {
		return isolatedEdges;
	}
	
	/**
	 * If this tile is a linked Location return the number of links it has, otherwise return 0
	 * @param tile
	 * @return
	 */
	protected LinkedLocation getLinkedLocation(Location tile) {
		for (LinkedLocation ll: linkedLocations) {
			if (ll.equals(tile)) {
				return ll;
			}
		}
		
		return null;
		
	}
	
	/**
	 * Returns a list of the locations of certain mines
	 */
	protected List<Location> getMines() {
		return this.mines;
	}
	
	protected List<LinkedLocation> getContraLinkedLocations() {
		return this.contraLinkedLocations;
	}
	
	protected Map<Integer, BigInteger> getValidMineCounts() {
		return mineCounts;
	}
	
}
