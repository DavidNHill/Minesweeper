package minesweeper.explorer.rollout;

import java.util.Random;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Preferences;
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
	private final RolloutGenerator rollout;
	private final SolverSettings preferences;
	private final long seed;
	
	//private final Random seeder;
	private int steps = 0;
	private int wins = 0;
	
	//private ResultsController resultsController;
	private boolean showGames;
	private boolean winsOnly;
	
	public BulkRunner(RolloutController controller, int iterations, RolloutGenerator rollout, Location startLocation, SolverSettings preferences, long seed) {
		
		this.controller = controller;
		this.maxSteps = iterations;
		this.rollout = rollout;
		this.startLocation = startLocation;
		this.preferences = preferences;
		this.seed = seed;
		
		if (showGames) {
			//resultsController = ResultsController.launch(null, gameSettings, gameType);
		}
		
		
	}

	@Override
	public void run() {
		
		System.out.println("At BulkRunner run method using seed " + seed);
		
		Random seeder = new Random(seed);
		
		while (!stop && steps < maxSteps) {
			
			steps++;
			
			GameStateModel gs = rollout.generateGame(seeder.nextLong());

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

			/*
			if (showGames && (win || !win && !winsOnly)) {
				if (!resultsController.update(gs)) {  // this returns false if the window has been closed
					showGames = false;
					resultsController = null;
					System.out.println("Results window has been closed... will no longer send data to it");
				}
			}
			*/
			
			controller.update(steps, maxSteps, wins);
			
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
