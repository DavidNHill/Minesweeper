package minesweeper.solver.bulk;

import java.math.BigDecimal;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.settings.SolverSettings;

public class BulkRequestGame {

	protected SolverSettings solverSettings;
	protected GameStateModel gs;
	
	protected int guesses = 0;
	protected BigDecimal fairness = BigDecimal.ZERO;
	protected boolean startedOkay = true;
	
	public GameStateModel getGame( ) {
		return this.gs;
	}
	
	public int getGuesses() {
		return this.guesses;
	}
	
}
