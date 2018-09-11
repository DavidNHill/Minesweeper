/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.gamestate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;

/**
 * A Version of Minesweeper which ensures the first click is not a mine
 * @author David
 */
public class GameStateStandard extends GameStateModelViewer {
    
    private final int[][] board;
    
    private RNG rng;
    
    public GameStateStandard(GameSettings gameSettings) {
        this(gameSettings, new Random().nextLong());
    }
    
 
    public GameStateStandard(GameSettings gameSettings, long seed) {
        super(gameSettings, seed);
        
        this.board = new int[width][height];
        
        this.rng = DefaultRNG.getRNG(seed); 
    }
    
    // in this gamestate we are building the board ourselves
    @Override
    protected void startHandle(Location m) {
        
        int i=0;
        
        while (i < mines) {
            int y1 = (int) rng.random(this.height);
            int x1 = (int) rng.random(this.width);
            Location l1 = new Location(x1, y1);
            
            // if the location is NOT the first square pressed
            // and the location is not already a mine then place a mine here
            if (!l1.equals(m)) {
                if (board[x1][y1] != GameStateModel.MINE) {
                    //System.out.println("Mine added at " + x1 + "," + y1);
                    board[x1][y1] = GameStateModel.MINE;
                    i++;
                    
                    // tell all the surrounding squares they are next to a mine
                    for (int j=0; j < DX.length; j++) {
                        if (x1 + DX[j] >= 0 && x1 + DX[j] < this.width && y1 + DY[j] >= 0 && y1 + DY[j] < this.height) {
                            if (board[x1+DX[j]][y1+DY[j]] != GameStateModel.MINE) {
                                board[x1+DX[j]][y1+DY[j]]++;
                            }
                        }
                    }

                }               
            }

        }        
        
    }
    
    // in this gamestate there is nothing to do
    @Override
    protected void placeFlagHandle(Location m) {
        
    }
    
    
    @Override
    protected int queryHandle(Location m) {
        return board[m.x][m.y];
    }
    
    
    // in this gamestate we need to expand the clear if no mines are adjacent
    @Override
    protected boolean clearSquareHitMine(Location m) {

        // if there are no mines next to this location expand reveal
        if (board[m.x][m.y] == 0) {
        	explode(m);
            //clearSurround(m);
        }
        
        if (board[m.x][m.y] == GameStateModel.MINE) {
            return true;
        } else {
            return false;
        }
        
        //return board[m.x][m.y];
        
    }    
    @Override
    public int privilegedQuery(Location m, boolean showMines) {
        
        int result = query(m);
        if (result == GameStateModel.MINE) {  // if we can see a mine using query it must be exploded
            return GameStateModel.EXPLODED_MINE;
        }
        
        if (showMines && result == GameStateModel.HIDDEN && board[m.x][m.y] == GameStateModel.MINE) {
            result = GameStateModel.MINE;
        }

        return result;
        
    }
    
    @Override
    public String showGameKey() {
    	
    	return "Seed = " + seed + " (" + rng.shortname() + ")";
    	
    }
    
    
    private void explode(Location loc) {
    	
    	boolean[][] done = new boolean[width][height];
    	
    	List<Location> interiorList = new ArrayList<>();
    	
        // add this location to the interior array list
        done[loc.x][loc.y] = true;
        interiorList.add(loc);
        
        int processFrom = 0;
        
        while (processFrom < interiorList.size()) {
        	
        	// get the current location to process surrounding squares
        	Location cl = interiorList.get(processFrom);
        	
            for (int i=0; i < DX.length; i++) {
                
                int x1 = cl.x + DX[i];
                int y1 = cl.y + DY[i];
                
                // check each of the surrounding squares which haven't already been checked
                if (x1 >= 0 && x1 < width && y1 >= 0 && y1 < height) {
                	if (!done[x1][y1] && query(new Location(x1, y1)) == GameStateModel.HIDDEN) {
                		
                		done[x1][y1] = true;
                    	setRevealed(x1,y1);
 
                        // if this square is also a zero then add it to the list of locations to be exploded
                        if (board[x1][y1] == 0) {
                        	interiorList.add(new Location(x1, y1));
                        }                		
                	}

                 }
            }     

            processFrom++;
        }    	
    	
    	
    }
    
	@Override
	protected boolean clearSurroundHandle(Location m) {
		
        // otherwise, clear around this revealed square
        for (int j=0; j < DX.length; j++) {
            if (m.x + DX[j] >= 0 && m.x + DX[j] < this.width && m.y + DY[j] >= 0 && m.y + DY[j] < this.height) {
                clearSquare(new Location(m.x+DX[j], m.y+DY[j]));
            }
        }      
        
        return true;
	}
    
    
    
}
