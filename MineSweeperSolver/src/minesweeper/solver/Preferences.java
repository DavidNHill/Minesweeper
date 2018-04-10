package minesweeper.solver;

import java.math.BigInteger;

abstract public class Preferences {

	protected int BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 400;
	protected int BRUTE_FORCE_ANALYSIS_MAX_NODES = 50000;
    protected BigInteger BRUTE_FORCE_MAX = new BigInteger("50000000");  // 50 million
    protected BigInteger ZONE_MAX = new BigInteger("10000000");  // 10 million
    protected boolean USE_MIN_MAX = true;
	
    public Preferences() {
    	setVariables();
    }
    
    abstract public void setVariables();
    
    
    /**
     * Does obvious and less obvious moves.
     * Looks for very small independent zones which have to be guessed.
     * No brute force.
     */
    final static public Preferences NO_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {
		   	
	    	BRUTE_FORCE_MAX = new BigInteger("0");  
	    	ZONE_MAX = new BigInteger("2500"); 
	    	USE_MIN_MAX = true;
	    	
		}
    	
    };
    
    /**
     * Does obvious and less obvious moves.
     * Looks for very small independent zones which have to be guessed.
     * Very small brute force at the end which optimally reduces the solution space.
     */
    final static public Preferences TINY_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {
		   	
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 100;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 5000;
	    	BRUTE_FORCE_MAX = new BigInteger("250000");  // 250 thousand
	    	ZONE_MAX = new BigInteger("25000"); 
	    	USE_MIN_MAX = true;
	    	
		}
    	
    };

 
    /**
     * Does obvious and less obvious moves.
     * Looks for small independent zones which have to be guessed.
     * Small brute force at the end of the game which optimally reduces the solution space.
     */
    final static public Preferences SMALL_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {
		   	
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 400;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 75000;
	    	BRUTE_FORCE_MAX = new BigInteger("500000"); // 500 thousand
	    	ZONE_MAX = new BigInteger("50000"); // 50 thousand
	    	USE_MIN_MAX = true;
	    	
		}
    	
    };
    
    
    
    
    /**
     * Does obvious and less obvious moves.
     * Analyses moderate Edges in order to determine a good guess.
     * Looks for small independent zones which have to be guessed 
     * Moderate brute force at the end of the game and uses guesses which minimise the solution space.
     */
    final static public Preferences MEDIUM_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {
			
			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 1000;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 500000;
	    	BRUTE_FORCE_MAX = new BigInteger("2500000"); // 2.5 million
	    	ZONE_MAX = new BigInteger("100000"); // 100 thousand
	    	USE_MIN_MAX = true;
	    	
		}
    	
    };
    
    /**
     * Does obvious and less obvious moves.
     * Analyses large Edges in order to determine a good guess.
     * Looks for large independent zones which have to be guessed.
     * Large brute force at the end of the game and uses guesses which minimise the solution space.
     */
    final static public Preferences LARGE_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {

			BRUTE_FORCE_ANALYSIS_MAX_SOLUTIONS = 4000;
			BRUTE_FORCE_ANALYSIS_MAX_NODES = 20000000;
			BRUTE_FORCE_MAX = new BigInteger("10000000");  // 10 million
		    ZONE_MAX = new BigInteger("1000000");  // 1 million
		    USE_MIN_MAX = true;
		}
    	
    };
    
    /**
     * Does obvious and less obvious moves.
     * Analyses very large Edges in order to determine a good guess.
     * Looks for very large independent zones which have to be guessed.
     * Very large brute force at the end of the game and uses guesses which minimise the solution space.
     */
    final static public Preferences VERY_LARGE_BRUTE_FORCE = new Preferences() {

		@Override
		public void setVariables() {

		    ZONE_MAX = new BigInteger("20000000");  // 20 million
		    USE_MIN_MAX = true;
		}
    	
    };
	
}
