package minesweeper.solver.settings;

import java.math.BigDecimal;
import java.math.BigInteger;

import minesweeper.structure.Location;


public class SolverSettings {

	//private final static BigDecimal PROGRESS_CONTRIBUTION = new BigDecimal("0.052");
	private final static BigDecimal PROGRESS_CONTRIBUTION = new BigDecimal("0.001");  // tiny amount to force a tie-break if everything else is the same
	
	public enum GuessMethod {
		SAFETY_PROGRESS("Safety with progress"),
		SECONDARY_SAFETY_PROGRESS("Secondary safety with progress");
		
		public final String name;
		
		private GuessMethod(String name) {
			this.name = name;
		}
	}
	
	protected BigDecimal progressContribution = PROGRESS_CONTRIBUTION;
	protected int bruteForceVariableSolutions = 200;
	protected int bruteForceMaxSolutions = 400;
	protected int bruteForceMaxNodes = 50000;
	protected int bruteForceTreeDepth = 50;
    protected BigInteger bruteForceMaxIterations = new BigInteger("50000000");  // 50 million

    protected boolean doTiebreak = true;
    protected int rolloutSolutions = 0;
    protected boolean doDomination = true;
    protected boolean do5050Check = true;
    protected boolean doEarly5050Check = false;
    protected boolean doLongTermSafety = true;
    protected boolean testMode = false;
    protected Location startLocation;
    protected GuessMethod guessMethod = GuessMethod.SECONDARY_SAFETY_PROGRESS;
    
    // weighted average of safest and 2nd safest guess 
    protected int weight1 = 4;
    protected int weight2 = 1;
	
    
    protected boolean singleThread = false;
    
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
    
    public SolverSettings setEarly5050Check(boolean check) {
    	
    	if (!locked) {
        	this.doEarly5050Check = check;
    	}
 
    	return this;
    }
    
    public SolverSettings setLongTermSafety(boolean isLongTermSafety) {
    	
    	if (!locked) {
        	this.doLongTermSafety = isLongTermSafety;
    	}
 
    	return this;
    }
    
    public SolverSettings setTestMode(boolean isTestMode) {
    	
    	if (!locked) {
        	this.testMode = isTestMode;
    	}
 
    	return this;
    }
    
    /**
     * Only use a single thread when running the solver
     */
    public SolverSettings setSingleThread(boolean singleThread) {
    	
    	if (!locked) {
        	this.singleThread = singleThread;
    	}
 
    	return this;
    }
    
	public SolverSettings setGuessMethod(GuessMethod guessMethod) {
		
    	if (!locked) {
    		this.guessMethod = guessMethod;
    	}
    	return this;
	}
    
	public SolverSettings setStartLocation(Location start) {
		
		// this can be changed
		this.startLocation = start;
     	return this;
	}
    
	public SolverSettings setProgressContribution(BigDecimal contribution) {
		if (contribution == null) {
			this.progressContribution = PROGRESS_CONTRIBUTION;
		} else {
			this.progressContribution = contribution;
		}
		return this;
	}
	
	public SolverSettings setSafetyWeights(int weight1, int weight2) {
		
    	if (!locked) {
    		this.weight1 = weight1;
    		this.weight2 = weight2;
    	}
    	return this;
	}
	
	public int getBruteForceMaxSolutions() {
		return bruteForceMaxSolutions;
	}

	public int getBruteForceVariableSolutions() {
		return bruteForceVariableSolutions;
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
	
	public boolean isEarly5050Check() {
		return this.doEarly5050Check;
	}
	
	public boolean considerLongTermSafety() {
		return this.doLongTermSafety;
	}
	
	public boolean isTestMode() {
		return testMode;
	}
	
	public boolean isSingleThread() {
		return singleThread;
	}	
	
	/*
	public boolean isExperimentalScoring() {
		return this.experimentalScoring;
	}
	*/
	
	public int getRolloutSolutions() {
		return this.rolloutSolutions;
	}
	
	public boolean isLocked() {
		return locked;
	}

	public GuessMethod getGuessMethod() {
		return guessMethod;
	}

	public Location getStartLocation() {
		return this.startLocation;
	}

	public BigDecimal getProgressContribution() {
		return this.progressContribution;
	}
	
	public int getWeight1() {
		return this.weight1;
	}
	public int getWeight2() {
		return this.weight2;
	}
}
