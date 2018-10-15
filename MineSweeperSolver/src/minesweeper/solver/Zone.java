/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import java.util.ArrayList;
import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;

/**
 *
 * @author David
 */
public class Zone implements Comparable<Zone>{
    
    private final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    
    //private GameStateModel gameState;
    private BoardState solver;
    
    private List<Location> interiorList;
    private List<Location> witnessList;
    private boolean allSquaresWitnessed = true;
            
    // assume the zone doesn't share a witness with any other for the moment
    private boolean independent = true;
    
    // if a zone is set to solve last then it means the solver thinks it doesn't have to guess in this zone if it can solve
    // the rest of the board first
    private boolean solveLast = false;
    
    public Zone(BoardState solver, Location l, boolean[][] boardCheck) {
        
    	this.solver = solver;
        //this.gameState = solver.getGame();
        
        createZone(l, boardCheck);
        
    }
    
    private void createZone(Location loc, boolean[][] boardCheck) {
        
       
        boolean[][] board = new boolean[solver.getGameWidth()][solver.getGameHeight()];
        
        interiorList = new ArrayList<>(240);
        witnessList = new ArrayList<>();
 
        // determine the interior and perimeter of this zone
        expand1(interiorList, witnessList, board, loc);
        
        //solver.display("Zone of size " + interiorList.size() + " created with " + witnessList.size() + " witnesses, all squares witnessed: " + allSquaresWitnessed);
        
    }
    
    private void expand1(List<Location> interiorList, List<Location> witnessList, boolean[][] board, Location loc) {
        
        // add this location to the interior array list
        board[loc.x][loc.y] = true;
        interiorList.add(loc);
        
        int processFrom = 0;
        
        while (processFrom < interiorList.size()) {
        	
        	// get the current location to process surrounding squares
        	Location cl = interiorList.get(processFrom);
        	
        	boolean noWitness = true;
        	
            for (int i=0; i < DX.length; i++) {
                
                int x1 = cl.x + DX[i];
                int y1 = cl.y + DY[i];

                
                // check each of the surrounding squares which haven't already been checked
                if (x1 >= 0 && x1 < solver.getGameWidth() && y1 >= 0 && y1 < solver.getGameHeight()) {

                    Location l = new Location(x1, y1);
                    
                    int n;
                    if (solver.isConfirmedFlag(l)) {
                    	n = GameStateModel.FLAG;
                    } else if (solver.isUnrevealed(l)) {
                    	n = GameStateModel.HIDDEN;
                    } else {
                    	n = solver.getWitnessValue(l);
                    }                	

                    if (n > 0) {
                    	noWitness = false;
                    }
                	
                	if (!board[x1][y1]) {
                       
                        // if the squares is hidden then add it to the list
                        if (n == GameStateModel.HIDDEN) {
                        	board[l.x][l.y] = true;
                            interiorList.add(l);
                        } else if ( n !=  GameStateModel.FLAG){
                            board[l.x][l.y] = true;
                            witnessList.add(l);
                        }
                	}
                }
            }     

            if (noWitness) {
            	allSquaresWitnessed = false;
            }
            
            processFrom++;
        }

        
    }

    // return a copy of the array which represents the interior of this zone
    public List<Location> getInterior() {
        
        return interiorList;
        
    }
    // return a copy of the array which represents the witnesses of this zone
    public List<Location> getWitness() {
        
        return witnessList;
        
    }    
    
    // does this zone contain the passed location?
    public boolean contains(Location loc) {
        
        for (Location l: interiorList) {
            if (l.equals(loc)) {
                return true;
            }
         }
        
        return false;
        
    }
    
    public boolean isWitness(Location loc) {
        
        for (Location l: witnessList) {
            if (l.equals(loc)) {
                return true;
            }
         }
        
        return false;        
        
    }
    
    // if the 2 zones share a witness then they are connected
    public boolean isConnected(Zone zone) {
        
        for (Location l: zone.witnessList) {
            if (isWitness(l)) {
                return true;
            }
         }        
        
        return false;
        
    }
    
    public boolean isIndependent() {
        
        return independent;
    }
    
    public void setIndependent(boolean value) {
        independent = value;
    }

    public boolean allSquaresWitnessed() {
    	return allSquaresWitnessed;
    }
    
    public void merge(Zone zone) {
        
        // combine the interiors
        this.interiorList.addAll(zone.interiorList);
        
        // add the non-duplicate witnesses
        for (Location l:zone.witnessList) {
            if (!this.isWitness(l)) {
                this.witnessList.add(l);
            }
        }
        
        // all the squares are witnessed if they are witnessed in both zones
        this.allSquaresWitnessed = this.allSquaresWitnessed() & zone.allSquaresWitnessed;
        
        // empty the merged zone
        zone.interiorList.clear();
        zone.witnessList.clear();
        
    }
    
    public void setSolveLast(boolean flag) {
    	this.solveLast = flag;
    }
    
    public boolean isSolveLast() {
    	return this.solveLast;
    }
    
    /**
     * turns this zone into a witness web 
     * @return
     */
    public WitnessWeb createWitnessWeb(BoardState solver) {
    	
    	WitnessWeb web = new WitnessWeb(solver, witnessList, interiorList);
    	
    	return web;
    	
    }

	@Override
	public int compareTo(Zone o) {
		// sort by the size of the zone
		return this.interiorList.size() - o.interiorList.size();
	}
    
}
