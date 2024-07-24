package minesweeperbulk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.bulk.BulkRequest;
import minesweeper.solver.bulk.GamePostListener;

public class EfficiencyMonitor extends GamePostListener {

	private final Map<Integer, Integer> table = new HashMap<>();
	private int eff100 = 0;
	private int wins = 0;
	private int played = 0;
	private int announcementCount = 0;
	private int clicks = 0;
	
	private final double reportThreshold;
	
	public EfficiencyMonitor(double reportThreshold) {
		this.reportThreshold = reportThreshold;
	}
	
	@Override
	public void postAction(BulkRequest request) {
		
		GameStateModel game = request.getRequestGames()[0].getGame();
		
		played++;
		clicks = clicks + game.getActionCount();
		
		if (game.getGameState() == GameStateModel.WON) {
			wins++;
			double efficiency = 100 * ((double) game.getTotal3BV() / (double) game.getActionCount());
			
			if (efficiency >= reportThreshold) {
				announcementCount++;
				System.out.println(announcementCount + ") " + game.getSeed() + " has 3BV " + game.getTotal3BV() + " efficiency " + efficiency);
				
				File saveFile = new File("C:\\Users\\david\\Documents\\Minesweeper\\Positions\\Saved", "Pos_" + game.getSeed() + ".mine");
				try {
					System.out.println("Saving position in file " + saveFile.getAbsolutePath());
					game.savePosition(saveFile, "Efficiency of " + efficiency);
				} catch (Exception e) {
					System.out.println("Save position failed: " + e.getMessage());
				}
				
			}	
			
			int inteff = (int) Math.floor(efficiency);
			if (inteff >= 100) {
				eff100++;
			}
			
			if (inteff >= 0) {
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

	public void postResults() {
		
		int clicksPerEff100 = 0;
		if (eff100 > 0) {
			clicksPerEff100 = clicks / eff100;
		}
		
		System.out.println("Efficiency >= 100% : " + eff100 + " from " + wins + " wins out of " + played + " played, clicks " + clicks + ", clicks/eff100 " + clicksPerEff100);
		
		List<Integer> results = new ArrayList<>(table.keySet());
		results.sort(null);
		
		for (int key: results) {
			System.out.println("Efficiency " + key + " occurs " + table.get(key));
		}
		
	}
	
}
