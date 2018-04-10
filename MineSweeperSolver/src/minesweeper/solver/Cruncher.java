/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import Asynchronous.Asynchronous;
import minesweeper.gamestate.Location;
import minesweeper.solver.constructs.HookLocation;
import minesweeper.solver.iterator.Iterator;

import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author David
 */
public class Cruncher implements Asynchronous<CrunchResult> {

    private Solver solver;
    private Iterator iterator;
    private List<Location> square;
    private List<? extends Location> witness;
    private boolean calculateDistribution;
    private List<HookLocation> hooks;
    private final BruteForceAnalysisModel bfa;
    
    private CrunchResult result;
    
   
    public Cruncher(Solver solver, List<Location> square, List<? extends Location> witness, List<HookLocation> hooks, Iterator iterator, boolean calculateDistribution, BruteForceAnalysisModel bfa) {
        
        this.solver = solver;
        this.iterator = iterator;
        this.square = square;
        this.witness = witness;
        this.hooks = hooks;
        this.calculateDistribution = calculateDistribution;
        this.bfa = bfa;
       
    }
    
    
    @Override
    public void start() {
        
        result = solver.crunch(square, witness, hooks, iterator, calculateDistribution, bfa);
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

    
}
