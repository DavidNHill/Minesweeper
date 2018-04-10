/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import Asynchronous.Asynchronous;
import Monitor.AsynchMonitor;
import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;
import minesweeper.solver.coach.CoachModel;
import minesweeper.solver.coach.CoachSilent;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.HookLocation;
import minesweeper.solver.constructs.LinkedLocation;
import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.SubLocation;
import minesweeper.solver.constructs.SuperLocation;
import minesweeper.solver.filter.ContraLinkedFilter;
import minesweeper.solver.filter.Filter;
import minesweeper.solver.filter.HookFilter;
import minesweeper.solver.filter.LinkedFilter;
import minesweeper.solver.filter.MinMaxFilter;
import minesweeper.solver.filter.NullFilter;
import minesweeper.solver.filter.SolveLastFilter;
import minesweeper.solver.filter.SubSquareFilter;
import minesweeper.solver.filter.SuperSquareFilter;
import minesweeper.solver.filter.ZeroFilter;
import minesweeper.solver.iterator.Iterator;
import minesweeper.solver.iterator.SequentialIterator;
import minesweeper.solver.iterator.WitnessWebIterator;
import minesweeper.solver.utility.Binomial;

/**
 *
 * @author David
 */
public class Solver implements Asynchronous<Action[]> {


    // used to hold valid moves which are about to be passed out of the solver
    private class FinalMoves {
        
        Action[] result = new Action[0];
        int suppressedFlags = 0;    // number of place flag moves suppressed because of playing Flag Free
        boolean moveFound = false; // this is set to true if a move is found, even if it is suppressed
        
    	private FinalMoves(Action...actions) {
    		result = actions;
    		moveFound = (actions.length > 0);
    	}

    }
    
    private class FilterTransport{
    	
    	private Filter hookFilter = NULL_FILTER;
    	private Filter solveLastFilter = NULL_FILTER;
    	private Filter subFilter = NULL_FILTER;
    	private Filter superFilter = NULL_FILTER;
    	private Filter linkedFilter = NULL_FILTER;
    	private Filter contraLinkedFilter = NULL_FILTER;
    	private Filter minMaxFilter = NULL_FILTER;
    	private Filter zeroFilter = NULL_FILTER;
    	private ProbabilityEngine probabilityEngine;
    	
    }
    
    private class LoopCheck implements Runnable {

    	private boolean finished = false;
    	
		@Override
		public void run() {
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			
			if (!finished) {
				System.out.println(myGame.showGameKey() + " might be looping");
			}
			
		}
		
    }
    
    public final static String[] METHOD = {"Undefined", "Trivial", "Locally resolved", "Brute Force", 
        "Brute Force Best Guess", "Probability Engine Best Guess", "Probability Engine", "Brute Force Deep Analysis", 
        "Hook", "Zonal Analysis", "Zonal Analysis Best Guess", "Opening book", "Guess"};
    protected final static int OBVIOUS = 1;
    protected final static int LESS_OBVIOUS = 2;
    protected final static int BRUTE_FORCE = 256;
    protected final static int BRUTE_FORCE_BEST_GUESS = 4;
    protected final static int PROBABILITY_ENGINE_BEST_GUESS = 5;
    protected final static int PROBABILITY_ENGINE = 6;
    protected final static int BRUTE_FORCE_DEEP_ANALYSIS = 7;
    protected final static int HOOK = 8;
    protected final static int ZONAL_ANALYSIS = 9;
    protected final static int ZONAL_ANALYSIS_BEST_GUESS = 10;
    protected final static int OPENING_BOOK = 11;
    protected final static int GUESS = 12;
    
    protected final static int DP = 20;

    //private final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    //private final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    
    private final static Filter NULL_FILTER = new NullFilter();
    
    // contour analysis is close to being as good as a full brute force attack and is a lot more efficient
    // so the numbers below prioritise contour analysis above a brute force attack
    //private final static BigDecimal VARIATION = new BigDecimal("0.95");  // how much a contour best guess needs to be be better than an off contour guess
    //private final static BigInteger WITNESSWEB_MAX = new BigInteger("150000000");  // 150 million
    //private final static BigInteger WITNESSWEB_WHOLE_MAX = new BigInteger("50000000"); // 50 million
    //private final static BigInteger BRUTE_FORCE_MAX = new BigInteger("200000000");  // 200 million
    //private final static BigInteger ZONE_MAX = new BigInteger("20000000");  // 20 million
    //private final static int ZONE_TOO_LARGE = 15;  // was 12
    //private final static boolean USE_MIN_MAX = true;
    
    final static BigDecimal EQUAL_TOLERENCE = new BigDecimal("1.00");
    final static BigDecimal PROB_ENGINE_TOLERENCE = new BigDecimal("0.96"); // was 0.96
    final static BigDecimal EDGE_TOLERENCE = new BigDecimal("0.98");
    final static boolean USE_OFF_EDGE_HOOKS = false;
    final static boolean PRUNE_BF_ANALYSIS = true;
    
    // playing chords will make the solver run slower, but should result in less moves
    // it is suggested to play chords if playing an external boarding using mouse controller and you wish to look impressive
    protected final static boolean PLAY_CHORDS = true;
    
    // won't play the book opening on start if false
    protected final static boolean PLAY_OPENING = true;
    
    /**
     * If the number of iterations is less than this then process sequential else go parallel
     */
    final static BigInteger PARALLEL_MINIMUM = new BigInteger("10000");
    
    private final static BigInteger PRIORITISE_MINMAX = new BigInteger("10");
    
    final static int CORES = Runtime.getRuntime().availableProcessors();
    
    // a binomial coefficient generator which allows up to (choose n from 1000000) and builds a cache of everything up to (choose n from 100) 
    static Binomial binomialEngine = new Binomial(1000000, 100); 
    
    
    protected final Preferences preferences;
    
    //private Location[] BOOK_OPEN; 
    //private int[] BOOK_NEIGHBOURS;
    //private int bookMove = 0;
    
    private final List<OpeningLocation> book = new ArrayList<>();
    
    // the class that knows the real board layout, which squares have been revealed and where the flags are
    private final GameStateModel myGame;
    
    // a class which can be used to display the summary information generated by the solver
    private final CoachModel coachDisplay;
    
    // a class which holds the solves current view of the board
    private final BoardState boardState;
    private ProbabilityEngine pe;
    private BruteForce bf;
    
    private BruteForceAnalysisModel bruteForceAnalysis;
    
    private BigDecimal offContourBigProb;
    //private double guessProb;
    private WitnessWeb wholeEdge; 
    private List<HookLocation> goodHooksOffEdge = new ArrayList<>();
    private List<HookLocation> goodHooksOnEdge = new ArrayList<>();
    
    private List<SubLocation> subSquares = new ArrayList<>();
    private List<SuperLocation> superSquares = new ArrayList<>();
    
    //final private List<Location> allWitnessesList = new ArrayList<>();
    
    private List<Zone> zones;
    
    private final boolean interactive;
    
    private FinalMoves answer;

    //private double avgMinesOffContour;
    //private BigInteger totalWeight;
    
    // used to indicate that the solver shouldn't bother placing flags on the board
    // this is considered expert tactics because it reduces the number of mouse actions.
    private boolean flagFree = false;
    
    
    /**
     * Start the solver without a coach display
     * @param myGame
     * @param preferences
     * @param interactive
     */
    public Solver(GameStateModel myGame, Preferences preferences, boolean interactive) {
        this(myGame, preferences, new CoachSilent(), interactive);
    }
    
    /**
     * Start the solver with a coach display
     * @param myGame
     * @param preferences
     * @param interactive
     */
    public Solver(GameStateModel myGame, Preferences preferences, CoachModel coachDisplay, boolean interactive) {
        
        this.myGame = myGame;
        this.interactive = interactive;
        this.preferences = preferences;
        
        this.boardState = new BoardState(this);
        
        // create book moves
        createBook(myGame.getx(), myGame.gety());
        
        display("Running with " + CORES + " Cores");
        display("Solving game " + myGame.showGameKey());
        
        this.coachDisplay = coachDisplay;
        
    }

   
    public void overrideBookOpenings(OpeningLocation...locations) {

    	display("Book openings cleared and replaced with");
    	book.clear();
    	
    	for (OpeningLocation ol: locations) {
    		book.add(ol);
    	}
    	
   	
    }
    
    private void createBook(int x, int y) {

    	/*
        Location[] bookStart = {new Location(0,0), new Location(x-1,0), new Location(x-1, y-1), new Location(0, y-1),
                                new Location(2,0), new Location(0,2), new Location(x-3,0), new Location(x-1,2),
                                new Location(x-3,y-1), new Location(x-1, y-3), new Location(2, y-1), new Location(0, y-3)};
    	*/
    	/*
        Location[] book = {new Location(0,0), new Location(x-1,0), new Location(x-1, y-1), new Location(0, y-1),
                new Location(3,0), new Location(0,3), new Location(x-4,0), new Location(x-1,3),
                new Location(x-4,y-1), new Location(x-1, y-4), new Location(3, y-1), new Location(0, y-4)};
        */
        //Location[] book = {new Location(0,0), new Location(x-1,0), new Location(x-1, y-1), new Location(0, y-1)};
        
        //overrideBookOpenings(bookStart);
 
        //book.add(new OpeningLocation(0,0).addChild(new OpeningLocation(2,0,2)).addChild(new OpeningLocation(0,2,2)));
        //book.add(new OpeningLocation(x-1,0).addChild(new OpeningLocation(x-3,0,2)).addChild(new OpeningLocation(x-1,2,2)));
        //book.add(new OpeningLocation(x-1,y-1).addChild(new OpeningLocation(x-3,y-1,2)).addChild(new OpeningLocation(x-1,y-3,2)));
        //book.add(new OpeningLocation(0,y-1).addChild(new OpeningLocation(2,y-1,2)).addChild(new OpeningLocation(0,y-3,2)));
        
        book.add(new OpeningLocation(0,0));
        book.add(new OpeningLocation(x-1,0));
        book.add(new OpeningLocation(x-1,y-1));
        book.add(new OpeningLocation(0,y-1));
        book.add(new OpeningLocation(2,0));
        book.add(new OpeningLocation(0,2));
        book.add(new OpeningLocation(x-3,0));
        book.add(new OpeningLocation(x-1,2));
        book.add(new OpeningLocation(x-3,y-1));
        book.add(new OpeningLocation(x-1,y-3));
        book.add(new OpeningLocation(2,y-1));
        book.add(new OpeningLocation(0,y-3));

        
    }
    
   
    // Start of Asynchronous methods 
    @Override
    public void start() {
    	
    	LoopCheck check = new LoopCheck();
    	
    	Thread checkThread  = new Thread(check);
    	checkThread.start();
    	
    	int loopSafe = 0;
    	
        answer = process();
        while (answer.moveFound && answer.result.length == 0) {
        	if (loopSafe++ >= 5) {
        		displayError("LOOPSAFE check!! - exiting the processing after " + loopSafe + " iterations");
        		break;
        	}
        	display("There are no moves provided (" + answer.suppressedFlags + " have been supressed) - rerunning the solver");
        	answer = process();
        }
        
        check.finished = true;
        checkThread.interrupt();
        
    }

