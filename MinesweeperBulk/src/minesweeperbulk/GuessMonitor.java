package minesweeperbulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.BulkRequest;
import minesweeper.solver.bulk.GamePostListener;

public class GuessMonitor extends GamePostListener {

	private final Map<Integer, Integer> winTable = new HashMap<>();
	private final Map<Integer, Integer> loseTable = new HashMap<>();
	private int wins = 0;
	private int played = 0;
	
	@Override
	public void postAction(BulkRequest request) {
		
		GameStateModel game = request.getGame();
		
		played++;
		
		Map<Integer, Integer> table;
		
		if (game.getGameState() == GameStateModel.WON) {
			table = winTable;
			wins++;
		} else {
			table = loseTable;
		}
		
		int guesses = request.getGuesses();
		
		if (table.containsKey(guesses)) {
			int tally = table.get(guesses);
			tally++;
			table.put(guesses, tally);
		} else {
			table.put(guesses, 1);
		}				
	} 

	@Override
	public void postResults() {
		
		System.out.println(wins + " wins out of " + played + " played");
		
		List<Integer> winResults = new ArrayList<>(winTable.keySet());
		winResults.sort(null);
		
		System.out.println("Histogram of guesses to win");
		for (int key: winResults) {
			System.out.println("guesses " + key + " occurs " + winTable.get(key));
		}
		
		List<Integer> loseResults = new ArrayList<>(loseTable.keySet());
		loseResults.sort(null);
		
		System.out.println("Histogram of guesses to lose");
		for (int key: loseResults) {
			System.out.println("guesses " + key + " occurs " + loseTable.get(key));
		}
		
	}
	
}
