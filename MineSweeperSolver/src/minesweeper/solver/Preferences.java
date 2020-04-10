package minesweeper.solver;

import java.math.BigInteger;

abstract public class Preferences {

	protected int BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 400;
	protected int BRUTE_FORCE_ANALYSIS_MAX_NODES = 50000;
	protected int BRUTE_FORCE_ANALYSIS_TREE_DEPTH = 50;
    protected BigInteger BRUTE_FORCE_MAX = new BigInteger("50000000");  // 50 million

    protected boolean doTiebreak = true;
	
    public Preferences() {
    	setVariables();
    }
    
    abstract public void setVariables();
    
    public Preferences setTieBreak(boolean doTiebreak) {
    	this.doTiebreak = doTiebreak;
    	return this;
    }
    
    
    /**
 	*  Testing, you probably shouldn't be using it
      */
    final static public Preferences TEST = new Preferences() {

		@Override
		public void setVariables() {
		   	
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 3200;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 5000000;
	    	BRUTE_FORCE_MAX = new BigInteger("15000000"); // 5 million
	    	
		}
    	
    };
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * No brute force.
     */
    final static public Preferences NO_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {
			
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 0;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 0;
	    	BRUTE_FORCE_MAX = BigInteger.ZERO;  
	    	
		}
    	
    };
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a small brute force search but with no deep analysis.
     */
    final static public Preferences NO_DEEP_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {
		   	
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 0;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 0;
	    	BRUTE_FORCE_MAX = new BigInteger("250000");  // 250 thousand
	    	
		}
    	
    };

    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a small brute force search with a 100 solution deep analysis.
     * This is suitable for bulk runs.
     */
    final static public Preferences VERY_SMALL_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {
		   	
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 100;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 100000;    // 100,000
	    	BRUTE_FORCE_MAX = new BigInteger("500000"); // 500,000
	    	
		}
    	
    };
 
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a small brute force search with a 400 solution deep analysis.
     * This is suitable for bulk runs.
     */
    final static public Preferences SMALL_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {
		   	
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 400;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 150000;
	    	BRUTE_FORCE_MAX = new BigInteger("5000000"); // 5 million
	    	
		}
    	
    };
    
    
    
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a medium brute force search with a 1000 solution deep analysis.
     */
    final static public Preferences MEDIUM_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {
			
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 1000;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 500000;
	    	BRUTE_FORCE_MAX = new BigInteger("10000000"); // 10 million
	    	
		}
    	
    };
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a large brute force search with a 4000 solution deep analysis.
     * This is suitable for individual games.
     */
    final static public Preferences LARGE_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {

			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 4000;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 20000000;     // 20 million
			BRUTE_FORCE_MAX = new BigInteger("10000000");  // 10 million

		}
    	
    };
    
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a very large brute force search with a 40000 solution deep analysis.
     * This WILL cause performance issues and should only be used for single move analysis.
     */
    final static public Preferences VERY_LARGE_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {

			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 40000;  // 40,000
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 200000000;  // 200 millions
			BRUTE_FORCE_ANALYSIS_TREE_DEPTH = 4;          // too much tree depth will cause memory to run out
			BRUTE_FORCE_MAX = new BigInteger("50000000");  // 50 million
		}
    	
    };
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a very large brute force search with a 400000 solution deep analysis.
     * This WILL cause performance issues and should only be used for single move analysis.
     */
    final static public Preferences MAX_ANALYSIS = new Preferences() {

		@Override
		public void setVariables() {

			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 400000;  // 400,000
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 2000000000;  // 2 billion
			BRUTE_FORCE_ANALYSIS_TREE_DEPTH = 3;          // too much tree depth will cause memory to run out
			BRUTE_FORCE_MAX = new BigInteger("100000000");  // 100 million

		}
    	
    };
}
