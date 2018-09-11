package minesweeper.bulk;

import java.math.BigDecimal;
import java.util.Random;

import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.MoveMethod;
import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.Preferences;
import minesweeper.solver.Solver;

public class BulkRunner implements Runnable {
	
	private boolean stop = false;
	private final int maxSteps;
	private final long seed;
	private final BulkController controller;
	private final GameSettings gameSettings;
	private final GameType gameType;
	//private final Random seeder;
	private int steps = 0;
	private int wins = 0;
	
	private final RNG seeder;
	
	public BulkRunner(BulkController controller, int iterations, GameSettings gameSettings, GameType gameType, long seed) {
		
		maxSteps = iterations;
		this.seed = seed;
		this.controller = controller;
		this.gameSettings = gameSettings;
		this.gameType = gameType;
		this.seeder = DefaultRNG.getRNG(seed);
		
	}

	@Override
	public void run() {
		
		System.out.println("At BulkRunner run method");
		
		while (!stop && steps < maxSteps) {
			
			steps++;
			
			GameStateModel gs = GameFactory.create(gameType, gameSettings, seeder.random(0));

			Solver solver = new Solver(gs, Preferences.SMALL_BRUTE_FORCE, false);
			
			if (playGame(gs, solver)) {
				wins++;
			}
			
			controller.update(steps, maxSteps, wins);
			
		}
		
		stop = true;
		System.out.println("BulkRunner run method ending");
		
	}
	
	
	private boolean playGame(GameStateModel gs, Solver solver) {

		int state;
		
		play: while (true) {

			Action[] moves;
			try {
				solver.start();
				moves = solver.getResult();
			} catch (Exception e) {
				System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
				stop = true;
				return false;
			}

			if (moves.length == 0) {
				System.err.println("No moves returned by the solver");
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

}
