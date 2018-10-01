package minesweeper.bulk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import minesweeper.gamestate.GameStateModel;

public class TableData {

	private final static BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private final static DecimalFormat PERCENT = new DecimalFormat("#0.000%");
	
	private final IntegerProperty count;
	private final LongProperty seed;
	private final IntegerProperty complete;
	private final StringProperty result;
	
	public TableData(int count, GameStateModel gs) {
		
		this.count = new SimpleIntegerProperty(count);
		
		seed = new SimpleLongProperty(gs.getSeed());
		
		/*
		BigDecimal areaToReveal = BigDecimal.valueOf(gs.getx() * gs.gety() - gs.getMines());
		BigDecimal areaLeftToReveal = BigDecimal.valueOf(gs.getHidden() - gs.getMines());
		
		BigDecimal percentageToDo = ONE_HUNDRED.multiply(BigDecimal.ONE.subtract(areaLeftToReveal.divide(areaToReveal, 6, RoundingMode.HALF_UP))).setScale(2, RoundingMode.HALF_UP);
		
		System.out.println(percentageToDo + " " + (gs.getGameState() == GameStateModel.WON) + " " + areaLeftToReveal);
		*/
		
		complete = new SimpleIntegerProperty(gs.getWidth() * gs.getHeight() - gs.getHidden());
		
		if (gs.getGameState() == GameStateModel.WON) {
			result = new SimpleStringProperty("Won");
		} else {
			result = new SimpleStringProperty("Lost");
		}
		
	}
	
	

	
	public LongProperty seedProperty() {
		return seed;
	}
	
	public IntegerProperty completeProperty() {
		return complete;
	}
	
	public StringProperty resultProperty() {
		return result;
	}
	
	public IntegerProperty countProperty() {
		return count;
	}
}
