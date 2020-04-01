package minesweeper.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.Witness;

/**
 * This class uses a non iterative approach to calculating the number of candidate solution in the game. It is driven by a {@link minesweeper.solver.WitnessWeb witness web}.
 * 
 */
public class SolutionCounter {

	private int[][] SMALL_COMBINATIONS = new int[][] {{1}, {1,1}, {1,2,1}, {1,3,3,1}, {1,4,6,4,1}, {1,5,10,10,5,1}, {1,6,15,20,15,6,1}, {1,7,21,35,35,21,7,1}, {1,8,28,56,70,56,28,8,1}};
	
	private class MergeSorter implements Comparator<ProbabilityLine> {

		int[] checks;
		
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
		
		{
			for (int i=0; i < mineBoxCount.length; i++) {
				mineBoxCount[i] = BigInteger.ZERO;
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
	
	private long duration;
	
	private List<ProbabilityLine> workingProbs = new ArrayList<>(); // as we work through an independent set of witnesses probabilities are held here
 	private List<ProbabilityLine> heldProbs = new ArrayList<>();  
	
	//when set to true indicates that the box has been part of this analysis
	private boolean[] mask;           
	
	final private BoardState solver;
	final private WitnessWeb web;
	final private int boxCount;
	final private List<Witness> witnesses;
	final private List<Box> boxes;
	final private int minesLeft;                 // number of mines undiscovered in the game
	final private int squaresLeft;               // number of squares undiscovered in the game and off the web
	
	private int independentGroups = 0;
	private int recursions = 0;
	
	private BigInteger finalSolutionsCount;
	private int clearCount;
	
	// these are the limits that can be on the edge
	final private int minTotalMines;
	final private int maxTotalMines;
	
	final private Map<Integer, BigInteger> mineCounts = new HashMap<>();
	
	
	public SolutionCounter(BoardState solver, WitnessWeb web, int squaresLeft, int minesLeft) {
		
		this.solver = solver;
		this.web = web;
		this.minesLeft = minesLeft;
		this.squaresLeft = squaresLeft - web.getSquares().size();
		
		this.minTotalMines = Math.max(0, minesLeft - this.squaresLeft);  //we can't use so few mines that we can't fit the remainder elsewhere on the board
		this.maxTotalMines = minesLeft;    // we can't use more mines than are left in the game
		
		//solver.display("Total mines " + minTotalMines + " to " + maxTotalMines);
		
		web.generateBoxes();
		
		this.witnesses = web.getPrunedWitnesses();
		this.boxes = web.getBoxes();
		
		this.boxCount = boxes.size();
		
		for (Witness w: witnesses) {
			w.setProcessed(false);
		}
		
		for (Box b: boxes) {
			b.setProcessed(false);
		}
		
	}

	/**
	 * Run the solution counter
	 */
	public void process() {
		
		if (!web.isWebValid()) {  // if the web is invalid then nothing we can do
			solver.display("Web is invalid - skipping the SolutionCounter processing");
			finalSolutionsCount = BigInteger.ZERO;
			return;
		}
		
		long startTime = System.currentTimeMillis();
		
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

		// have we got a valid position
		if (!heldProbs.isEmpty()) {
			calculateBoxProbabilities();
		} else {
			finalSolutionsCount = BigInteger.ZERO;
			clearCount = 0;
		}

		
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
			
			if (current == null) {
				current = pl;
			} else if (sorter.compare(current, pl) != 0) {
				result.add(current);
				current = pl;
			} else {
				combineProbabilities(current, pl);
			}
			
		}

		result.add(current);

		return result;
		
	}

	
	// calculate how many ways this solution can be generated and roll them into one
	private void combineProbabilities(ProbabilityLine npl, ProbabilityLine pl) {
		
		npl.solutionCount = npl.solutionCount.add(pl.solutionCount);
		
		for (int i = 0; i < pl.mineBoxCount.length; i++) {
			if (mask[i]) {  
	 			npl.mineBoxCount[i] = npl.mineBoxCount[i].add(pl.mineBoxCount[i]);
			}
	
		}
		
	}
	
	// this combines newly generated probabilities with ones we have already stored from other independent sets of witnesses
	private void storeProbabilities() {
		
		List<ProbabilityLine> result = new ArrayList<>(); 
		
		// if there are no lines to store then we don't have a valid position
		if (workingProbs.isEmpty()) {
			//solver.display("working probabilites list is empty!!");
			heldProbs.clear();
			return;
		} 
		
		List<ProbabilityLine> crunched = workingProbs;
		
		//solver.display("New data has " + crunched.size() + " entries");
		
		for (ProbabilityLine pl: crunched) {
			
			for (ProbabilityLine epl: heldProbs) {
				
				if (pl.mineCount + epl.mineCount <= maxTotalMines) {
					
					ProbabilityLine npl = new ProbabilityLine(pl.solutionCount.multiply(epl.solutionCount));
					npl.mineCount = pl.mineCount + epl.mineCount;
					
					for (int i=0; i < npl.mineBoxCount.length; i++) {
						
						BigInteger w1 = pl.mineBoxCount[i].multiply(epl.solutionCount);
						BigInteger w2 = epl.mineBoxCount[i].multiply(pl.solutionCount);
						npl.mineBoxCount[i] = w1.add(w2);
						
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
			
			for (int i = 0; i < pl.mineBoxCount.length; i++) {
				npl.mineBoxCount[i] = npl.mineBoxCount[i].add(pl.mineBoxCount[i]);
			}
		}

		heldProbs.add(npl);


	}
	
	// here we calculate the total number of candidate solutions left in the game
	private void calculateBoxProbabilities() {
		
		// total game tally
		BigInteger totalTally = BigInteger.ZERO;
		
		// outside a box tally
		BigInteger outsideTally = BigInteger.ZERO;
		
		boolean[] emptyBox = new boolean[boxCount];
		for (int i=0; i < emptyBox.length; i++) {
			emptyBox[i] = true;
		}						
		
		// calculate how many mines 
		for (ProbabilityLine pl: heldProbs) {
			
			if (pl.mineCount >= minTotalMines) {    // if the mine count for this solution is less than the minimum it can't be valid
				
				if (mineCounts.put(pl.mineCount, pl.solutionCount) != null) {
					System.out.println("Duplicate mines in probability Engine");
				}
				
				BigInteger mult = Solver.combination(minesLeft - pl.mineCount, squaresLeft);  //# of ways the rest of the board can be formed
				
				outsideTally = outsideTally.add(mult.multiply(BigInteger.valueOf(minesLeft - pl.mineCount)).multiply(pl.solutionCount));
				
				// this is all the possible ways the mines can be placed across the whole game
				totalTally = totalTally.add(mult.multiply(pl.solutionCount));
				
				for (int i=0; i < emptyBox.length; i++) {
					if (pl.mineBoxCount[i].signum() != 0) {
						emptyBox[i] = false;
					}
				}				
			}
		}		

		// determine how many clear squares there are
		if (totalTally.signum() > 0) {
			for (int i=0; i < emptyBox.length; i++) {
				if (emptyBox[i]) {
					 clearCount = clearCount + boxes.get(i).getSquares().size();
				}
			}						
		}
	
		//solver.display("Game has " + clearCount + " clears available");
		finalSolutionsCount = totalTally;

		//solver.display("Number of solutions is " + finalSolutionsCount);
		
		
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
		
		//solver.display("Processed witness " + nw.witness.display());
		
		// flag the last set of details as processed
		nw.witness.setProcessed(true);
		for (Box b: nw.newBoxes) {
			b.setProcessed(true);
		}

		
		
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
		//solver.display("Boxes partially processed " + boundaryBoxes.size());
		
		MergeSorter sorter = new MergeSorter(boundaryBoxes);
		
		newProbs = crunchByMineCount(newProbs, sorter);

		return newProbs;
		
		
	}
	
	// this is used to recursively place the missing Mines into the available boxes for the probability line
	private List<ProbabilityLine> distributeMissingMines(ProbabilityLine pl, NextWitness nw, int missingMines, int index) {
		
		recursions++;
		if (recursions % 10000 == 0) {
			solver.display("Solution counter recursion = " + recursions);
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
			result.add(extendProbabilityLine(pl, nw.newBoxes.get(index), missingMines));
			return result;
		}
		
		
		// this is the recursion
		int maxToPlace = Math.min(nw.newBoxes.get(index).getMaxMines(), missingMines);
		
		for (int i=nw.newBoxes.get(index).getMinMines(); i <= maxToPlace; i++) {
			ProbabilityLine npl = extendProbabilityLine(pl, nw.newBoxes.get(index), i);
			
			result.addAll(distributeMissingMines(npl, nw, missingMines - i, index + 1));
		}
		
		return result;
		
	}
	
	// create a new probability line by taking the old and adding the mines to the new Box
	private ProbabilityLine extendProbabilityLine(ProbabilityLine pl, Box newBox, int mines) {
		
		int combination = SMALL_COMBINATIONS[newBox.getSquares().size()][mines];
		
		BigInteger newSolutionCount = pl.solutionCount.multiply(BigInteger.valueOf(combination));
		
		ProbabilityLine result = new ProbabilityLine(newSolutionCount);
		
		result.mineCount = pl.mineCount + mines;
		
		// copy the probability array
		if (combination == 1) {
			System.arraycopy(pl.mineBoxCount, 0, result.mineBoxCount, 0, pl.mineBoxCount.length);
		} else {
			BigInteger multiplier = BigInteger.valueOf(combination);
			for (int i=0; i < pl.mineBoxCount.length; i++) {
				if (mask[i]) {
					result.mineBoxCount[i] = pl.mineBoxCount[i].multiply(multiplier);	
				}
			}
		}

		result.allocatedMines = pl.allocatedMines.clone();
		
		result.mineBoxCount[newBox.getUID()] = BigInteger.valueOf(mines).multiply(newSolutionCount);
		result.allocatedMines[newBox.getUID()] = mines;
		
		return result;
	}
	
	// counts the number of mines already placed
	private int countPlacedMines(ProbabilityLine pl, NextWitness nw) {
		
		BigInteger result = BigInteger.ZERO;
		
		for (Box b: nw.oldBoxes) {
			result = result.add(pl.mineBoxCount[b.getUID()]);
		}
		
		BigInteger[] divide = result.divideAndRemainder(pl.solutionCount);
		if (divide[1].signum() != 0) {
			System.out.println("Min Box Count divide has non-zero remainder " + divide[1]);
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
		
		// flag the last set of details as processed
		//prevWitness.witness.setProcessed(true);
		//for (Box b: prevWitness.newBoxes) {
		//	b.setProcessed(true);
		//}


		int bestTodo = 99999;
		Witness bestWitness = null;

		// find the next witness which reduces the boundary to the smallest
		/*
		for (Witness w: witnesses) {
			if (w.isProcessed()) {
				continue;
			}
			
			w.setProcessed(true);
			
			int boundary = 0;
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
						boundary++;
						break;
					}
				}
			}
			
			w.setProcessed(false);
			
			if (boundary < bestTodo) {
				bestTodo = boundary;
				bestWitness = w;
			}
			
		}
		*/
		
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
		
		// get an unprocessed witness
		NextWitness nw =  findFirstWitness();
		
		// only crunch it down for non-trivial probability lines unless it is the last set - this is an efficiency decision
		//if (workingProbs.size() > 0 || nw == null) {
			storeProbabilities();
			
			// reset the working array so we can start building up one for the new set of witnesses
			workingProbs.clear();
			workingProbs.add(new ProbabilityLine(BigInteger.ONE));
			
			// reset the mask indicating that no boxes have been processed 
			mask = new boolean[boxCount]; 
		//}
		
		// if the position is invalid exit now
		if (heldProbs.isEmpty()) {
			return null;
		}
		
		// return the next witness to process
		return nw;
		
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
	
	protected Map<Integer, BigInteger> getValidMineCounts() {
		return mineCounts;
	}
	
}
