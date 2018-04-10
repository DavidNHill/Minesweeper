package minesweeper.solver.coach;

public interface CoachModel {
	
	
	abstract public void clearScreen();
	
	abstract public void writeLine(String text);
	
	abstract public void setOkay();
    
	abstract public void setWarn();
    
	abstract public void setError();
	
	abstract public void kill();
	
	abstract public boolean analyseFlags();

}
