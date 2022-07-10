package minesweeperbulk;

import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.GamePreListener;
import minesweeper.structure.Action;

public class PreActions extends GamePreListener {

	private List<Action> preActions;
	
	public PreActions(List<Action> preActions) {
		this.preActions = preActions;
	}
	
	
	@Override
	public void preAction(GameStateModel game) {
		
		for (Action a: preActions) {
			game.doAction(a);
			if (game.getGameState() == GameStateModel.LOST  || game.getGameState() == GameStateModel.WON) {
				break;
			}
		}
		
	} 

}
