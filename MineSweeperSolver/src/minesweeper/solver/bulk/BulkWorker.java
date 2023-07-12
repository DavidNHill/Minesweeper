package minesweeper.solver.bulk;

import java.math.BigDecimal;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Solver;
import minesweeper.solver.bulk.BulkRequest.BulkAction;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;

public class BulkWorker implements Runnable {

	private boolean stop = false;
	private final BulkController controller;
	private final SolverSettings solverSettings;

	protected BulkWorker(BulkController controller, SolverSettings solverSettings) {
		this.controller = controller;
		this.solverSettings = solverSettings;
	}

	@Override
	public void run() {

		//System.out.println(Thread.currentThread().getName() + " is starting");
		
		BulkRequest request = controller.getNextRequest(null);

		while (!stop) {

			if (request.action == BulkAction.STOP) {
				stop = true;
				break;

			} else if (request.action == BulkAction.WAIT) { // wait and then ask again
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}

				request = controller.getNextRequest(null);	
			} else {

				//System.out.println("Playing game sequence " + request.sequence);
				// play the game
				playGame(request);

				// return it to the controller
				request = controller.getNextRequest(request);	

			}

		}
		
		//System.out.println(Thread.currentThread().getName() + " is stopping");

	}

	private void playGame(BulkRequest request) {

		int state;

		// if the game is won or lost already then nothing to do.  This can be the case since we don't know what state the Game State model is in.
		if (request.gs.getGameState() == GameStateModel.WON || request.gs.getGameState() == GameStateModel.LOST) {
			return;
		}
		
		Solver solver = new Solver(request.gs, this.solverSettings, false);
		solver.setPlayStyle(controller.getPlayStyle());
		//solver.setFlagFree(controller.getPlayStyle().flagless);
		//solver.setPlayChords(controller.getPlayStyle().useChords);
		
		int loopCounter = 0;
		
		play: while (true) {

			loopCounter++;
			if (loopCounter % 1000 == 0) {
				System.err.println("Game " + request.gs.showGameKey() + " is looping");
				break play;
			}
			
			Action[] moves;
			try {
				solver.start();
				moves = solver.getResult();
			} catch (Exception e) {
				System.err.println("Game " + request.gs.showGameKey() + " has thrown an exception!");
				e.printStackTrace();
				return;
			}

			if (moves.length == 0) {
				System.err.println(request.gs.getSeed() + " - No moves returned by the solver");
				return;
			}            

			// play all the moves until all done, or the game is won or lost
			for (int i=0; i < moves.length; i++) {

				BigDecimal prob = moves[i].getBigProb();

				if (prob.compareTo(BigDecimal.ZERO) <= 0 || prob.compareTo(BigDecimal.ONE) > 0) {
					System.err.println("Game (" + request.gs.showGameKey() + ") move with probability of " + prob + "! - " + moves[i].toString());
				} 

				boolean result = request.gs.doAction(moves[i]);

				state = request.gs.getGameState();

				// only monitor good guesses (brute force, probability engine, zonal, opening book and hooks)
				if (state == GameStateModel.STARTED || state == GameStateModel.WON) {
					if (!moves[i].isCertainty() ) { 
						request.guesses++;
						//request.fairness = request.fairness + 1d;
						request.fairness = request.fairness.add(BigDecimal.ONE).subtract(prob);
					}
				} else { // otherwise the guess resulted in a loss
					if (!moves[i].isCertainty()) {
						request.guesses++;
						//request.fairness = request.fairness - prob.doubleValue() / (1d - prob.doubleValue());
						request.fairness = request.fairness.subtract(prob);
					}                    
				}

				if (state == GameStateModel.LOST && moves[i].isCertainty()) {
					System.err.println("Game (" + request.gs.showGameKey() + ") lost on move with probability = " + prob + " :" + moves[i].toString());
				}

				if (state == GameStateModel.LOST || state == GameStateModel.WON) {
					break play;
				}
			}            
		}
		
		request.gameValue = solver.getWinValue();
	}
	
	protected void stop() {
		stop = true;
	}
}
