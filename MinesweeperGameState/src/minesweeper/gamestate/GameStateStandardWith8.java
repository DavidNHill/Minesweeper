/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.gamestate;

import java.util.Random;

import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

/**
 * A Version of Minesweeper which ensures the first click is not a mine
 * @author David
 */
public class GameStateStandardWith8 extends GameStateModelViewer {
    
    private final int[][] board;
    
    private RNG rng;
    
    public GameStateStandardWith8(GameSettings gameSettings) {
        this(gameSettings, new Random().nextLong());
    }
    
 
    public GameStateStandardWith8(GameSettings gameSettings, long seed) {
        super(gameSettings, seed);
        
        this.board = new int[width][height];
        
        this.rng = DefaultRNG.getRNG(seed); 
    }
    
    // in this gamestate we are building the board ourselves
    @Override
    protected void startHandle(Location m) {
        
        int i=0;
        
        Location locationOf8 = null;
        boolean placed8 = false;
        while (!placed8) {
            int y1 = 1 + (int) rng.random(this.height - 2);
            int x1 = 1 + (int) rng.random(this.width - 2); 
            locationOf8 = new Location(x1, y1);
        	
            // place the 8 mines around a single tile
            if (!locationOf8.equals(m) && !locationOf8.isAdjacent(m)) {
                for (int k=0; k < DX.length; k++) {
                	
                	int x2 = x1 + DX[k];
                	int y2 = y1 + DY[k];
                	
                    //System.out.println("Mine added at " + x2 + "," + y2);
                    board[x2][y2] = GameStateModel.MINE;
                    i++;
                    
                    // tell all the surrounding squares they are next to a mine
                    for (int j=0; j < DX.length; j++) {
                        if (x2 + DX[j] >= 0 && x2 + DX[j] < this.width && y2 + DY[j] >= 0 && y2 + DY[j] < this.height) {
                            if (board[x2+DX[j]][y2+DY[j]] != GameStateModel.MINE) {
                                board[x2+DX[j]][y2+DY[j]]++;
                            }
                        }
                    }

                }     
                placed8 = true;
            }
        }
        
        while (i < mines) {
            int y1 = (int) rng.random(this.height);
            int x1 = (int) rng.random(this.width);
            Location l1 = new Location(x1, y1);
            
            // if the location is NOT the first square pressed
            // and the location is not already a mine then place a mine here
            if (!l1.equals(m) && !l1.equals(locationOf8)) {
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
    protected int queryHandle(int x, int y) {
        return board[x][y];
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

        if (showMines && result == GameStateModel.FLAG && board[m.x][m.y] != GameStateModel.MINE ) {
        	result = GameStateModel.BAD_FLAG;
        }
        
        return result;
        
    }
    
    @Override
    public String showGameKey() {
    	
    	return "Seed = " + seed + " (" + rng.shortname() + ")";
    	
    }
    
	@Override
    public boolean supports3BV() {
    	return true;
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
