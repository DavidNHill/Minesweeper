package minesweeper.explorer.structure;

import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import minesweeper.explorer.main.Graphics;

public class LedDigit extends ImageView {
	
	// generic mouse scroll event for led digits
	private final static EventHandler<ScrollEvent> SCROLLED = new EventHandler<ScrollEvent>() {

		@Override
		public void handle(ScrollEvent event) {
	
			LedDigit digit = (LedDigit) event.getSource();
			
			if (digit.owner.isLocked()) {
				return;
			}
			
			//System.out.println("Scroll detected on digit " + event.getTextDeltaY());
			
			int delta;
			if (event.getTextDeltaY() < 0) {
				delta = 1;
			} else {
				delta = -1;
			}
			
			
			digit.rotateValue(delta);
			
		}
		
	};
	
	
	
	private final LedDigits owner;
	
	private int value = 0;
	
	
	public LedDigit(LedDigits owner) {
		
		this.owner = owner;
		
		this.setOnScroll(SCROLLED);
		this.setPickOnBounds(true);
		
		doDraw();
		
	}
	
	
	private void doDraw() {
		setImage(Graphics.getLed(value));
	}
	
	public int getValue() {
		return this.value;
	}
	
	
	public void setValue(int value) {
		this.value = value;
		doDraw();
	}
	
	private void rotateValue(int delta) {
		
		int newValue = (value + delta) % 10;
		if (newValue < 0) {
			newValue = newValue + 10;
		}
		
		setValue(newValue);
	}
	

}
