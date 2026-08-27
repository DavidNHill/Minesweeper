package minesweeper.solver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.constructs.TranspositionTable;
import minesweeper.solver.constructs.TranspositionTable.TTEntry;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.solver.utility.Timer;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class BruteForceAnalysisExperimental extends BruteForceAnalysisModel{
	
	// used to hold all the solutions left in the game
	private class SolutionTable {
		
		private final Random rng = new Random();
		
		private final byte[][] solutions;
		private final long[] hash;
		private int size = 0;

		private SolutionTable(int maxSize) {
			solutions = new byte[maxSize][];
			hash = new long[maxSize];
		}
		
		private void addSolution(byte[] solution) {
			solutions[size] = solution;
			hash[size] = rng.nextInt();
			size++;
		};
		
		private int size() {
			return size;
		}
		
		private byte[] get(int index) {
			return solutions[index];
		}
		
		private long getHash(int index) {
			return hash[index];
		}
		
		private SolutionTable copySolutionTable() {
			
			SolutionTable result = new SolutionTable(hash.length);
			
			for (byte[] row: solutions) {
				result.addSolution(row);
			}
			
			return result;
			
		}
		
		private void sortSolutions(int start, int end, int index) {

			Arrays.sort(solutions, start, end, sorters[index]);
			
		}

	}
	
	/**
	 * This sorts solutions by the value of a position
	 */
	private class SortSolutions implements Comparator<byte[]> {

		private final int sortIndex;
		
		public SortSolutions(int index) {
			sortIndex = index;
		}
		
		@Override
		public int compare(byte[] o1, byte[] o2) {
			return o1[sortIndex] - o2[sortIndex];
		}
		
	}
	
	/**
	 * A key to uniquely identify a position
	 */
	/*
	private static class Position {
		
		private static long startZHash;
		private static long[][] zHash;
		
		private final long zobristHash;
		private short depth;
		
		private Position() {
			
			this.zobristHash = startZHash;
			
			this.depth = 0;
		}
		
		private Position(Position p, int index, byte value) {

			// update the hash with the new position
			this.zobristHash = p.zobristHash ^ zHash[index][value];
			
			this.depth = (short) (p.depth + 1);
			
		}

		// generate the positional Zobrist hash values
		private static void initialise(int numOfLocations) {
			
			SplittableRandom random = new SplittableRandom();
			
			zHash = new long[numOfLocations][9];
			
			for (int i=0; i < numOfLocations; i++) {
				for (int j=0; j < 9; j++) {
					zHash[i][j] = random.nextLong();
				}
			}
			
			startZHash = random.nextLong();
			
		}
		
		@Override
		// copied from String hash
		public int hashCode() {
			return (int) zobristHash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Position) {
				return ((Position) o).zobristHash == this.zobristHash;
			} else {
				return false;
			}
		}
	}
	*/
	
	/**
	 * A key to uniquely identify a position
	 */
	private static class HashController {
		
		private static long startZHash;
		private static long[][] zHash;
		
		private static long RootValue() {
			return startZHash;
		}
		
		private static long newValue(long oldHash, int index, byte value) {
			return oldHash ^ zHash[index][value];
		}

		// generate the positional Zobrist hash values
		private static void initialise(int numOfLocations) {
			
			SplittableRandom random = new SplittableRandom();
			
			zHash = new long[numOfLocations][9];
			
			for (int i=0; i < numOfLocations; i++) {
				for (int j=0; j < 9; j++) {
					zHash[i][j] = random.nextLong();
				}
			}
			
			startZHash = random.nextLong();
			
		}
		
	}
	
	/**
	 * Positions on the board which can still reveal information about the game.
	 */
	private class LivingLocation implements Comparable<LivingLocation>{
		
		//private int winningLines = 0;
		private boolean pruned = false;  // stopped because this move can't beat the best
		
		//private int winningLines = 0;
		
		private final short locationIndex;
		private int mineCount = 0;  // number of remaining solutions which have a mine in this position
		//private int maxSolutions = 0;    // the maximum number of solutions that can be remaining after clicking here
		//private int zeroSolutions = 0;    // the number of solutions that have a '0' value here
		private byte maxValue = -1;
		private byte minValue = -1;
		private byte distinctValues;  // number of possible values at this location
		
		// Used to determine when this tile is linked to an earlier one.
		// Only one of the linked tiles needs to be analysed
		private boolean linked = false;
		private long linkedHash;
		
		private Node[] children = new Node[9];
		//private List<Node> childrenList;

		private LivingLocation(short index) {
			this.locationIndex = index;
		}

		/**
		 * Determine the Nodes which are created if we play this move. Up to 9 positions where this locations reveals a value [0-8].
		 */
		private List<Node> buildChildNodes(SolutionTable solutions, Node parent) {
			
			// sort the solutions by possible values
			solutions.sortSolutions(parent.startSolution, parent.endSolution, this.locationIndex);
			int solutionIndex = parent.startSolution;
			
			// skip over the mines
			while (solutionIndex < parent.endSolution && solutions.get(solutionIndex)[this.locationIndex] == GameStateModel.MINE) {
				solutionIndex++;
			}

			List<Node> result = new ArrayList<>(9);
			for (byte i=this.minValue; i < this.maxValue + 1; i++) {
			//for (byte i=0; i < 9; i++) {	
				int start = solutionIndex;
				// find all solutions for this values at this location
				while (solutionIndex < parent.endSolution && solutions.get(solutionIndex)[this.locationIndex] == i) {
					solutionIndex++;
				}					
								
				if (start != solutionIndex) {
					Node temp = new Node(HashController.newValue(parent.hash, this.locationIndex, i), start, solutionIndex);

					this.children[i] = temp;
					result.add(temp);
				}
			}

			if (solutionIndex != parent.endSolution) {
				System.out.println("Didn't read all the elements in the array; index = " + solutionIndex + " end = " + parent.endSolution);
			}

			Collections.sort(result);
			return result;
			
		}
		
		@Override
		public int compareTo(LivingLocation o) {

			// return location most likely to be safe  - this has to be first, the logic depends upon it
			int test = this.mineCount - o.mineCount;
			if (test != 0) {
				return test;
			}
			
			/*
			// then the location most likely to have a zero
			test = o.zeroSolutions - this.zeroSolutions;
			if (test != 0) {
				return test;
			}
			
			// then by most number of different possible values
			test = o.count - this.count;
			if (test != 0) {
				return test;
			}
			
			// then by the maxSolutions - ascending
			return this.maxSolutions - o.maxSolutions;
			*/
			
			return 0;
		}
		
	}

	/**
	 * A representation of a possible state of the game
	 */
	private class Node implements Comparable<Node> {

		private final long hash ;         		// representation of the position we are analysing / have reached
		private final int startSolution;         // the first solution in the solution array that applies to this position
		private final int endSolution;          // the last + 1 solution in the solution array that applies to this position
		
		private int bestWinningLines = 0;      // this is the number of winning lines below this position in the tree
		
		private List<LivingLocation> livingLocations;   // these are the locations which need to be analysed
		private LivingLocation bestLiving;              // after analysis this is the location that represents best play

		//private Node() {
		//	position = new Position();
		//}
		
		private Node(long hash, int startSolution, int endSolution) {
			this.hash = hash;
			this.startSolution = startSolution;
			this.endSolution = endSolution;
		}
		
		private List<LivingLocation> getLivingLocations() {
			return livingLocations;
		}
		
		private int getSolutionSize() {
			return endSolution - startSolution;
		}
		
		/**
		 * Get the probability of winning the game from the position this node represents  (winningLines / solution size)
		 * @return
		 */
		private BigDecimal getProbability() {
			return BigDecimal.valueOf(bestWinningLines).divide(BigDecimal.valueOf(getSolutionSize()), Solver.DP, RoundingMode.HALF_UP); 
		}
		
		/**
		 * Calculate the number of winning lines if this move is played at this position
		 * Used at top of the game tree
		 */
		private int getWinningLines(ControlData control, LivingLocation move) {

			//if we can never exceed the cutoff then no point continuing
			//if (Solver.PRUNE_BF_ANALYSIS && this.getSolutionSize() - move.mineCount <= this.winningLines) {
			//	move.pruned = true;
			//	return (this.getSolutionSize() - move.mineCount);
			//}
			
			int winningLines;
			if (Solver.PRUNE_BF_ANALYSIS) {
				winningLines = getWinningLines(control, 1, move, this.bestWinningLines);
				
			} else {
				winningLines = getWinningLines(control, 1, move, 0);
			}
			
			
			if (winningLines > this.bestWinningLines) {
				this.bestWinningLines = winningLines;
			}
			
			return winningLines;
		}
		
		
		/**
		 * Calculate the number of winning lines if this move is played at this position
		 * Used when exploring the game tree
		 */
		private int getWinningLines(ControlData control, final int depth, LivingLocation move, int cutoff) {

			// this is the sum of the best winning lines for each child node.
			int totalWinningLines = 0;

			int notMines = this.getSolutionSize() - move.mineCount;
			
			// if the max possible winning lines is less than the current cutoff then no point doing the analysis
			if (notMines <= cutoff) {
				move.pruned = true;
				//move.winningLines = notMines;
				return notMines;
			}
			
			// have we been interrupted?
			if (Thread.interrupted())  {
				aborted = true;
			    maxProcessCount = 1000;
			}
			
			// we're going to have to do some work
			processCount++;
			if (processCount > maxProcessCount) {
				return totalWinningLines;
			}
		
			//List<Node> nodes = move.buildChildNodes(control.solutionTable, this);   
		
			//for (Node child: move.children) {
			for (Node childNode:  move.buildChildNodes(control.solutionTable, this)) {

				// see if this position is in the cache
				int temp1 = -1;
				if (childNode.getSolutionSize() > cacheThreshold) {
					temp1 = cache.probe(childNode.hash);
					
					//TODO error checking
					if (temp1 != -1 && (temp1 > getSolutionSize() || temp1 < 1)) {
						//System.err.println("Winning lines outside solution table size " + temp1 + " , hash is " + childNode.position.zobristHash);
						temp1 = -1;
					}
				}
		
				if (temp1 != -1) {
					childNode.bestWinningLines = temp1;
					
					cacheHit++;
					cacheWinningLines = cacheWinningLines + (long) temp1;
					
				} else {
					
					childNode.determineLivingLocations(control, this.livingLocations, move.locationIndex);
									
					if (childNode.getLivingLocations().isEmpty()) {  // no further information ==> all solution indistinguishable ==> 1 winning line

						childNode.bestWinningLines = 1;
						terminalNodesReached = terminalNodesReached + 1;
							
					} else {  // not cached and not terminal node, so we need to do the recursion
						
						for (LivingLocation childMove: childNode.getLivingLocations()) {
							
							if (childMove.linked) {
								continue;  // only need to analyse the first tile of a linked set
							}
							
							// if the number of safe solutions <= the best winning lines then we can't do any better, so skip the rest
							if (childNode.getSolutionSize() - childMove.mineCount <= childNode.bestWinningLines) {
								break;
							}
							
							// now calculate the winning lines for each of these children
							int wl;
							if (childNode.getSolutionSize() - childMove.mineCount == childMove.distinctValues) {
								wl = childMove.distinctValues;
							} else {
								wl = childNode.getWinningLines(control, depth + 1, childMove, childNode.bestWinningLines);
							}
							
							// now calculate the winning lines for each of these children
							//int wl = childNode.getWinningLines(control, depth + 1, childMove, childNode.bestWinningLines);
							if (!childMove.pruned) {
								if (childNode.bestWinningLines < wl || (childNode.bestLiving != null && childNode.bestWinningLines == wl && childNode.bestLiving.mineCount < childMove.mineCount)) {
									childNode.bestWinningLines = wl;
									
									if (childNode.bestLiving != null) {
										childNode.bestLiving.children = null;
									}
									childNode.bestLiving = childMove;
								} else {
									childNode.bestLiving.children = null;
								}
							} else {
								childNode.bestLiving.children = null;
							}
							
							// if there are no mines then this is a 100% safe move, so skip any further analysis since it can't be any better
							if (childMove.mineCount == 0) {
								break;
						 	}
							
						}

						// no need to hold onto the living location once we have determined the best of them
						//child.livingLocations = null;

						if (depth > solver.preferences.getBruteForceTreeDepth()) {  // stop holding the tree beyond this depth
							childNode.bestLiving = null;
						}
						
						// add the child to the cache if it didn't come from there and it is carrying sufficient winning lines
						if (childNode.getSolutionSize() > cacheThreshold && !cacheLocked) {
							//cacheSize++;
							cache.store(childNode.hash, depth, childNode.bestWinningLines);
							//cacheQuantity[depth]++;
						}
					}
				}
			
				// no need to hold onto the living location once we have determined the best of them
				childNode.livingLocations = null;
				
				// store the aggregate winning lines 
				//result = result + childNode.winningLines;	
				
				totalWinningLines += childNode.bestWinningLines;
				
				notMines = notMines - childNode.getSolutionSize();  // reduce the number of not mines
				
				// if we are pruning at depth 1 then update with the best line fully completed
				if (Solver.PRUNE_BF_ANALYSIS && depth == 1) {
					cutoff = getBestWinningLines();
				}
				
				// if the max possible winning lines is less than the current cutoff then no point doing the analysis
				if (totalWinningLines + notMines <= cutoff) {
					move.pruned = true;
					move.children = null;
					totalWinningLines += notMines;
					
					return totalWinningLines;
				}
				
			}
			
			return totalWinningLines;
			
			
		}
		
		/**
		 * this generates a list of Location that are still alive, (i.e. have more than one possible value) from a list of previously living locations
		 * Index is the move which has just been played (in terms of the off-set to the position[] array)
		 */
		private void determineLivingLocations(ControlData control, List<LivingLocation> liveLocs, final int index) {
			
			List<LivingLocation> living = new ArrayList<>(liveLocs.size());
			
			boolean stopDetail = false;
			for (LivingLocation location: liveLocs) {
				
				if (location.locationIndex == index) {  // if this is the same move we just played then no need to analyse it - definitely now non-living.
					continue;
				}
				
				int mineTally = 0;
				byte distinctValues = 0;
				byte minValue = 0;
				byte maxValue = 0;
				long hash = 0;
				
				if (stopDetail) {
					
					mineTally = location.mineCount;
					distinctValues = 2;    // 2 is needed to keep the location living
					minValue = location.minValue;
					maxValue = location.maxValue;
					hash = location.linkedHash;
					
				} else {

					int valueCount[] = control.resetValues();
					
					for (int j=this.startSolution; j < this.endSolution; j++) {
						int value = control.solutionTable.get(j)[location.locationIndex];
						if (value != GameStateModel.MINE) {
							valueCount[value]++;
							hash += control.solutionTable.getHash(j);
						} else {
							mineTally++;
							hash -= control.solutionTable.getHash(j);
						}
					}
					

					// find the new minimum value and maximum value for this location (can't be wider than the previous min and max)
					for (byte j=location.minValue; j <= location.maxValue; j++) {
						if (valueCount[j] > 0) {
							if (distinctValues == 0) {
								minValue = j;
							}
							maxValue = j;
							distinctValues++;
							//if (maxSolutions < valueCount[j]) {
							//	maxSolutions = valueCount[j];
							//}
						}
					}
					

				}

				if (distinctValues > 1) {
					LivingLocation alive = new LivingLocation(location.locationIndex);
					alive.mineCount = mineTally;
					alive.distinctValues = distinctValues;
					alive.minValue = minValue;
					alive.maxValue = maxValue;
					//alive.maxSolutions = maxSolutions;
					//alive.zeroSolutions = valueCount[0];
					
					alive.linkedHash = hash;
					for (LivingLocation ll: living) {
						if (ll.linkedHash == alive.linkedHash) {
							alive.linked = true;
							break;
						}
					}
					
					living.add(alive);
					
					// if we have a safe file then we don't need to do any more processing because the safe tile is always picked
					if (mineTally == 0) {
						stopDetail = true;
					}
				}
				
			}
			
			Collections.sort(living);
			
			this.livingLocations = living;
			
		}
		
		@Override
		public int hashCode() {
			return (int) this.hash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Node) {
				return (this.hash == ((Node) o).hash); 
			} else {
				return false;
			}
		}

		/**
		 * Sort so the node with the most solutions is first
		 */
		@Override
		public int compareTo(Node o) {
			return - (this.endSolution - this.startSolution) - (o.endSolution - o.startSolution);
		}
		
	}
	
	private class ProcessedMove implements Comparable<ProcessedMove> {
		private final Location location;
		private final int winningLines;
		private final boolean pruned;
		private final long linkedHash;
		
		private ProcessedMove(Location loc, int winningLines, boolean pruned, long linkedHash) {
			this.location = loc;
			this.winningLines = winningLines;
			this.pruned = pruned;
			this.linkedHash = linkedHash;
		}
		
		@Override
		public int compareTo(ProcessedMove o) {
			
			int c = o.winningLines - this.winningLines;
			if (c == 0) {
				if (!this.pruned && o.pruned) {
					c = -1;
				} else if (this.pruned && !o.pruned) {
					c = 1;
				} else {
					c = 0;
				}
			}
			
			return c;
		}
		
	}
	
	
	// this is the class that processes a single top move, designed to be run in parallel 
	private class ProcessMove implements Callable<ProcessResult> {

		private final ControlData control;
		private final Node topNode;
		private final LivingLocation living;
		
		private ProcessMove(Node top, LivingLocation ll) {
			this.topNode = top;
			this.living = ll;
			
			this.control = new ControlData(allSolutions.copySolutionTable());
			
		}
		
		
		@Override
		public ProcessResult call() throws Exception {
			
			int winningLines = topNode.getWinningLines(control, living);
			
			return new ProcessResult(living, winningLines);
		}
		
	}
	
	private class ControlData {
		
		private final SolutionTable solutionTable;
		private final int[] valueCount = new int[9];
		
		private ControlData(SolutionTable solutions) {
			this.solutionTable = solutions;
		}
		
		private int[] resetValues() {
			for (int i=0; i < valueCount.length; i++) {
				valueCount[i] = 0;
			}
			return valueCount;
		}
		
	}
	
	
	private class ProcessResult {
		private final LivingLocation living;
		private final int winningLines;
		
		private boolean done = false;
		
		private ProcessResult(LivingLocation ll, int wl) {
			this.living = ll;
			this.winningLines = wl;
		}
		
	}
	
	
	private static final String INDENT = "................................................................................";
	
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private static final DecimalFormat FORMAT_NUMBER = new DecimalFormat("#,###");
	
	private long processCount = 0;
	private long maxProcessCount;
	private long processCountExtension;
	
	private int movesProcessed = 0;
	private int movesToProcess = 0;
	
	private final Solver solver;
	private final int maxSolutionSize;
	
	//private Node top;
	
	private final List<? extends Location> locations;         // the positions being analysed
	private final List<? extends Location> startLocations;    // the positions which will be considered for the first move
	
	private final List<ProcessedMove> processedMoves = new ArrayList<>();    // moves which have been processed
	
	private final SolutionTable allSolutions;
	
	private final String scope;
	
	private Node currentNode;
	private Location expectedMove;
	private int bestWinningLines = 0;
	
	private final SortSolutions[] sorters;
	
	private long cacheHit = 0;
	//private long cacheSize = 0;
	private long cacheWinningLines = 0;
	private boolean allDead = false;   // this is true if all the locations are dead
	private Area deadLocations = Area.EMPTY_AREA;
	
	// 60 million
	private final int maxCacheSize;
	
	private boolean cacheLocked = false;
	private TranspositionTable cache;
	//private int[] cacheQuantity;
	private static final int cacheThreshold = 10;
	
	private long terminalNodesReached = 0;
	
	//private int cacheCleanMod = 1;
	//private short cacheMaxDepth;
	
	// this is set when a thread discovers it has been interrupted
	private boolean aborted = false;
	
	public BruteForceAnalysisExperimental(Solver solver, List<? extends Location> locations, int size, String scope, List<Location> startLocations) {
	
		this.solver = solver;
		this.locations = locations;
		this.maxSolutionSize = size;
		this.scope = scope;
		this.allSolutions = new SolutionTable(size);
		//this.top = new Node();
		this.sorters = new SortSolutions[locations.size()];
		for (int i=0; i < sorters.length; i++) {
			this.sorters[i] = new SortSolutions(i);
		}
		
		this.startLocations = startLocations;
		
		this.maxCacheSize = solver.preferences.getBruteForceMaxCache();
		
		cache = new TranspositionTable(16000);
		
		//this.cacheQuantity = new int[locations.size()+ 1];
		
		//this.cacheMaxDepth = (short) (locations.size() - 1);

	}
	
	// this can be called by different threads when brute force is running on multiple threads
	@Override
	protected synchronized void addSolution(byte[] solution) {
		
		if (solution.length != locations.size()) {
			throw new RuntimeException("Solution does not have the correct number of locations");
		}
		
		if (allSolutions.size() >= maxSolutionSize) {
			if (!tooMany) {
				solver.logger.log(Level.WARN, "BruteForceAnalysis solution table overflow after %d solutions found (%s)", allSolutions.size(), this.scope);
			}
			tooMany = true;
			return;
		}
		
		/*
		String text = "";
		for (int i=0; i < solution.length; i++) {
			text = text + solution[i] + " ";
		}
		solver.display(text);
		*/
		
		allSolutions.addSolution(solution);
		
	}

	@Override
	protected void process() {

		solver.logger.log(Level.INFO, "EXPERIMENTAL BRUTE FORCE");
		
		long start = System.currentTimeMillis();
		solver.logger.log(Level.INFO,"System processors: " + Runtime.getRuntime().availableProcessors() + ", Total memory " + Runtime.getRuntime().totalMemory() + ", Maximum cache size " + this.maxCacheSize);
		solver.logger.log(Level.INFO, "----- Brute Force Deep Analysis starting ----");
		solver.logger.log(Level.INFO, "%d solutions in BruteForceAnalysis", allSolutions.size());

		// initialise the hash processing
		HashController.initialise(this.locations.size());
		
		// create the top node 
		Node top = buildTopNode(allSolutions);
		
		if (top.getLivingLocations().isEmpty()) {
			allDead = true;
			bestWinningLines = 1;  // only 1 winning line if everything is dead
		}
	
		this.movesToProcess = top.getLivingLocations().size();
		this.maxProcessCount = solver.preferences.getBruteForceMaxNodes() * solver.preferences.getBruteForceThreads();
		
		if (startLocations == null || startLocations.size() == 0) {
			this.processCountExtension = this.maxProcessCount / 2;
		} else {
			this.processCountExtension = 0;
		}
		
		// create a fixed thread pool
		//ExecutorService pool = Executors.newFixedThreadPool(solver.preferences.getBruteForceThreads());
		ThreadManager pool = ThreadManager.get();
		
		List<Future<ProcessResult>> futures = new ArrayList<>(); 
		for (LivingLocation move: top.getLivingLocations()) {
			
			// check that the move is in the startLocation list
			if (startLocations != null) {
				boolean found = false;
				for (Location l: startLocations) {
					if (locations.get(move.locationIndex).equals(l)) {
						found = true;
						break;
					}
				}
				if (!found) {  // if not then skip this move
					solver.logger.log(Level.INFO, "%d %s is not a starting location", move.locationIndex, locations.get(move.locationIndex));
					continue;
				}				
			}
			
			// if the move isn't linked then process it
			if (!move.linked) {
				ProcessMove call = new ProcessMove(top, move);
				Future<ProcessResult> result = pool.submit(call);
				futures.add(result);
			}
			
		}
		
		// wait for each future to complete and show the result
		List<ProcessResult> results = new ArrayList<>();
		
		boolean allDone = futures.isEmpty();
		
		while (!allDone && !aborted) {
			
			// wait on the first incomplete future for a bit
			for (Future<ProcessResult> future: futures) {
				
				if (!future.isDone()) {
					try {
						ProcessResult pr;
						try {
							pr = future.get(1000, TimeUnit.MILLISECONDS);
						} catch (TimeoutException e) {
							solver.logger.log(Level.INFO, "Process count is %,d. Terminal nodes reached %,d. Cache used %s. Cache hits %,d. Lines saved %,d. Elapsed time %s",
									this.processCount, this.terminalNodesReached, percentage(this.cache.usedSizeRatio()), this.cacheHit, this.cacheWinningLines, Timer.humanReadable(System.currentTimeMillis() - start));
							
							//if (this.cacheSize > this.maxCacheSize && !this.cacheLocked) {
							//	reduceCache();
							//}
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					
					// see if anything else has completed
					break;
				}

			}	
			
			// report on each completed process
			allDone = true;
			for (Future<ProcessResult> future: futures) {

				if (future.isDone()) {
					ProcessResult pr;
					try {
						pr = future.get();
						if (pr.done) {  // already reported it
							continue;
							
						} else {
							
							pr.done = true;
							
							LivingLocation move = pr.living;
							
							BigDecimal singleProb = BigDecimal.valueOf(allSolutions.size() - move.mineCount).divide(BigDecimal.valueOf(allSolutions.size()), Solver.DP, RoundingMode.HALF_UP);
							
							if (move.pruned) {
								solver.logger.log(Level.INFO, "%d of %d ==> Tile %s is living with safety %s, this location was pruned (max winning lines %,d)",
										this.movesProcessed + 1, this.movesToProcess, locations.get(move.locationIndex), percentage(singleProb),  pr.winningLines);
							} else {
								solver.logger.log(Level.INFO, "%d of %d ==> Tile %s is living with safety %s, winning lines %,d", 
										this.movesProcessed + 1, this.movesToProcess, locations.get(move.locationIndex), percentage(singleProb), pr.winningLines);
							}
							
							if (!move.pruned) {
								if (bestWinningLines < pr.winningLines || (top.bestLiving != null && bestWinningLines == pr.winningLines && top.bestLiving.mineCount < move.mineCount)) {
									bestWinningLines = pr.winningLines;
									top.bestLiving = move;
								}
							}
							
							results.add(pr);
							
							if (processCount < this.maxProcessCount) {
								movesProcessed++;
								
								Location loc = this.locations.get(move.locationIndex);
								processedMoves.add(new ProcessedMove(loc, pr.winningLines, move.pruned, move.linkedHash));
								
								// if we've got to half way then allow extra cycles to finish up
								if (this.processCountExtension !=0 && this.movesProcessed * 2 > this.movesToProcess) {
									this.maxProcessCount = this.maxProcessCount + this.processCountExtension;
									this.processCountExtension = 0;
									solver.logger.log(Level.INFO, "Extending BFDA cycles to %,d after %,d of %d moves analysed", this.maxProcessCount, this.movesProcessed, this.movesToProcess);
								}
							}
							
						}
					} catch (InterruptedException | ExecutionException e) {
						
						e.printStackTrace();
					}

				} else {
					allDone = false;
				}

			}	
		}

		//pool.shutdown();
		
		// sort the processed moves into best move at the top
		processedMoves.sort(null);  // use the comparable method to sort
		
		solver.logger.log(Level.INFO, "Results...");
		
		// repeat the result
		for (ProcessedMove pm: processedMoves) {
			
			BigDecimal winRate = BigDecimal.valueOf(pm.winningLines).divide(BigDecimal.valueOf(allSolutions.size()), Solver.DP, RoundingMode.HALF_UP);
			
			if (pm.pruned) {
				solver.logger.log(Level.INFO, "Tile %s has max winning lines %,d (pruned) giving max win rate of %s", pm.location, pm.winningLines, percentage(winRate));
			} else {
				solver.logger.log(Level.INFO, "Tile %s has winning lines %,d giving win rate of %s", pm.location, pm.winningLines, percentage(winRate));
			}
		}
		
		// Report on the linked tiles
		for (LivingLocation move: top.getLivingLocations()) {

			// if the tile is linked then find the link and set the winning lines
			if (move.linked) {
				for (ProcessedMove pm: processedMoves) {
					if (pm.linkedHash == move.linkedHash) {
						
						BigDecimal singleProb = BigDecimal.valueOf(allSolutions.size() - move.mineCount).divide(BigDecimal.valueOf(allSolutions.size()), Solver.DP, RoundingMode.HALF_UP);
						solver.logger.log(Level.INFO, "Tile %s is living with safety %s, this location is linked with %s", locations.get(move.locationIndex), percentage(singleProb), pm.location);

						break;
					}
				}
			} 
	
		}

		// remember what the best move is
		top.bestWinningLines = bestWinningLines;
		currentNode = top;
		
		if (processCount < this.maxProcessCount) {
			this.completed = true;
			if (solver.isShowProbabilityTree()) {
				solver.newLine("--------- Probability Tree dump start ---------");
				showTree(0, 0, top);
				solver.newLine("---------- Probability Tree dump end ----------");
			}
		}
		
		long end = System.currentTimeMillis();
		solver.logger.log(Level.INFO, "Total nodes in cache %,d, total cache hits %,d, total winning lines saved %,d", cache.usedBuckets(), cacheHit, this.cacheWinningLines);
		solver.logger.log(Level.INFO, "process explored %,d nodes in %s", processCount, Timer.humanReadable(end - start));
		solver.logger.log(Level.INFO, "----- Brute Force Deep Analysis finished ----");

		// clear down the cache
		//cache.clear();
		cache = null;
		System.gc();

	}
	
	/*
	private void reduceCache() {

		long start = System.currentTimeMillis();
		
		this.cacheLocked = true;

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long total = 0;
		long toDelete = this.cacheSize - (this.maxCacheSize / 2);
		int cutoff = 0;
		
		for (int i = this.cacheQuantity.length - 1; i > 1; i--) {
			total = total + this.cacheQuantity[i];
			this.cacheQuantity[i] = 0;
			
			if (total > toDelete) {
				cutoff = i;
				break;
			}
		}

		//System.out.println("Reducing cache below depth " + cutoff + " where depth mod is " + cacheCleanMod);
		solver.logger.log(Level.INFO, "Reducing cache below depth %d", cutoff);

		final int finalCutoff = cutoff;
	
		Predicate<Position> predicate = new Predicate<Position>() {
			@Override
			public boolean test(Position t) {
				return (t.depth >= finalCutoff);
			}
		};
		
		cache.keySet().removeIf(predicate);
		
		this.cacheSize = cache.size();
		
		solver.logger.log(Level.INFO, "Reducing cache size to %d took %s", this.cacheSize, Timer.humanReadable(System.currentTimeMillis() - start));

		// alternate between the odd and the even depths
		//if (cacheCleanMod == 1) {
		//	cacheCleanMod = 3;
		//} else {
		//	cacheCleanMod = 1;
		//}
		
		this.cacheLocked = false;
	}
	*/
	
	// return the best winning lines
	private int getBestWinningLines() {
		return this.bestWinningLines;
	}
	
	/**
	 * Builds a top of tree node based on the solutions provided
	 */
	private Node buildTopNode(SolutionTable solutionTable) {
		
		List<Location> deadLocations = new ArrayList<>();
		
		Node result = new Node(HashController.RootValue(), 0, solutionTable.size());
		
		//result.startSolution = 0;
		//result.endSolution = solutionTable.size();
		
		List<LivingLocation> living = new ArrayList<>();
		
		for (short i=0; i < locations.size(); i++) {
			int value;
			
			int valueCount[] = new int[9];
			
			int mines = 0;
			//int maxSolutions = 0;
			byte distinctValues = 0;
			byte minValue = 0;
			byte maxValue = 0;
			long hash = 0;
			
			for (int j=0; j < result.getSolutionSize(); j++) {
				if (solutionTable.get(j)[i] != GameStateModel.MINE) {
					value = solutionTable.get(j)[i];
					valueCount[value]++;
					hash += solutionTable.getHash(j);
				} else {
					mines++;
					hash -= solutionTable.getHash(j);
				}
			}
			
			for (byte j=0; j < valueCount.length; j++) {
				if (valueCount[j] > 0) {
					if (distinctValues == 0) {
						minValue = j;
					}
					maxValue = j;
					distinctValues++;
					//if (maxSolutions < valueCount[j]) {
					//	maxSolutions = valueCount[j];
					//}
				}
			}
			if (distinctValues > 1) {
				LivingLocation alive = new LivingLocation(i);
				alive.mineCount = mines;
				//alive.distinctValues = distinctValues;
				alive.minValue = minValue;
				alive.maxValue = maxValue;
				//alive.maxSolutions = maxSolutions;
				//alive.zeroSolutions = valueCount[0];
				
				alive.linkedHash = hash;
				for (LivingLocation ll: living) {
					if (ll.linkedHash == alive.linkedHash) {
						alive.linked = true;
						alive.pruned = true;
						break;
					}
				}
				
				living.add(alive);
			} else {
				if (mines == result.getSolutionSize()) {
					solver.logger.log(Level.INFO, "Tile %s is a mine", locations.get(i));
				} else {
					solver.logger.log(Level.INFO, "Tile %s is dead with value %d", locations.get(i), minValue);
					deadLocations.add(locations.get(i));
				}
			}
			
		}
		
		Collections.sort(living);
		
		result.livingLocations = living;
		
		this.deadLocations = new Area(deadLocations);
		
		return result;
	}
	
	/*
	private int[] resetValues(int thread) {
		for (int i=0; i < valueCount[thread].length; i++) {
			valueCount[thread][i] = 0;
		}
		return valueCount[thread];
	}
	*/
	
	@Override
	protected int getSolutionCount() {
		return allSolutions.size();
	}
	
	@Override
	protected long getNodeCount() {
		return processCount;
	}
	
	@Override
	protected Action getNextMove(BoardState boardState) {
		
		LivingLocation bestLiving = getBestLocation(currentNode);
		
		if (bestLiving == null) {
			return null;
		}
		
		Location loc = this.locations.get(bestLiving.locationIndex);

		//solver.display("first best move is " + loc.display());
		BigDecimal prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(bestLiving.mineCount).divide(BigDecimal.valueOf(currentNode.getSolutionSize()), Solver.DP, RoundingMode.HALF_UP));
		
		while (boardState.isRevealed(loc)) {
			int value = boardState.getWitnessValue(loc);
			
			currentNode = bestLiving.children[value];
			bestLiving = getBestLocation(currentNode);
			if (bestLiving == null) {
				return null;
			}
			prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(bestLiving.mineCount).divide(BigDecimal.valueOf(currentNode.getSolutionSize()), Solver.DP, RoundingMode.HALF_UP));

			loc = this.locations.get(bestLiving.locationIndex);
			
		}
		
		solver.logger.log(Level.INFO, "Solutions with mines is %d out of %d", bestLiving.mineCount, currentNode.getSolutionSize());
		for (int i=0; i < bestLiving.children.length; i++) {
			if (bestLiving.children[i] == null) {
				//solver.display("Value of " + i + " is not possible");
				continue; //ignore this node but continue the loop
			}
			
			String probText;
			if (bestLiving.children[i].bestLiving == null) {
				probText =  Action.FORMAT_2DP.format(ONE_HUNDRED.divide(BigDecimal.valueOf(bestLiving.children[i].getSolutionSize()), Solver.DP, RoundingMode.HALF_UP)) + "%";
			} else {
				probText = Action.FORMAT_2DP.format(bestLiving.children[i].getProbability().multiply(ONE_HUNDRED)) + "%";
			}
			solver.logger.log(Level.INFO, "Value of %d leaves %d solutions and winning probability %s", i, bestLiving.children[i].getSolutionSize(), probText);
		}
		
		String text = " (solve " + scope + " " + Action.FORMAT_2DP.format(currentNode.getProbability().multiply(ONE_HUNDRED)) + "%)";
		Action action = new Action(loc, Action.CLEAR, MoveMethod.BRUTE_FORCE_DEEP_ANALYSIS, text, prob);
		
		expectedMove = loc;
		
		return action;
		
	}
	
	private LivingLocation getBestLocation(Node node) {
		
		return node.bestLiving;
		
	}
	
	
	private void showTree(int depth, int value, Node node) {
		
		String condition;
		if (depth == 0) {
			condition = node.getSolutionSize() + " solutions remain"; 
		} else {
			condition = "When '" + value + "' ==> " + node.getSolutionSize() + " solutions remain";
		}
		
		if (node.bestLiving == null) {
			String line = INDENT.substring(0, depth*3) + condition + " Solve chance " + Action.FORMAT_2DP.format(node.getProbability().multiply(ONE_HUNDRED)) + "%";
			System.out.println(line);
			solver.newLine(line);
			return;
		}
		
		Location loc = this.locations.get(node.bestLiving.locationIndex);

		BigDecimal prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(node.bestLiving.mineCount).divide(BigDecimal.valueOf(node.getSolutionSize()), Solver.DP, RoundingMode.HALF_UP));
		
		
		String line = INDENT.substring(0, depth*3) + condition + " play " + loc.toString() + " Survival chance " + Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED)) + "%, Solve chance " + Action.FORMAT_2DP.format(node.getProbability().multiply(ONE_HUNDRED)) + "%";
		
		System.out.println(line);
		solver.newLine(line);
		
		//for (Node nextNode: node.bestLiving.children) {
		for (int val=0; val < node.bestLiving.children.length; val++) {
			Node nextNode = node.bestLiving.children[val];
			if (nextNode != null) {
				showTree(depth + 1, val, nextNode);
			}
			
		}
		
	}
	
	
	@Override
	protected Location getExpectedMove() {
		return expectedMove;
	}
	
	private String percentage(BigDecimal prob) {
		
		return Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED)) + "%";
	}

	private String percentage(float prob) {
		
		return Action.FORMAT_2DP.format(100 * prob) + "%";
	}
	
	@Override
	protected boolean allDead() {
		return allDead;
	}

	@Override
	Area getDeadLocations() {
		return deadLocations;
	}

	@Override
	protected int getMovesProcessed() {
		return movesProcessed;
	}

	@Override
	protected int getMovesToProcess() {
		return this.movesToProcess;
	}

	@Override
	protected Location checkForBetterMove(Location location) {
		
		// no moves processed
		if (processedMoves.size() == 0) {
			return null;
		}
		
		ProcessedMove best = processedMoves.get(0);
		
		// the move is already the best 
		if (location.equals(best.location)) {
			solver.logger.log(Level.INFO, "Tile %s (Winning %d) is best according to partial BFDA", location, best.winningLines);
			return null;
		}
		
		// if the chosen location has been processed and it isn't the best then send the best
		for (ProcessedMove pm: processedMoves) {
			if (pm.location.equals(location)) {
				solver.logger.log(Level.INFO, "Tile %s (Winning %d pruned %b) replaced by %s (winning %d pruned %b)", location, pm.winningLines, pm.pruned, best.location, best.winningLines, best.pruned);
				return best.location;
			}
		}

		// the chosen location hasn't been processed
		return null;
	}

	@Override
	BigDecimal getSolveChance() {
		return this.currentNode.getProbability();
	}

	@Override
	List<? extends Location> getLocations() {
		return locations;
	}
	
}
