package minesweeper.explorer.main;

import java.math.BigInteger;

import minesweeper.explorer.gamestate.GameStateExplorer;
import minesweeper.explorer.structure.Board;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.solver.settings.SettingsFactory.Setting;

public class BoardMonitor implements Runnable {

	private final MainScreenController controller;
	private int lastHash = 1;
	
	public BoardMonitor(MainScreenController controller) {
		this.controller = controller;
	}
	
	@Override
	public void run() {

		System.out.println("Starting monitor thread");

		String msg = "";
		boolean activeButtons = true;
		while (true) {
			
			
			Board board = controller.getCurrentBoard();
			
			int hash = board.getHashValue();
			
			// if we haven't placed too many mines
			if (hash != lastHash) {
				
				controller.removeIndicators();
				
				if (board.getFlagsPlaced() <= controller.getTotalMines()) {
					
					lastHash = hash;
					
					GameStateModel gs = null;
					try {
						gs = GameStateExplorer.build(board, controller.getTotalMines());
						SolverSettings settings = SettingsFactory.GetSettings(Setting.VERY_LARGE_ANALYSIS);
						Solver solver = new Solver(gs, settings, false);
						
						System.out.println("Checking solution count");
						BigInteger solutionCount = solver.getSolutionCount();
						
						if (solutionCount.signum() == 0) {
							msg = "There are no solutions for this board";
							activeButtons = false;
						} else {
							//System.out.println(solutionCount.bitLength());
							activeButtons = true;
							if (solutionCount.bitLength() > 50) {
								msg = MainScreenController.EXPONENT_DISPLAY.format(solutionCount) + " solutions remain";
							} else {
								msg = MainScreenController.NUMBER_DISPLAY.format(solutionCount) + " solutions remain";
							}
							
						}
						
						//System.out.println(solutionCount);
						
					} catch (Exception e) {
						msg = "Unable to calculate solution count: " + e.getMessage();
						activeButtons = false;
						e.printStackTrace();
					}

				} else {
					msg = "Invalid number of mines";
					activeButtons = false;
					//System.out.println("Too many mines placed");
				}				
			}

			
			controller.setSolutionLine(msg);
			controller.setButtonsEnabled(activeButtons);
			board = null;
	
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}

}
