package minesweeper.solver.bulk;

import java.util.Random;

import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.settings.SolverSettings;

public class BulkPlayer extends BulkController {
	
	protected final GameType gameType;
	protected final GameSettings gameSettings;
	//private List<Action> preActions;
	
	/**
	 * Use the bulk controller to play games from the beginning
	 */
	public BulkPlayer(Random seeder, int gamesToPlay, GameType gameType, GameSettings gameSettings, SolverSettings solverSettings, int workers) {
		super(seeder, gamesToPlay, solverSettings, workers);
		this.gameType = gameType;
		this.gameSettings = gameSettings;

	}
	
	public BulkPlayer(Random seeder, int gamesToPlay, GameType gameType, GameSettings gameSettings, SolverSettings solverSettings, int workers, int bufferPerWorker) {
		super(seeder, gamesToPlay, new SolverSettings[] {solverSettings}, workers, bufferPerWorker);
		this.gameType = gameType;
		this.gameSettings = gameSettings;
	}
	
	public BulkPlayer(Random seeder, int gamesToPlay, GameType gameType, GameSettings gameSettings, SolverSettings[] solverSettings, int workers, int bufferPerWorker) {
		super(seeder, gamesToPlay, solverSettings, workers, bufferPerWorker);
		this.gameType = gameType;
		this.gameSettings = gameSettings;
	}
	
	protected GameStateModel getGameState(long seed) {;
	
		GameStateModel gs = GameFactory.create(gameType, gameSettings, seed);
	
		return gs;
		
	}

	
}
