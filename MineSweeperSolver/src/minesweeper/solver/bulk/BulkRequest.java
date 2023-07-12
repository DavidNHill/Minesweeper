package minesweeper.solver.bulk;

import java.math.BigDecimal;

import minesweeper.gamestate.GameStateModel;

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
	protected int guesses = 0;
	protected BigDecimal fairness = BigDecimal.ZERO;
	protected BigDecimal gameValue = BigDecimal.ONE;
	protected boolean startedOkay = true;
	
	public GameStateModel getGame( ) {
		return this.gs;
	}
	
	public int getGuesses() {
		return this.guesses;
	}
	
}