    @Override
    public void requestStop() {
    }

    @Override
    public Action[] getResult() {
        return answer.result;
    }    
    // end of Asynchronous methods
    
    /**
     * True indicates the solver shouldn't place flags
     * @param flagFree
     */
    public void setFlagFree(boolean flagFree) {
    	this.flagFree = flagFree;
    }
    
    public boolean isFlagFree() {
    	return this.flagFree;
    }
    
    private FinalMoves process() {
        
    	display("--- Starting Analysis ---");
    	
        Action[] result = null;
        
        //openLocation.clear();
        
        FinalMoves fm = new FinalMoves();
        
        long time1 = System.currentTimeMillis();

        coachDisplay.clearScreen();
        pe = null;
        bf = null;
        
        topLine("The Minesweeper Coach's analysis indicates that...");
               
        if (myGame.getGameState() == GameStateModel.LOST) {
            topLine("The game has been lost, so no further analysis is possible");
            return fm;
        }
        
        if (myGame.getGameState() == GameStateModel.WON) {
            topLine("The game has been won, so no further analysis is required");
            return fm;
        }        

        // query the game State object to get the current board position
        boardState.process();
        
        // if there are some 100% moves left to play then do these
        //if (boardState.getActionsCount() != 0) {
        //	return new FinalMoves(boardState.getActions().toArray(new Action[0]));
        //}
        
         // being asked to start the game
        if (myGame.getGameState() == GameStateModel.NOT_STARTED &&  PLAY_OPENING) {
        	
        	offContourBigProb = BigDecimal.ONE;   // this assumes the first guess is not a mine, not always the case
        	
        	fm = guess();
        	
        	newLine("This is the first move");
            newLine("Note: if you aren't accepting guesses nothing will happen!");
            newLine("---------- Recommended Move ----------");
            newLine(fm.result[0].asString());
            newLine("----------  Analysis Ended -----------");        
            
            return fm;
        }                
        
        if (bruteForceAnalysis != null) {
        	Location expectedMove = bruteForceAnalysis.getExpectedMove();
        	if (bruteForceAnalysis.isShallow() || expectedMove == null) {  // if the analysis was shallow then don't rely on it
        		bruteForceAnalysis = null;
        	} else {
             	if (expectedMove != null && !boardState.isRevealed(expectedMove)) {  // we haven't played the recommended move - so the analysis is probably useless
            		display("The expected Brute Force Analysis move " + expectedMove.display() + " wasn't played");
            		bruteForceAnalysis = null;
            	} else {
            		if (myGame.query(expectedMove) != 0) {
                    	Action move = bruteForceAnalysis.getNextMove(boardState);
                    	if (move != null) {
                    		display(myGame.showGameKey() + " Brute Force Deep Analysis " + move.asString());
                            newLine("-------- Brute Force Deep Analysis Tree --------");
                        	newLine(move.asString());
                            newLine("--------  Brute Force Deep Analysis Tree---------");
                    		return new FinalMoves(move);
                    	}        		            			
            		} else {
            			display("After a zero the board can be in an unexpected state, so cancelling Brute Force Analysis moves");
            			bruteForceAnalysis = null;
            		}

            	}       		
        	}
        }
        
        
        // used to hold what we have discovered
        //Moves working = new Moves();

        zones = determineZones();

        int unrevealed = boardState.getTotalUnrevealedCount();
        
        // get all the witnesses on the board and all the squares next to them
        //Location[] allWitnesses = allWitnessesList.toArray(new Location[0]);
        List<Location> allWitnesses = getAllWitnesses(zones);
        List<Location> allWitnessedSquares = boardState.getUnrevealedSquares(allWitnesses);
        newLine("There are " + allWitnesses.size() + " witness(es)");  
        newLine("There are " + allWitnessedSquares.size() + " square(s) witnessed, out of " + unrevealed);
        
        if (unrevealed == 0) {
        	newLine("Nothing to analyse!");
        	//return fm;
        }
        
        
        wholeEdge = new WitnessWeb(boardState, allWitnesses, allWitnessedSquares);
        
        
        int zonal = 0;
        
        
        int obvious = findObvious(allWitnesses);
        
        long time2 = System.currentTimeMillis();

        int lessObvious = findLessObvious(allWitnesses);

        long time3 = System.currentTimeMillis();
        
        // output some text describing the results
        
        int displayObvious = obvious + boardState.getUnplayedMoves(Solver.OBVIOUS);
        int displayLessObvious = lessObvious + boardState.getUnplayedMoves(Solver.LESS_OBVIOUS);
        
        newLine("----------- Basic Analysis -----------");
        newLine("There are " + displayObvious + " trivial moves found in " + (time2 - time1) + " milliseconds");        
        newLine("There are " + displayLessObvious + " locally certain moves found in " + (time3 - time2) + " milliseconds");
        
        display("There are " + (displayObvious + displayLessObvious) + " trivial / locally discoverable certain moves");
        
        
        //display("Obvious / less obvious moves found is " + boardState.getActionsCount());
        fm = new FinalMoves(boardState.getActions1().toArray(new Action[0]));
        if (obvious + lessObvious > 0) {  // in flag free mode we can find moves which we don't play 
        	fm.moveFound = true;
        }
        result = fm.result;
        
        display("There are " + goodHooksOnEdge.size() + " hooks on the edge");
        display("There are " + goodHooksOffEdge.size() + " hooks off the edge");
        
       
        // look for small, self contained areas and try to solve these first
        if (obvious + lessObvious == 0 && !fm.moveFound) {
        	
        	// if the 100% checking hasn't turned up anything get the sub & super squares
        	determineSubSquares();
        	
            if (zones.size() > 1) {  // if there is only one zone then leave it for the brute force analysis
                newLine("----------- Zonal Analysis -----------");
                newLine("There are " + zones.size() + " independent zones on this board");
                fm = zonalAnalysis(zones);
                result = fm.result;
                zonal = result.length;
                if (zonal == 1 && !result[0].isCertainty()) {
                    newLine("One of the zones can never be solved, so guess now");
                } else if (zonal > 1 || zonal == 1 && result[0].isCertainty()) {
                    newLine("There are " + zonal + " moves found by checking the zone");
                } 
            }
        }        

        int minesLeft = myGame.getMines() - boardState.getConfirmedFlagCount();

        if (interactive) {  // can be expensive to do this, so only if we are actually going to display it
            BigInteger comb = combination(minesLeft, unrevealed);
            display("Combinations: choose " + minesLeft + " from " + unrevealed + " gives " + comb);        	
        }

        
        // if we haven't found any moves yet then consider a brute force approach
        //int bruteForce = 0;

        if (obvious + lessObvious + zonal == 0 && !fm.moveFound) {
            display("----- Brute Force starting -----");
            newLine("----------- Brute Force Analysis -----------");
            
            WitnessWeb wholeBoard = new WitnessWeb(boardState, allWitnesses, boardState.getAllUnrevealedSquares());
            bf = new BruteForce(this, boardState, wholeBoard, minesLeft, preferences.BRUTE_FORCE_MAX);
            bf.process();
            if (bf.hasRun()) {
            	newLine("Found  " + bf.getSolutionCount() + " candidate solutions from " + bf.getIterations() + " iterations");
            }

			// Interpret the brute force data if we have some
            this.bruteForceAnalysis = bf.getBruteForceAnalysis();
            if (!bf.hasCertainClear() && bruteForceAnalysis != null) {  // f we haven't found some 100% clears and we can do a deeper analysis
            	bruteForceAnalysis.process();
            	// if after trying to process the data we can't complete then abandon it
            	if (!bruteForceAnalysis.isComplete()) {
            		displayAlways(myGame.showGameKey() + " Abandoned the Brute Force Analysis");
            		bruteForceAnalysis = null;
            		
            	} else { // otherwise try and get the best long term move
            		if (bruteForceAnalysis.isShallow()) {
            			newLine("Built shallow probability tree from " + bruteForceAnalysis.getSolutionCount() + " solutions");
            		}
            		newLine("Built probability tree from " + bruteForceAnalysis.getSolutionCount() + " solutions");
                	Action move = bruteForceAnalysis.getNextMove(boardState);
                	if (move != null) {
                		display(myGame.showGameKey() + " Brute Force Analysis: " + move.asString());
                		fm = new FinalMoves(move);
                	}            		
            	}
            }

            display("----- Brute Force finished -----");

        
            // if we can't do the Brute Force Analysis then find the best move using alternative methods
            if (!fm.moveFound) {
            	
                List<CandidateLocation> best = bf.getBestSolutions(PROB_ENGINE_TOLERENCE);
                
                if (!bf.hasRun()) {
                	newLine("Brute Force rejected - too many iterations to analyse");
                } else if (best.isEmpty()) {
                	newLine("Brute Force didn't find any moves...?");
                } else if (best.get(0).getProbability().compareTo(BigDecimal.ONE) == 0) {
                	newLine("There are " + best.size() + " certain moves");
                } else {
                	newLine("There are no certain moves, so use the best guess");
                }
                
                FilterTransport ft = new FilterTransport();
                
                if (bf.hasRun()) {
                	ft.minMaxFilter = new MinMaxFilter(bf.getCrunchResult());
                	ft.zeroFilter = new ZeroFilter(bf.getZeroLocations());
                	
                	goodHooksOnEdge = bf.setProbabilities(goodHooksOnEdge);
                	goodHooksOffEdge = bf.setProbabilities(goodHooksOffEdge);
                	superSquares = bf.setProbabilities(superSquares);
                	subSquares = bf.setProbabilities(subSquares);

                }
     
            	ft.subFilter = new SubSquareFilter(subSquares);
            	ft.superFilter = new SuperSquareFilter(superSquares);
            	ft.hookFilter = new HookFilter(goodHooksOnEdge, goodHooksOffEdge);
            	ft.solveLastFilter = new SolveLastFilter(zones);
            	
            	
            	display("--- Starting probability engine ---");
            	//ProbabilityEngineOriginal pe = new ProbabilityEngineOriginal(this, wholeEdge, unrevealed, minesLeft);
            	pe = new ProbabilityEngine(boardState, wholeEdge, unrevealed, minesLeft);
            	pe.process();
            	offContourBigProb = pe.getOutsideProb();
            	if (offContourBigProb.compareTo(BigDecimal.ONE) > 0) {
            		displayAlways("Probability off contour is " + offContourBigProb + " (BigDecimal)");
            	} else {
            		display("Probability off contour is " + offContourBigProb + " (BigDecimal)");
            	}
     
            	ft.probabilityEngine = pe;
                ft.linkedFilter = new LinkedFilter(pe.getLinkedLocations());
                ft.contraLinkedFilter = new ContraLinkedFilter(pe.getContraLinkedLocations());
            	
                // if the brute force hasn't run then use the probability engine results
                if (!bf.hasRun()) {
                	best = pe.getBestCandidates(PROB_ENGINE_TOLERENCE);
                }

            	fm = refineBestGuess(PROBABILITY_ENGINE_BEST_GUESS, PROBABILITY_ENGINE, best, pe.getSolutionCount(), ft);
            	
            	// if the best guess is off the edge and we have hooks and a guess on the edge has only a slightly worse probability
            	// compare them against the 
                if (pe.isBestGuessOffEdge() && goodHooksOffEdge.size() > 0 && !best.isEmpty()) {

                }

            	display("--- Probability engine finished ---");
            	
                newLine("----------- Edge Analysis -----------");
                newLine("There are " + pe.getIndependentGroups() + " independent edges on the board");
                newLine("Probability Engine processing took " + pe.getDuration() + " milliseconds");

                result = fm.result;
                
                if (result.length == 1 && !result[0].isCertainty()) {
                    newLine("There are no certain moves, but a good guess has been found on the edge");
                } else if (result.length > 1 || result.length == 1 && result[0].isCertainty()) {
                    newLine("There are " + result.length + " moves found from checking the edges");
                } else {
                    newLine("No moves can be found from checking the edges");
                }                	
            }

        }

        
        
        // if we have't found a move yet then look for Hooks Off the Edge 
        if (!fm.moveFound) {
            newLine("-----Off Edge Guess Analysis-----");
            if (USE_OFF_EDGE_HOOKS && goodHooksOffEdge.size() > 0) {

            	// sorting tries to find a hook with the least hidden adjacent locations... which should maximise the chance of hook coming good
            	Collections.sort(goodHooksOffEdge, HookLocation.SORT_BY_ADJ_HIDDEN);
                Action action = new Action(goodHooksOffEdge.get(0), Action.CLEAR, Solver.HOOK,  METHOD[Solver.HOOK], offContourBigProb);

                // by sending to boardState we centralise the logic which checks the move isn't blocked by a flag
                boardState.setAction(action);
                fm = new FinalMoves(boardState.getActions().toArray(new Action[0]));
                
                display("using a Hook " + action.asString());
                newLine("No certain, or high probability moves found, but there are hooks");
            }
        }
        
        if (!fm.moveFound) {
            newLine("No certain, or high probability moves found, guess away from a witness");
            fm = guess();
        }
        
        if (coachDisplay.analyseFlags() && boardState.getTotalFlagCount() > 0) {
            newLine("----------  Flag Analysis -----------");
            if (boardState.getConfirmedFlagCount() == boardState.getTotalFlagCount()) {
            	coachDisplay.setOkay();
                newLine("All " + boardState.getTotalFlagCount() + " flags have been confirmed as correct");
            } else { 
                newLine((boardState.getTotalFlagCount() - boardState.getConfirmedFlagCount()) + " flags can not be confirmed as correct");
                if (boardState.validateData()) {
                	coachDisplay.setWarn();
                } else {
                    newLine("At least 1 flag is definitely wrong!");
                    coachDisplay.setError();
                }
            }
        } else {
        	coachDisplay.setOkay();
        }
    

        if (fm.result.length > 0) {
            newLine("---------- Recommended Move ----------");
        	newLine(fm.result[0].asString());
            newLine("----------  Analysis Ended -----------");
        }
        
        return fm;
        
    }
   
