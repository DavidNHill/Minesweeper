package minesweeper.solver;

import java.math.BigDecimal;
import java.util.List;

import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

abstract public class BruteForceAnalysisModel {

	protected boolean completed = false;
	protected boolean tooMany = false;

	abstract protected void addSolution(byte[] solution);

	abstract protected void process();


	protected boolean isComplete() {
		return this.completed;
	}

	protected boolean tooMany() {
		return this.tooMany;
	}

	protected boolean isShallow() {
		return false;
	}

	abstract protected int getSolutionCount();
	
	abstract protected int getMovesProcessed();
	
	abstract protected int getMovesToProcess();
	
	abstract protected Location checkForBetterMove(Location location);
	
	abstract protected long getNodeCount();

	abstract protected Action getNextMove(BoardState boardState);
	
	abstract protected Location getExpectedMove();
	
	abstract protected boolean allDead();
	
	abstract Area getDeadLocations();
	
	abstract BigDecimal getSolveChance();
	
	abstract List<? extends Location> getLocations();
	
}
