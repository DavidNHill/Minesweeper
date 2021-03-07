package minesweeper.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import Asynchronous.Asynchronous;
import Monitor.AsynchMonitor;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.gamestate.GameStateReader;
import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SettingsFactory.Setting;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

/**
 * This class determines the possible distribution of mines along the edge and the number of ways that can be done
 * 
 */
public class RolloutGenerator {

	private int[][] SMALL_COMBINATIONS = new int[][] {{1}, {1,1}, {1,2,1}, {1,3,3,1}, {1,4,6,4,1}, {1,5,10,10,5,1}, {1,6,15,20,15,6,1}, {1,7,21,35,35,21,7,1}, {1,8,28,56,70,56,28,8,1}};
	
	// used to hold a viable solution 
	private class ProbabilityLine implements Comparable<ProbabilityLine> {
		private int mineCount = 0;
		private BigInteger solutionCount = BigInteger.ZERO;
		private int[] allocatedMines  = new int[boxCount];   // this is the number of mines originally allocate to a box
		private int weight;    // all lines normalized to sum to 1 million
		
		
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
	
	//when set to true indicates that the box has been part of this analysis
	private boolean[] mask;           
	
	final private BoardState boardState;
	final private WitnessWeb web;
	final private int boxCount;
	final private List<Witness> witnesses;
	final private List<Box> boxes;
	final private int minesLeft;                 // number of mines undiscovered in the game
	final private int tilesOfEdge;               // number of squares undiscovered in the game and off the web
	
	final private List<Location> offWebTiles;
	final private List<Location> revealedTiles = new ArrayList<>();
	final private List<Location> placedMines = new ArrayList<>();
	
	private int recursions = 0;
	
	// these are the limits that can be on the edge
	final private int minTotalMines;
	final private int maxTotalMines;
	
	private int totalWeight = 0;
	
	private boolean valid = true;
	
	public RolloutGenerator(BoardState boardState, WitnessWeb web, int squaresLeft, int minesLeft) {
		
		this.boardState = boardState;
		this.web = web;
		this.minesLeft = minesLeft;
		this.tilesOfEdge = squaresLeft - web.getSquares().size();
		
		this.minTotalMines = Math.max(0, minesLeft - this.tilesOfEdge);  //we can't use so few mines that we can't fit the remainder elsewhere on the board
		this.maxTotalMines = minesLeft;    // we can't use more mines than are left in the game
		
		boardState.display("Total mines " + minTotalMines + " to " + maxTotalMines);
		
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
		
		// all tile ...
		Set<Location> offWeb = new HashSet<>(boardState.getAllUnrevealedSquares());
		
		// ... minus those on the edge
		for (Location tile: web.getSquares()) {
			offWeb.remove(tile);
		}
		
		offWebTiles = new ArrayList<>(offWeb);
		
		boardState.display("Total tiles off web " + offWebTiles.size());
		
	   	int width = boardState.getGameWidth();
    	int height = boardState.getGameHeight();
    	
    	for (int x=0; x < width; x++) {
    		for (int y=0; y < height; y++) {
    			if (boardState.isRevealed(x, y)) {
    				revealedTiles.add(new Location(x,y));
    			}
    			if (boardState.isConfirmedFlag(x,y)) {
    				placedMines.add(new Location(x,y));
    			}
    		}
    	}
    	
    	boardState.display("Total tiles revealed " + revealedTiles.size());
    	boardState.display("Total mines placed " + placedMines.size());
	}

