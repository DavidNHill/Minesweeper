package minesweeperbulk;

import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.GameListener;
import minesweeper.structure.Action;

public class PreActions extends GameListener {

	private List<Action> preActions;
	
	public PreActions(List<Action> preActions) {
		this.preActions = preActions;
	}
	
	
	@Override
	public void gameAction(GameStateModel game) {
		
		for (Action a: preActions) {
			game.doAction(a);
			if (game.getGameState() == GameStateModel.LOST  || game.getGameState() == GameStateModel.WON) {
				break;
			}
		}
		
	} 

}
