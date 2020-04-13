package minesweeper.bulk;

import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class BulkRunner implements Runnable {
	
	private boolean stop = false;
	private final int maxSteps;
	private final long seed;
	private final BulkController controller;
	private final GameSettings gameSettings;
	private final GameType gameType;
	private final Location startLocation;
	private final SolverSettings preferences;

	//private final Random seeder;
	private int steps = 0;
	private int wins = 0;
	
	private final RNG seeder;
	
	private ResultsController resultsController;
	private boolean showGames;
	private boolean winsOnly;
	
	public BulkRunner(BulkController controller, int iterations, GameSettings gameSettings, GameType gameType, 
			long seed, Location startLocation, boolean showGames, boolean winsOnly, SolverSettings preferences) {
		
		maxSteps = iterations;
		this.seed = seed;
		this.controller = controller;
		this.gameSettings = gameSettings;
		this.gameType = gameType;
		this.startLocation = startLocation;
		this.seeder = DefaultRNG.getRNG(seed);
		this.showGames = showGames;
		this.winsOnly = winsOnly;
		this.preferences = preferences;
		
		if (showGames) {
			resultsController = ResultsController.launch(null, gameSettings, gameType);
		}
		
		
	}

	@Override
	public void run() {
		
		System.out.println("At BulkRunner run method");
		
		while (!stop && steps < maxSteps) {
			
			steps++;
			
			GameStateModel gs = GameFactory.create(gameType, gameSettings, seeder.random(0));

			Solver solver = new Solver(gs, preferences, false);
			if (startLocation != null) {
				solver.setStartLocation(startLocation);
			}
			
			boolean win = playGame(gs, solver);
			
			if (win) {
				wins++;
			}
			
			if (showGames && (win || !win && !winsOnly)) {
				if (!resultsController.update(gs)) {  // this returns false if the window has been closed
					showGames = false;
					resultsController = null;
					System.out.println("Results window has been closed... will no longer send data to it");
				}
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

}