	/**
	 * Run the Rollout generator
	 */
	public void process() {
		
		if (!web.isWebValid()) {  // if the web is invalid then nothing we can do
			boardState.display("Web is invalid - exiting the Rollout generator processing");
			valid = false;
			return;
		}
		
		long startTime = System.currentTimeMillis();
		
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
		if (!workingProbs.isEmpty()) {
			calculateBoxProbabilities();
		} else {
			valid = false;
		}

		
		duration = System.currentTimeMillis() - startTime;
	}
	
	
	// here we calculate the total number of candidate solutions left in the game
	private void calculateBoxProbabilities() {
		
		boardState.display("showing " + workingProbs.size() + " probability Lines...");
		
		// total game tally
		BigInteger totalTally = BigInteger.ZERO;
		
		// outside a box tally
		BigInteger outsideTally = BigInteger.ZERO;
		
		BigInteger hcf = null;
		
		// calculate how many solutions are in each line / Highest common divisor
		for (ProbabilityLine pl: workingProbs) {
			
			if (pl.mineCount >= minTotalMines) {    // if the mine count for this solution is less than the minimum it can't be valid
				
				BigInteger mult = Solver.combination(minesLeft - pl.mineCount, tilesOfEdge);  //# of ways the rest of the board can be formed
				
				pl.solutionCount = pl.solutionCount.multiply(mult);
				
				totalTally = totalTally.add(pl.solutionCount);
				
				if (hcf == null) {
					hcf = pl.solutionCount;
				} else {
					hcf = hcf.gcd(pl.solutionCount);
				}

			}
		}		
		
		BigInteger million = BigInteger.valueOf(1000000);
		
		// display the lines with a weight as a part of 1,000,000
		for (ProbabilityLine pl: workingProbs) {
			
			if (pl.mineCount >= minTotalMines) {    // if the mine count for this solution is less than the minimum it can't be valid
				
				pl.weight = pl.solutionCount.multiply(million).divide(totalTally).intValue();
				
				totalWeight = totalWeight + pl.weight;
				
				String display = "Mines=" + pl.mineCount + " Weight=" + pl.weight;
				for (int i=0; i < pl.allocatedMines.length; i++) {
					
					display = display + " "  + boxes.get(i).getSquares().size() + "(" + pl.allocatedMines[i] + ") ";
					
				}

				boardState.display(display);
				
			}
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

		//solver.display("Processed witness " + nw.witness.display());
		
		// flag the last set of details as processed
		nw.witness.setProcessed(true);
		for (Box b: nw.newBoxes) {
			b.setProcessed(true);
		}

		return newProbs;
		
		
	}
	
	// this is used to recursively place the missing Mines into the available boxes for the probability line
	private List<ProbabilityLine> distributeMissingMines(ProbabilityLine pl, NextWitness nw, int missingMines, int index) {
		
		recursions++;
		if (recursions % 10000 == 0) {
			boardState.display("Solution counter recursion = " + recursions);
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
		
		result.allocatedMines = pl.allocatedMines.clone();
		
		result.allocatedMines[newBox.getUID()] = mines;
		
		return result;
	}
	
	// counts the number of mines already placed
	private int countPlacedMines(ProbabilityLine pl, NextWitness nw) {
		
		int result = 0;
		
		for (Box b: nw.oldBoxes) {
			result = result + pl.allocatedMines[b.getUID()];
		}
		
		return result;
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
		
		// get an unprocessed witness
		NextWitness nw =  findFirstWitness();

		// return the next witness to process
		return nw;
		
	}

	/**
	 * The duration to do the processing in milliseconds
	 * @return
	 */
	protected long getDuration() {
		return this.duration;
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public int getWidth() {
		return boardState.getGameWidth();
	}
	
	public int getHeight() {
		return boardState.getGameHeight();
	}
	
	public synchronized GameStateModelViewer generateGame(long seed) {
		
		GameStateModelViewer result;
		
		int width = boardState.getGameWidth();
		int height = boardState.getGameHeight();
		int mineCount = this.minesLeft;
		
		Random rng = new Random(seed);
		
		int edge = (int) (rng.nextDouble()*totalWeight);
		//boardState.display("Random number is " + edge);
		
		int soFar = 0;
		ProbabilityLine line = null;
		for (ProbabilityLine pl: workingProbs) {
			soFar = soFar + pl.weight;
			if (soFar > edge) {
				line = pl;
				break;
			}
		}

		mineCount = mineCount - line.mineCount;
		
		List<Location> mines = new ArrayList<>(placedMines);  // start with the mines we have already placed
		
		for (int i=0; i < line.allocatedMines.length; i++) {
			
			if (line.allocatedMines[i] == 0) { // if no mines here nothing to do
			
			} else if (line.allocatedMines[i] == boxes.get(i).getSquares().size()) {  // if the box is full of mines then all tile in the box are mines
				for (Square tile: boxes.get(i).getSquares()) {
					mines.add(tile);
				}
			
			} else {  // shuffle the tiles in the box and take the first ones as the mines
				
				// in order to make this repeatable with the same seed, we can't shuffle the underlying data. So create a copy.
				List<Location> boxTiles = new ArrayList<>(boxes.get(i).getSquares());
				
				Collections.shuffle(boxTiles, rng);
				
				for (int j=0; j < line.allocatedMines[i]; j++) {
					mines.add(boxTiles.get(j));
				}
			}
			
		}
		
		
		// in order to make this repeatable with the same seed, we can't shuffle the underlying data. So create a copy.
		List<Location> owt = new ArrayList<>(offWebTiles);
		
		Collections.shuffle(owt, rng);
		for (int j=0; j < mineCount; j++) {
			mines.add(owt.get(j));
		}
		
		result = GameStateReader.loadMines(width, height, mines, revealedTiles);
		
		/*
		// show the board
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				
				int tile = result.privilegedQuery(new Location(x,y) , true);
				
				if (tile == GameStateModel.MINE) {
					System.out.print("M");
					
				} else if (tile == GameStateModel.HIDDEN) {
					System.out.print(".");
					
				} else {
					System.out.print(tile);
				}
			}
			System.out.println();
		}					
		*/
		
		return result;
		
	}
	
	public class Adversarial<T> implements Comparable<Adversarial<T>> {
		public final T original;
		public int wins;
		public int played;
		private Adversarial(T original) {
			this.original = original;
		}
		@Override
		public int compareTo(Adversarial<T> o) {
			return o.wins - this.wins;
		}
	}
	
	public class RolloutWork implements Asynchronous<Boolean> {

		private final Adversarial<? extends Location> player;
		private final int plays;
		
		public RolloutWork(Adversarial<? extends Location> player, int plays) {
			this.player = player;
			this.plays = plays;
		}
		
		
		@Override
		public void start() {
			int wins = playGames(player.original, plays);
			player.wins = player.wins + wins;
			player.played = player.played + plays;
		}

		@Override
		public void requestStop() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Boolean getResult() {
			return true;
		}
		
	}
	
	public <T extends Location> List<Adversarial<T>> adversarial(List<T> candidates) {

		List<Adversarial<T>> players = new ArrayList<>();
		
		for (T candidate: candidates) {
			players.add(new Adversarial<T>(candidate));
		}
		
		int check = players.size();
		
		final int plays = 200;
		
		while (check > 1) {
			
			RolloutWork[] workers = new RolloutWork[check];
			
			for (int i=0; i < check; i++) {
				Adversarial<T> player = players.get(i);
				
				workers[i] = new RolloutWork(player, plays);
				
				//int wins = playGames(player.original, plays);
				//player.wins = player.wins + wins;
				//player.played = player.played + plays;
			}

			AsynchMonitor monitor = new AsynchMonitor(workers);
			monitor.setMaxThreads(6);
			try {
				monitor.startAndWait();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Collections.sort(players);
			if (check > 4) {
				check = check * 3 / 4;
			} else {
				check--;
			}
			
		}

		for (Adversarial<T> player: players) {
			boardState.display(player.original.display() + " had " + player.wins + " wins out of " + player.played);
		}
		
		return players;
		
	}
	
	private int playGames(Location startLocation, int count) {
		
		int wins = 0;
		
		Random seeder = new Random();
		
		SolverSettings preferences = SettingsFactory.GetSettings(Setting.TINY_ANALYSIS).setTieBreak(false);
		
		int steps = 0;
		int maxSteps = count;
		
		while (steps < maxSteps) {
			
			steps++;
			
			GameStateModel gs = generateGame(seeder.nextLong());

			Solver solver = new Solver(gs, preferences, false);
			
			gs.doAction(new Action(startLocation, Action.CLEAR));
			int state = gs.getGameState();

			boolean win;
			if (state == GameStateModel.LOST || state == GameStateModel.WON) {  // if we have won or lost on the first move nothing more to do
				win = (state == GameStateModel.WON);
			} else { // otherwise use the solver to play the game
				 win = playGame(gs, solver);
			}
		
			if (win) {
				wins++;
			}

		}
		
		return wins;
		
	}
	
	private boolean playGame(GameStateModel gs, Solver solver) {

		int state;
		
		play: while (true) {

			Action[] moves;
			try {
				solver.start();
				moves = solver.getResult();
			} catch (Exception e) {
				System.out.println("Game " + gs.showGameKey() + " has thrown an exception! ");
				e.printStackTrace();
				return false;
			}

			if (moves.length == 0) {
				System.err.println("No moves returned by the solver for game " + gs.showGameKey());
				return false;
			}            

			// play all the moves until all done, or the game is won or lost
			for (int i=0; i < moves.length; i++) {

				gs.doAction(moves[i]);

				state = gs.getGameState();

				if (state == GameStateModel.LOST || state == GameStateModel.WON) {
					break play;
				}
			}            
		}

		if (state == GameStateModel.LOST) {
			return false;
		} else {
			return true;
		}

	}
	
}
