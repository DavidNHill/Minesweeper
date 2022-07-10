package minesweeper.solver.bulk;

public abstract class GamePostListener {

	/**
	 * This is run after each game finishes
	 */
	abstract public void postAction(BulkRequest request);
	
	/**
	 * Place the results you want to show here
	 */
	abstract public void postResults();
	
}
