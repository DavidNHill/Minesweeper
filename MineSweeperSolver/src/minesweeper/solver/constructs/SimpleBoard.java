package minesweeper.solver.constructs;

import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.structure.Location;

public class SimpleBoard {

    private final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    
    final private int width;
    final private int height;
	final private byte[][] board;


	public SimpleBoard(int width, int height) {

		this.width = width;
		this.height = height;
		board = new byte[width][height];
		
	}

	public void addMines(List<Location> mines) {
		for (Location mine: mines) {
			addMine(mine);
		}
	}
	
	public void addMine(Location l) {
		addMine(l.x, l.y);
	}
	
	public void addMine(int x, int y) {
		
		board[x][y] = GameStateModel.MINE;
		
        for (int j=0; j < DX.length; j++) {
            if (x + DX[j] >= 0 && x + DX[j] < this.width && y + DY[j] >= 0 && y + DY[j] < this.height) {
				if (board[x + DX[j]][y + DY[j]] != GameStateModel.MINE) {
					board[x + DX[j]][y + DY[j]]++;
				}
            }
         }                       
		
	}

	/*
	public void removeMines(List<Location> mines) {
		for (Location mine: mines) {
			removeMine(mine);
		}
	}
	
	public void removeMine(Location l) {
		removeMine(l.x, l.y);
	}
	
	public void removeMine(int x, int y) {
		
        for (int j=0; j < DX.length; j++) {
            if (x + DX[j] >= 0 && x + DX[j] < this.width && y + DY[j] >= 0 && y + DY[j] < this.height) {
				if (board[x + DX[j]][y + DY[j]] != GameStateModel.MINE) {
					board[x + DX[j]][y + DY[j]]++;
				}
            }
         }                       
	}
	*/
	
	public byte[] getSolution(List<Location> locs) {
		
		byte[] result = new byte[locs.size()];
		
		int index = 0;
		for (Location l: locs) {
			result[index] = board[l.x][l.y];
			index++;
		}
		
		return result;
		
	}


}
