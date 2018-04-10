package minesweeper.solver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;

public class BruteForceAnalysisOld {
	
	private class Living implements Comparable<Living>{
		
		private BigDecimal probability = BigDecimal.ZERO;
		private boolean pruned = false;
		private final int index;
		private int mines = 0;
		private int[] values = new int[9];
		private int count;  // number of possible values at this location
		
		private List<Node> children;
		
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
		
		private int index;
		private int value;
		private final List<int[]> solutions;

		private List<Living> alive;
		
		private Living bestLiving;

		private Node() {
			this(250);
		}
		
		
		private Node(int size) {
			solutions = new ArrayList<>(size);
		}

		private List<Living> getLivingLocations() {
			return alive;
		}
		
		
		private List<Node> getChildren(int index) {
			
			List<Node> result = new ArrayList<>();
			
			Node[] work = new Node[9];
			for (int i=0; i < work.length; i++) {
				work[i] = new Node(this.solutions.size());
				work[i].index = index;
				work[i].value = i;
			}

			// add the solution into the correct node base upon the value it has
			for (int[] sol: this.solutions) {
				if (sol[index] != GameStateModel.MINE) {
					work[sol[index]].solutions.add(sol);
				}
			}			

			for (int i=0; i < work.length; i++) {
				if (work[i].solutions.size() > 0) {
					work[i].determineLivingLocations(this.alive);
					result.add(work[i]);
				}
			}

			return result;
			
		}
		
