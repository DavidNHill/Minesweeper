package minesweeper.solver.bulk;

import java.util.List;
import java.util.Random;

import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;

public class BulkPlayer extends BulkController {
	
	protected final GameType gameType;
	protected final GameSettings gameSettings;
	private List<Action> preActions;
	
	/**
	 * Use the bulk controller to play games from the beginning
	 */
	public BulkPlayer(Random seeder, int gamesToPlay, GameType gameType, GameSettings gameSettings, SolverSettings solverSettings, int workers) {
		super(seeder, gamesToPlay, solverSettings, workers);
		
		this.gameType = gameType;
		this.gameSettings = gameSettings;
	}
	
	/**
	 * Pre actions are actions that are played on the game before it gets access to the solver
	 * e.g. clear all 4 corners
	 */
	public void setPreActions(List<Action> actions) {
		this.preActions = actions;
	}

	protected GameStateModel getGameState(long seed) {;
	
		GameStateModel gs = GameFactory.create(gameType, gameSettings, seed);
	
		// play the pre-actions
		if (preActions != null) {
			for (Action a: preActions) {
				gs.doAction(a);
				if (gs.getGameState() == GameStateModel.LOST) {
					break;
				}
			}
		}
		
		return gs;
		
	}

	
}
