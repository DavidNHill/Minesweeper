/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.Square;
import minesweeper.solver.constructs.Witness;
import minesweeper.solver.utility.Logger;
import minesweeper.solver.utility.Logger.Level;
import minesweeper.structure.Location;

/**
 * A witness web is a construct which holds the connectivity information of the game. 
 */
public class WitnessWeb {
    
    final private List<Witness> prunedWitnesses = new ArrayList<>();      // the de-duplicated witnesses
    final private List<? extends Location> originalWitnesses;  					 // the witnesses passed into this web
    final private List<Square> squares = new ArrayList<>();
    
    final private List<Box> boxes = new ArrayList<>();
    
    final private List<Witness> independentWitness = new ArrayList<>();
    private final Logger logger;
    
    private int independentMines;
    private BigInteger independentIterations = BigInteger.ONE;
    private int remainingSquares;
    
    private BoardState boardState;
    
    private int pruned = 0;
    
    private int webNum = 0;
    
    private boolean validWeb = true;  // if the web contains contradictions it is invalid
    
    private List<CrunchResult> solutions = new ArrayList<>();
    
    public WitnessWeb(BoardState boardState, List<? extends Location> allWit, Collection<Location> allSqu) {
    	this(boardState, allWit, allSqu, boardState.getLogger());
    }
    
    public WitnessWeb(BoardState boardState, List<? extends Location> allWit, Collection<Location> allSqu, Logger logger) {
        
    	//long nanoStart = System.nanoTime();
    	
    	this.logger = logger;
        this.boardState = boardState;
        this.originalWitnesses = allWit;
        
        // create squares for all the Square locations provided
        for (Location squ: allSqu) {
            squares.add(new Square(squ));
        }     
        
        // create witnesses for all the Witness locations provided
        // and attach adjacent Squares
        List<Square> adjSqu;
        for (Location wit: originalWitnesses) {
            // calculate how many mines are left to find
            //int mines = gs.query(wit) - solver.countConfirmedFlags(wit);
            int mines = boardState.getWitnessValue(wit) - boardState.countAdjacentConfirmedFlags(wit);
            
            adjSqu = new ArrayList<>();
            for (Square squ: squares) {
                if (squ.isAdjacent(wit)) {
                	adjSqu.add(squ);
                }
            }
            if (mines > adjSqu.size() || mines < 0) {
            	validWeb = false;
            	return;
            }
            addWitness(new Witness(wit, mines, adjSqu));
        }        
        
        // this sorts the witnesses by the number of iterations around them
        Collections.sort(prunedWitnesses, Witness.SORT_BY_ITERATIONS_DESC);        
        
        // now attach non-pruned witnesses to adjacent Squares
        for (Witness wit: prunedWitnesses) {
            //System.out.println("Witness " + wit.getLocation().display() + " has " + wit.getSquares().size() + " squares");
            for (Square squ: squares) {
                if (squ.isAdjacent(wit)) {
                    squ.addWitness(wit);
                }
            }
            //solver.display(wit.getLocation().display() + " has " + wit.getMines() + " Mines to find " + wit.getSquares().size() + " adjacent squares");
        }                


    	//long nanoEnd = System.nanoTime();
        
    	//boardState.display("Created witness web in " + (nanoEnd - nanoStart) + " nano-seconds");
    	
    }
    
   
    private void setWeb(Square squ, int num) {
        
        if (squ.getWebNum() != 0 && squ.getWebNum() != num) {
            System.err.println("Square already assigned to a different web!!!!");
        }
        
        // if the square is already part of this web then no more to do here
        if (squ.getWebNum() == num) {
            return;
        }
        
        // claim this square
        squ.setWebNum(num);
        
        // claim all the Witnesses around this square and
        // recursively claim all the other squares around those witnesses
        for (Witness w: squ.getWitnesses()) {
            w.setWebNum(num);
            for (Square s: w.getSquares()) {
                setWeb(s, num);
            }
        }

    }
    
    private void addWitness(Witness wit) {
        
        // if the witness is a duplicate then don't store it
        for (Witness w: prunedWitnesses) {
            if (w.equivalent(wit)) {
            	if (boardState.getWitnessValue(w) - boardState.countAdjacentConfirmedFlags(w) != boardState.getWitnessValue(wit) - boardState.countAdjacentConfirmedFlags(wit)) {
            		logger.log(Level.WARN, "%s and %s share unrevealed squares but have different mine totals!", w, wit);
            		validWeb = false;
            	}
                pruned++;
                return;
            }
        }
        
        prunedWitnesses.add(wit);
        
    }
    
