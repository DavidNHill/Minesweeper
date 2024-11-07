package minesweeperbulk;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.BulkRequest;
import minesweeper.solver.bulk.BulkRequestGame;
import minesweeper.solver.bulk.GamePostListener;

public class GuessMonitor extends GamePostListener {

	private static final DecimalFormat MASK = new DecimalFormat("#0.000");
	
	private class GuessData {
		private int total;
		private int sum3BV;
		private int[] values = new int[9];
	}
	
	private final Map<Integer, GuessData> winTable = new HashMap<>();
	private final Map<Integer, GuessData> loseTable = new HashMap<>();
	private int wins = 0;
	private int played = 0;
	private int eights = 0;
	private int ng8 = 0;
	
	@Override
	public void postAction(BulkRequest request) {
		
		BulkRequestGame req = request.getRequestGames()[0];
		GameStateModel game = request.getRequestGames()[0].getGame();
		
		played++;
		
		Map<Integer, GuessData> table;
		
		if (game.getGameState() == GameStateModel.WON) {
			table = winTable;
			wins++;
		} else {
			table = loseTable;
		}
		
		// save every 1000th game
		/*
		if (played % 1000 == 0) {
			File saveFile = new File("C:\\Users\\david\\Documents\\Minesweeper\\Positions\\Saved", "Pos_" + game.getSeed() + ".mine");
			try {
				System.out.println("Saving position in file " + saveFile.getAbsolutePath());
				game.savePosition(saveFile, "");
			} catch (Exception e) {
				System.out.println("Save position failed: " + e.getMessage());
			}
		}
		*/
		
		int guesses = req.getGuesses();
		
		eights = eights + game.getValueCount(8);
		if (game.getGameState() == GameStateModel.WON && req.getGuesses() == 0) {
			ng8 = ng8 + + game.getValueCount(8);
		}
		
		if (table.containsKey(guesses)) {
			GuessData gd = table.get(guesses);
			gd.total++;
			gd.sum3BV = gd.sum3BV + game.getTotal3BV();
			
			for (int i=0; i < gd.values.length; i++) {
				gd.values[i] += game.getValueCount(i);
			}
			
			table.put(guesses, gd);
		} else {
			GuessData gd = new GuessData();
			gd.total = 1;
			gd.sum3BV = game.getTotal3BV();
			table.put(guesses, gd);
		}				
	} 

	@Override
	public void postResults() {
		
		System.out.println(wins + " wins out of " + played + " played");
		System.out.println(eights + " Eights, of which " + ng8 + " in no-guess games");
		
		List<Integer> winResults = new ArrayList<>(winTable.keySet());
		winResults.sort(null);
		
		int winWeight = 0;
		
		System.out.println("Histogram of guesses to win");
		for (int key: winResults) {
			GuessData gd = winTable.get(key);
			double avg3BV = (double) gd.sum3BV / (double) gd.total;
			
			System.out.println("guesses " + key + " occurs " + gd.total + " average 3bv " + MASK.format(avg3BV) + " number of 8's " + gd.values[8]);
			
			winWeight = winWeight + gd.total * key;
		}
		if (wins != 0) {
			double avgGuessesToWin = (double) winWeight / (double) wins;
			System.out.println("Average guesses to win " + MASK.format(avgGuessesToWin));
		}

		
		List<Integer> loseResults = new ArrayList<>(loseTable.keySet());
		loseResults.sort(null);
		
		int loseWeight = 0;
		
		System.out.println("Histogram of guesses to lose");
		for (int key: loseResults) {
			GuessData gd = loseTable.get(key);
			double avg3BV = (double) gd.sum3BV / (double) gd.total;
			
			System.out.println("guesses " + key + " occurs " + gd.total + " average 3bv " + MASK.format(avg3BV) + " number of 8's " + gd.values[8]);
			
			loseWeight = loseWeight + gd.total * key;
		}
		if (wins != 0) {
			double avgGuessesToLose = (double) loseWeight / (double) (played - wins) ;
			System.out.println("Average guesses to lose " + MASK.format(avgGuessesToLose));
		}
		
		
	}
	
}
