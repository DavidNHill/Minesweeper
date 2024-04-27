package minesweeper.solver.bulk;

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
	
	protected BulkRequestGame[] games;
	
	public BulkRequestGame[] getRequestGames( ) {
		return this.games;
	}
	

}
