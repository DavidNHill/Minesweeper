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
import java.util.List;

import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

/**
 * A Version of Minesweeper which reads the board state from a file
 * @author David
 */
public class GameStateReader extends GameStateModelViewer {
    
    private final int[][] board;
    
    private File file;
    private boolean safeOpening;
    
  
    private GameStateReader(GameSettings gameSettings) {
        super(gameSettings, 0);
        
        this.board = new int[width][height];
    }
    
   
    public final static GameStateModelViewer load(File file) throws Exception {
    	
    	if (file.getName().toUpperCase().endsWith(".MBF")) {
    		return loadMBF(file);
    	}
    	
    	
    	int width;
    	int height;
    	int mines;
    	
    	int minesCount = 0;
    	
    	GameStateReader result;


    	int[][] tempBoard;
    	
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
				BufferedReader reader  = new BufferedReader(isr)
		){
			
			String data = reader.readLine();
			
			if (data == null) {
				throw new Exception("File is empty!");
			}
			
			String[] header = data.trim().split("x");
			if (header.length != 3) {
				throw new Exception("Header (" + data + ") doesn't contain width, height, mine separated by 'x'");
			}
			
			try {
				width = Integer.parseInt(header[0]);
				height = Integer.parseInt(header[1]);
				mines = Integer.parseInt(header[2]);
			} catch (Exception e) {
				throw new Exception("Unable to parse the values in the header (" + data + ")");
			}

			result = new GameStateReader(GameSettings.create(width, height, mines));

	    	tempBoard = new int[width][height];
			
			data = reader.readLine();
			int cy=0;
			
			while (data != null) {
				
				if (data.trim().length() != width) {
					throw new Exception("Detail row is not the same width as the header's width value");
				}
				
				int cx = 0;
				for (char c: data.trim().toCharArray()) {
					if (c == 'M' || c == 'm') {
						
						minesCount++;
						
						result.board[cx][cy] = GameStateModel.MINE;
						
	                    // tell all the surrounding squares they are next to a mine
	                    for (int j=0; j < DX.length; j++) {
	                        if (cx + DX[j] >= 0 && cx + DX[j] < result.width && cy + DY[j] >= 0 && cy + DY[j] < result.height) {
	                            if (result.board[cx+DX[j]][cy+DY[j]] != GameStateModel.MINE) {
	                                result.board[cx+DX[j]][cy+DY[j]]++;
	                            }
	                        }
	                    }
						
						if (c == 'M') {
							result.setFlag(cx, cy);
						}
					} else if (c != 'H' && c != 'h') {
						int val = Character.getNumericValue(c);
						result.setRevealed(cx, cy);
						tempBoard[cx][cy] = val;
						result.safeOpening = false;     // if we have already revealed some square it isn't a safe opening
					}
					cx++;
				}
			
				cy++;
				data = reader.readLine();	
				
				if (cy == height) {
					break;
				}
				
			};

			if (cy != height) {
				throw new Exception("Not enough rows in the file for the game defined in the header");
			}
		
		} catch (Exception e) {
			throw e;
		}	
    	

		
		if (mines != minesCount) {
			System.out.println("Mines in puzzle is " + minesCount + ", but mines declared is " + mines);
			result.partialGame = true;
			
			// for partial games use the revealed values as given in the file
			for (int i=0; i < width; i++) {
				for (int j=0; j < height; j++) {
					result.board[i][j] = tempBoard[i][j];
				}
			}
			
			
		} else {
			result.partialGame = false;
		}
		
    	result.file = file;
		
    	result.start(new Location(0,0));
    	
		return result;
		
    }
    
    public final static GameStateModelViewer loadMBF(File file) throws Exception {
    	
    	System.out.println("Loading MBF file");
    	
    	int width;
    	int height;
    	int mines;
   	
    	GameStateReader result;

    	byte[] data = new byte[70000];
    	
		try (FileInputStream fis = new FileInputStream(file)){
			
			int size = 0;
			int length = 0;
			while (length != -1) {
				size = size + length;
				length = fis.read(data, size, 1024);
				System.out.println("Read " + length + " bytes");
			}
			System.out.println("Loaded " + size + " bytes in total");
			
			if (size == 0) {
				throw new Exception("File is empty!");
			}
			
			
			try {
				width = data[0];
				height = data[1];
				mines = data[2] * 256 + data[3];
				System.out.println("Width " + width+ " height " + height + " mines " + mines);
			} catch (Exception e) { 
				throw new Exception("Unable to parse the board values");
			}

			result = new GameStateReader(GameSettings.create(width, height, mines));

			for (int i=4; i < size; i+=2) {
				int x = data[i];
				int y = data[i+1];
				
				//System.out.println("mine at (" + x + "," + y + ")");
                result.board[x][y] = GameStateModel.MINE;
                
                // tell all the surrounding squares they are next to a mine
                for (int j=0; j < DX.length; j++) {
                    if (x + DX[j] >= 0 && x + DX[j] < result.width && y + DY[j] >= 0 && y + DY[j] < result.height) {
                        if (result.board[x+DX[j]][y+DY[j]] != GameStateModel.MINE) {
                            result.board[x+DX[j]][y+DY[j]]++;
                        }
                    }
                }

			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}	
    	
    	result.file = file;
		
    	result.start(new Location(0,0));
    	
		return result;
		
    }
    
    public final static GameStateModelViewer loadMines(int width, int height, List<Location> mines, List<Location> revealed) {
    	
    	GameStateReader result = new GameStateReader(GameSettings.create(width, height, mines.size()));
    	
    	for (Location mine: mines) {
    		
			int x = mine.x;
			int y = mine.y;
			
			//System.out.println("mine at (" + x + "," + y + ")");
            result.board[x][y] = GameStateModel.MINE;
            
            // tell all the surrounding squares they are next to a mine
            for (int j=0; j < DX.length; j++) {
                if (x + DX[j] >= 0 && x + DX[j] < result.width && y + DY[j] >= 0 && y + DY[j] < result.height) {
                    if (result.board[x+DX[j]][y+DY[j]] != GameStateModel.MINE) {
                        result.board[x+DX[j]][y+DY[j]]++;
                    }
                }
            }
    	}
    	
    	// set the revealed locations
    	for (Location tile: revealed) {
    		
			int x = tile.x;
			int y = tile.y;
			
			result.setRevealed(x, y);
    	}   	
     	
    	result.partialGame = false;  // indicates that the board is complete
    	result.safeOpening = false;
    	
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
    	
    	String gameKey = "";
    	
    	if (file != null) {
    		gameKey = "file = " + file.getAbsolutePath();
    	} else {
    		gameKey = "Generated";
    	}
    	
    	if (partialGame) {
    		gameKey = gameKey + " (Mines missing!)";
    	}
    	
    	return gameKey;
    	
    }
    
    public boolean supports3BV() {
    	return !partialGame;
    }
    
    /*
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
    */
    
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
