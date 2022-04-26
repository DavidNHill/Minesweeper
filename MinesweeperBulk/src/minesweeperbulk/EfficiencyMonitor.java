package minesweeperbulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.GameListener;

public class EfficiencyMonitor extends GameListener {

	private final Map<Integer, Integer> table = new HashMap<>();
	private int eff100 = 0;
	private int wins = 0;
	private int played = 0;
	
	@Override
	public void gameAction(GameStateModel game) {
		
		played++;
		
		if (game.getGameState() == GameStateModel.WON) {
			wins++;
			double efficiency = 100 * ((double) game.getTotal3BV() / (double) game.getActionCount());
			if (efficiency >= 140) {
				System.out.println(game.getSeed() + " has 3BV " + game.getTotal3BV() + " efficiency " + efficiency);
			}		
			
			int inteff = (int) Math.floor(efficiency);

			if (inteff >= 100) {
				eff100++;
				if (table.containsKey(inteff)) {
					int tally = table.get(inteff);
					tally++;
					table.put(inteff, tally);
				} else {
					table.put(inteff, 1);
				}				
			}
		}

	} 

	public void displayTable() {
		
		System.out.println("Efficiency > 100% : " + eff100 + " from " + wins + " wins out of " + played + " played");
		
		List<Integer> results = new ArrayList<>(table.keySet());
		results.sort(null);
		
		for (int key: results) {
			System.out.println("Efficiency " + key + " occurs " + table.get(key));
		}
		
	}
	
}
