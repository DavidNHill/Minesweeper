package minesweeper.solver;

import java.util.ArrayList;
import java.util.List;

import minesweeper.structure.Location;

public class SpaceCounter {

    protected final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    protected final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
	
	private final BoardState board;
	private final int threshold;
	
	private final byte[][] data;
	private final int width;
	private final int height;
	
	
	public SpaceCounter(BoardState board, int threshold) {
		
		this.board = board;
		this.threshold = threshold;  // an area is considered large if greater than or equal to the threshold
		this.height = this.board.getGameHeight();
		this.width = this.board.getGameWidth();
		
		this.data = new byte[this.width][this.height];
		
	}
	
	
	public boolean meetsThreshold(Location loc) {
		
		boolean large = false;
		
		
		if (data[loc.x][loc.y] != 0) {
			large = (data[loc.x][loc.y] == 1);
		
		} else {
			
			int index = 0;
			
			List<Location> tiles = new ArrayList<>();

			tiles.add(loc);
			
			data[loc.x][loc.y] = -1;

			top: while (tiles.size() != index) {
		        
				Location m = tiles.get(index);
				
		        for (int j=0; j < DX.length; j++) {
		        	final int x1 = m.x + DX[j];
		        	final int y1 = m.y + DY[j];
		        	
		            if (x1 >= 0 && x1 < this.width && y1 >= 0 && y1 < this.height) {
		                if (this.board.isUnrevealed(x1, y1)) {
		                	if (data[x1][y1] == 0) {  // unprocessed tile
			                	data[x1][y1] = -1;
			                	tiles.add(this.board.getLocation(x1, y1));
		                	
		                	} else if (data[x1][y1] == 1) {  // if we meet a tile which is already in a large area, we are in a large area
		                		large = true;
		                		break top;
		                		
		                	} else {  // something has gone wrong since we can't encounter a small area
		                		//this.board.getLogger().log(Level.ERROR, "Space counter encountered an area below threshold");  
		                	}

		                } else if (this.board.isRevealed(x1, y1)) {  // if he board is revealed then see if we can hop over the tile into more open space
		                	
		    		        for (int k=0; k < DX.length; k++) {
		    		        	final int x2 = x1 + DX[k];
		    		        	final int y2 = y1 + DY[k];
		    		            if (x2 >= 0 && x2 < this.width && y2 >= 0 && y2 < this.height) {
			    		        	if (this.board.isUnrevealed(x2, y2)) {
					                	if (data[x2][y2] == 0) {  // unprocessed tile
						                	data[x2][y2] = -1;
						                	tiles.add(this.board.getLocation(x2, y2));
					                	
					                	} else if (data[x2][y2] == 1) {  // if we meet a tile which is already in a large area, we are in a large area
					                		large = true;
					                		break top;
					                		
					                	}		    		        		
			    		        	}		    		            	
		    		            }

		    		        
		    		        }
		                }
		            	
		            }
		        }      
		        
		        if (tiles.size() >= this.threshold) {
		        	large = true;
		        	break;
		        }
		        
		        index++;
				
			}
			
			// record the area if it exceeds the threshold
			if (large) {
				for (Location l: tiles) {
					data[l.x][l.y] = 1;
				}			
			}
			
		}
		
		if (large) {
			return true;
		} else {
			return false;
		}
	
	}
	
	
}
