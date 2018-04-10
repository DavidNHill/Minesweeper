/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.constructs;

import java.util.ArrayList;
import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;

/**
 *
 * @author David
 */
public class Square extends Location {
    
    private final List<Witness> witnesses = new ArrayList<>();
    
    private int webNum = 0;
    
    
    public Square(Location loc) {
    	super(loc.x, loc.y);

    }
    
    public void addWitness(Witness wit) {
        witnesses.add(wit);
    }
    
    public List<Witness> getWitnesses() {
        return witnesses;
    }
    

    public int getWebNum() {
        return webNum;
    }

    public void setWebNum(int webNum) {
        this.webNum = webNum;
    }    

}