    private int findObvious(List<Location> witnesses) {
    	
    	int count = 0;
    	
    	for (Location loc: witnesses) {
    		
			if (isObviousClear(loc)) {
				boolean accepted = boardState.setChordLocation(loc);
				
				for (Location l: boardState.getAdjacentSquaresIterable(loc)) {
					if (boardState.isUnrevealed(l)) {
			            if (!boardState.alreadyActioned(l)) {
			                count++;
		                    boardState.setAction(new Action(l, Action.CLEAR, Solver.OBVIOUS, METHOD[Solver.OBVIOUS], BigDecimal.ONE), !accepted);
		                    
			            }
						
					}
				}
			    
			} else if (isObviousFlag(loc)) {
				for (Location l: boardState.getAdjacentSquaresIterable(loc)) {
					if (boardState.isUnrevealed(l)) {
						if (!boardState.alreadyActioned(l)) {
			                count++;

		                    boardState.setAction(new Action(l, Action.FLAG, Solver.OBVIOUS, METHOD[Solver.OBVIOUS],  BigDecimal.ONE));
			                boardState.setFlagConfirmed(l);
			            }
						
					}
				}                		
        	}
    	}
    	
    	
    	
    	return count;
    	
    }
    

    private boolean isObviousClear(Location loc) {
    	
        //if (boardState.isRevealed(x,y) && boardState.getWitnessValue(x,y) != 0) {
            int flags = boardState.countAdjacentConfirmedFlags(loc);
            
            // if we have all the flags and there is something to clear
            if (boardState.getWitnessValue(loc) == flags && boardState.countAdjacentUnrevealed(loc) > 0) {
            	return true;
            }
        //}      	
    	
        return false;
    	
    }
    

     private boolean isObviousFlag(Location loc) {
     	
         //if (boardState.isRevealed(x,y) && boardState.getWitnessValue(x,y) != 0) {
             int flags = boardState.countAdjacentConfirmedFlags(loc);
             int free = boardState.countAdjacentUnrevealed(loc); 
             
             // if we only have space for the flags and there is some space
             if (boardState.getWitnessValue(loc) == flags + free && free > 0) {
             	return true;
             } 
         //}      	
     	
         return false;
     	
     }
     
     private int findLessObvious(List<Location> witnesses) {

    	 int count = 0;

    	 List<Location> square;
    	 List<Location> witness;
    	 List<HookLocation> hooks;

    	 goodHooksOffEdge.clear();
    	 goodHooksOnEdge.clear();

    	 for (Location loc: witnesses) {

    		 int flags = boardState.countAdjacentConfirmedFlags(loc);
    		 int free = boardState.countAdjacentUnrevealed(loc); 

    		 //square = new Location[free];

    		 // if there are still some flags to find and there are 
    		 // too many places for it to be obvious ...
    		 if (free > 0 && boardState.getWitnessValue(loc) > flags && boardState.getWitnessValue(loc) < flags + free) {

    			 // get the un-revealed squares
    			 square = boardState.getAdjacentUnrevealedSquares(loc);

    			 // now get the witnesses
    			 witness = boardState.getWitnesses(square);

    			 // and crunch the result
    			 if (witness.size() > 1) {

    				 //display(i + " " + j + " board " + board[i][j] + " flags = " + flags + " free = " + free);

    				 //hooks = getPossibleHooks(loc.x,loc.y);
    				 hooks = null;

    				 CrunchResult output = crunch(square, witness, hooks, new SequentialIterator(boardState.getWitnessValue(loc) - flags, square.size()), false, null);
    				 count = count + checkBigTally(output, Solver.LESS_OBVIOUS);
    				 count = count + checkWitnesses(output, Solver.LESS_OBVIOUS);


    				 if (hooks != null) {
        				 for (HookLocation hl: hooks) {
        					 if (hl.getState() == HookLocation.OKAY && hl.getValue() > 0) {
        						 if (wholeEdge.isOnWeb(hl)) {
        							 addHook(hl, goodHooksOnEdge);
         						 } else {
        							 addHook(hl, goodHooksOffEdge);
        						 }    								 
        					 } else {
        						 //display(hl.display() + " has failed to be a hook");
        					 }
        				 }    					 
    				 }
    			 }                            

    		 }                                        


    	 }

    	 return count;

     }


     /**
      * Checks whether this location can have the value using a localised check
      */
     private boolean validateLocation(Location superLocation, int value) {
    	 
		 int minesToFit = value - boardState.countAdjacentConfirmedFlags(superLocation);
		 
		 if (minesToFit == 0) {
		 	 return true;
		 } else if (minesToFit < 0) { 
			 return false;
		 }
    	 
    	 // make the move
		 boardState.setWitnessValue(superLocation, value);
		 
		 // get the un-revealed squares
		 List<Location> square = boardState.getAdjacentUnrevealedSquares(superLocation);
		 
		 // now get the witnesses
		 List<Location> witness = boardState.getWitnesses(square);

		 
		 // and crunch the result
		 if (witness.size() > 1) {

			 //display(i + " " + j + " board " + board[i][j] + " flags = " + flags + " free = " + free);
			 
			 CrunchResult output = crunch(square, witness, null, new SequentialIterator(boardState.getWitnessValue(superLocation) - boardState.countAdjacentConfirmedFlags(superLocation), square.size()), false, null);

			 // undo the move
			 boardState.clearWitness(superLocation);
			 
			 if (output.bigGoodCandidates.compareTo(BigInteger.ZERO) > 0) {
				 return true;
			 } else {
				 return false;
			 }
		 
		 } else {                        
			 return false;
		 }
     	 
     }
     
     private void addHook(HookLocation hl, List<HookLocation> list) {
    	 
		if (!validateLocation(hl, hl.getValue())) {
			display(hl.display() + " has failed validation");
			return;
		}
    	 
    	 // if they share the same location then merge the two hooks
    	 for (HookLocation l: list) {
    		 if (l.equals(hl)) {
    			 l.merge(hl);
    			 return;
    		 }
    	 }
    	 // otherwise create a new entry
    	 list.add(hl);
    	 
     }
     
     
    private List<HookLocation> getPossibleHooks(int x1, int y1) {
        
        List<HookLocation> list = new ArrayList<>();
        
        int x = x1 + 2;
        int y = y1;
        if (x < myGame.getx() && boardState.isUnrevealed(x,y)) {
            HookLocation l = new HookLocation(x, y, boardState.countAdjacentConfirmedFlags(x, y), boardState.countAdjacentUnrevealed(x, y));
            //if (!wholeWeb.isOnWeb(l)) {
                list.add(l);
            //}
        }
        
        x = x1 - 2;
        y = y1;
        if (x > 0 && boardState.isUnrevealed(x,y)) {
        	HookLocation l = new HookLocation(x, y, boardState.countAdjacentConfirmedFlags(x, y), boardState.countAdjacentUnrevealed(x, y));
            //if (!wholeWeb.isOnWeb(l)) {
                list.add(l);
            //}
        }        
        
        x = x1;
        y = y1 + 2;
        if (y < myGame.gety() && boardState.isUnrevealed(x,y)) {
        	HookLocation l = new HookLocation(x, y, boardState.countAdjacentConfirmedFlags(x, y), boardState.countAdjacentUnrevealed(x, y));
            //if (!wholeWeb.isOnWeb(l)) {
                list.add(l);
            //}
        }
        
        x = x1;
        y = y1 - 2;
        if (y > 0 && boardState.isUnrevealed(x,y)) {
        	HookLocation l = new HookLocation(x, y, boardState.countAdjacentConfirmedFlags(x, y), boardState.countAdjacentUnrevealed(x, y));
            //if (!wholeWeb.isOnWeb(l)) {
                list.add(l);
            //}
        }
        
        return list;
        
    }
    
