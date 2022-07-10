package minesweeper.solver.bulk;

import java.util.Random;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.RolloutGenerator;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public class BulkRollout extends BulkController {
	
	protected final RolloutGenerator generator;
	protected final Location safeTile;
	protected final Location startTile;
	
	/**
	 * Use the bulk controller to play games from the beginning
	 */
	public BulkRollout(Random seeder, int gamesToPlay, RolloutGenerator generator, Location startTile, boolean safeStart, SolverSettings solverSettings, int workers) {
		super(seeder, gamesToPlay, solverSettings, workers);
		
		this.generator = generator;
		this.startTile = startTile;
		
		if (safeStart) {
			this.safeTile = startTile;
		} else {
			this.safeTile = null;
		}

	}
	

	protected GameStateModel getGameState(long seed) {
	
		GameStateModel gs = generator.generateGame(seed, safeTile);
		
		// play the start tile and return the game
		gs.doAction(new Action(startTile, Action.CLEAR));
	
		return gs;
		
	}

	
}
