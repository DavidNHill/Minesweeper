package minesweeper.explorer.structure;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import minesweeper.explorer.main.Graphics.GraphicsSet;
import minesweeper.explorer.structure.Board.AdjacentDetails;
import minesweeper.structure.Location;

public class Tile extends StackPane {

	// generic mouse click event for tiles
	private final static EventHandler<MouseEvent> CLICKED = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {
			
			Tile tile = (Tile) event.getSource();
			
			//System.out.println("Click detected on tile " + tile.asText());
			
			if (event.getButton() == MouseButton.SECONDARY) {
				if (tile.isFlagged()) {
					tile.board.RemoveFlag(tile);
				} else {
					tile.board.setFlag(tile , true);
				}
			}
			
			if (event.getButton() == MouseButton.PRIMARY) {

				if (tile.isFlagged()) {   // if flagged do nothing

				} else {  // otherwise toggle between covered and uncovered
					tile.setCovered(!tile.isCovered());
					tile.board.setDraggedTile(tile);  // let the board know which tile is being dragged (if it is)
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
	
	private ImageView image;
	private Text text;
	
	private final int x;
	private final int y;
	private final Board board;
	private final Location location;
	private String textValue = "";
	
	private GraphicsSet graphicsSet;
	private boolean covered;
	private boolean flagged;
	
	private int value;

	public Tile(GraphicsSet graphicsSet, Board board, int x, int y) {
		
		this.image = new ImageView();
		this.text = new Text("");
		
		this.text.setScaleX(graphicsSet.getSize() / 24d);
		this.text.setScaleY(graphicsSet.getSize() / 24d);
		
		this.getChildren().addAll(image, text);
		
		this.board = board;
		this.x = x;
		this.y = y;
		this.location = new Location(x, y);
		
		this.graphicsSet = graphicsSet;
		
		this.relocate(x * graphicsSet.getSize(), y * graphicsSet.getSize());
		
		reset();
		
		this.setOnMousePressed(CLICKED);
		this.setOnScroll(SCROLLED);
		
	}

	private void doDraw() {
		
		if (flagged) {
			this.image.setImage(graphicsSet.getFlag());
		} else if (covered) {
			this.image.setImage(graphicsSet.getHidden());
		} else {
			this.image.setImage(graphicsSet.getNumber(value));
		}
		
		showTextValue();
		
	}
	
	
	public void reset() {
		covered = false;
		//mine = false;
		flagged = false;
		value = 0;
		doDraw();
	}
	
	public void resizeTile(GraphicsSet graphicsSet) {
		
		if (this.graphicsSet.getSize() == graphicsSet.getSize()) {
			return;
		}
	
		this.graphicsSet = graphicsSet;
		
		this.text.setScaleX(graphicsSet.getSize() / 24d);
		this.text.setScaleY(graphicsSet.getSize() / 24d);
		
		this.relocate(x * graphicsSet.getSize(), y * graphicsSet.getSize());
		doDraw();
		
	}
	
	
	
	public boolean isCovered() {
		return covered;
	}

	public void setCovered(boolean covered) {
		this.covered = covered;
		doDraw();
	}

	protected void setTextValue(BigDecimal safety) {
		
		safety = BigDecimal.ONE.subtract(safety).multiply(BigDecimal.valueOf(100));
		
		if (safety.compareTo(BigDecimal.TEN) < 0) {
			safety = safety.setScale(1, RoundingMode.HALF_UP);
		} else {
			safety = safety.setScale(0, RoundingMode.HALF_UP);
		}
		
		textValue = safety.toPlainString();
		showTextValue();
		
	}
	
	private void showTextValue() {
		
		if (isCovered() && !isFlagged()) {
			text.setText(textValue);
		} else {
			text.setText("");
		}
		
		
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
		
		int minValue = adjDetails.flags;
		int maxValue = adjDetails.flags + adjDetails.notflags;
		int range = maxValue - minValue;
		
		//System.out.println("Min = " + minValue + " max = " + maxValue);
		
		int newValue;
		
		if (this.covered) {
			this.covered = false;
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
		if (this.flagged) {
			return;
		}
		
		this.value = value;
		doDraw();
	}
	
	@Override
	public String toString() {
		
		String text = "(" + this.x + "," + this.y + ")";
		return text;
	}
	
	public int getTileX() {
		return this.x;
	}
	
	public int getTileY() {
		return this.y;
	}
	
	public Location getLocation() {
		return this.location;
	}
	
}