    // look for small-ish zones which can be processed
    private FinalMoves zonalAnalysis(List<Zone> targetZones) {
        
        display("Starting Zonal Analysis approach for " + targetZones.size() + " zones");    
         
        Action[] actions = new Action[0];
        
        FinalMoves fm = new FinalMoves();
                
        // if we have found all the mines then all the remaining squares are clears 
        if (myGame.getMines() == boardState.getConfirmedFlagCount() ) {
            display("Zonal Analysis but found all the mines!");
            actions = new Action[targetZones.get(0).getInterior().size()];
            for (int i=0; i < targetZones.get(0).getInterior().size(); i++) {
                actions[i] = new Action(targetZones.get(0).getInterior().get(i), Action.CLEAR, 1, "No mines left",  BigDecimal.ONE);
            }       
            fm = new FinalMoves(actions);
            return fm;
        }        

        //CrunchResult output = null;
        BruteForce bf = null;
        
        for (int i=0; i < targetZones.size(); i++) {
            //if (zone[i].getInterior().length > Solver.ZONE_TOO_LARGE) {
            //   display("Zone " + (i+1) + " is too large");
            //} else
        	if (targetZones.get(i).getWitness().size() == 0) {
                display("Zone " + (i+1) + " has no witnesses");
            } if (!targetZones.get(i).allSquaresWitnessed()) {
            	display("Zone " + (i+1) + " has unwitnessed squares, so can't have a constant number of mines");
            } else {
                
                display("Zone " + (i+1) + " has " + targetZones.get(i).getInterior().size() + " interior squares and "  + targetZones.get(i).getWitness().size() + " witnesses");
        
                // this only brings back a result if all the solutions have the same number of mines
                bf = zonalAnalysis1(i, targetZones.get(i));
                if (bf != null) {
                    break;
                }
            } 
        }
        
        if (bf != null) {
            int count = checkBigTally(bf.getCrunchResult(), Solver.ZONAL_ANALYSIS);
            if (count == 0) {
                display("Zonal Analysis approach looking for best guess");

                // see if we can do a full brute force analysis
                BruteForceAnalysisModel bfa = bf.getBruteForceAnalysis();
                if (bfa != null) {
                	bfa.process();

                	// if after trying to process the data we can't complete then abandon it
                	if (!bfa.isComplete()) {
                		displayAlways(myGame.showGameKey() + " Abandoned the Brute Force Analysis");
                	} else { // otherwise try and get the best long term move
                    	Action move = bfa.getNextMove(boardState);
                    	if (move != null) {
                    		display(myGame.showGameKey() + " Brute Force Analysis: " + move.asString());
                    		fm = new FinalMoves(move);
                    	}            		
                	}
                }
                
                // if we couldn't then do the normal analysis
                if (!fm.moveFound) {
                	fm = determineBestGuess(Solver.ZONAL_ANALYSIS_BEST_GUESS,Solver.ZONAL_ANALYSIS, bf.getCrunchResult());
                }
 
            
            } else {
            	display("Zonal Analysis found some certain moves");
                fm = new FinalMoves(boardState.getActions1().toArray(new Action[0]));
                fm.moveFound = true;  // be explicit about it because flag free is a thing
            }            
        }
        
        display("Ending Zonal Analysis approach with result = " + fm.result.length);    
        
        return fm;
        
    }

    // we don't know how many mines are in this sub-zone, so start with 1 mine.
    // If it transpires that the zone always has the same number of mines in all its valid candidates, then we
    // may as well solve it now, even if that means guessing - since we know we will
    // have to do this at some point.
    private CrunchResult zonalAnalysis(int index, Zone z) {
        
        List<Location> squares = z.getInterior();
        //List<Location> witnesses = z.getWitness();
        
        CrunchResult output = null;

    	WitnessWeb web = z.createWitnessWeb(boardState);
        
    	// check whether there are too many iterations
    	BigInteger iterations = BigInteger.ZERO;
    	for (int n=web.getMinesPlaced(); n <= squares.size(); n++) {
    		iterations = iterations.add(web.getIterations(n));
    	}
    	if (iterations.compareTo(preferences.ZONE_MAX) > 0) {
    		display("Zone " + (index + 1) + " is too large, iterations = " + iterations);
    		return null;
    	} 

    	boolean solveLast = true;
    	
        // this logic assumes that if n-mines has no solutions ==> m-mines where m>n has no solutions. 
        //boolean okay = true;
        for (int n=web.getMinesPlaced(); n <= squares.size(); n++) {
        	
        	BigInteger expectedIterations = web.getIterations(n);
        	
        	WitnessWebIterator[] iterators = buildParallelIterators(web, n, expectedIterations);
        	
        	CrunchResult output1  = crunchParallel(web.getSquares(), web.getWitnesses(), true, iterators);
        	
            int actIterations = 0;
            for (WitnessWebIterator i: iterators) {
                actIterations = actIterations + i.getIterations();
            }
            
            display("Expected iterations = " + expectedIterations + " Actual iterations = " + actIterations);
            //CrunchResult output1 = crunch(squares, witnesses, null, new SequentialIterator(n, squares.length), true);
            
            output1.setWeight(BigInteger.ONE);  // we aren't doing any comparisons across zones, so we can safely give this a weight of one (even though it probably isn't true)
            display("Zonal Analysis crunch mines = " + n + " solutions found = " + output1.bigGoodCandidates);
            if (output1.bigGoodCandidates.compareTo(BigInteger.ZERO) != 0) {
            	if (output1.bigGoodCandidates.compareTo(BigInteger.ONE) != 0) {
            		solveLast = false;
            	}
                if (output == null) {
                    output = output1;
                } else {
                    output = null;
                    //okay = false;
                    break;
                }
            } else {
                if (output != null) {
                    break;
                }
            }       		
 
        }        
        
        // this happens if there are two solutions with different numbers of mine and they both have a solution count of 1.
        // i.e. The zone can be decided with 100% accuracy if we know how many mines go in it, which we might do if we wait until the end
        if (output == null && solveLast) {
        	z.setSolveLast(true);
        	display("Zone has been set to 'solve last' because solution count is 1 for all values of mine examined");
        }
        
        return output;
        
        
    }
    
