package minesweeper.solver.bulk;

public abstract class BulkListener {

	/**
	 * This is run at regular intervals and should be used to provide any out put that is needed
	 */
	abstract public void intervalAction(BulkEventMain event);
	
}
