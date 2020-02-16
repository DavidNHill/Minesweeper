package minesweeper.explorer.main;

import java.math.BigDecimal;
import java.math.BigInteger;

import minesweeper.explorer.gamestate.GameStateExplorer;
import minesweeper.explorer.structure.Board;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Preferences;
import minesweeper.solver.Solver;

public class BoardMonitor implements Runnable {

	private final MainScreenController controller;
	
	public BoardMonitor(MainScreenController controller) {
		this.controller = controller;
	}
	
	@Override
	public void run() {

		System.out.println("Starting monitor thread");

		String msg = "";
		while (true) {
			
			
			Board board = controller.getCurrentBoard();
			
			// if we haven't placed too many mines
			if (board.getMinesPlaced() <= controller.getGameMines()) {
				
				GameStateModel gs = null;
				try {
					gs = GameStateExplorer.build(board, controller.getGameMines());
					Solver solver = new Solver(gs, Preferences.VERY_LARGE_ANALYSIS, true);
					BigInteger solutionCount = solver.getSolutionCount();
					
					if (solutionCount.signum() == 0) {
						msg = "There are no solutions for this board";
					} else {
						System.out.println(solutionCount.bitLength());
						if (solutionCount.bitLength() > 50) {
							msg = MainScreenController.EXPONENT_DISPLAY.format(solutionCount) + " solutions remain";
						} else {
							msg = MainScreenController.NUMBER_DISPLAY.format(solutionCount) + " solutions remain";
						}
						
					}
					
					System.out.println(solutionCount);
					
				} catch (Exception e) {
					msg = "Unable to calculate solution count: " + e.getMessage();
					e.printStackTrace();
				}

			} else {
				msg = "Invalid number of mines";
				System.out.println("Too many mines placed");
			}
			
			controller.setSolutionLine(msg);
			board = null;
	
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}

}