    private BruteForce zonalAnalysis1(int index, Zone z) {
        
        List<Location> squares = z.getInterior();
        //List<Location> witnesses = z.getWitness();
        
        //CrunchResult output = null;
        BruteForce result = null;

    	WitnessWeb web = z.createWitnessWeb(boardState);
        
    	// check whether there are too many iterations
    	BigInteger iterations = BigInteger.ZERO;
    	for (int n=web.getMinesPlaced(); n <= squares.size(); n++) {
    		iterations = iterations.add(web.getIterations(n));
    	}
    	if (iterations.compareTo(preferences.ZONE_MAX) > 0) {
    		display("Zone " + (index + 1) + " is too large, iterations = " + iterations);
    		return null;
    	} 

    	boolean solveLast = true;
    	
        // this logic assumes that if n-mines has no solutions ==> m-mines where m>n has no solutions. 
        //boolean okay = true;
        for (int n=web.getMinesPlaced(); n <= squares.size(); n++) {
        	
        	
        	BruteForce bf = new BruteForce(this, boardState, web, n, preferences.ZONE_MAX);
        	
        	bf.process();
        	
            display("Zonal Analysis crunch mines = " + n + " solutions found = " + bf.getSolutionCount());
            if (bf.getSolutionCount().compareTo(BigInteger.ZERO) != 0) {
            	if (bf.getSolutionCount().compareTo(BigInteger.ONE) != 0) {
            		solveLast = false;
            	}
                if (result == null) {
                    result = bf;
                } else {
                    result = null;
                    break;
                }
            } else {
                if (result != null) {
                    break;
                }
            }       		
 
        }        
        
        // this happens if there are two solutions with different numbers of mine and they both have a solution count of 1.
        // i.e. The zone can be decided with 100% accuracy if we know how many mines go in it, which we might do if we wait until the end
        if (result == null && solveLast) {
        	z.setSolveLast(true);
        	display("Zone has been set to 'solve last' because solution count is 1 for all values of mine examined");
        }
        
        return result;
        
    }
    
    
    // break a witness web search into a number of non-overlapping iterators
    private WitnessWebIterator[] buildParallelIterators(WitnessWeb web, int mines, BigInteger totalIterations) {
        
    	display("Building parallel iterators");
    	
        //WitnessWebIterator[] result1 = new WitnessWebIterator[1];
        //result1[0] = new WitnessWebIterator(web, mines);
        //return result1;
        
        display("Non independent iterations = " + web.getNonIndependentIterations(mines));
    	
   
        // if there is only one cog then we can't lock it,so send back a single iterator
        if (web.getIndependentWitnesses().size() == 1 && web.getIndependentMines() >= mines || totalIterations.compareTo(PARALLEL_MINIMUM) < 0) {
            display("Only a single iterator will be used");
            WitnessWebIterator[] result = new WitnessWebIterator[1];
            result[0] = new WitnessWebIterator(web, mines);
            return result;
        }
        
        int witMines = web.getIndependentWitnesses().get(0).getMines();
        int squares = web.getIndependentWitnesses().get(0).getSquares().size();
        
        BigInteger bigIterations = Solver.combination(witMines, squares);
        
        int iterations = bigIterations.intValue();
        
        display("The first cog has " + iterations + " iterations, so parallel processing is possible");
        
        WitnessWebIterator[] result = new WitnessWebIterator[iterations];
        
        for (int i=0; i < iterations; i++) {
            result[i] = new WitnessWebIterator(web, mines, i);   // create a iterator with a lock first got at position i
        }
        
        
        
        return result;
        
    }
    
 
    // process the iterators in parallel
    private CrunchResult crunchParallel(List<Square> square, List<? extends Location> witness, boolean calculateDistribution, WitnessWebIterator... iterator) {
        
        display("At parallel crunch");
        
        Cruncher[] crunchers = new Cruncher[iterator.length];
        
        for (int i=0; i < iterator.length; i++) {
            crunchers[i] = new Cruncher(this, iterator[i].getLocations(), witness, null, iterator[i], calculateDistribution, null);
        }
        //Cruncher cruncher = new Cruncher(this, square, witness, hooks, iterator, calculateDistribution);
        
        AsynchMonitor monitor = new AsynchMonitor(crunchers);
        monitor.setMaxThreads(CORES);
        try {
            monitor.startAndWait();
        } catch (Exception ex) {
            System.out.println("Parallel processing caused an error!");
            ex.printStackTrace();
        }
        
        CrunchResult[] results = new CrunchResult[crunchers.length];
        for (int i=0; i < crunchers.length; i++) {
            results[i] = crunchers[i].getResult();
        }

        CrunchResult result = CrunchResult.bigMerge(results);
        
        return result;
        //return crunchers[0].getResult();
        
    }
    
    
    protected CrunchResult crunch(final List<Location> square, final List<? extends Location> witness, List<HookLocation> possibleHooks, Iterator iterator, boolean calculateDistribution, BruteForceAnalysisModel bfa) {
        
        //display("crunching " + iterator.numberBalls + " Mines in " + square.length + " Squares with " + witness.length + " witnesses");

        // the distribution is the number of times a square reveals as the number 0-8
        BigInteger[][] bigDistribution = null;
        if (calculateDistribution) {
            bigDistribution = new BigInteger[square.size()][9];
            for (int i=0; i < square.size(); i++) {
                for (int j=0; j < 9; j++) {
                    bigDistribution[i][j] = BigInteger.ZERO;
                }
            }
        }

        if (possibleHooks == null) {
            possibleHooks = new ArrayList<>();
        }
        
       
        BigInteger bign = BigInteger.ZERO;
        
        //BigInteger[] bigTally = new BigInteger[square.size()];
        //for (int i = 0; i < bigTally.length; i++) {
        //    bigTally[i] = BigInteger.ZERO;
        //}
        
        // determine the witness type
        int[] witnessGood1 = generateWitnessType(witness, square);
        
        // encapsulate the witness data
        final WitnessData[] witnessData = new WitnessData[witness.size()];
        for (int i=0; i < witness.size(); i++) {
            WitnessData d = new WitnessData();
            d.location = witness.get(i);
            d.witnessGood = witnessGood1[i];
            d.witnessRestClear = true;
            d.witnessRestFlag = true;
            d.currentFlags = boardState.countAdjacentConfirmedFlags(d.location);
            d.alwaysSatisfied = iterator.witnessAlwaysSatisfied(d.location);
            //display("Witness " + i + " location " + d.location.display() + " current flags = " + d.currentFlags + " good witness = " + d.witnessGood + " Satisified = " + d.alwaysSatisfied);
            //d.alwaysSatisfied = false;
            witnessData[i] = d;
        }
 
        /*
        for (int i=0; i < square.length; i++) {
            display("Square " + i + " is " + square[i].display());
        }
        */
        
        int[] sample = iterator.getSample();

        int[] tally = new int[square.size()];
        int candidates = 0;
        
        while (sample != null) {
           
            if (checkSample(sample, square, witnessData, possibleHooks, bigDistribution, bfa)) {
                for (int i=0; i < sample.length; i++) {
                	tally[sample[i]]++;
                    //bigTally[sample[i]] = bigTally[sample[i]].add(BigInteger.ONE);
                }   
                candidates++;
                //bign = bign.add(BigInteger.ONE);
            }
            
            sample = iterator.getSample();
            
        }
        
        BigInteger[] bigTally = new BigInteger[square.size()];
        for (int i = 0; i < bigTally.length; i++) {
            //bigTally[i] = new BigInteger(String.valueOf(tally[i]));
            bigTally[i] = BigInteger.valueOf(tally[i]);
            
        }
        
        bign = BigInteger.valueOf(candidates);
        
        // store all the information we have gathered into this object for
        // later analysis
        CrunchResult output = new CrunchResult();
        output.setSquare(square);
        output.bigDistribution = bigDistribution;
        
        if (bigDistribution != null) {
            
            output.calculateMinMax();
            
        }

        //output.witness = witness;
        
        //output.hookMines = null;
        output.originalNumMines = iterator.getBalls();
        output.bigGoodCandidates = bign;
        output.bigTally = bigTally;
        
        // return data on the witnesses
        output.witness = new Location[witnessData.length];
        output.witnessGood = new int[witnessData.length];
        output.witnessRestClear = new boolean[witnessData.length];
        output.witnessRestFlags = new boolean[witnessData.length];
        
        for (int i=0; i < witnessData.length; i++) {
            output.witness[i] = witnessData[i].location;
            output.witnessGood[i] = witnessData[i].witnessGood;
            output.witnessRestClear[i] = witnessData[i].witnessRestClear;
            output.witnessRestFlags[i] = witnessData[i].witnessRestFlag;
        }
        
        return output;
        
    } 
    
    // this checks whether the positions of the mines are a valid candidate solution
    protected boolean checkSample(final int[] sample, final List<Location> square, WitnessData[] witnessData, List<HookLocation> hooks, BigInteger[][] bigDistribution, BruteForceAnalysisModel bfa) {
        
        /*
        String s= "";
        for (int i = 0; i < sample.length; i++) {
            s = s + " " + sample[i];
        }
        display(s);
        */

        boolean[] workRestNotFlags = new boolean[witnessData.length];
        boolean[] workRestNotClear = new boolean[witnessData.length];
        
        // get the location of the mines
        Location[] mine = new Location[sample.length];
        for (int i=0; i < sample.length; i++) {
            mine[i] = square.get(sample[i]);
        }
        
        for (int i=0; i < witnessData.length; i++) {
            
            if (!witnessData[i].alwaysSatisfied) {
                int flags1 = witnessData[i].currentFlags;
                int flags2 = 0;

                // count how many candidate mines are next to this witness
                for (int j = 0; j < mine.length; j++) {
                    if (mine[j].isAdjacent(witnessData[i].location)) {
                        flags2++;
                    }
                }

                int flags3 = boardState.getWitnessValue(witnessData[i].location);
                //int flags3 = board[witnessData[i].location.x][witnessData[i].location.y];

                // if the candidate solution puts more flags around the witness
                // than it says it has then the solution is invalid
                if (flags3 < flags1 + flags2) {
                    WitnessData d = witnessData[0];
                    witnessData[0] = witnessData[i];
                    witnessData[i] = d;
                    return false;
                }

                // if this is a 'good' witness and the number of flags around it
                // does not match with it exactly then the solution is invalid
                if (witnessData[i].witnessGood == 0 && flags3 != flags1 + flags2) {
                    WitnessData d = witnessData[0];
                    witnessData[0] = witnessData[i];
                    witnessData[i] = d;
                    return false;
                }

                if (flags3 != flags1 + flags2) {
                    workRestNotClear[i] = true;
                }
                if (flags3 != flags1 + flags2 + witnessData[i].witnessGood) {
                    workRestNotFlags[i] = true;
                }
            } else {
                // always satisfied means flag3 = flag1 + flag2, so the checks above can be simplified to
                if (witnessData[i].witnessGood != 0) {
                    workRestNotFlags[i] = true;
                }                
            }
             
        }
        
        // if we have got this far then the solution is valid
        
        // if it is a good candidate then check the potential hooks to see if they
        // are still potential hooks - every solution must have the same number of mines around the hook.
        /*
        for (HookLocation hook: hooks) {
        	if (hook.getState() != HookLocation.REJECTED) {
                int count = 0;
                for (Location m: mine) {
                    if (m.isAdjacent(hook)) {
                        count++;
                    }
                }        		
                if (hook.getState() == HookLocation.NO_DECISION) {
                    hook.setValue(count);
                    hook.setState(HookLocation.OKAY);
                } else if (hook.getExtraValue() != count) {
                    hook.setState(HookLocation.REJECTED);
                }                               
        		
        	}
        }
        */
        
        // if it is a good candidate solution then the witness information is valid
        for (int i=0; i < witnessData.length; i++) {
            if (workRestNotClear[i]) {
                witnessData[i].witnessRestClear = false;
            }
            if (workRestNotFlags[i]) {
                witnessData[i].witnessRestFlag = false;
            }
        }

        //if it is a good solution then calculate the distribution if required
        if (bigDistribution != null) {
        	byte[] solution = new byte[square.size()];
        	
            for (int i=0; i < square.size(); i++) {
                
                boolean isMine = false;
                for (int j=0; j < sample.length; j++) {
                    if (i == sample[j]) {
                        isMine = true;
                        break;
                    }
                }
                
                // if we are a mine then it doesn't matter how many mines surround us
                if (!isMine) {
                    byte flags2 = (byte) boardState.countAdjacentConfirmedFlags(square.get(i));
                    // count how many candidate mines are next to this square
                    for (Location mine1 : mine) {
                        if (mine1.isAdjacent(square.get(i))) {
                            flags2++;
                        }
                    }            
                    solution[i] = flags2;
                    bigDistribution[i][flags2] = bigDistribution[i][flags2].add(BigInteger.ONE);
                } else {
                	solution[i] = GameStateModel.MINE;
                }

            }
            if (bfa != null && !bfa.tooMany()) {
            	bfa.addSolution(solution);
            }
        }

        return true;
        
    }
    
    
    protected CrunchResult crunch1(final List<Location> square, final List<? extends Location> witness, List<HookLocation> possibleHooks, Iterator iterator, boolean calculateDistribution, BruteForceAnalysis bfa) {
        
        //display("crunching " + iterator.numberBalls + " Mines in " + square.length + " Squares with " + witness.length + " witnesses");

        // the distribution is the number of times a square reveals as the number 0-8
        BigInteger[][] bigDistribution = null;
        if (calculateDistribution) {
            bigDistribution = new BigInteger[square.size()][9];
            for (int i=0; i < square.size(); i++) {
                for (int j=0; j < 9; j++) {
                    bigDistribution[i][j] = BigInteger.ZERO;
                }
            }
        }

        if (possibleHooks == null) {
            possibleHooks = new ArrayList<>();
        }
        
       
        BigInteger bign = BigInteger.ZERO;
        
        //BigInteger[] bigTally = new BigInteger[square.size()];
        //for (int i = 0; i < bigTally.length; i++) {
        //    bigTally[i] = BigInteger.ZERO;
        //}
        
        // determine the witness type
        //int[] witnessGood1 = generateWitnessType(witness, square);
        
        CrunchResult output = new CrunchResult();
        output.witness = witness.toArray(new Location[0]);
        //output.witnessGood = witnessGood1;
        output.witnessRestClear = new boolean[witness.size()];
        output.witnessRestFlags = new boolean[witness.size()];
        output.setSquare(square);
        output.bigDistribution = bigDistribution;
        output.originalNumMines = iterator.getBalls();
        
        output.alwaysSatisfied = new boolean[witness.size()];
        output.currentFlags = new int[witness.size()];
        output.witnessGood = new int[witness.size()];
        
        for (int i=0; i < witness.size(); i++) {
        	
        	 output.alwaysSatisfied[i] = iterator.witnessAlwaysSatisfied(witness.get(i));
        	 output.currentFlags[i] = boardState.countAdjacentConfirmedFlags(witness.get(i));
        	
            for (Location l: boardState.getAdjacentUnrevealedSquares(witness.get(i))) {
                boolean found = false;
            	for (Location squ: square) {
            		if (l.equals(squ)) {
                        found = true;
                        break;            			
            		}
            	}
                if (!found) {
                	output.witnessGood[i]++; 
                }            	
            }

        }
        
        /*
        // encapsulate the witness data
        final WitnessData[] witnessData = new WitnessData[witness.size()];
        for (int i=0; i < witness.size(); i++) {
            WitnessData d = new WitnessData();
            //d.location = witness.get(i);
            //d.witnessGood = witnessGood1[i];
            //d.witnessRestClear = true;
            //d.witnessRestFlag = true;
            d.currentFlags = boardState.countAdjacentConfirmedFlags(witness.get(i));
            d.alwaysSatisfied = iterator.witnessAlwaysSatisfied(witness.get(i));
            //display("Witness " + i + " location " + d.location.display() + " current flags = " + d.currentFlags + " good witness = " + d.witnessGood + " Satisified = " + d.alwaysSatisfied);
            //d.alwaysSatisfied = false;
            witnessData[i] = d;
        }
 		*/
        
        /*
        for (int i=0; i < square.length; i++) {
            display("Square " + i + " is " + square[i].display());
        }
        */
        
        int[] sample = iterator.getSample();

        int[] tally = new int[square.size()];
        int candidates = 0;
        
        while (sample != null) {
           
            if (checkSample1(sample, possibleHooks, output, bfa)) {
                for (int i=0; i < sample.length; i++) {
                	tally[sample[i]]++;
                    //bigTally[sample[i]] = bigTally[sample[i]].add(BigInteger.ONE);
                }   
                candidates++;
                //bign = bign.add(BigInteger.ONE);
            }
            
            sample = iterator.getSample();
            
        }
        
        output.bigTally = new BigInteger[square.size()];
        for (int i = 0; i < output.bigTally.length; i++) {
        	output.bigTally[i] = BigInteger.valueOf(tally[i]);
            
        }
        
        //bign = BigInteger.valueOf(candidates);
        
        // store all the information we have gathered into this object for
        // later analysis
       //CrunchResult output = new CrunchResult();
        
        if (output.bigDistribution != null) {
             output.calculateMinMax();
        }

        //output.witness = witness;
        
        //output.hookMines = null;

        output.bigGoodCandidates = BigInteger.valueOf(candidates);;
        
        // return data on the witnesses
        //output.witness = new Location[witnessData.length];
        //output.witnessGood = new int[witnessData.length];
        //output.witnessRestClear = new boolean[witnessData.length];
        //output.witnessRestFlags = new boolean[witnessData.length];
        
        //for (int i=0; i < witnessData.length; i++) {
        //    output.witness[i] = witnessData[i].location;
        //    output.witnessGood[i] = witnessData[i].witnessGood;
        //    output.witnessRestClear[i] = witnessData[i].witnessRestClear;
        //    output.witnessRestFlags[i] = witnessData[i].witnessRestFlag;
       //}
        
        return output;
        
    } 
    
