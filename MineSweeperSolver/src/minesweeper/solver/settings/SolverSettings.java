package minesweeper.solver.settings;

import java.math.BigInteger;


public class SolverSettings {

	protected int bruteForceMaxSolutions = 400;
	protected int bruteForceMaxNodes = 50000;
	protected int bruteForceTreeDepth = 50;
    protected BigInteger bruteForceMaxIterations = new BigInteger("50000000");  // 50 million

    protected boolean doTiebreak = true;
    protected int rolloutSolutions = 0;
    protected boolean doDomination = true;
    protected boolean do5050Check = true;
    protected boolean testMode = false;
    protected boolean experimentalScoring = false;
	
    private boolean locked;
    
    public SolverSettings lockSettings() {
    	locked = true;
    	
    	return this;
    }
    
    public SolverSettings setTieBreak(boolean doTiebreak) {
    	
    	if (!locked) {
        	this.doTiebreak = doTiebreak;
    	}
 
    	return this;
    }

    public SolverSettings setDomination(boolean doDomination) {
    	
    	if (!locked) {
        	this.doDomination = doDomination;
    	}
 
    	return this;
    }
    
    public SolverSettings setRolloutSolutions(int rolloutSolutions) {
    	
    	if (!locked) {
        	this.rolloutSolutions = rolloutSolutions;
    	}
 
    	return this;
    }
    
    public SolverSettings set5050Check(boolean check) {
    	
    	if (!locked) {
        	this.do5050Check = check;
    	}
 
    	return this;
    }
    
    public SolverSettings setTestMode(boolean isTestMode) {
    	
    	if (!locked) {
        	this.testMode = isTestMode;
    	}
 
    	return this;
    }
    
    public SolverSettings setExperimentalScoring(boolean isExperimental) {
    	
    	if (!locked) {
        	this.experimentalScoring = isExperimental;
    	}
 
    	return this;
    }
    
	public int getBruteForceMaxSolutions() {
		return bruteForceMaxSolutions;
	}

	public int getBruteForceMaxNodes() {
		return bruteForceMaxNodes;
	}

	public int getBruteForceTreeDepth() {
		return bruteForceTreeDepth;
	}

	public BigInteger getBruteForceMaxIterations() {
		return bruteForceMaxIterations;
	}

	public boolean isDoTiebreak() {
		return doTiebreak;
	}

	public boolean isDoDomination() {
		return doDomination;
	}
	
	public boolean isDo5050Check() {
		return this.do5050Check;
	}
	
	public boolean isTestMode() {
		return testMode;
	}
	
	public boolean isExperimentalScoring() {
		return this.experimentalScoring;
	}
	
	public int getRolloutSolutions() {
		return this.rolloutSolutions;
	}
	
	public boolean isLocked() {
		return locked;
	}
    
}
