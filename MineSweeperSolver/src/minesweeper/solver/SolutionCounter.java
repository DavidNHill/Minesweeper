package minesweeper.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

/**
 * This class uses a non iterative approach to calculating the number of candidate solution in the game. It is driven by a {@link minesweeper.solver.WitnessWeb witness web}.
 * 
 */
public class SolutionCounter {

	private int[][] SMALL_COMBINATIONS = new int[][] {{1}, {1,1}, {1,2,1}, {1,3,3,1}, {1,4,6,4,1}, {1,5,10,10,5,1}, {1,6,15,20,15,6,1}, {1,7,21,35,35,21,7,1}, {1,8,28,56,70,56,28,8,1}};
	
	private static final boolean CHECK_FOR_DEAD_LOCATIONS = false;
	
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
	
	//when set to true indicates that the box has been part of this analysis
	private boolean[] mask;           
	
	final private BoardState boardState;
	final private WitnessWeb web;
	final private int boxCount;
	final private List<Witness> witnesses;
	final private List<Box> boxes;
	final private int minesLeft;                 // number of mines undiscovered in the game
	private int squaresLeft;               // number of squares undiscovered in the game and off the web
	
	private int independentGroups = 0;
	private int recursions = 0;
	
	private BigInteger finalSolutionsCount;
	private int clearCount;
	private int livingClearCount;
	final private List<Box> emptyBoxes = new ArrayList<>();
	
	// these are the limits that can be on the edge
	private int minTotalMines;
	final private int maxTotalMines;
	
	final private Map<Integer, BigInteger> mineCounts = new HashMap<>();
	
	// list of locations which are potentially dead
	private List<DeadCandidate> deadCandidates = new ArrayList<>();
	private Area deadLocations = Area.EMPTY_AREA;
	private boolean canDoDeadTileAnalysis;
	
	private boolean valid = true;
	