    // this checks whether the positions of the mines are a valid candidate solution
    protected boolean checkSample1(final int[] sample, List<HookLocation> hooks, final CrunchResult result, BruteForceAnalysis bfa) {
        
        /*
        String s= "";
        for (int i = 0; i < sample.length; i++) {
            s = s + " " + sample[i];
        }
        display(s);
        */

        boolean[] workRestNotFlags = new boolean[result.witness.length];
        boolean[] workRestNotClear = new boolean[result.witness.length];
        
        // get the location of the mines
        Location[] mine = new Location[sample.length];
        for (int i=0; i < sample.length; i++) {
            mine[i] = result.square.get(sample[i]);
        }
        
        for (int i=0; i < result.witness.length; i++) {
            
            if (!result.alwaysSatisfied[i]) {
                int flags1 = result.currentFlags[i];
                int flags2 = 0;

                // count how many candidate mines are next to this witness
                for (int j = 0; j < mine.length; j++) {
                    if (mine[j].isAdjacent(result.witness[i])) {
                        flags2++;
                    }
                }

                int flags3 = boardState.getWitnessValue(result.witness[i]);
                //int flags3 = board[witnessData[i].location.x][witnessData[i].location.y];

                // if the candidate solution puts more flags around the witness
                // than it says it has then the solution is invalid
                if (flags3 < flags1 + flags2) {
                    //WitnessData d = witnessData[0];
                    //witnessData[0] = witnessData[i];
                    //witnessData[i] = d;
                    return false;
                }

                // if this is a 'good' witness and the number of flags around it
                // does not match with it exactly then the solution is invalid
                if (result.witnessGood[i] == 0 && flags3 != flags1 + flags2) {
                    //WitnessData d = witnessData[0];
                    //witnessData[0] = witnessData[i];
                    //witnessData[i] = d;
                    return false;
                }

                if (flags3 != flags1 + flags2) {
                    workRestNotClear[i] = true;
                }
                if (flags3 != flags1 + flags2 + result.witnessGood[i]) {
                    workRestNotFlags[i] = true;
                }
            } else {
                // always satisfied means flag3 = flag1 + flag2, so the checks above can be simplified to
                if (result.witnessGood[i] != 0) {
                    workRestNotFlags[i] = true;
                }                
            }
             
        }
        
        // if we have got this far then the solution is valid
        
        // if it is a good candidate then check the potential hooks to see if they
        // are still potential hooks - every solution must have the same number of mines around the hook.
        /*
        for (HookLocation hook: hooks) {
        	if (hook.getState() != HookLocation.REJECTED) {
                int count = 0;
                for (Location m: mine) {
                    if (m.isAdjacent(hook)) {
                        count++;
                    }
                }        		
                if (hook.getState() == HookLocation.NO_DECISION) {
                    hook.setValue(count);
                    hook.setState(HookLocation.OKAY);
                } else if (hook.getExtraValue() != count) {
                    hook.setState(HookLocation.REJECTED);
                }                               
        		
        	}
        }
        */
        
        // if it is a good candidate solution then the witness information is valid
        for (int i=0; i < result.witness.length; i++) {
            if (workRestNotClear[i]) {
                result.witnessRestClear[i] = false;
            }
            if (workRestNotFlags[i]) {
                result.witnessRestFlags[i] = false;
            }
        }

        //if it is a good solution then calculate the distribution if required
        if (result.bigDistribution != null) {
        	byte[] solution = new byte[result.square.size()];
        	
            for (int i=0; i < result.square.size(); i++) {
                
                boolean isMine = false;
                for (int j=0; j < sample.length; j++) {
                    if (i == sample[j]) {
                        isMine = true;
                        break;
                    }
                }
                
                // if we are a mine then it doesn't matter how many mines surround us
                if (!isMine) {
                    byte flags2 = (byte) boardState.countAdjacentConfirmedFlags(result.square.get(i));
                    // count how many candidate mines are next to this square
                    for (Location mine1 : mine) {
                        if (mine1.isAdjacent(result.square.get(i))) {
                            flags2++;
                        }
                    }            
                    solution[i] = flags2;
                    result.bigDistribution[i][flags2] = result.bigDistribution[i][flags2].add(BigInteger.ONE);
                } else {
                	solution[i] = GameStateModel.MINE;
                }

            }
            if (bfa != null && !bfa.tooMany()) {
            	bfa.addSolution(solution);
            }
        }

        return true;
        
    }
    
    // a witness is a 'good' witness if all its adjacent free squares are also
    // contained in the set of squares being analysed. A 'good' witness must
    // always be satisfied for the candidate solution to be valid.
    // this method returns the number of squares around the witness not being
    // analysed - a good witness has a value of zero
   protected int[] generateWitnessType(List<? extends Location> witness, List<Location> square) {
        
        int[] result = new int[witness.size()];
        
        for (int i=0; i < witness.size(); i++) {
            
            result[i] = 0;
            for (Location l: boardState.getAdjacentUnrevealedSquares(witness.get(i))) {
                boolean found = false;
            	for (Location squ: square) {
            		if (l.equals(squ)) {
                        found = true;
                        break;            			
            		}
            	}
                if (!found) {
                    result[i]++; 
                }            	
            }

        }
        
        return result;
        
    }
    
    // do the tally check using the BigInteger values
    private int checkBigTally(CrunchResult output, int method) {
        
        int result=0;
        
        // if there were no good candidates then there is nothing to check
        if (output.bigGoodCandidates.compareTo(BigInteger.ZERO) == 0) {
            return 0;
        }

        // check the tally information to see if we have a square where a
        // mine is always present or never present
        for (int i=0; i < output.bigTally.length; i++) {
            
            if (output.bigTally[i].compareTo(output.bigGoodCandidates) == 0) {
            	Location l = output.getSquare().get(i);
                //int x = output.getSquare()[i].x;
                //int y = output.getSquare()[i].y;
                if (!boardState.alreadyActioned(l)) {
                    result++;

                    //int index = method;
                    //String comment = METHOD[method];
                    
                    boardState.setAction(new Action(l, Action.FLAG, method, METHOD[method], BigDecimal.ONE));
                    boardState.setFlagConfirmed(l);
                    
                }

            } else if (output.bigTally[i].compareTo(BigInteger.ZERO) == 0) {
            	Location l = output.getSquare().get(i);
                //int x = output.getSquare()[i].x;
                //int y = output.getSquare()[i].y;
                if (!boardState.alreadyActioned(l)) {
                    result++;

                    //int index = method>>8;
                    //String comment = METHOD[index];
                    
                    boardState.setAction(new Action(l, Action.CLEAR, method, METHOD[method], BigDecimal.ONE));
                    //display("clear found at " + x + " " + y);
                }                
            }

        }        
        
        return result;
        
    }
    
