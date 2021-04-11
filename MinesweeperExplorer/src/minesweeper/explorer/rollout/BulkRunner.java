package minesweeper.explorer.rollout;

import java.math.BigDecimal;
import java.util.Random;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.RolloutGenerator;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class BulkRunner implements Runnable {
	
	private boolean stop = false;
	private final int maxSteps;
	private final RolloutController controller;
	private final Location startLocation;
	private final Location safeTile;
	private final RolloutGenerator rollout;
	private final SolverSettings preferences;
	private final long seed;
	
	//private final Random seeder;
	private int steps = 0;
	private int wins = 0;
	
	private boolean[] mastery = new boolean[100];
	private int masteryCount = 0;
	private int maxMasteryCount = 0;
	private int winStreak;
	private int maxWinStreak;
	
	private int guesses;
	private double fairness = 0;
	
	//private ResultsController resultsController;
	private boolean showGames;
	private boolean winsOnly;
	
	public BulkRunner(RolloutController controller, int iterations, RolloutGenerator rollout, Location startLocation, boolean safeStart, SolverSettings preferences, long seed) {
		
		this.controller = controller;
		this.maxSteps = iterations;
		this.rollout = rollout;
		this.startLocation = startLocation;
		this.preferences = preferences;
		this.seed = seed;
		
		if (safeStart) {
			this.safeTile = startLocation;
		} else {
			this.safeTile = null;
		}
		
		if (showGames) {
			//resultsController = ResultsController.launch(null, gameSettings, gameType);
		}
		
		
	}

	@Override
	public void run() {
		
		System.out.println("At BulkRunner run method using seed " + seed);
		
		Random seeder = new Random(seed);
		
		while (!stop && steps < maxSteps) {

			GameStateModel gs = rollout.generateGame(seeder.nextLong(), safeTile);

			Solver solver = new Solver(gs, preferences, false);
			
			gs.doAction(new Action(startLocation, Action.CLEAR));
			int state = gs.getGameState();

			boolean win;
			if (state == GameStateModel.LOST || state == GameStateModel.WON) {  // if we have won or lost on the first move nothing more to do
				win = (state == GameStateModel.WON);
			} else { // otherwise use the solver to play the game
				 win = playGame(gs, solver);
			}
		
			// reduce mastery if the game 100 ago was a win
			int masteryIndex = steps % 100;
			if (mastery[masteryIndex]) {
				masteryCount--;
			}
			
			if (win) {
				wins++;
				
				// update win streak
				winStreak++;
				maxWinStreak = Math.max(maxWinStreak, winStreak);
				
				// update mastery
				mastery[masteryIndex] = true;
				masteryCount++;
				maxMasteryCount = Math.max(masteryCount, maxMasteryCount);
				
			} else {
				winStreak = 0;
				mastery[masteryIndex] = false;
			}

			/*
			if (showGames && (win || !win && !winsOnly)) {
				if (!resultsController.update(gs)) {  // this returns false if the window has been closed
					showGames = false;
					resultsController = null;
					System.out.println("Results window has been closed... will no longer send data to it");
				}
			}
			*/
			
			steps++;
			
			//controller.update(steps, maxSteps, wins, guesses, fairness, maxWinStreak, maxMasteryCount);
			
		}
		
		stop = true;
		System.out.println("BulkRunner run method ending with wins = " +  wins + " of " + steps);
		
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
				stop = true;
				return false;
			}

			if (moves.length == 0) {
				System.err.println("No moves returned by the solver for game " + gs.showGameKey());
				stop = true;
				return false;
			}            

			// play all the moves until all done, or the game is won or lost
			for (int i=0; i < moves.length; i++) {

				boolean result = gs.doAction(moves[i]);

				state = gs.getGameState();

				// keep track of how many guesses and their fairness
				if (state == GameStateModel.STARTED || state == GameStateModel.WON) {
					if (!moves[i].isCertainty() ) { 
						guesses++;
						fairness = fairness + 1d;
					}
				} else { // otherwise the guess resulted in a loss
					if (!moves[i].isCertainty()) {
						guesses++;
						BigDecimal prob = moves[i].getBigProb();
						fairness = fairness - prob.doubleValue() / (1d - prob.doubleValue());
					}                    
				}
				
				
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
	
	public void forceStop() {
		System.out.println("Bulk run being requested to stop");
		
		stop = true;
	}
	
	public boolean isFinished() {
		return stop;
	}
	
	public int getWins() {
		return wins;
	}

}
