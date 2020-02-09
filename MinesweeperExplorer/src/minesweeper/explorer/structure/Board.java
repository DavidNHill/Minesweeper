package minesweeper.explorer.structure;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import minesweeper.explorer.main.Graphics.GraphicsSet;

public class Board extends AnchorPane {

	protected class AdjacentDetails {
		
		public final int mines;
		public final int notMines;
		
		private AdjacentDetails(int mines, int notMines) {
			this.mines = mines;
			this.notMines = notMines;
		}
		
	}
	
	
	private final int width;
	private final int height;
	private GraphicsSet graphicsSet;
	
	private ReadOnlyIntegerWrapper minesPlaced = new ReadOnlyIntegerWrapper();
	
	private final Tile[][] tiles;
	
	public Board(GraphicsSet graphicsSet, int width, int height) {
		super();
		
		this.width = width;
		this.height = height;
		
		this.graphicsSet = graphicsSet;
		
		this.tiles = new Tile[width][height];
		
		clearBoard(false);
		
		for (int x=0; x < this.width; x++) {
			this.getChildren().addAll(tiles[x]);
		}		
		
	}
	
	public void setAsMine(Tile tile, boolean spread) {
		
		if (tile.isMine()) {
			return;
		}
		
		tile.setMine(true);
		minesPlaced.set(minesPlaced.get() + 1);
		
		
		if (!spread) {
			return;
		}
		
		int startx = Math.max(0, tile.getTileX() - 1);
		int endx = Math.min(width - 1, tile.getTileX() + 1);
		
		int starty = Math.max(0, tile.getTileY() - 1);
		int endy = Math.min(height - 1, tile.getTileY() + 1);
		
		//System.out.println("x: " + startx + " - " + endx);
		//System.out.println("y: " + starty + " - " + endy);
		
		for (int x=startx; x <= endx; x++) {
			for (int y=starty; y <= endy; y++) {
				
				if (x == tile.getTileX() && y == tile.getTileY()) {
					continue;
				}
				
				Tile adjTile = tiles[x][y];
				
				int adjMines = getAdjacentDetails(adjTile).mines;
				
				// keep the adjacent value in step if it was in step to start with
				if (adjTile.getValue() == adjMines - 1) {
					adjTile.setValue(adjMines);
				}

			}
		}

	}
	
	public void RemoveMine(Tile tile) {
		
		if (!tile.isMine()) {
			return;
		}
		
		tile.setMine(false);
		minesPlaced.set(minesPlaced.get() - 1);
		
		int startx = Math.max(0, tile.getTileX() - 1);
		int endx = Math.min(width - 1, tile.getTileX() + 1);
		
		int starty = Math.max(0, tile.getTileY() - 1);
		int endy = Math.min(height - 1, tile.getTileY() + 1);
		
		//System.out.println("x: " + startx + " - " + endx);
		//System.out.println("y: " + starty + " - " + endy);
		
		for (int x=startx; x <= endx; x++) {
			for (int y=starty; y <= endy; y++) {
				
				if (x == tile.getTileX() && y == tile.getTileY()) {
					continue;
				}
				
				Tile adjTile = tiles[x][y];
				int adjMines = getAdjacentDetails(adjTile).mines;
				
				// keep the adjacent value in step if it was in step to start with
				if (adjTile.getValue() == adjMines + 1) {
					adjTile.setValue(adjMines);
				}
			}
		}
		
	}
	
	protected void coverAdjacentZeroTiles(Tile tile, boolean covered) {
	
		int startx = Math.max(0, tile.getTileX() - 1);
		int endx = Math.min(width - 1, tile.getTileX() + 1);
		
		int starty = Math.max(0, tile.getTileY() - 1);
		int endy = Math.min(height - 1, tile.getTileY() + 1);
		
		//System.out.println("x: " + startx + " - " + endx);
		//System.out.println("y: " + starty + " - " + endy);
		
		for (int x=startx; x <= endx; x++) {
			for (int y=starty; y <= endy; y++) {
				
				if (x == tile.getTileX() && y == tile.getTileY()) {
					continue;
				}
				
				Tile adjTile = tiles[x][y];
				
				if (adjTile.getValue() == 0) {
					adjTile.setCovered(covered);					
				}

			}
		}
		
	}
	
	
	protected AdjacentDetails getAdjacentDetails(Tile tile) {
		
		int mines = 0;
		int notMines = 0;
		
		int startx = Math.max(0, tile.getTileX() - 1);
		int endx = Math.min(width - 1, tile.getTileX() + 1);
		
		int starty = Math.max(0, tile.getTileY() - 1);
		int endy = Math.min(height - 1, tile.getTileY() + 1);
		
		//System.out.println("x: " + startx + " - " + endx);
		//System.out.println("y: " + starty + " - " + endy);
		
		for (int x=startx; x <= endx; x++) {
			for (int y=starty; y <= endy; y++) {
				
				if (x == tile.getTileX() && y == tile.getTileY()) {
					continue;
				}
				
				Tile adjTile = tiles[x][y];
				
				if (adjTile.isMine()) {
					mines++;
				} else {
					notMines++;
				}
			}
		}		
		
		return new AdjacentDetails(mines, notMines);
	}
	

	/*
	public void load(ObservableList<Node> container) {
		for (int x=0; x < this.width; x++) {
			container.addAll(tiles[x]);
		}		
	}
	*/
	
	public void clearBoard(boolean covered) {
		
		for (int x=0; x < this.width; x++) {
			for (int y=0; y < this.height; y++) {
				if (tiles[x][y] == null) {
					tiles[x][y] = new Tile(graphicsSet, this, x, y);
				} else {
					tiles[x][y].reset();
					tiles[x][y].setCovered(covered);
				}
			}
		}
		
		minesPlaced.set(0);
		
	}
	
   public ReadOnlyIntegerProperty getMinesPlacedProperty() {
      return minesPlaced.getReadOnlyProperty();
   }
   
   public Tile getTile(int x, int y) {
	   return tiles[x][y];
   }
   
   
   public int getGameWidth() {
	   return this.width;
   }
   
   public int getGameHeight() {
	   return this.height;
   }
   
   @Override
   protected void finalize() {
	   System.out.println("At finalize() for Board.java");
  
   }
}
	