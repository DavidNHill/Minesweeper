/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import minesweeper.gamestate.Location;

/**
 *
 * @author David
 */
public class WitnessData {
    
    public Location location; 
    public boolean witnessRestFlag = true;
    public boolean witnessRestClear = true;
    public int witnessGood;
    public int currentFlags;
    public boolean alwaysSatisfied;
    
}