    private FinalMoves determineBestGuess(int methodGuess, int methodCertain, CrunchResult crunchResult) {

    	//display("Crunch Results passed to determine best guess = " + output.length);

        List<CandidateLocation> candidates = new ArrayList<>();
        
		boolean ignoreBad = true;
		if (crunchResult.getMaxCount() <= 1) {
			ignoreBad = false;
			display("No candidates provide additional information");
		}
        
        
        
        // Calculate the probability of a mine being in the square and store in a list
       for (int i=0; i < crunchResult.bigTally.length; i++) {

           BigDecimal mine = new BigDecimal(crunchResult.bigTally[i]).divide(new BigDecimal(crunchResult.bigGoodCandidates), DP, RoundingMode.HALF_UP);
           BigDecimal notMine = BigDecimal.ONE.subtract(mine);
           
           Location l = crunchResult.getSquare().get(i);
           
			if (crunchResult.getBigCount()[i]  > 1 || !ignoreBad || notMine.compareTo(BigDecimal.ZERO) == 0 || notMine.compareTo(BigDecimal.ONE) == 0) {
				candidates.add(new CandidateLocation(l.x, l.y, notMine, boardState.countAdjacentUnrevealed(l), boardState.countAdjacentConfirmedFlags(l), crunchResult.getBigCount()[i]));
			} else {
				display(l.display() + " clear probability " + notMine + " discarded because it reveals no further information");
			}
           
           //candidates.add(new CandidateLocation(l.x, l.y, notMine, boardState.countAdjacentUnrevealed(l), boardState.countAdjacentConfirmedFlags(l)));

       }        
        
       // sort the candidates into descending order by probability
       Collections.sort(candidates, CandidateLocation.SORT_BY_PROB_FLAG_FREE);
        
       BigDecimal bestNotMineTolerence = candidates.get(0).getProbability().multiply(Solver.EQUAL_TOLERENCE);
       
        List<CandidateLocation> best = new ArrayList<>();
        
        for (CandidateLocation cl: candidates) {
        	if (cl.getProbability().compareTo(bestNotMineTolerence) >= 0) {
        		best.add(cl);
        	} else {
        		break;
        	}
        }

        display("Best Guess: " + candidates.size() + " candidates, " + best.size() + " passed tolerence at " + bestNotMineTolerence);
        
        
        boolean found = false;
         
        FilterTransport ft = new FilterTransport();

    	ft.subFilter = new SubSquareFilter(subSquares);
    	ft.superFilter = new SuperSquareFilter(superSquares);
    	ft.hookFilter = new HookFilter(goodHooksOnEdge, goodHooksOffEdge);
    	ft.solveLastFilter = new SolveLastFilter(zones);
        
        if (preferences.USE_MIN_MAX && !found && crunchResult.bigDistribution != null && (methodGuess == Solver.BRUTE_FORCE_BEST_GUESS || methodGuess == Solver.ZONAL_ANALYSIS_BEST_GUESS)) {

        	ft.minMaxFilter = new MinMaxFilter(crunchResult);

        } else {
        	ft.minMaxFilter = new NullFilter();
        }
         
        return refineBestGuess(methodGuess, methodCertain, best, crunchResult.bigGoodCandidates, ft);
        
    }    
    
    
    //private Action[] refineBestGuess(Moves working, int method, List<CandidateLocation> best, BigInteger solutionsCount) {
    //	return  refineBestGuess(working, method, best, solutionsCount, new NullFilter());
    //}
    
    
    private FinalMoves refineBestGuess(int methodGuess, int methodCertain, List<CandidateLocation> best, BigInteger solutionsCount, FilterTransport ft) {
    	
    	Collections.sort(best, CandidateLocation.SORT_BY_PROB_FLAG_FREE);  // this is better then prob, free, flag
    	//Collections.sort(best, CandidateLocation.SORT_BY_PROB_FREE_FLAG);
    	
    	display("Number of ways the mines can be placed is " + solutionsCount);
    	display("Number of locations being considered " + best.size());
    	
    	if (best.isEmpty()) {
    		return new FinalMoves();
    	}

    	BigDecimal bestProbability = best.get(0).getProbability();
    	
    	// show all the considered moves by probability
    	String disp = "";
    	BigDecimal val = bestProbability;
    	for (CandidateLocation cl: best) {
    		if (cl.getProbability().compareTo(val) != 0) {
    			display(disp + " " + val);
    			disp = "";
    			val = cl.getProbability();
    		}
    		disp = disp + cl.display();
    	}
    	display(disp + " " + val);
    	

    	// if we have some certainties then no need to do any other analysis
    	if (bestProbability.compareTo(BigDecimal.ONE) == 0) {
    		for (CandidateLocation cl: best) {
    			if (cl.getProbability().compareTo(BigDecimal.ONE) == 0) {
    				boardState.setAction(cl.buildAction(methodCertain));
    			}
    		}
    		
    		FinalMoves fm = new FinalMoves(boardState.getActions1().toArray(new Action[0]));
    		fm.moveFound = true;   // this ensures the moves are recognised even if they are all flags in flag-free mode
    		
    		return fm;
    		
    	}

        // if there is only one best solution then label it as such
        if (best.size() == 1) {
            best.get(0).appendDescription(" [only one]");
        } else {
        	// this makes a small difference by deferring guessing locations which we hope will become obvious later
        	best = ft.solveLastFilter.filter(best);
        }


        best = ft.minMaxFilter.filter(best, MinMaxFilter.WINS_ONLY);  // look for a win
        	
       	best = ft.linkedFilter.filter(best);
       	
       	best = ft.superFilter.filter(best);

       	//best = ft.contraLinkedFilter.filter(best);

    	
       	if (!ft.minMaxFilter.didFilter() && best.size() > 1) {
       		//best = ft.minMaxFilter.filter(best, MinMaxFilter.FIFTY_FIFTY);
       		best = ft.minMaxFilter.filter(best, 3);   // 3 seems to be best based on a small sample
       	}
       	
     	//HookFilter hookFilter = new HookFilter(goodHooksOnEdge, goodHooksOffEdge);
    	best = ft.hookFilter.filter(best);

     	//SubSquareFilter subFilter = new SubSquareFilter(subSquares);
    	//best = ft.subFilter.filter(best);
    	
    	// look for guesses which have a high probability of already being satisfied by their adjacent flags
       	//best = ft.zeroFilter.filter(best);
       	
       	// if the wins only filter worked then no point doing it again. This really only tidies up the display. 
       	if (!ft.minMaxFilter.didFilter() && best.size() > 1) {
       		best = ft.minMaxFilter.filter(best); // pick the guess which most reduces the solution space
       	}
    	
    	
        // Use the first on the list after all the above filtering
    	CandidateLocation bestLoc = best.get(0);

    	
    	// avoid this work if we aren't interactive
    	if (interactive) {
    		
          	if (ft.minMaxFilter instanceof MinMaxFilter) {
           		displayDistribution(bestLoc, ((MinMaxFilter) ft.minMaxFilter).getCrunchResults());
           	}
        	
          	if (ft.probabilityEngine != null) {
              	for (LinkedLocation ll: ft.probabilityEngine.getLinkedLocations()) {
           			if (ll.equals(bestLoc)) {
           				display(ll.display() + " has " + ll.getLinks() + " clears linked to it");
           			}          		
              	}
              	
              	for (LinkedLocation ll: ft.probabilityEngine.getContraLinkedLocations()) {
           			if (ll.equals(bestLoc)) {
           				display(ll.display() + " has " + ll.getLinks() + " flags linked to it");
           			}          		
              	}
          	}

          	
           	if (ft.superFilter.didFilter()) {
           		for (SuperLocation sl: superSquares) {
           			if (sl.equals(bestLoc)) {
           				display(sl.display() + " has probability " + sl.getProbabilityString() );
           			}
           		}
           	}      	
           	if (ft.subFilter.didFilter()) {
           		for (SubLocation sl: subSquares) {
           			if (sl.equals(bestLoc)) {
           				display(sl.display() + " has probability " + sl.getProbabilityString() );
           			}
           		}
           	}      	
           	if (ft.hookFilter.didFilter()) {
           		for (HookLocation sl: goodHooksOnEdge) {
           			if (sl.equals(bestLoc)) {
           				display(sl.display() + " has probability " + sl.getProbabilityString());
           			}
           		}
           		for (HookLocation sl: goodHooksOffEdge) {
           			if (sl.equals(bestLoc)) {
           				display(sl.display() + " has probability " + sl.getProbabilityString());
           			}
           		}
           	}    		
    	}

    	Action action = bestLoc.buildAction(methodGuess);

    	// let the boardState decide what to do with this action
		boardState.setAction(action);
    	
        Action[] result = boardState.getActions().toArray(new Action[0]);

        display("Best Guess: " + action.asString());

        return new FinalMoves(result);
    	
    }
    
    
    // search all the crunch result for this location and display the distribution details for it
    private void displayDistribution(Location loc, CrunchResult... results) {

    	
    	top: for (CrunchResult cr: results) {
    		if (cr.bigDistribution != null) {
        		for (int i=0; i < cr.getSquare().size(); i++) {
        			if (cr.getSquare().get(i).equals(loc)) {
        				for (int j=0; j < 9; j++) {
        					if (cr.bigDistribution[i][j].compareTo(BigInteger.ZERO) != 0) { // display for non-zero values
        	   					display("Square value = " + j + ", " + cr.bigDistribution[i][j] + " times");
        					}
        				}
        				break top;
        			}
        		}    			
    		}
    	}
    }
    
    // in some cases we learn more about the other witnesses during the crunch
    // this only happens for less obvious analysis. Contour analysis picks up all 100% moves regardless.
    private int checkWitnesses(CrunchResult output, int method) {
        
        int result = 0;
 
        // check the witnesses to see if they have discovered something
        for (int i=0; i < output.witnessRestFlags.length; i++) {
            if (output.witnessGood[i] != 0) {
                if (output.witnessRestFlags[i]) {
                	//display("CheckWitnesses has found a FLAG " + output.witness[i].display());
                    result = result + restKnown(output.witness[i], output.getSquare(), Action.FLAG, method);
                }                
                if (output.witnessRestClear[i]) {
                	//display("CheckWitnesses has found a CLEAR " + output.witness[i].display());
                    result = result + restKnown(output.witness[i], output.getSquare(), Action.CLEAR, method);
                }                
            }
        } 

        return result;
        
    }
    
    
    
    private int restKnown(Location witness, List<? extends Location> square, int action, int method) {
        
        int result=0;
        
		for (Location l: boardState.getAdjacentSquaresIterable(witness)) {
			
            // find all the unflagged and unrevealed squares
            if (!boardState.isRevealed(l) && !boardState.isConfirmedFlag(l)) {

                //Location t = new Location(x1, y1);

                boolean found = false;
                for (Location k: square) {
                	if (l.equals(k)) {
                		found = true;
                		break;
                	}
                }
                
                if (!found && !boardState.alreadyActioned(l)) {

                	Action act;
                    if (action == Action.FLAG) {
                    	act = new Action(l, Action.FLAG, method, METHOD[method], BigDecimal.ONE);
                    	boardState.setFlagConfirmed(act);
                    } else {
                    	act = new Action(l, Action.CLEAR, method, METHOD[method], BigDecimal.ONE);
                    }
                    result++;
     
                    boardState.setAction(act);
                    //display("Discovered witness information at " + x1 + " " + y1);
                }

            }
			
		}

	
        return result;
        
    } 
    
