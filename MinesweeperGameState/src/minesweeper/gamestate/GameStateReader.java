/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.gamestate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A Version of Minesweeper which reads the board state from a file
 * @author David
 */
public class GameStateReader extends GameStateModelViewer {
    
    private final int[][] board;
    
    private Random rng;
    
    private long seed;
    
    private File file;
    
    private GameStateReader(int x, int y, int mines) {
        this(x, y, mines, new Random().nextLong());
    }
    
    public GameStateReader(int x, int y, int mines, long seed) {
        this(x,y,mines, new Random(seed));
        
        this.seed = seed;
       
    }
    
    private GameStateReader(int x, int y, int mines, Random rng) {
        super(x,y,mines);
        
        this.board = new int[x][y];
        
        this.rng = rng; 
    }
    
    public final static GameStateModelViewer load(File file) {
    	
    	int x;
    	int y;
    	int mines;
    	

    	
    	GameStateReader result;

    	
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
				BufferedReader reader  = new BufferedReader(isr)
		){
			
			String data = reader.readLine();
			
			if (data == null) {
				return null;
			}
			
			String[] header = data.split("x");
			if (header.length != 3) {
				return null;
			}
			
			try {
				x = Integer.parseInt(header[0]);
				y = Integer.parseInt(header[1]);
				mines = Integer.parseInt(header[2]);
			} catch (Exception e) {
				return null;
			}

			result = new GameStateReader(x, y, mines);
			
			data = reader.readLine();
			int cy=0;
			
			while (data != null) {
				
				if (data.length() != x) {
					return null;
				}
				
				int cx = 0;
				for (char c: data.toCharArray()) {
					if (c == 'M' || c == 'm') {
						//result.setFlag(cx, cy);
					} else if (c != 'H' && c != 'h') {
						int val = Character.getNumericValue(c);
						result.setRevealed(cx, cy);
						result.board[cx][cy] = val;
					}
					cx++;
				}
			
				cy++;
				data = reader.readLine();	
			};

		
		} catch (Exception e) {
			return null;
		}	
    	
    	result.file = file;
		
		return result;
		
    }
    
    // in this gamestate the board is built from the file
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
    	
    	return "file = " + file.getAbsolutePath();
    	
    }
    
    
    private void explode(Location loc) {
    	
    	boolean[][] done = new boolean[x][y];
    	
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
                if (x1 >= 0 && x1 < x && y1 >= 0 && y1 < y) {
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
            if (m.x + DX[j] >= 0 && m.x + DX[j] < this.x && m.y + DY[j] >= 0 && m.y + DY[j] < this.y) {
                clearSquare(new Location(m.x+DX[j], m.y+DY[j]));
            }
        }      
        
        return true;
	}
    
    
    
}
