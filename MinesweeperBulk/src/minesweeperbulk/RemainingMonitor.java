package minesweeperbulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.GameListener;

public class RemainingMonitor extends GameListener {

	private final Map<Integer, Integer> table = new HashMap<>();
	private int wins = 0;
	private int played = 0;
	
	@Override
	public void gameAction(GameStateModel game) {
		
		played++;
		
		if (game.getGameState() == GameStateModel.WON) {
			wins++;
		}
		
		int bbvLeft = game.getTotal3BV() - game.getCleared3BV();
		int tilesLeft = game.getHidden() - game.getMines();
		
		if (table.containsKey(tilesLeft)) {
			int tally = table.get(tilesLeft);
			tally++;
			table.put(tilesLeft, tally);
		} else {
			table.put(tilesLeft, 1);
		}				
	} 

	public void displayTable() {
		
		System.out.println(wins + " wins out of " + played + " played");
		
		List<Integer> results = new ArrayList<>(table.keySet());
		results.sort(null);
		
		for (int key: results) {
			System.out.println("Tiles left to clear " + key + " occurs " + table.get(key));
		}
		
	}
	
}
