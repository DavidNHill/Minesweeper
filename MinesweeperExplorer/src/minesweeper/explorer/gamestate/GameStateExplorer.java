/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.explorer.gamestate;

import java.util.ArrayList;
import java.util.List;

import minesweeper.explorer.structure.Board;
import minesweeper.explorer.structure.Tile;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

/**
 * A Version of Minesweeper which reads the board state from the explorer details
 * @author David
 */
public class GameStateExplorer extends GameStateModelViewer {
    
    private final int[][] board;
    
    private boolean safeOpening;
    
  
    private GameStateExplorer(GameSettings gameSettings) {
        super(gameSettings, 0);
        
        this.board = new int[width][height];
    }
    
    public final static GameStateExplorer build(Board board, int mines) throws Exception {
    	
  	
    	int width = board.getGameWidth();
    	int height = board.getGameHeight();
    	
    	GameStateExplorer result = new GameStateExplorer(GameSettings.create(width, height, mines));
    	
    	result.partialGame = true;  // indicates that the board is not complete
    	//result.start(new Location(0,0));
    	result.safeOpening = false;

    	for (int x=0; x < width; x++) {
    		for (int y=0; y < height; y++) {
    			
    			Tile tile = board.getTile(x, y);
    			
    			if (tile.isFlagged()) {
    				result.setFlag(x, y);
    			}
    			
    			if (tile.isMine()) {
    				result.board[x][y] = GameStateModel.MINE;
    				
    				/*
                    // tell all the surrounding squares they are next to a mine
                    for (int j=0; j < DX.length; j++) {
                        if (x + DX[j] >= 0 && x + DX[j] < result.width && y + DY[j] >= 0 && y + DY[j] < result.height) {
                            if (result.board[x+DX[j]][y+DY[j]] != GameStateModel.MINE) {
                                result.board[x+DX[j]][y+DY[j]]++;
                            }
                        }
                    }
    				*/
    				
    			} else if (!tile.isCovered()) {
    				result.board[x][y] = tile.getValue();
    				result.setRevealed(x, y);
    			} else {
    				result.setHidden(x, y);
    			}
    			
    		}
    		
    	}
    	
    	result.start(new Location(0,0));
    	
		return result;
		
    }
    
     // in this gamestate the board is pre-built
    @Override
    protected void startHandle(Location m) {
        
        
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
    	
    	String partial = "";
    	if (partialGame) {
    		partial = " (Mines missing!)";
    	}
    	
    	return "explorer game";
    	
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
    
	@Override
    public boolean safeOpening() {
    	return safeOpening;
    }
    
    
}