    /**
     * Generate boxes of tiles which all share the same witnesses. These are used in the probability engine and must have the same probability.
     */
    public void generateBoxes() {
    	
        int boxCount = 0;
        // put each square in a box
        for (Square squ: squares) {
        	boolean found = false;
        	// see if the square fits an existing box
        	for (Box b: boxes) {
        		if (b.fitsBox(squ)) {
        			b.addSquare(squ);
        			found = true;
        			break;
        		}
        	}
        	// if not create a new box for it
        	if (!found) {
        		boxes.add(new Box(squ, boxCount));
        		boxCount++;
        	}
        }
        
        
        int minesLeft = boardState.getMines() - boardState.getConfirmedFlagCount();
    	for (Box b: boxes) {
    		b.calculate(minesLeft);
    		//b.display();
    	}
    	
    }

    /**
     * Generate independent witnesses which can be used by the brute force iteration processing
     */
    public void generateIndependentWitnesses() {
    	
        remainingSquares = this.squares.size();
        
        // find a set of witnesses which don't share any squares (there can be many of these, but we just want one to use with the brute force iterator)
        for (Witness w: prunedWitnesses) {
        	if (w.getMines() == 0) {
        		continue;
        	}
        	
            boolean okay = true;
            for (Witness iw: independentWitness) {
                if (w.overlap(iw)) {
                    okay = false;
                    break;
                }
            }
            if (okay) {
                remainingSquares = remainingSquares - w.getSquares().size();
                independentIterations = independentIterations.multiply(Solver.combination(w.getMines(), w.getSquares().size()));
                independentMines = independentMines + w.getMines();
                independentWitness.add(w);
            }
        }
    	
    }
    
    
    /**
     * Returns the number of mines around the independent witnesses. The number of mines in any solution can't be less than this.
     * @return
     */
    public int getMinesPlaced() {
        
        return independentMines;
        
    }
    
    /**
     * The locations on the edge
     */
    public List<Square> getSquares() {
        return squares;
    }
    
    /**
     * The deduplicated witnesses in this web
     */
    public List<Witness> getPrunedWitnesses() {
        return prunedWitnesses;
    }
    
    /**
     * The witnesses passed into this web
     */
    public List<? extends Location> getOriginalWitnesses() {
        
        return originalWitnesses;
        
    }
    
    public List<Witness> getIndependentWitnesses() {
        
        return independentWitness;
        
    }
        
    
    public List<Box> getBoxes() {
    	return this.boxes;
    }
    
    
    // how many iterations will be required to process this web with the provided number of mines
    public BigInteger getIterations(int mines) {
        
        // if too many or too few mines then no work needs to be done
        if (mines < independentMines || mines > independentMines + remainingSquares) {
            return BigInteger.ZERO;
        }
        
        BigInteger result = independentIterations.multiply(Solver.combination(mines - independentMines, remainingSquares));
        
        return result;
        
    }
    
    /**
     * The number of ways the non-independent squares and mines can be arranged
     * @param mines
     * @return
     */
    public BigInteger getNonIndependentIterations(int mines) {
    	
        // if too many or too few mines then no work needs to be done
        if (mines < independentMines || mines > independentMines + remainingSquares) {
            return BigInteger.ZERO;
        }
        
        BigInteger result = Solver.combination(mines - independentMines, remainingSquares);
        
        return result;    	
    	
    }
    
    
    
    //public int getSharedSquares() {
    //    return remainingSquares;
    //}
    
    public int getIndependentMines() {
        return independentMines;
    }
        
    /*
    public List<WitnessWeb> getSubWebs() {
        
        List<WitnessWeb> result = new ArrayList<>();
                
        if (webNum == 1) {
            result.add(this);
        } else {
            for (int i=0; i < webNum; i++) {
                result.add(createSubWeb(i+1));
            }            
        }

        return result;
        
    }
    
    
    // create a WitnessWeb from a sub web of this one
    private WitnessWeb createSubWeb(int n) {
        
        if (n < 1 || n > webNum ) {
            System.err.println("requesting sub-web " + n + " of ( 1  to " + webNum + ")");
        }
        
        List<Location> wit = new ArrayList<Location>();
        List<Location> squ = new ArrayList<Location>();
        
        for (Witness w: prunedWitnesses) {
            if (w.getWebNum() == n) {
                wit.add(w);
            }
        }
        for (Square s: squares) {
            if (s.getWebNum() == n) {
                squ.add(s);
            }
        }        
        
        WitnessWeb result = new WitnessWeb(boardState, wit, squ);
        
        return result;
        
    }
    */
    
    // if the location passed is a square in the web then return true;
    public boolean isOnWeb(Location l) {
        
        for (Square s: squares) {
            if (s.equals(l)) {
                return true;
            }
        }
        
        return false;
        
    }
    
    public void addSolution(CrunchResult e) {
        solutions.add(e);
    }
    
    public List<CrunchResult> getSolutions() {
        return solutions;
    }
    
    public boolean isWebValid() {
    	return validWeb;
    }
    
}
