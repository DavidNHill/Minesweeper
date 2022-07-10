package minesweeper.solver.bulk;

import minesweeper.gamestate.GameStateModel;

public abstract class GamePreListener {

	/**
	 * This is run before each game starts
	 */
	abstract public void preAction(GameStateModel game);
	
}
