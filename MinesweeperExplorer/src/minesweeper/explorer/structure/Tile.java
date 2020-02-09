package minesweeper.explorer.structure;

import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import minesweeper.explorer.main.Graphics;
import minesweeper.explorer.main.Graphics.GraphicsSet;
import minesweeper.explorer.structure.Board.AdjacentDetails;

public class Tile extends ImageView {

	// generic mouse click event for tiles
	private final static EventHandler<MouseEvent> CLICKED = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {

			
			Tile tile = (Tile) event.getSource();
			
			//System.out.println("Click detected on tile " + tile.asText());
			
			if (event.getButton() == MouseButton.PRIMARY) {
				if (tile.isMine()) {
					tile.board.RemoveMine(tile);
				} else {
					tile.board.setAsMine(tile , true);
				}
				
			}

			if (event.getButton() == MouseButton.SECONDARY) {

				if (tile.isFlagged()) {   // if flagged remove it
					tile.setFlagged(false);
				} else if (tile.isMine()){  // if mine flag it
					tile.setFlagged(true);
				} else if (tile.isCovered()){  // if covered mine and flag
					tile.board.setAsMine(tile, false);
					tile.setFlagged(true);
				} else {    // otherwise toggle between covered and uncovered
					tile.setCovered(!tile.isCovered());
				}
				
			}
			
		}
		
	};
	
	// generic mouse click event for tiles
	private final static EventHandler<ScrollEvent> SCROLLED = new EventHandler<ScrollEvent>() {

		@Override
		public void handle(ScrollEvent event) {
			
			Tile tile = (Tile) event.getSource();
			
			//System.out.println("Scroll detected on tile " + tile.asText() + " TextDeltaY() is " + event.getTextDeltaY());
			
			int delta;
			if (event.getTextDeltaY() < 0) {
				delta = 1;
			} else {
				delta = -1;
			}
			
			tile.rotateValue(delta);
			
		}
		
	};
	
	
	private final int x;
	private final int y;
	private final Board board;
	
	private GraphicsSet graphicsSet;
	private boolean covered;
	private boolean mine;
	private boolean flagged;
	
	private int value;

	public Tile(GraphicsSet graphicsSet, Board board, int x, int y) {
		
		this.board = board;
		this.x = x;
		this.y = y;
		
		this.graphicsSet = graphicsSet;
		
		this.relocate(x * graphicsSet.getSize(), y * graphicsSet.getSize());
		
		reset();
		
		this.setOnMouseClicked(CLICKED);
		this.setOnScroll(SCROLLED);

	}

	private void doDraw() {
		
		if (flagged) {
			this.setImage(graphicsSet.getFlag());
		} else if (mine) {
			this.setImage(graphicsSet.getMine());
		} else if (covered) {
			this.setImage(graphicsSet.getHidden());
		} else {
			this.setImage(graphicsSet.getNumber(value));
		}
		
		
	}
	
	
	public void reset() {
		covered = false;
		mine = false;
		flagged = false;
		value = 0;
		doDraw();
	}
	
	
	
	public boolean isCovered() {
		return covered;
	}

	public void setCovered(boolean covered) {
		this.covered = covered;
		doDraw();
	}

	public boolean isMine() {
		return mine;
	}

	protected void setMine(boolean mine) {
		this.mine = mine;
		doDraw();
	}

	public boolean isFlagged() {
		return flagged;
	}

	public void setFlagged(boolean flagged) {
		this.flagged = flagged;
		if (this.flagged) { // if flagged then also covered
			this.covered = true;
		}
		
		doDraw();
	}

	
	private void rotateValue(int delta) {
		
		AdjacentDetails adjDetails = board.getAdjacentDetails(this);
		
		int minValue = adjDetails.mines;
		int maxValue = adjDetails.mines + adjDetails.notMines;
		int range = maxValue - minValue;
		
		//System.out.println("Min = " + minValue + " max = " + maxValue);
		
		int newValue;
		
		if (this.covered) {
			if (delta < 0) {
				newValue = maxValue;
			} else {
				newValue = minValue;
			}
			
		} else if (range == 0) {
			newValue = minValue;
		} else {
			newValue = minValue + ((value - minValue + delta) % (range + 1));
		}
		

		if (newValue < minValue) {
			newValue = newValue + range + 1;
		}
		
		setValue(newValue);

	}
	
	public int getValue() {
		return value;

	}

	public void setValue(int value) {
		
		// don't set a value for tiles which are mines or flagged
		if (this.flagged || this.mine) {
			return;
		}
		
		/*
		AdjacentDetails adjDetails = board.countAdjMines(this);
		
		int minValue = adjDetails.mines;
		int maxValue = adjDetails.mines + adjDetails.notMines;
		
		if (value == minValue) {
			board.coverAdjacentZeroTiles(this, false);
		} else {
			board.coverAdjacentZeroTiles(this, true);
		}
		*/
		
		this.covered = false;
		this.value = value;
		doDraw();
	}
	
	public String asText() {
		String text = "(" + this.x + "," + this.y + ")";
		return text;
	}
	
	public int getTileX() {
		return this.x;
	}
	
	public int getTileY() {
		return this.y;
	}
	
}
