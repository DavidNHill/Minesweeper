package minesweeper.solver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;

public class BruteForceAnalysis extends BruteForceAnalysisModel{
	
	private class Position {
		
		private char[] position;
		private String key;
		
		// probability to solve the puzzle from this position
		//private BigDecimal solveProbability;
		
		private Position() {
			position = new char[locations.size()];
			for (int i=0; i < position.length; i++) {
				position[i] = 15;
			}
		}
		
		private Position(Position p, int index, int value) {
			// copy and update to reflect the new position
			position = p.position.clone();
			position[index] = (char) (value + 50);			
			key = new String(position);
		}
		
		@Override
		public int hashCode() {
			return key.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Position) {
				return key.equals(((Position) o).key); 
			} else {
				return false;
			}
		}
	}
	
	
	private class Living implements Comparable<Living>{
		
		private BigDecimal probability = BigDecimal.ZERO;
		private boolean pruned = false;
		private final int index;
		private int mines = 0;
		private byte maxValue = -1;
		private byte minValue = -1;
		//private int[] values = new int[9];
		private int count;  // number of possible values at this location
		
		//private List<Node> children;
		private Node[] children;

		private Living(int index) {
			this.index = index;
		}

		@Override
		public int compareTo(Living o) {

			// return in most likely to be clear
			int test = this.mines - o.mines;
			if (test != 0) {
				return test;
			}
			
			// then by most number of different possible values
			return o.count - this.count;
			
		}
		
	}
	
	private class Node {
		
		//private int index;
		//private int value;
		
		// this is the best probability from the list of living locations at this node
		private BigDecimal probability = BigDecimal.ZERO;
		private boolean fromCache = false; // indicates whether this position came from the cache
		
		// holds the position we are analysing / have reached
		private Position position ; 
		
		private final List<byte[]> solutions;

		private List<Living> alive;
		
		private Living bestLiving;

		private Node() {
			this(250);
		}
		
		
		private Node(int size) {
			solutions = new ArrayList<>(size);
			position = new Position();

		}

		// create a child node which has an updated position
		private Node(int size, Node parent, int index, int value) {
			solutions = new ArrayList<>(size);
			//this.index = index;
			//this.value = value;
			
			position = new Position(parent.position, index, value);

		}
		
		private List<Living> getLivingLocations() {
			return alive;
		}
		
		
		private Node[] getChildren(Living living) {
			
			//List<Node> result = new ArrayList<>();
			
			Node[] work = new Node[9];
			for (int i=living.minValue; i < living.maxValue + 1; i++) {
				
				// if the node is in the cache then use it
				Node temp = new Node(this.solutions.size(), this, living.index, i);
				Node temp1 = cache.get(temp);
				if (temp1 == null) {
					work[i] = temp;
				} else {
					//System.out.println("In cache " + temp.position.key + " " + temp1.position.key);
					//if (!temp.equals(temp1)) {
					//	System.out.println("Cache not equal!!");
					//}
					temp1.fromCache = true;
					work[i] = temp1;
				}
				
				//work[i] = new Node(this.solutions.size(), this, index, i);

			}

			// add the solution into the correct node base upon the value it has
			for (byte[] sol: this.solutions) {
				if (sol[living.index] != GameStateModel.MINE) {
					if (!work[sol[living.index]].fromCache) {
						work[sol[living.index]].solutions.add(sol);
					}
				}
			}			

			for (int i=living.minValue; i < living.maxValue + 1; i++) {
				if (work[i].solutions.size() > 0) {
					if (!work[i].fromCache) {
						work[i].determineLivingLocations(this.alive);
					}
					//result.add(work[i]);
				} else {
					work[i] = null;   // if no solutions then don't hold on to the details
				}
				
			}

			return work;
			//return result;
			
		}
		
		/**
		 * this is a list of indices to Location that are still alive, i.e. have more than one possible value
		 */
		private void determineLivingLocations() {
			
			List<Living> living = new ArrayList<>();
			
			for (int i=0; i < locations.size(); i++) {
				int value;
				//Living alive = new Living(i);
				
				boolean[] values = new boolean[9];
				int mines = 0;
				byte count = 0;
				byte minValue = 0;
				byte maxValue = 0;
				
				for (int j=0; j < solutions.size(); j++) {
					if (solutions.get(j)[i] != GameStateModel.MINE) {
						value = solutions.get(j)[i];
						values[value] = true;
					} else {
						mines++;
					}
				}
				
				for (byte j=0; j < values.length; j++) {
					if (values[j]) {
						if (count == 0) {
							minValue = j;
						}
						maxValue = j;
						count++;
					}
				}
				if (count > 1) {
					Living alive = new Living(i);
					alive.mines = mines;
					alive.count = count;
					alive.minValue = minValue;
					alive.maxValue = maxValue;
					living.add(alive);
				}
				
			}
			
			Collections.sort(living);
			
			this.alive = living;
			
		}
		
		/**
		 * this is a list of indices to Location that are still alive, i.e. have more than one possible value
		 */
		private void determineLivingLocations(List<Living> liveLocs) {
			
			List<Living> living = new ArrayList<>(liveLocs.size());
			
			for (Living live: liveLocs) {
				int value;
				//Living alive = new Living(live.index);
				
				// TODO can also ceate this once and then reset it to prevent object creation
				boolean[] values = new boolean[9];
				int mines = 0;
				byte count = 0;
				byte minValue = 0;
				byte maxValue = 0;
				
				for (int j=0; j < solutions.size(); j++) {
					value = solutions.get(j)[live.index];
					if (value != GameStateModel.MINE) {
						values[value] = true;
					} else {
						mines++;
					}
				}
				
				for (byte j=0; j < values.length; j++) {
					if (values[j]) {
						if (count == 0) {
							minValue = j;
						}
						maxValue = j;
						count++;
					}
				}
				if (count > 1) {
					Living alive = new Living(live.index);
					alive.mines = mines;
					alive.count = count;
					alive.minValue = minValue;
					alive.maxValue = maxValue;
					living.add(alive);
				}
				
			}
			
			Collections.sort(living);
			
			this.alive = living;
			
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
	
	//private static final int MAX_PROCESSING = 20000;
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	
	private int processCount = 0;
	
	private final Solver solver;
	private final int maxSolutionSize;
	
	private final Node top;
	
	private final List<? extends Location> locations;
	
	private Node currentNode;
	private Location expectedMove;
	
	//private boolean completed = false;
	//private boolean tooMany = false;
	//private boolean shallow = false;
	
	private Map<Node, Node> cache = new HashMap<>();
	
	public BruteForceAnalysis(Solver solver, List<? extends Location> locations, int size) {
		
		this.solver = solver;
		this.locations = locations;
		this.maxSolutionSize = size;
		this.top = new Node(size);
	}
	
	// this can be called by different threads when brute force is running on multiple threads
	@Override
	protected synchronized void addSolution(byte[] solution) {
		
		if (solution.length != locations.size()) {
			throw new RuntimeException("Solution does not have the correct number of locations");
		}
		
		if (top.solutions.size() >= maxSolutionSize) {
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
		
		top.solutions.add(solution);
		
	}

	@Override
	protected void process() {

		long start = System.currentTimeMillis();
		
		solver.display("----- Brute Force Deep Analysis starting ----");
		solver.display(top.solutions.size() + " solutions in BruteForceAnalysis");
		
		// determine which locations are alive
		top.determineLivingLocations();
		
		BigDecimal best = BigDecimal.ZERO;
		
		for (Living alive: top.getLivingLocations()) {
			
			BigDecimal prob = process(1, top, alive, best);
			
			if (best.compareTo(prob) < 0) {
				best = prob;
				top.bestLiving = alive;
			}
			
			BigDecimal singleProb = BigDecimal.valueOf(top.solutions.size() - alive.mines).divide(BigDecimal.valueOf(top.solutions.size()), Solver.DP, RoundingMode.HALF_UP);
			
			if (alive.pruned) {
				solver.display(alive.index + " " + locations.get(alive.index).display() + " is living with " + alive.count + " possible values and probability " + percentage(singleProb) + ", this location was pruned");
			} else {
				solver.display(alive.index + " " + locations.get(alive.index).display() + " is living with " + alive.count + " possible values and probability " + percentage(singleProb) + ", winning probability is " + percentage(prob));
			}
			
			
		}
		
		currentNode = top;
		
		if (processCount < solver.preferences.BRUTE_FORCE_ANALYSIS_MAX_NODES) {
			this.completed = true;
		}
		
		long end = System.currentTimeMillis();
		solver.display("process took " + (end - start) + " milliseconds and explored " + processCount + " nodes" );
		solver.display("----- Brute Force Deep Analysis finished ----");
	}
	
	
	// cut off is the best solution we have so far
	private BigDecimal process(int depth, Node parent, Living parentAlive, BigDecimal cutoff) {

		BigDecimal result = BigDecimal.ZERO;
		
		processCount++;
		if (processCount > solver.preferences.BRUTE_FORCE_ANALYSIS_MAX_NODES) {
			return result;
		}

		// make the cutoff larger to avoid rounding errors making a difference
		BigDecimal useCutoff = cutoff.setScale(Solver.DP -2, RoundingMode.CEILING);
		//BigDecimal useCutoff = cutoff;
		
		parentAlive.children = parent.getChildren(parentAlive);

		int notMines = parent.solutions.size() - parentAlive.mines;
		
		for (Node child: parentAlive.children) {

			if (child == null) {
				continue;  // continue the loop but ignore this entry
			}
			
			// this is the maximum probability that this move can yield, i.e. if everything else in the loop is 100%
			BigDecimal maxProb = result.add(BigDecimal.valueOf(notMines).divide(BigDecimal.valueOf(parent.solutions.size()), Solver.DP, RoundingMode.HALF_UP)); 
			
			// if the max probability is less than the current cutoff then no point doing the analysis
			if (Solver.PRUNE_BF_ANALYSIS && maxProb.compareTo(useCutoff) <= 0) {
				parentAlive.pruned = true;
				return result;
			}
			
			boolean doCache = false;
			BigDecimal best = BigDecimal.ZERO;
			if (child.fromCache) {
				best = child.probability;
			} else if (child.solutions.size() == 0) {
				solver.display("Zero solutions!");
				best = BigDecimal.ZERO;
			} else if (child.solutions.size() == 1) {
				best = BigDecimal.ONE;   // if only one solution left then we have solved it
				//solver.display("End - one solution");
			} else if (child.getLivingLocations().isEmpty()) {
				best = BigDecimal.ONE.divide(BigDecimal.valueOf(child.solutions.size()), Solver.DP, RoundingMode.HALF_UP);  // no further information ==> all solution indistinguishable ==> 1 / number of solutions
				//solver.display("End - nothing alive");
			} else {
				doCache = true;
				for (Living alive: child.getLivingLocations()) {
					BigDecimal prob = process(depth + 1, child, alive, best);
					if (best.compareTo(prob) < 0) {
						best = prob;
						child.bestLiving = alive;
					}
					// if there are no mines then this is a 100% certain move, so skip any further analysis
					if (alive.mines == 0) {
						break;
					}
				}
			}
			
			// only hold the details to a certain depth , this means we will need re-analyse the position after that many moves time
			// it doesn't affect the quality of this tree search, only what is remembered about it.
			//if (depth > 4 && child.bestLiving != null && child.bestLiving.mines != 0) {
			//	child.bestLiving = null;
			//}
			
			//BigDecimal work = best.multiply(BigDecimal.valueOf(parentAlive.values[child.value])).divide(BigDecimal.valueOf(parent.solutions.size()), Solver.DP, RoundingMode.HALF_UP);
			
			BigDecimal work = best.multiply(BigDecimal.valueOf(child.solutions.size())).divide(BigDecimal.valueOf(parent.solutions.size()), Solver.DP, RoundingMode.HALF_UP);
			
			// best probability available at this node

			// add the child to the cache if it didn't come from there
			if (!child.fromCache && doCache && depth < 10) {
				child.probability = best;
				cache.put(child, child);
			}
			
			result = result.add(work);	
			notMines = notMines - child.solutions.size();  // reduce the number of not mines
			
		}
		
		parentAlive.probability = result;

		return result;
		
	}

	/*
	protected boolean isComplete() {
		return this.completed;
	}
	
	protected boolean tooMany() {
		return this.tooMany;
	}
	
	protected boolean isShallow() {
		return false;
	}
	*/
	
	@Override
	protected int getSolutionCount() {
		return top.solutions.size();
	}
	
	@Override
	protected Action getNextMove(BoardState boardState) {
		
		//Node parentNode = currentNode;
		
		Living bestLiving = getBestLocation(currentNode);
		
		if (bestLiving == null) {
			return null;
		}
		
		Location loc = this.locations.get(bestLiving.index);

		//solver.display("first best move is " + loc.display());
		BigDecimal prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(bestLiving.mines).divide(BigDecimal.valueOf(currentNode.solutions.size()), Solver.DP, RoundingMode.HALF_UP));
		
		while (boardState.isRevealed(loc)) {
			int value = boardState.getWitnessValue(loc);
			
			//parentNode = currentNode;
			currentNode = bestLiving.children[value];
			bestLiving = getBestLocation(currentNode);
			if (bestLiving == null) {
				return null;
			}
			prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(bestLiving.mines).divide(BigDecimal.valueOf(currentNode.solutions.size()), Solver.DP, RoundingMode.HALF_UP));

			loc = this.locations.get(bestLiving.index);
			
		}
		
		solver.display("mines = "  + bestLiving.mines + " solutions = " + currentNode.solutions.size());
		for (int i=0; i < bestLiving.children.length; i++) {
			if (bestLiving.children[i] == null) {
				continue; //ignore this node but continue the loop
			}
			
			String probText;
			if (bestLiving.children[i].bestLiving == null) {
				probText =  Action.FORMAT_2DP.format(ONE_HUNDRED.divide(BigDecimal.valueOf(bestLiving.children[i].solutions.size()), Solver.DP, RoundingMode.HALF_UP)) + "%";
			} else {
				probText = Action.FORMAT_2DP.format(bestLiving.children[i].bestLiving.probability.multiply(ONE_HUNDRED)) + "%";
			}
			solver.display("Value of " + i + " leaves " + bestLiving.children[i].solutions.size() + " solutions and winning probability " + probText);
		}
		
		String text = " (" + Action.FORMAT_2DP.format(bestLiving.probability.multiply(ONE_HUNDRED)) + "%)";
		Action action = new Action(loc, Action.CLEAR, Solver.BRUTE_FORCE_DEEP_ANALYSIS, Solver.METHOD[Solver.BRUTE_FORCE_DEEP_ANALYSIS] + text, prob);
		
		expectedMove = loc;
		
		return action;
		
	}
	
	private Living getBestLocation(Node node) {
		
		Living result = null;
		
		BigDecimal best = BigDecimal.ZERO;
		
		for (Living alive: node.getLivingLocations()) {
			
			if (alive.probability.compareTo(best) > 0 ) {
				best = alive.probability;
				result = alive;
			}
			
			
		}
		
		return result;
		
	}
	
	@Override
	protected Location getExpectedMove() {
		return expectedMove;
	}
	
	private String percentage(BigDecimal prob) {
		
		return Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED));
	}
	
}
