package minesweeper.explorer.structure;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;

public class Expander extends Circle {

	// generic mouse click event for tiles
	private final EventHandler<MouseEvent> DRAGGED = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
		
			Expander expander = (Expander) event.getSource();
			
			Point2D xy = expander.localToParent(event.getX(), event.getY());
			
			expander.setCenterX(xy.getX());
			expander.setCenterY(xy.getY());
			
			toolTip.setX(event.getScreenX() + 10);
			toolTip.setY(event.getScreenY() - 10);
			
			int boardX = (int) (xy.getX() / size);
			int boardY = (int) (xy.getY() / size);
			
			String text = "(" + boardX + "," + boardY + ")";
			
			popupText.setText(text);
			
			toolTip.show(me().getScene().getWindow());
			
			/*
			if (event.getEventType() == MouseEvent.MOUSE_EXITED) {
				toolTip.hide();
			} else if (event.getEventType() == MouseEvent.MOUSE_ENTERED) {
				toolTip.show(window.getScene().getWindow());
			}
			*/
			
		}
	};
	
	// generic mouse click event for tiles
	private final EventHandler<MouseEvent> DRAG_ENDED = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
		
			toolTip.hide();
			
		}
	};
	
	// generic mouse click event for tiles
	private final EventHandler<MouseEvent> ENTERED = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
		
			toolTip.setX(event.getScreenX() + 10);
			toolTip.setY(event.getScreenY() - 10);
			
			toolTip.show(me().getScene().getWindow());
			
		}
	};
	
    private Popup toolTip = new Popup();
    private Text popupText = new Text();
    private double size;
	
	
	public Expander(int x, int y, int radius, double size, Color fill) {
		super(x * size, y * size, radius, fill);
		
        toolTip.getContent().addAll(popupText);
        popupText.setText("Test");
        popupText.setFont(new Font(20));
		this.size = size;
		this.setOnMouseDragged(DRAGGED);
		this.setOnMouseReleased(DRAG_ENDED);
		//this.setOnMouseEntered(ENTERED);
		//this.setOnMouseExited(DRAG_ENDED);
		
	}
	
	private Expander me() {
		return this;
	}

}
