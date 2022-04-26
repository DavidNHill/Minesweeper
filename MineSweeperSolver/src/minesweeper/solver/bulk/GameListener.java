package minesweeper.solver.bulk;

import minesweeper.gamestate.GameStateModel;

public abstract class GameListener {

	/**
	 * This is run after each game finishes
	 */
	abstract public void gameAction(GameStateModel game);
	
}
