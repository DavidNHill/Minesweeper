package minesweeperbulk;

import java.text.DecimalFormat;

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
	
	private GuessData data1 = new GuessData();
	private GuessData data2 = new GuessData();
	
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
			target = data1;
		} else {
			target = data2;
		}
		
		target.total++;
		target.sum3BV = target.sum3BV + game.getTotal3BV();
		
	
	} 

	@Override
	public void postResults() {
		
		if (data1.total != 0) {
			double avg3BV1 = (double) data1.sum3BV / (double) data1.total;
	 		System.out.println("No Guess boards " + data1.total + " total 3BV " + data1.sum3BV + " average 3BV " + MASK.format(avg3BV1));
		}
		
		if (data2.total != 0) {
			double avg3BV2 = (double) data2.sum3BV / (double) data2.total;
	 		System.out.println("Guessing boards " + data2.total + " total 3BV " + data2.sum3BV + " average 3BV " + MASK.format(avg3BV2));
		}
		
		double avg3BV = (double) total3BV / (double) played;
 		System.out.println("All boards " + played + " total 3BV " + total3BV + " average 3BV " + MASK.format(avg3BV));
	}
	
}
