package bulkCompare;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.BulkRequest;
import minesweeper.solver.bulk.BulkRequestGame;
import minesweeper.solver.bulk.GamePostListener;

public class CompareMonitor extends GamePostListener {

	private int[] wins;
	private int played = 0;
	
	@Override
	public void postAction(BulkRequest request) {
		
		played++;
		
		//  initialise
		if (wins == null) {
			wins = new int[request.getRequestGames().length];
		}
		
		int index = 0;
		StringBuilder sb = new StringBuilder();
		for (BulkRequestGame brg: request.getRequestGames()) {
			
			GameStateModel game = brg.getGame();
			if (game.getGameState() == GameStateModel.WON ) {
				wins[index]++;
			}
			
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append("Run ").append(index).append(" wins=").append(wins[index]);
			index++;
		}
		
		// every 100th set of games show the comparison
		if (played % 100 == 0) {
			System.out.println(sb);
		}

		
	} 

	public void postResults() {
		
		if (wins == null) {
			System.out.println("-- No data to report --");
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < wins.length; i++) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append("Run ").append(i).append(" wins=").append(wins[i]);
		}
		System.out.println(sb);
		
	}
	
}
