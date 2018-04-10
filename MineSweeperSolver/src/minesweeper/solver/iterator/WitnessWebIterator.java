/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.iterator;

import java.util.ArrayList;
import java.util.List;

import minesweeper.gamestate.Location;
import minesweeper.solver.WitnessWeb;
import minesweeper.solver.constructs.Witness;

/**
 *
 * @author David
 */
public class WitnessWebIterator extends Iterator {
    
    private int[] sample;
    
    private List<Location> location;
    
    private SequentialIterator[] cogs;
    private int[] squareOffset;
    private int[] mineOffset;
    
    private int iterationsDone = 0;
    
    final private int top;
    final private int bottom;
    
    private boolean done = false;
    
    private WitnessWeb web;
    
    public WitnessWebIterator(WitnessWeb web, int mines) {
        this(web, mines, -1);
    }
    
    
    // create an iterator which is like a set of rotating wheels
    public WitnessWebIterator(WitnessWeb web, int mines, int rotation) {
         super(mines, web.getSquares().size());
         
        this.web = web;
         
        // if we are setting the position of the top cog then it can't ever change
        if (rotation == -1) {
            bottom = 0;
        } else {
            bottom = 1;
        }
        
        int indSquares = 0;
        int indMines = 0;
        
        cogs = new SequentialIterator[web.getIndependentWitnesses().size() + 1];
        squareOffset =  new int[web.getIndependentWitnesses().size() + 1];
        mineOffset =  new int[web.getIndependentWitnesses().size() + 1];
        int cogi = 0;
        
        List<Location> loc = new ArrayList<>();
        
        // create an array of locations in the order of independent witnesses
        for (Witness w: web.getIndependentWitnesses()) {
            squareOffset[cogi] = indSquares;
            mineOffset[cogi] = indMines;
            cogs[cogi] = new SequentialIterator(w.getMines(), w.getSquares().size());
            //System.out.println("Cog has " + cogs[cogi].numberBalls + " mines and " + cogs[cogi].numberHoles + " squares");
            cogi++;

            indSquares = indSquares + w.getSquares().size();
            indMines = indMines + w.getMines();
            
            loc.addAll(w.getSquares());
            //for (Square s: w.getSquares()) {
            //    loc.add(s);
            //}
        }
        
        //System.out.println("Mines left = " + (mines - indMines));
        //System.out.println("Squrs left = " + (web.getSquares().length - indSquares));
        
        // the last cog has the remaining squares and mines
        
        //add the rest of the locations
        for (Location l: web.getSquares()) {
            boolean skip = false;
            for (Location m: loc) {
                if (l.equals(m)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                 loc.add(l);
            }                
        }
        
        location = loc;
                
        // if there are more mines left then squares then no solution is possible
        // if there are not enough mines to satisfy the minimum we know are needed
        if (mines - indMines > web.getSquares().size() - indSquares
            || indMines > mines) {
            done = true;
            top = 0;
            return;
        }
    
        // if there are no mines left then no need for a cog
        if (mines > indMines) {
            squareOffset[cogi] = indSquares;
            mineOffset[cogi] = indMines;
            cogs[cogi] = new SequentialIterator(mines - indMines, web.getSquares().size() - indSquares);
            top = cogi;
        } else {
            top = cogi - 1;
        }

        sample = new int[mines];
        
        // if we are locking and rotating the top cog then do it
        if (rotation != -1) {
            for (int i=0; i < rotation; i++) {
                cogs[0].getSample();
            }
        }
        
        // now set up the initial sample position
        for (int i=0; i < top; i++) {
            int[] s = cogs[i].getSample();
            for (int j=0; j < s.length; j++) {
                sample[mineOffset[i] + j] = squareOffset[i] + s[j];
            }
        }
        
        /*
        for (int i=0; i < sample.length; i++) {
            System.out.print(sample[i] + " ");
        }
        System.out.println("");
        */  
    }
    
 
    @Override
    public int[] getSample(int start) {
 
        // now we are running in parallel we need to ensure that this code isn't being run by more than 1 thread at a time
        //synchronized (this) {
            
            if (done) {
                return null;
            }
            int index = top;

            int[] s = cogs[index].getSample();

            while (s == null && index != bottom) {
                index--;
                s = cogs[index].getSample();
            }

            if (index == bottom && s == null) {
                done = true;
                return null;
            }

            for (int j=0; j < s.length; j++) {
                sample[mineOffset[index] + j] = squareOffset[index] + s[j];
            }
            index++;
            while (index <= top) {
                cogs[index] = new SequentialIterator(cogs[index].getBalls(), cogs[index].getHoles());
                s = cogs[index].getSample();
                for (int j=0; j < s.length; j++) {
                    sample[mineOffset[index] + j] = squareOffset[index] + s[j];
                }
                index++;
            }
            
        /*
        for (int i: sample) {
            System.out.print(i + " ");
        }
        System.out.println();
        */
            
        iterationsDone++;
        
        return sample;
        
        //return Arrays.copyOf(sample, sample.length);
        
        //}
        
    }
    
    public List<Location> getLocations() {
        return location;
    }

    public int getIterations() {
        return iterationsDone;
    }
       
    // if the location is a Independent witness then we know it will always
    // have exactly the correct amount of mines around it since that is what
    // this iterator does
    @Override
    public boolean witnessAlwaysSatisfied(Location l) {
        
        for (Witness w: web.getIndependentWitnesses()) {
            if (w.equals(l)) {
                return true;
            }
        }
            
        return false;
        
    }
 
}
