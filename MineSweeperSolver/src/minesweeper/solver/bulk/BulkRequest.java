package minesweeper.solver.bulk;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SolverSettings;

public class BulkRequest {

	protected final static BulkRequest WAIT = new BulkRequest() {
		{
			action = BulkAction.WAIT;
		}
	};
	
	protected final static BulkRequest STOP = new BulkRequest() {
		{
			action = BulkAction.STOP;
		}
	};
	
	public enum BulkAction {
		STOP,
		WAIT,
		RUN;
	}
	
	protected BulkAction action;
	protected int sequence;    // the sequence number for this request
	protected int slot;        // the slot the request is to be store in the buffer
	protected GameStateModel gs;
	//protected Solver solver;
	protected int guesses = 0;
	protected double fairness = 0;
	
}
