package minesweeper.solver.settings;

import java.math.BigInteger;

public class SettingsFactory {
	
	public enum Setting {
		NO_BRUTE_FORCE,
		SMALL_ANALYSIS,
		LARGE_ANALYSIS,
		VERY_LARGE_ANALYSIS;
	}
	
	final static public SolverSettings GetSettings(Setting setting) {
		
		if (setting == Setting.SMALL_ANALYSIS) {
			return smallAnalysis();
		} else if (setting == Setting.LARGE_ANALYSIS) {
			return largeAnalysis();
		} else if (setting == Setting.NO_BRUTE_FORCE) {
			return noBruteForce();
		} else if (setting == Setting.VERY_LARGE_ANALYSIS) {
			return veryLargeAnalysis();
		} 
		
		return smallAnalysis();
		
	}
	

    private static SolverSettings noBruteForce() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 0;
    	settings.bruteForceMaxNodes = 0;
    	settings.bruteForceTreeDepth = 10;
    	settings.bruteForceMaxIterations = BigInteger.ZERO;

    	return settings;
    };
	
	
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a small brute force search with a 400 solution deep analysis.
     * This is suitable for bulk runs.
     */
    private static SolverSettings smallAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 400;
    	settings.bruteForceMaxNodes = 150000;
    	settings.bruteForceTreeDepth = 10;
    	settings.bruteForceMaxIterations = new BigInteger("5000000"); // 5 million

    	return settings;
    };
	
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a small brute force search with a 400 solution deep analysis.
     * This is suitable for bulk runs.
     */
    private static SolverSettings largeAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 4000;
		settings.bruteForceMaxNodes = 20000000;     // 20 million
		settings.bruteForceTreeDepth = 10;
		settings.bruteForceMaxIterations = new BigInteger("10000000");  // 10 million

    	return settings;
    };
 
    private static SolverSettings veryLargeAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 40000;
		settings.bruteForceMaxNodes = 200000000;     // 200 million
		settings.bruteForceTreeDepth = 3;
		settings.bruteForceMaxIterations = new BigInteger("50000000");  // 50 million

    	return settings;
    };
    
}
