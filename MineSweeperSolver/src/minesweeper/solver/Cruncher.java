/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import Asynchronous.Asynchronous;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.constructs.WitnessData;
import minesweeper.solver.iterator.Iterator;
import minesweeper.structure.Location;

import java.math.BigInteger;
import java.util.List;

/**
 *  Performs a brute force search on the provided squares using the iterator 
 * 
 */
public class Cruncher implements Asynchronous<CrunchResult> {

    private final BoardState boardState;
    private final Iterator iterator;
    private final List<Location> square;
    private final List<? extends Location> witness;
    private final boolean calculateDistribution;
    private final BruteForceAnalysisModel bfa;
    
    private final boolean[] workRestNotFlags;
    private final boolean[] workRestNotClear;
    
    private CrunchResult result;
    
   
    public Cruncher(BoardState boardState, List<Location> square, List<? extends Location> witness, Iterator iterator, boolean calculateDistribution, BruteForceAnalysisModel bfa) {
        
        this.iterator = iterator;
        this.square = square;
        this.witness = witness;
        this.calculateDistribution = calculateDistribution;
        this.bfa = bfa;
        this.boardState = boardState;
        
        workRestNotFlags = new boolean[witness.size()];
        workRestNotClear = new boolean[witness.size()];
       
    }
    
    
    @Override
    public void start() {
        
        result = crunch();
        //System.out.println("Candidates = " + result.bigGoodCandidates);
        result.setWeight(BigInteger.ONE);
        
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public CrunchResult getResult() {
        return result;
    }

    
    private CrunchResult crunch() {
        
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


        BigInteger bign = BigInteger.ZERO;

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
           
            if (checkSample(sample, square, witnessData, bigDistribution, bfa)) {
                for (int i=0; i < sample.length; i++) {
                	tally[sample[i]]++;
                }   
                candidates++;
            }
            
            sample = iterator.getSample();
            
        }
        
        BigInteger[] bigTally = new BigInteger[square.size()];
        for (int i = 0; i < bigTally.length; i++) {
            bigTally[i] = BigInteger.valueOf(tally[i]);
            
        }
        
        bign = BigInteger.valueOf(candidates);
        
        // store all the information we have gathered into this object for
        // later analysis
        CrunchResult output = new CrunchResult();
        output.setSquare(square);
        output.bigDistribution = bigDistribution;
        
     
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
    protected boolean checkSample(final int[] sample, final List<Location> square, WitnessData[] witnessData, BigInteger[][] bigDistribution, BruteForceAnalysisModel bfa) {
        
        /*
        String s= "";
        for (int i = 0; i < sample.length; i++) {
            s = s + " " + sample[i];
        }
        display(s);
        */

        //boolean[] workRestNotFlags = new boolean[witnessData.length];
        //boolean[] workRestNotClear = new boolean[witnessData.length];
    	for (int i=0; i < witnessData.length; i++) {
    		workRestNotFlags[i] = false;
    		workRestNotClear[i] = false;
    	}
        
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
        if (bfa != null && !bfa.tooMany()) {
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
                    if (bigDistribution != null) {
                    	bigDistribution[i][flags2] = bigDistribution[i][flags2].add(BigInteger.ONE);
                    }

                } else {
                	solution[i] = GameStateModel.MINE;
                }

            }
            //if (bfa != null && !bfa.tooMany()) {
            	bfa.addSolution(solution);
            //}
        }

        return true;
        
    }
    
    protected BruteForceAnalysisModel getBFA() {
    	return bfa;
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
    
}
