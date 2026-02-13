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

public class BbbvMonitor extends GamePostListener {

	private static final DecimalFormat MASK = new DecimalFormat("#0.000");
	
	private class GuessData {
		private int total = 0;
		private int sum3BV = 0;
	}
	
	private class WinData {
		private int played;
		private int won;
	}
	
	private final Map<Integer, WinData> table = new HashMap<>();
	
	private GuessData noGuess = new GuessData();
	private GuessData guess = new GuessData();
	
	private int played = 0;
	private int total3BV = 0;
	
	@Override
	public void postAction(BulkRequest request) {
		
		BulkRequestGame req = request.getRequestGames()[0];
		GameStateModel game = request.getRequestGames()[0].getGame();
		
		played++;
		total3BV = total3BV + game.getTotal3BV();
		
		GuessData target;
		if (req.getGuesses() == 0) {
			target = noGuess;
		} else {
			target = guess;
		}
		
		target.total++;
		target.sum3BV = target.sum3BV + game.getTotal3BV();
		
		// 3bv distribution of played games
		WinData wd;
		if (table.containsKey(game.getTotal3BV())) {
			wd = table.get(game.getTotal3BV());
		} else {
			wd = new WinData();
			table.put(game.getTotal3BV(), wd);
		}				

		wd.played++;
		if (game.getGameState() == GameStateModel.WON) {
			wd.won++;
		}
		
	} 

	@Override
	public void postResults() {
		
		if (noGuess.total != 0) {
			double avg3BV1 = (double) noGuess.sum3BV / (double) noGuess.total;
	 		System.out.println("No Guess boards " + noGuess.total + " total 3BV " + noGuess.sum3BV + " average 3BV " + MASK.format(avg3BV1));
		}
		
		if (guess.total != 0) {
			double avg3BV2 = (double) guess.sum3BV / (double) guess.total;
	 		System.out.println("Guessing boards " + guess.total + " total 3BV " + guess.sum3BV + " average 3BV " + MASK.format(avg3BV2));
		}
		
		double avg3BV = (double) total3BV / (double) played;
 		System.out.println("All boards " + played + " total 3BV " + total3BV + " average 3BV " + MASK.format(avg3BV));
 		
		List<Integer> results = new ArrayList<>(table.keySet());
		results.sort(null);
		
		for (int key: results) {
			WinData wd = table.get(key); 
			//System.out.println("3BV " + key + " wins " + wd.won + " from " + wd.played);
			System.out.println(key + "\t" + wd.played + "\t" + wd.won);
		}
		
	}
	
}
