/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.iterator;

import minesweeper.structure.Location;

/**
 *
 * @author David
 */
abstract public class Iterator {
    
    final int numberBalls;
    final int numberHoles;
    
    public Iterator(int n, int m) {
    
        this.numberBalls = n;
        this.numberHoles = m;
        
    }
    
    
    public int[] getSample(int start) {
        return null;
    }
 
    public int[] getSample() {
        return getSample(numberBalls - 1);
    }    
    
    public int getBalls() {
        return numberBalls;
    }
    
    public int getHoles() {
        return numberHoles;
    }
    
    // if this is true then the checkSample logic can ignore this witness
    // This is used by the WitnessWebIterator since the IndependentWitnesses
    // must always be satisified.
    public boolean witnessAlwaysSatisfied(Location l) {
        return false;
    }
    
}
