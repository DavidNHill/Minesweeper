package minesweeper.solver.coach;

/**
 * A implementation of the CoachModel class which does nothing 
 * @author David
 *
 */
public class CoachSilent implements CoachModel {

	
	@Override
	public void clearScreen() {
	}

	@Override
	public void writeLine(String text) {
	}

	@Override
	public void setOkay() {
	}

	@Override
	public void setWarn() {
	}

	@Override
	public void setError() {
	}

	@Override
	public void kill() {
	}

	@Override
	public boolean analyseFlags() {
		return false;
	}

}