		/**
		 * this is a list of indices to Location that are still alive, i.e. have more than one possible value
		 */
		private void determineLivingLocations() {
			
			List<Living> living = new ArrayList<>();
			
			for (int i=0; i < locations.size(); i++) {
				int value;
				Living alive = new Living(i);
				
				for (int j=0; j < solutions.size(); j++) {
					if (solutions.get(j)[i] != GameStateModel.MINE) {
						value = solutions.get(j)[i];
						alive.values[value]++;
					} else {
						alive.mines++;
					}
				}
				
				for (int j=0; j < alive.values.length; j++) {
					if (alive.values[j] > 0) {
						alive.count++;
					}
				}
				if (alive.count > 1) {
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
			
			List<Living> living = new ArrayList<>();
			
			for (Living live: liveLocs) {
				int value;
				Living alive = new Living(live.index);
				
				for (int j=0; j < solutions.size(); j++) {
					if (solutions.get(j)[live.index] != GameStateModel.MINE) {
						value = solutions.get(j)[live.index];
						alive.values[value]++;
					} else {
						alive.mines++;
					}
				}
				
				for (int j=0; j < alive.values.length; j++) {
					if (alive.values[j] > 0) {
						alive.count++;
					}
				}
				if (alive.count > 1) {
					living.add(alive);
				}
				
			}
			
			Collections.sort(living);
			
			this.alive = living;
			
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
	
	private boolean completed = false;
	private boolean tooMany = false;
	
	public BruteForceAnalysisOld(Solver solver, List<? extends Location> locations, int size) {
		
		this.solver = solver;
		this.locations = locations;
		this.maxSolutionSize = size;
		this.top = new Node(size);
	}
	
	// this can be called by different threads when brute force is running on multiple threads
	protected synchronized void addSolution(int[] solution) {
		
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

	protected void process() {

		long start = System.currentTimeMillis();
		
		solver.display("----- Brute Force Deep Analysis starting ----");
		solver.display(top.solutions.size() + " solutions in BruteForceAnalysis");
		
		// determine which locations are alive
		top.determineLivingLocations();
		
		BigDecimal best = BigDecimal.ZERO;
		
		for (Living alive: top.getLivingLocations()) {
			
			BigDecimal prob = process(top, alive, best);
			
			//best = best.max(prob);
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
	private BigDecimal process(Node parent, Living parentAlive, BigDecimal cutoff) {

		BigDecimal result = BigDecimal.ZERO;
		
		processCount++;
		if (processCount > solver.preferences.BRUTE_FORCE_ANALYSIS_MAX_NODES) {
			return result;
		}

		parentAlive.children = parent.getChildren(parentAlive.index);

		int notMines = parent.solutions.size() - parentAlive.mines;
		
		for (Node child: parentAlive.children) {

			// this is the maximum probability that this move can yield, i.e. if everything else in the loop is 100%
			BigDecimal maxProb = result.add(BigDecimal.valueOf(notMines).divide(BigDecimal.valueOf(parent.solutions.size()), Solver.DP, RoundingMode.HALF_UP));
			
			// if the max probability is less than the current cutoff then no point doing the analysis
			if (maxProb.compareTo(cutoff) <= 0) {
				parentAlive.pruned = true;
				return result;
			}
			
			BigDecimal best = BigDecimal.ZERO;
			if (child.solutions.size() == 0) {
				solver.display("Zero solutions!");
				best = BigDecimal.ZERO;
			} else if (child.solutions.size() == 1) {
				best = BigDecimal.ONE;   // if only one solution left then we have solved it
				//solver.display("End - one solution");
			} else if (child.getLivingLocations().isEmpty()) {
				best = BigDecimal.ONE.divide(BigDecimal.valueOf(child.solutions.size()), Solver.DP, RoundingMode.HALF_UP);  // no further information ==> all solution indistinguishable ==> 1 / number of solutions
				//solver.display("End - nothing alive");
			} else {
				for (Living alive: child.getLivingLocations()) {
					BigDecimal prob = process(child, alive, best);
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
			
			BigDecimal work = best.multiply(BigDecimal.valueOf(parentAlive.values[child.value])).divide(BigDecimal.valueOf(parent.solutions.size()), Solver.DP, RoundingMode.HALF_UP);
			
			result = result.add(work);	
			notMines = notMines - child.solutions.size();  // reduce the number of not mines
			
		}
		
		parentAlive.probability = result;

		return result;
		
	}


	
	protected boolean isComplete() {
		return this.completed;
	}
	
	protected boolean tooMany() {
		return this.tooMany;
	}
	
	protected int getSolutionCount() {
		return top.solutions.size();
	}
	
	protected Action getNextMove(BoardState boardState) {
		
		Living bestLiving = getBestLocation(currentNode);
		
		if (bestLiving == null) {
			return null;
		}
		
		Location loc = this.locations.get(bestLiving.index);

		//solver.display("first best move is " + loc.display());
		BigDecimal prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(bestLiving.mines).divide(BigDecimal.valueOf(currentNode.solutions.size()), Solver.DP, RoundingMode.HALF_UP));
		
		while (boardState.isRevealed(loc)) {
			int value = boardState.getWitnessValue(loc);
			for (Node node:bestLiving.children) {
				if (node.value == value) {
					currentNode = node;
					bestLiving = getBestLocation(currentNode);
					if (bestLiving == null) {
						return null;
					}
					prob = BigDecimal.ONE.subtract(BigDecimal.valueOf(bestLiving.mines).divide(BigDecimal.valueOf(currentNode.solutions.size()), Solver.DP, RoundingMode.HALF_UP));

					loc = this.locations.get(bestLiving.index);
					break;
				}
			}
		}
		
		
		
		solver.display("mines = "  + bestLiving.mines + " solutions = " + currentNode.solutions.size());
		for (Node n: bestLiving.children) {
			String probText;
			if (n.bestLiving == null) {
				probText =  Action.FORMAT_2DP.format(ONE_HUNDRED.divide(BigDecimal.valueOf(n.solutions.size()), Solver.DP, RoundingMode.HALF_UP)) + "%";
			} else {
				probText = Action.FORMAT_2DP.format(n.bestLiving.probability.multiply(ONE_HUNDRED)) + "%";
			}
			solver.display("Value of " + n.value + " leaves " + n.solutions.size() + " solutions and winning probability " + probText);
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
	
	protected Location getExpectedMove() {
		return expectedMove;
	}
	
	private String percentage(BigDecimal prob) {
		
		return Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED));
	}
	
}
