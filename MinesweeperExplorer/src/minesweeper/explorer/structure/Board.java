package minesweeper.explorer.structure;

import java.math.BigInteger;
import java.util.Map;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import minesweeper.explorer.main.Explorer;
import minesweeper.explorer.main.Graphics.GraphicsSet;
import minesweeper.explorer.main.MainScreenController;
import minesweeper.solver.constructs.InformationLocation;
import minesweeper.structure.Location;

public class Board extends AnchorPane {

    private final EventHandler<MouseEvent> TOOLTIP = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {
			
			//System.out.println(event.getX() + "," + event.getY());
			
			// exited the board
			if (event.getEventType() == MouseEvent.MOUSE_EXITED) {
				hideTooltip();
				return;
			} 
			
			toolTip.setX(event.getScreenX() + 10);
			toolTip.setY(event.getScreenY() - 10);

			int boardX = (int) (event.getX() / graphicsSet.getSize());
			int boardY = (int) (event.getY() / graphicsSet.getSize());
			
			// exited the board
			if (boardX < 0 || boardX >= width || boardY < 0 || boardY >= height) {
				hideTooltip();
				return;
			}
			
			Tile tile = tiles[boardX][boardY];
			
			if (!tile.isCovered() || tile.isFlagged()) {   // flag or not hidden
				hideTooltip();
			} else {
				showTooltip(event.getScreenX() + 10, event.getScreenY() - 10, tile);
				populateTileDetails(tile);
			}
			
		
		}
    	
    };
	
	protected class AdjacentDetails {
		
		public final int flags;
		public final int notflags;
		
		private AdjacentDetails(int mines, int notMines) {
			this.flags = mines;
			this.notflags = notMines;
		}
		
	}
	
    private Popup toolTip = new Popup();
    private Text tooltipText = new Text();
    
    
	private final int width;
	private final int height;
	private GraphicsSet graphicsSet;
	private final MainScreenController controller;
	
	private ReadOnlyIntegerWrapper flagsPlaced = new ReadOnlyIntegerWrapper();
	private Map<Location, InformationLocation> gameInformation;
	
	private final Tile[][] tiles;
	
	public Board(MainScreenController controller, int width, int height) {
		super();
		
		this.width = width;
		this.height = height;
		
		this.controller = controller;
		
		this.graphicsSet = controller.getGraphicsSet();
		
		this.tiles = new Tile[width][height];
		
		clearBoard(false);
		
		for (int x=0; x < this.width; x++) {
			this.getChildren().addAll(tiles[x]);
		}		
		
        toolTip.getContent().addAll(tooltipText);
        tooltipText.setText("Test");
        tooltipText.setFont(new Font(20));
        
        this.setOnMouseMoved(TOOLTIP);
        this.setOnMouseEntered(TOOLTIP);
        this.setOnMouseExited(TOOLTIP);
		
	}
	
	public void setFlag(Tile tile, boolean spread) {
		
		if (tile.isFlagged()) {
			return;
		}
		
		tile.setFlagged(true);
		flagsPlaced.set(flagsPlaced.get() + 1);
		
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
				
				int adjMines = getAdjacentDetails(adjTile).flags;
				
				// keep the adjacent value in step if it was in step to start with
				if (adjTile.getValue() == adjMines - 1) {
					adjTile.setValue(adjMines);
				}

			}
		}

	}
	
	public void RemoveFlag(Tile tile) {
		
		if (!tile.isFlagged()) {
			return;
		}
		
		tile.setFlagged(false);
		flagsPlaced.set(flagsPlaced.get() - 1);
		
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
				int adjMines = getAdjacentDetails(adjTile).flags;
				
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
		
		int flags = 0;
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
				
				if (adjTile.isFlagged()) {
					flags++;
				} else {
					notMines++;
				}
			}
		}		
		
		return new AdjacentDetails(flags, notMines);
	}
	

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
		
		flagsPlaced.set(0);
		setGameInformation(null);
		
	}
	
   public ReadOnlyIntegerProperty getMinesPlacedProperty() {
      return flagsPlaced.getReadOnlyProperty();
   }
   
   public int getFlagsPlaced() {
	   return flagsPlaced.get();
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
   
	public void showTooltip(double x, double y, Tile tile) {
		
		if (gameInformation == null) {
			tooltipText.setText("?");
		} else {
			InformationLocation il = gameInformation.get(tile.getLocation());
			if (il != null) {
				tooltipText.setText(Explorer.PERCENT.format(il.getProbability()) + " safe");
			}
		}
		
		toolTip.setX(x);
		toolTip.setY(y);
		toolTip.show(this.getScene().getWindow());
	}
	
	public void populateTileDetails(Tile tile) {
		
		if (gameInformation == null) {
			//System.out.println("Game information not found");
			controller.getTileValueController().update(null);
			return;
		}

		InformationLocation il = gameInformation.get(tile.getLocation());
		//if (il == null) {
		//	//System.out.println("Tile information not found for " + tile.asText() + " out of " + gameInformation.size());
		//	controller.getTileValueController().update(null);
		//	return;
		//}
		
		controller.getTileValueController().update(il);
		
	}
	
	public void hideTooltip() {
		toolTip.hide();
	}
   
	public void setGameInformation(Map<Location, InformationLocation> info) {
		this.gameInformation = info;
	}
	
	public int getHashValue() {
		
		int hash = 31*31*31 * controller.getGameMines() + 31*31 * flagsPlaced.get() + 31 * width + height;
		
		for (int x=0; x < this.width; x++) {
			for (int y=0; y < this.height; y++) {
				Tile tile = tiles[x][y];
				if (tile == null) {
					
				} else {
					if (tile.isFlagged()) {
						hash = 31 * hash + 13;
					} else if (tile.isCovered()) {
						hash = 31 * hash + 12;
					} else {
						hash = 31 * hash + tile.getValue();
					}
					
				}
			}
		}
		
		return hash;
	}
	
	
   @Override
   protected void finalize() {
	   System.out.println("At finalize() for Board.java");
  
   }
}
	