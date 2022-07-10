package minesweeperbulk;

import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.GamePreListener;
import minesweeper.structure.Action;

public class StartStrategy extends GamePreListener {

	private List<Action> preActions;
	private int requiredZeros;
	
	public StartStrategy(List<Action> preActions, int requiredZeros) {
		this.preActions = preActions;
		this.requiredZeros = requiredZeros;
	}
	
	
	@Override
	public void preAction(GameStateModel game) {
		
		int zeros = 0;
		int actionCount = 0;
		
		for (Action a: preActions) {
			game.doAction(a);
			actionCount++;
			
			if (game.getGameState() == GameStateModel.LOST  || game.getGameState() == GameStateModel.WON) {
				break;
			}
			if (game.query(a) == 0) {
				zeros++;
				if (zeros >= requiredZeros) {
					break;
				}
			}
		}
		
	} 

}
