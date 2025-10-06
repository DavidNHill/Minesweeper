package minesweeper.solver.settings;

import java.math.BigInteger;

public class SettingsFactory {
	
	public enum Setting {
		NO_BRUTE_FORCE,
		TINY_ANALYSIS,
		SMALL_ANALYSIS,
		LARGE_ANALYSIS,
		VERY_LARGE_ANALYSIS,
		MAX_ANALYSIS;
	}
	
	final static public SolverSettings GetSettings(Setting setting) {
		
		if (setting == Setting.SMALL_ANALYSIS) {
			return smallAnalysis();
		} else if (setting == Setting.TINY_ANALYSIS) {
			return tinyAnalysis();
		} else if (setting == Setting.LARGE_ANALYSIS) {
			return largeAnalysis();
		} else if (setting == Setting.NO_BRUTE_FORCE) {
			return noBruteForce();
		} else if (setting == Setting.VERY_LARGE_ANALYSIS) {
			return veryLargeAnalysis();
		} else if (setting == Setting.MAX_ANALYSIS) {
			return maxAnalysis();
		} 
		
		return smallAnalysis();
		
	}
	

    private static SolverSettings noBruteForce() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 0;
    	settings.bruteForceVariableSolutions = 0;
    	settings.bruteForceMaxNodes = 0;
    	settings.bruteForceTreeDepth = 10;
    	settings.bruteForceMaxIterations = BigInteger.ZERO;

    	return settings;
    };
	
    private static SolverSettings tinyAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 40;
    	settings.bruteForceVariableSolutions = 15;
    	settings.bruteForceMaxNodes = 150000;
    	settings.bruteForceTreeDepth = 10;
    	settings.bruteForceMaxIterations = new BigInteger("1000000"); // 5 million

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
    	settings.bruteForceVariableSolutions = 250;
    	settings.bruteForceMaxNodes = 300000;
    	settings.bruteForceTreeDepth = 10;
    	settings.bruteForceMaxIterations = new BigInteger("10000000"); // 10 million

    	return settings;
    };
	
    
    /**
     * Does trivial, Local and Probability Engine searches.
     * Does a large brute force search with a 4000 solution deep analysis.
     * This is probably not suitable for bulk runs.
     */
    private static SolverSettings largeAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 4000;
    	settings.bruteForceVariableSolutions = 2000;
		settings.bruteForceMaxNodes = 20000000;     // 20 million
		settings.bruteForceTreeDepth = 10;
		settings.bruteForceMaxIterations = new BigInteger("10000000");  // 10 million

    	return settings;
    };
 
    private static SolverSettings veryLargeAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 20000;
    	settings.bruteForceVariableSolutions = 10000;
		settings.bruteForceMaxNodes = 200000000;     // 200 million
		settings.bruteForceTreeDepth = 3;
		settings.bruteForceMaxIterations = new BigInteger("50000000");  // 50 million

    	return settings;
    };
    
    private static SolverSettings maxAnalysis() {

    	SolverSettings settings = new SolverSettings();
    	
    	settings.bruteForceMaxSolutions = 200000;
    	settings.bruteForceVariableSolutions = 100000;
		settings.bruteForceMaxNodes = 2000000000;     // 2000 million
		settings.bruteForceTreeDepth = 3;
		settings.bruteForceMaxIterations = new BigInteger("500000000");  // 500 million

    	return settings;
    };
    
}