    /**
     * Find the best guess off the edge
     * @return
     */
    private FinalMoves guess() {
        
    	Action action = null;
        
    	// get the starting move if we are at the start of the game
    	if (myGame.getGameState() == GameStateModel.NOT_STARTED && PLAY_OPENING) {
    		action = new Action(myGame.getStartLocation(), Action.CLEAR, Solver.OPENING_BOOK, METHOD[Solver.OPENING_BOOK], offContourBigProb);
    	}
    	
    	// look for a book opening
    	/*
        for (OpeningLocation ol: book) {
        	OpeningLocation move = ol.getNextMove(boardState);
        	if (move != null) {
        		//display("Opening move is " + move.display());
        		action = new Action(move, Action.CLEAR, Solver.OPENING_BOOK, METHOD[Solver.OPENING_BOOK], offContourBigProb);
        		break;
        	}
        }
  		*/
       
        // if there is no book move then look for a guess off the edge
        if (action == null) {
            List<CandidateLocation> list = new ArrayList<>();
            
            // add the best hook to the list of possibilities
            /*
            if (goodHooksOffEdge != null && !goodHooksOffEdge.isEmpty()) {
                Collections.sort(goodHooksOffEdge, HookLocation.SORT_BY_ADJ_HIDDEN);
                HookLocation l = goodHooksOffEdge.get(0);
                list.add(new CandidateLocation(l.x, l.y, offContourBigProb, l.getExtraValue(), boardState.countAdjacentConfirmedFlags(l))); 
                //list.add(new CandidateLocation(l.x, l.y, offContourBigProb, boardState.countAdjacentUnrevealed(l), boardState.countAdjacentConfirmedFlags(l)));            	
            }
			*/
            
            for (int i=0; i < myGame.getx(); i++) {
                for (int j=0; j < myGame.gety(); j++) {
                    // if we are an unrevealed square and we aren't on the contour
                    // then store the location
                    if (boardState.isUnrevealed(i,j)) {
                    	Location l = new Location(i, j);
                        if (wholeEdge == null || !wholeEdge.isOnWeb(l)) {
                            list.add(new CandidateLocation(l.x, l.y, offContourBigProb, boardState.countAdjacentUnrevealed(l), boardState.countAdjacentConfirmedFlags(l)));
                        }
                        
                    } 
                }
            }               
            
            // sort into most favourable order 
            //Collections.sort(list, CandidateLocation.SORT_BY_PROB_FLAG_FREE);
            Collections.sort(list, CandidateLocation.SORT_BY_PROB_FREE_FLAG);
            
            // ... and pick the first one
            action = list.get(0).buildAction(GUESS);
        }
        
        // this will check there isn't a flag blocking the move 
        boardState.setAction(action);

        return new FinalMoves(boardState.getActions().toArray(new Action[0]));
        
    }
    
    
    private List<Zone> determineZones() {
    
        ArrayList<Zone> work = new ArrayList<>();
        
        boolean[][] boardCheck = new boolean[myGame.getx()][myGame.gety()];
        
        for (int i=0; i < myGame.getx(); i++) {
            for (int j=0; j < myGame.gety(); j++) {
                
                if (!boardCheck[i][j]) {
                    Location l = new Location(i,j);

                    int n = myGame.query(l);

                    // if we have a un-revealed square create the zone it belongs in
                    // and remove all the squares in that zone from being checked
                    // again
                    if (n == GameStateModel.HIDDEN && !boardState.isConfirmedFlag(i,j)) {
                    	//display("creating zone from " + l.display());
                        Zone z = new Zone(boardState, l, boardCheck);

                        List<Location> loc = z.getInterior();

                        for (Location t: loc) {
                            boardCheck[t.x][t.y] = true;
                        }
                        
                        work.add(z);
                        
                    } else {
                        boardCheck[i][j] = true;
                    }
                }
            }
        }                        
       
        // merge connected zones into a single zone
        boolean done = false;
        while (!done) {
            done = true;
            for (int i=0; i < work.size(); i++) {
            	if (work.get(i).isIndependent()) {
		            for (int j=i+1; j < work.size(); j++) {
		                if (work.get(j).isIndependent()) {
		                    if (work.get(i).isConnected(work.get(j))) {
		                        work.get(i).merge(work.get(j));
		                        work.get(j).setIndependent(false);
		                        done = false;
		                    }
		                }
		            }
            	}
            }            
        }

        
        // copy the non-merged zones to here
        ArrayList<Zone> work1 = new ArrayList<Zone>();
        
        for (Zone z: work) {
            if (z.isIndependent()) {
                work1.add(z);
            }
        }

        return work1;
        
    }
    
    private void determineSubSquares() {
        
        subSquares.clear();;
        superSquares.clear();
        
        
        // on edge hooks are really a type of super location - so copy them in the super square list
        //for (HookLocation hl: goodHooksOnEdge) {;
        //	SuperLocation sl = new SuperLocation(hl.x, hl.y, hl.getValue(), hl.getSize());
        //	superSquares.add(sl);
        //}
        
        for (int i=0; i < myGame.getx(); i++) {
            for (int j=0; j < myGame.gety(); j++) {
                
                // if we have a witness then get the surrounding squares
                if (boardState.isRevealed(i,j)) {
                    Location l = new Location(i,j);
                    List<Location> adjacent = boardState.getAdjacentUnrevealedSquares(l);
                    for (Location m: boardState.getAdjacentUnrevealedSquares(l, 2)) {
                        findSubSquare(m, adjacent, boardState.getWitnessValue(l) - boardState.countAdjacentConfirmedFlags(l));
                    }
                }
            }
        }                
        
        display("There are " + superSquares.size() + " SuperSquares");
        display("There are " + subSquares.size() + " SubSquares");
        //for (SubLocation l: subSquares) {
        //	display(l.display() + " is a sub square");
        //}
        
     }
    
    // if the unrevealed squares around location l are also in the superset
    // then create a SubSquare and add it to the result list
    private void findSubSquare(Location l, List<Location> superset, int minesNeeded) {
        
    	boolean doSubSquare = true;
    	
    	int free = boardState.countAdjacentUnrevealed(l);
    	if (free < minesNeeded) { // if the number of free squares is less than the number of extra mines it needs around it then it is impossible to achieve it. 
    		doSubSquare = false;
    	}
    	
        // if this location is already a SubSquare then nothing more to do
        for (SubLocation s: subSquares) {
            if (s.equals(l)) {
                doSubSquare = false;
            }
        }

        List<Location> subset = boardState.getAdjacentUnrevealedSquares(l);
        
        boolean okay = false;
        if (doSubSquare) {
            for (Location m: subset) {
                okay = false;
                for (Location n: superset) {
                    if (m.equals(n)) {
                        okay = true;
                        break;
                    }
                }
                if (!okay) {
                    break;
                }
            }
            
            // create a new SubSquare
            if (okay) {
                SubLocation s = new SubLocation(l.x, l.y, superset.size() - subset.size() - 1,  minesNeeded + boardState.countAdjacentConfirmedFlags(l));
        		if (!validateLocation(s, s.getValue())) {
        			display(s.display() + " has failed validation");
        		} else if (s.getSize() != 0) {
                    subSquares.add(s);         
                    //display("subsquare " + s.location.display() + " size = " + s.size + " value needs to be " + s.value);
                }
            }        	
        }

        // if this location is already a SuperSquare then nothing more to do
        SuperLocation superSquare = null;
        for (SuperLocation s: superSquares) {
            if (s.equals(l)) {
                superSquare = s;
                return;
                //break;
            }
        }
        
        // if all the original surrounding squares are a sub-set of the 
        // squares surrounding l, then l is a superSquare.
        okay = false;
        int inBoth = 0;  // if the target location is in the superset then this affects the size of the superLocation
        for (Location m : superset) {
            okay = false;
            if (m.equals(l)) {
                okay = true;
                inBoth = 1;
            } else {
                for (Location n : subset) {
                    if (m.equals(n)) {
                        okay = true;
                        break;
                    }
                }
            }

            if (!okay) {
                break;
            }
        }
        

        
        // create a new SuperSquare
        if (okay) {
            SuperLocation s = new SuperLocation(l.x, l.y, subset.size() - superset.size() + inBoth, minesNeeded + boardState.countAdjacentConfirmedFlags(l));
            if (s.getSize() != 0) {
        		if (!validateLocation(s, s.getValue())) {
        			display(s.display() + " has failed validation");
        		} else if (superSquare == null ) {
            		superSquares.add(s);
            	} else {
            		if (s.getSize() != superSquare.getSize() || s.getValue() != superSquare.getValue()) {
            			display(s.display() + " can't be created because " + superSquare.display() + " already exists");   
            		}
            	}
            }
        }        
        
        
    }
    


    
    // take all the zones and find all the witnesses they have
    private List<Location> getAllWitnesses(List<Zone> zones) {
        
        ArrayList<Location> work = new ArrayList<>();

        // find all the witnesses in all the zones and store them in a list
        for (Zone z: zones) {
            for (Location l: z.getWitness()) {
                boolean found = false;
                for (Location m: work) {
                    if (m.equals(l)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    work.add(l);
                }
            }
        }

        return work;
        
    }
    
    public BigDecimal getProbability(int x, int y) {
    	
    	if (bf != null && bf.hasRun()) {
    		return bf.getProbability(x, y);
    	} else if (pe != null) {
    		return pe.getProbability(new Location(x,y));
    	} else {
    		return null;
    	}
    	
    }
 
    private void topLine(final String s) {
        
    	coachDisplay.clearScreen();
    	coachDisplay.writeLine(s);
    	
    }
    
    private void newLine(final String s) {

    	coachDisplay.writeLine(s);
        
    }
    
    protected void display(String text) {
        
        if (interactive) {
            displayAlways(text);
        }
 
    }
    
    protected void displayAlways(String text) {
        
        System.out.println(text);
  
    }
    
    protected void displayError(String text) {
        
        System.out.println(myGame.showGameKey() + " raised error : " + text);
 
    }
    
   
    public void kill() {
        
        display("Killing the Solver Object");
        
        // free the game state object
        //myGame = null;
        
        coachDisplay.kill();
 
    }
    
    /**
     * calculate the number of distinct ways mines can be placed in squares
     */
    public static BigInteger combination(int mines, int squares) {
        
        try {
			return binomialEngine.generate(mines, squares);
		} catch (Exception e) {
			e.printStackTrace();
			return BigInteger.ONE;
		}
        
    }    
    
    @Override
    protected void finalize() {
        try {
            super.finalize();
        } catch (Throwable ex) {
            Logger.getLogger(Solver.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        display("Solver Class finalize method invoked");
        
    }
    
    public GameStateModel getGame() {
        return myGame;
    }
    
    protected void debug() {
        
        System.out.println("mines left = " + myGame.getMinesLeft());
        System.out.println("Zones found = " + zones.size());
        System.out.println("Whole web witnesses = " + wholeEdge.getWitnesses().size());        
        System.out.println("Whole web squares = " + wholeEdge.getSquares().size());  
    }
    
}
