package minesweeper.solver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class BruteForceAnalysis extends BruteForceAnalysisModel{
	
	// used to hold all the solutions left in the game
	private class SolutionTable {
		
		private final byte[][] solutions;
		private int size = 0;

		private SolutionTable(int maxSize) {
			solutions = new byte[maxSize][];
		}
		
		private void addSolution(byte[] solution) {
			solutions[size] = solution;
			size++;
		};
		
		private int size() {
			return size;
		}
		
		private byte[] get(int index) {
			return solutions[index];
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
	private class Position {
		
		private final byte[] position;
		private int hash;
		
		private Position() {
			position = new byte[locations.size()];
			for (int i=0; i < position.length; i++) {
				position[i] = 15;
			}
		}
		
		private Position(Position p, int index, int value) {
			// copy and update to reflect the new position
			position = Arrays.copyOf(p.position, p.position.length);
			position[index] = (byte) (value + 50);			
		}
		
		@Override
		// copied from String hash
		public int hashCode() {
	        int h = hash;
	        if (h == 0 && position.length > 0) {
	            for (int i = 0; i < position.length; i++) {
	                h = 31 * h + position[i];
	            }
	            hash = h;
	        }
	        return h;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Position) {
				for (int i=0; i < position.length; i++) {
					if (this.position[i] != ((Position) o).position[i]) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		}
	}
	

	
	/**
	 * Positions on the board which can still reveal information about the game.
	 */
	private class LivingLocation implements Comparable<LivingLocation>{
		
		//private int winningLines = 0;
		private boolean pruned = false;
		private final short index;
		private int mineCount = 0;  // number of remaining solutions which have a mine in this position
		private int maxSolutions = 0;    // the maximum number of solutions that can be remaining after clicking here
		private int zeroSolutions = 0;    // the number of solutions that have a '0' value here
		private byte maxValue = -1;
		private byte minValue = -1;
		private byte count;  // number of possible values at this location
		
		private Node[] children;

		private LivingLocation(short index) {
			this.index = index;
		}

		/**
		 * Determine the Nodes which are created if we play this move. Up to 9 positions where this locations reveals a value [0-8].
		 * @param location
		 * @return
		 */
		private void buildChildNodes(Node parent) {
			
			// sort the solutions by possible values
			allSolutions.sortSolutions(parent.startLocation, parent.endLocation, this.index);
			int index = parent.startLocation;
			
			// skip over the mines
			while (index < parent.endLocation && allSolutions.get(index)[this.index] == GameStateModel.MINE) {
				index++;
			}
			
			Node[] work = new Node[9];
			for (int i=this.minValue; i < this.maxValue + 1; i++) {
				
				// if the node is in the cache then use it
				Position pos = new Position(parent.position, this.index, i);
				
				Node temp1 = cache.get(pos);
				if (temp1 == null) {

					Node temp = new Node(pos);
					
					temp.startLocation = index;
					// find all solutions for this values at this location
					while (index < parent.endLocation && allSolutions.get(index)[this.index] == i) {
						index++;
					}					
					temp.endLocation = index;
					
					work[i] = temp;
					
				} else {
					//System.out.println("In cache " + temp.position.key + " " + temp1.position.key);
					//if (!temp.equals(temp1)) {
					//	System.out.println("Cache not equal!!");
					//}
					//temp1.fromCache = true;
					work[i] = temp1;
					cacheHit++;
					cacheWinningLines = cacheWinningLines + temp1.winningLines;
					// skip past these details in the array
					while (index < parent.endLocation && allSolutions.get(index)[this.index] <= i) {
						index++;
					}					
				}

			}

			if (index != parent.endLocation) {
				System.out.println("Didn't read all the elements in the array; index = " + index + " end = " + parent.endLocation);
			}
			
			
			for (int i=this.minValue; i <= this.maxValue; i++) {
				if (work[i].getSolutionSize() > 0) {
					//if (!work[i].fromCache) {
					//	work[i].determineLivingLocations(this.livingLocations, living.index);
					//}
				} else {
					work[i] = null;   // if no solutions then don't hold on to the details
				}
				
			}

			this.children = work;
			
		}
		
		@Override
		public int compareTo(LivingLocation o) {

			// return location most likely to be clear  - this has to be first, the logic depends upon it
			int test = this.mineCount - o.mineCount;
			if (test != 0) {
				return test;
			}
			
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
	
		}
		
	}
	
	/**
	 * A representation of a possible state of the game
	 */
	private class Node {

		private Position position ;        // representation of the position we are analysing / have reached
		
		private int winningLines = 0;      // this is the number of winning lines below this position in the tree
		private int work = 0;              // this is a measure of how much work was needed to calculate WinningLines value
		private boolean fromCache = false; // indicates whether this position came from the cache
		
		private int startLocation;              // the first solution in the solution array that applies to this position
		private int endLocation;                // the last + 1 solution in the solution array that applies to this position
		
		private List<LivingLocation> livingLocations;   // these are the locations which need to be analysed
		 
		private LivingLocation bestLiving;              // after analysis this is the location that represents best play

		private Node() {
			position = new Position();
		}
		
		private Node(Position position) {
			this.position = position;
		}
		
		private List<LivingLocation> getLivingLocations() {
			return livingLocations;
		}
		
		private int getSolutionSize() {
			return endLocation - startLocation;
		}
		
		/**
		 * Get the probability of winning the game from the position this node represents  (winningLines / solution size)
		 * @return
		 */
		private BigDecimal getProbability() {
			
			return BigDecimal.valueOf(winningLines).divide(BigDecimal.valueOf(getSolutionSize()), Solver.DP, RoundingMode.HALF_UP); 
			
		}
		
		/**
		 * Calculate the number of winning lines if this move is played at this position
		 * Used at top of the game tree
		 */
		private int getWinningLines(LivingLocation move) {

			//if we can never exceed the cutoff then no point continuing
			if (Solver.PRUNE_BF_ANALYSIS && this.getSolutionSize() - move.mineCount <= this.winningLines) {
				move.pruned = true;
				return (this.getSolutionSize() - move.mineCount);
			}
			
			int winningLines = getWinningLines(1, move, this.winningLines);
			
			if (winningLines > this.winningLines) {
				this.winningLines = winningLines;
			}
			
			return winningLines;
		}
		
		
		/**
		 * Calculate the number of winning lines if this move is played at this position
		 * Used when exploring the game tree
		 */
		private int getWinningLines(int depth, LivingLocation move, int cutoff) {

			int result = 0;

			int notMines = this.getSolutionSize() - move.mineCount;
			
			// if the max possible winning lines is less than the current cutoff then no point doing the analysis
			if (Solver.PRUNE_BF_ANALYSIS && notMines <= cutoff) {
				move.pruned = true;
				return notMines;
			}
			
			// we're going to have to do some work
			processCount++;
			if (processCount > maxProcessCount) {
				return 0;
			}
			
			move.buildChildNodes(this);   
			
			for (Node child: move.children) {

				if (child == null) {
					continue;  // continue the loop but ignore this entry
				}
				
				if (child.fromCache) {  // nothing more to do, since we did it before
					this.work++;
				} else {
					
					child.determineLivingLocations(this.livingLocations, move.index);
					this.work++;
									
					if (child.getLivingLocations().isEmpty()) {  // no further information ==> all solution indistinguishable ==> 1 winning line

						child.winningLines = 1;
							
					} else {  // not cached and not terminal node, so we need to do the recursion
						
						for (LivingLocation childMove: child.getLivingLocations()) {
							
							// if the number of safe solutions <= the best winning lines then we can't do any better, so skip the rest
							if (child.getSolutionSize() - childMove.mineCount <= child.winningLines) {
								break;
							}
							
							// now calculate the winning lines for each of these children
							int winningLines = child.getWinningLines(depth + 1, childMove, child.winningLines);
							if (!childMove.pruned) {
								if (child.winningLines < winningLines || (child.bestLiving != null && child.winningLines == winningLines && child.bestLiving.mineCount < childMove.mineCount)) {
									child.winningLines = winningLines;
									child.bestLiving = childMove;
								}
							}
							
							// if there are no mines then this is a 100% safe move, so skip any further analysis since it can't be any better
							if (childMove.mineCount == 0) {
								break;
						 	}
							
						}

						// no need to hold onto the living location once we have determined the best of them
						child.livingLocations = null;

						//if (depth > solver.preferences.BRUTE_FORCE_ANALYSIS_TREE_DEPTH) {  // stop holding the tree beyond this depth
						//	child.bestLiving = null;
						//}
						
						// add the child to the cache if it didn't come from there and it is carrying sufficient winning lines
						if (child.work > 30) {
							child.work = 0;
							child.fromCache = true;
							cacheSize++;
							cache.put(child.position, child);
						} else {
							this.work = this.work + child.work;
						}

						
					}
					
				}
			
				if (depth > solver.preferences.getBruteForceTreeDepth()) {  // stop holding the tree beyond this depth
					child.bestLiving = null;
				}
				
				// store the aggregate winning lines 
				result = result + child.winningLines;	
				
				notMines = notMines - child.getSolutionSize();  // reduce the number of not mines
				
				// if the max possible winning lines is less than the current cutoff then no point doing the analysis
				if (Solver.PRUNE_BF_ANALYSIS && result + notMines <= cutoff) {
					move.pruned = true;
					return (result + notMines);
				}
				
			}
			
			return result;
			
		}
		
		/**
		 * this generates a list of Location that are still alive, (i.e. have more than one possible value) from a list of previously living locations
		 * Index is the move which has just been played (in terms of the off-set to the position[] array)
		 */
		private void determineLivingLocations(List<LivingLocation> liveLocs, int index) {
			
			List<LivingLocation> living = new ArrayList<>(liveLocs.size());
			
			for (LivingLocation live: liveLocs) {
				
				if (live.index == index) {  // if this is the same move we just played then no need to analyse it - definitely now non-living.
					continue;
				}
				
				int value;
				
				int valueCount[] = resetValues(0);
				int mines = 0;
				int maxSolutions = 0;
				byte count = 0;
				byte minValue = 0;
				byte maxValue = 0;
				
				for (int j=startLocation; j < endLocation; j++) {
					value = allSolutions.get(j)[live.index];
					if (value != GameStateModel.MINE) {
						//values[value] = true;
						valueCount[value]++;
					} else {
						mines++;
					}
				}
				
				// find the new minimum value and maximum value for this location (can't be wider than the previous min and max)
				for (byte j=live.minValue; j <= live.maxValue; j++) {
					if (valueCount[j] > 0) {
						if (count == 0) {
							minValue = j;
						}
						maxValue = j;
						count++;
						if (maxSolutions < valueCount[j]) {
							maxSolutions = valueCount[j];
						}
					}
				}
				if (count > 1) {
					LivingLocation alive = new LivingLocation(live.index);
					alive.mineCount = mines;
					alive.count = count;
					alive.minValue = minValue;
					alive.maxValue = maxValue;
					alive.maxSolutions = maxSolutions;
					alive.zeroSolutions = valueCount[0];
					living.add(alive);
				}
				
			}
			
			Collections.sort(living);
			
			this.livingLocations = living;
			
		}
		
		@Override
		public int hashCode() {
			return position.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Node) {
				return position.equals(((Node) o).position); 
			} else {
				return false;
			}
		}
		
	}
	
	private class ProcessedMove implements Comparable<ProcessedMove> {
		private final Location location;
		private final int winningLines;
		private final boolean pruned;
		
		private ProcessedMove(Location loc, int winningLines, boolean pruned) {
			this.location = loc;
			this.winningLines = winningLines;
			this.pruned = pruned;
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
	
	
	private static final String INDENT = "................................................................................";
	
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	
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
	
	private final SortSolutions[] sorters;
	
	private int cacheHit = 0;
	private int cacheSize = 0;
	private int cacheWinningLines = 0;
	private boolean allDead = false;   // this is true if all the locations are dead
	private Area deadLocations = Area.EMPTY_AREA;
	
	// some work areas to prevent having to instantiate many 1000's of copies of them 
	//private final boolean[] values = new boolean[9];
	private final int[][] valueCount = new int[2][9];
	
	private Map<Position, Node> cache = new HashMap<>(5000);
	
	public BruteForceAnalysis(Solver solver, List<? extends Location> locations, int size, String scope, List<Location> startLocations) {
		
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

		long start = System.currentTimeMillis();
		
		solver.logger.log(Level.INFO, "----- Brute Force Deep Analysis starting ----");
		solver.logger.log(Level.INFO, "%d solutions in BruteForceAnalysis", allSolutions.size());
		
		// create the top node 
		Node top = buildTopNode(allSolutions);
		
		if (top.getLivingLocations().isEmpty()) {
			allDead = true;
		}
		
		this.movesToProcess = top.getLivingLocations().size();
		this.maxProcessCount = solver.preferences.getBruteForceMaxNodes();
		
		if (startLocations == null || startLocations.size() == 0) {
			this.processCountExtension = this.maxProcessCount / 2;
		} else {
			this.processCountExtension = 0;
		}
		
		int best = 0;
		
		for (LivingLocation move: top.getLivingLocations()) {
			
			// check that the move is in the startLocation list
			if (startLocations != null) {
				boolean found = false;
				for (Location l: startLocations) {
					if (locations.get(move.index).equals(l)) {
						found = true;
						break;
					}
				}
				if (!found) {  // if not then skip this move
					solver.logger.log(Level.INFO, "%d %s is not a starting location", move.index, locations.get(move.index));
					continue;
				}				
			}

			int winningLines = top.getWinningLines(move);  // calculate the number of winning lines if this move is played
			
			if (!move.pruned) {
				if (best < winningLines || (top.bestLiving != null && best == winningLines && top.bestLiving.mineCount < move.mineCount)) {
					best = winningLines;
					top.bestLiving = move;
				}
			}
			
			BigDecimal singleProb = BigDecimal.valueOf(allSolutions.size() - move.mineCount).divide(BigDecimal.valueOf(allSolutions.size()), Solver.DP, RoundingMode.HALF_UP);
			
			if (move.pruned) {
				solver.logger.log(Level.INFO, "%d %s is living with %d possible values and probability %s, this location was pruned (max winning lines %d)", move.index, locations.get(move.index), move.count, percentage(singleProb),  winningLines);
			} else {
				solver.logger.log(Level.INFO, "%d %s is living with %d possible values and probability %s, winning lines %d", move.index, locations.get(move.index), move.count, percentage(singleProb),  winningLines);
			}
			
			if (processCount < this.maxProcessCount) {
				movesProcessed++;
				
				Location loc = this.locations.get(move.index);
				processedMoves.add(new ProcessedMove(loc, winningLines, move.pruned));
				
				// if we've got to half way then allow extra cycles to finish up
				if (this.processCountExtension !=0 && this.movesProcessed * 2 > this.movesToProcess) {
					this.maxProcessCount = this.maxProcessCount + this.processCountExtension;
					this.processCountExtension = 0;
					solver.logger.log(Level.INFO, "Extending BFDA cycles to %d after %d of %d moves analysed", this.maxProcessCount, this.movesProcessed, this.movesToProcess);
				}
			}
			
		}
		
		// sort the processed moves into best move at the top
		processedMoves.sort(null);  // use the comparable method to sort
		
		top.winningLines = best;
		
		currentNode = top;
		
		if (processCount < this.maxProcessCount) {
			this.completed = true;
			if (solver.isShowProbabilityTree()) {
				solver.newLine("--------- Probability Tree dump start ---------");
				showTree(0, 0, top);
				solver.newLine("---------- Probability Tree dump end ----------");
			}
		}
		
		
		// clear down the cache
		cache.clear();
		
		long end = System.currentTimeMillis();
		solver.logger.log(Level.INFO, "Total nodes in cache %d, total cache hits %d, total winning lines saved %d", cacheSize, cacheHit, this.cacheWinningLines);
		solver.logger.log(Level.INFO, "process took %d milliseconds and explored %d nodes", (end - start), processCount);
		solver.logger.log(Level.INFO, "----- Brute Force Deep Analysis finished ----");
	}
	
	/**
	 * Builds a top of tree node based on the solutions provided
	 */
	private Node buildTopNode(SolutionTable solutionTable) {
		
		List<Location> deadLocations = new ArrayList<>();
		
		Node result = new Node();
		
		result.startLocation = 0;
		result.endLocation = solutionTable.size();
		
		List<LivingLocation> living = new ArrayList<>();
		
		for (short i=0; i < locations.size(); i++) {
			int value;
			
			int valueCount[] = resetValues(0);
			int mines = 0;
			int maxSolutions = 0;
			byte count = 0;
			byte minValue = 0;
			byte maxValue = 0;
			
			for (int j=0; j < result.getSolutionSize(); j++) {
				if (solutionTable.get(j)[i] != GameStateModel.MINE) {
					value = solutionTable.get(j)[i];
					//values[value] = true;
					valueCount[value]++;
				} else {
					mines++;
				}
			}
			
			for (byte j=0; j < valueCount.length; j++) {
				if (valueCount[j] > 0) {
					if (count == 0) {
						minValue = j;
					}
					maxValue = j;
					count++;
					if (maxSolutions < valueCount[j]) {
						maxSolutions = valueCount[j];
					}
				}
			}
			if (count > 1) {
				LivingLocation alive = new LivingLocation(i);
				alive.mineCount = mines;
				alive.count = count;
				alive.minValue = minValue;
				alive.maxValue = maxValue;
				alive.maxSolutions = maxSolutions;
				alive.zeroSolutions = valueCount[0];
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
	
	private int[] resetValues(int thread) {
		for (int i=0; i < valueCount[thread].length; i++) {
			valueCount[thread][i] = 0;
		}
		return valueCount[thread];
	}
	
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
		
		Location loc = this.locations.get(bestLiving.index);

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

			loc = this.locations.get(bestLiving.index);
			
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
		
		Location loc = this.locations.get(node.bestLiving.index);

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
		
		return Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED));
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
	
}