	public SolutionCounter(BoardState boardState, WitnessWeb web, int squaresLeft, int minesLeft) {
		
		this.boardState = boardState;
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
	public void process(Area deadLocations) {
		
		if (!web.isWebValid() || !this.valid) {  // if the web is invalid then nothing we can do
			boardState.getLogger().log(Level.INFO, "Web is invalid - skipping the SolutionCounter processing");
			finalSolutionsCount = BigInteger.ZERO;
			return;
		}
		
		long startTime = System.currentTimeMillis();
		
		// if we compress the probability lines before the edge is completely processed we can't use the data to look for dead tiles
		canDoDeadTileAnalysis = CHECK_FOR_DEAD_LOCATIONS;
		
		if (CHECK_FOR_DEAD_LOCATIONS) {
			determineCandidateDeadLocations();			
		} else {
			this.deadLocations = deadLocations;
		}
		
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
			livingClearCount = 0;
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
					 
					 for (Square sq: boxes.get(i).getSquares()) {
						 if (!deadLocations.contains(sq)) {
							 livingClearCount++;
						 }
					 }
					 
					 if (boxes.get(i).getSquares().size() > 0) {
						 emptyBoxes.add(boxes.get(i));
					 }
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

		//if we haven't compressed yet and we are still a small edge then don't compress
		if (newProbs.size() < 50 && canDoDeadTileAnalysis) {
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
		//solver.display("Boxes partially processed " + boundaryBoxes.size());
		
		MergeSorter sorter = new MergeSorter(boundaryBoxes);
		
		newProbs = crunchByMineCount(newProbs, sorter);

		return newProbs;
		
		
	}
	
	// this is used to recursively place the missing Mines into the available boxes for the probability line
	private List<ProbabilityLine> distributeMissingMines(ProbabilityLine pl, NextWitness nw, int missingMines, int index) {
		
		recursions++;
		if (recursions % 10000 == 0) {
			boardState.getLogger().log(Level.WARN, "Probability Engine recursion exceeding %d iterations", recursions);
		}
		
		List<ProbabilityLine> result = new ArrayList<>();
		
		Box box = nw.newBoxes.get(index);
		
		// if there is only one box left to put the missing mines we have reach this end of this branch of recursion
		if (nw.newBoxes.size() - index == 1) {
			// if there are too many for this box then the probability can't be valid
			if (box.getMaxMines() < missingMines) {
				return result;
			}
			
			// if there are too few for this box then the probability can't be valid
			if (box.getMinMines() > missingMines) {
				return result;
			}
			// if there are too many for this game then the probability can't be valid
			if (pl.mineCount + missingMines > maxTotalMines) {
				return result;
			}			
			
			// otherwise place the mines in the probability line
			result.add(extendProbabilityLine(pl, box, missingMines));
			return result;
		}
		
		// this is the recursion
		int maxToPlace = Math.min(box.getMaxMines(), missingMines);
		
		for (int i=box.getMinMines(); i <= maxToPlace; i++) {
			ProbabilityLine npl = extendProbabilityLine(pl, box, i);
			
			result.addAll(distributeMissingMines(npl, nw, missingMines - i, index + 1));
		}


		return result;
		
	}
	
	// create a new probability line by taking the old and adding the mines to the new Box
	private ProbabilityLine extendProbabilityLine(ProbabilityLine pl, Box newBox, int mines) {
		
		// reduce the number of tile which can have mines by the number we know are empty
		int modifiedTilesCount = newBox.getSquares().size() - newBox.getEmptyTiles();
		
		//int combination = SMALL_COMBINATIONS[newBox.getSquares().size()][mines];
		int combination = SMALL_COMBINATIONS[modifiedTilesCount][mines];
		
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
				display("This is a complete scan");
			} else {
				display("This is not a complete scan");
			}			
		} else {
			completeScan = false;
			display("This is not a complete scan because there are squares off the edge");
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
				display("Location " + dc.candidate.toString() + " has some boxes in scope and some out of scope so assumed alive");
				dc.isAlive = true;
				continue;
			}
			
			//if we can't do the check because the edge has been compressed mid process then assume alive
			if (!checkPossible) {
				display("Location " + dc.candidate.toString() + " was on compressed edge so assumed alive");
				dc.isAlive = true;
				continue;
			}
			
			boolean okay = true;
			int mineCount = 0;
			line: for (ProbabilityLine pl: workingProbs) {

				if (completeScan && pl.mineCount != minesLeft) {
					continue;
				}
				
				// ignore probability lines where the candidate is a mine
				//if (pl.mineBoxCount[dc.myBox.getUID()].compareTo(BigInteger.valueOf(dc.myBox.getSquares().size())) == 0) {
				if (pl.allocatedMines[dc.myBox.getUID()] == dc.myBox.getSquares().size()) {
					//boardState.display("Location " + dc.candidate.display() + " I'm a mine on this Probability line");
					mineCount++;
					continue line;
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
						display("Location " + dc.candidate.toString() + " is not dead because a bad box is neither empty nor full of mines");
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
						display("Location " + dc.candidate.toString() + " is not dead because the sum of mines in good boxes is not constant. Was "
					                       + dc.total + " now " + tally + ". Mines in probability line " + pl.mineCount);
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
				display(dc.candidate.toString() + " is dead");
			}
			
		}
		
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
			if (dc.goodBoxes.isEmpty() && dc.badBoxes.isEmpty()) {
				deadLocations = deadLocations.add(dc.candidate);
				display(dc.candidate.toString() + " is dead since it has no open tiles around it");
			} else {
				deadCandidates.add(dc);
			}

		}

		for (DeadCandidate dc: deadCandidates) {
			display(dc.candidate.toString() + " is candidate dead with " + dc.goodBoxes.size() + " good boxes and " + dc.badBoxes.size() + " bad boxes");
		}
		
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
					}
				}
			}
			
			// if a box can't be found for the adjacent square then the location can't be dead
			if (!boxFound) {
				return null;
			}
			
		}		
		
		return result;
		
	}
	
	private Box getBox(Location l) {
		
		for (Box b: boxes) {
			if (b.contains(l)) {
				return b;
			}
		}
		
		return null;
	}
	
	private void display(String text) {
		//solver.display(text);
	}

	// forces a box to contain a tile chich isn't a mine.  If the location isn't in a box false is returned. If the box can't support zero mines false is returned.
	public boolean setMustBeEmpty(Location loc) {
		Box box = getBox(loc);
		
		if (box == null) {  // if the tile isn't on the edge then adjust the off edge values
			this.squaresLeft--;
			//this.minTotalMines++;
			this.minTotalMines = Math.max(0, minesLeft - this.squaresLeft);  //we can't use so few mines that we can't fit the remainder elsewhere on the board
			
			//this.valid = false;
			//return false;
		//} else if (box.getMinMines() != 0) {
		//	this.valid = false;
		//	return false;
		} else {
			box.incrementEmptyTiles();
		}
		
		return true;
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
	protected int getLivingClearCount() {
		return livingClearCount;
	}
	
	/**
	 * The boxes which contain no mines
	 * @return
	 */
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
	
	protected Map<Integer, BigInteger> getValidMineCounts() {
		return mineCounts;
	}
	
}
