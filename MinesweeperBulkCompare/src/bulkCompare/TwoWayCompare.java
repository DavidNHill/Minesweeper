package bulkCompare;

import java.io.File;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.BulkRequest;
import minesweeper.solver.bulk.BulkRequestGame;
import minesweeper.solver.bulk.GamePostListener;

public class TwoWayCompare extends GamePostListener {

	private int oneWon = 0;
	private int twoWon = 0;
	private int played = 0;
	private int bothWon = 0;
	private int bothLost = 0;
	private int oneWonTwoLost = 0;
	private int oneLostTwoWon = 0;
	
	@Override
	public void postAction(BulkRequest request) {
		
		if (request.getRequestGames().length < 2) {
			System.out.println("This Monitor must have 2 players");
			System.exit(1);
		}
		played++;
		
		GameStateModel game1 = request.getRequestGames()[0].getGame();
		GameStateModel game2 = request.getRequestGames()[1].getGame();
		
		if (game1.getGameState() == GameStateModel.WON) {
			oneWon++;
			if (game2.getGameState() == GameStateModel.WON) {
				twoWon++;
				bothWon++;
			} else {
				oneWonTwoLost++;
				
				//savePosition(game2, "Advanced pseudo lost");
			}
		} else {
			if (game2.getGameState() == GameStateModel.WON) {
				twoWon++;
				oneLostTwoWon++;
			} else {
				bothLost++;
			}			
		}
		

		
		// every 100th set of games show the comparison
		if (played % 100 == 0) {
			String sb = "Played " + played + ", One wins " + oneWon + ", One wins & Two loses " + oneWonTwoLost + ", One loses & Two wins " + oneLostTwoWon + ", Difference " + (oneLostTwoWon - oneWonTwoLost);
			System.out.println(sb);
		}

		
	} 

	public void postResults() {
		
		System.out.println("Final Result:-");
		String sb = "Played " + played + ", One wins " + oneWon + ", One wins & Two loses " + oneWonTwoLost + ", One loses & Two wins " + oneLostTwoWon + ", Difference " + (oneLostTwoWon - oneWonTwoLost);
		System.out.println(sb);
		
	}
	
	void savePosition(GameStateModel game, String text) {
		
		File saveFile = new File("C:\\Users\\david\\Documents\\Minesweeper\\Positions\\Saved", "Pos_" + game.getSeed() + "_lost.mine");
		if (saveFile.exists()) {
			return;
		}
		try {
			System.out.println("Saving position in file " + saveFile.getAbsolutePath());
			game.savePosition(saveFile, text);
		} catch (Exception e) {
			System.out.println("Save position failed: " + e.getMessage());
		}
		
	}
	
}
