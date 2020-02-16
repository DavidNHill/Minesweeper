package minesweeper.explorer.main;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import minesweeper.solver.constructs.InformationLocation.ByValue;

public class TileValueData {

	private final static DecimalFormat PERCENT = new DecimalFormat("#0.000%");
	
	private final IntegerProperty value;
	private final StringProperty probability;
	private final IntegerProperty clears;
	
	public TileValueData(ByValue bv) {
		
		this.value = new SimpleIntegerProperty(bv.value);
		
		probability = new SimpleStringProperty(PERCENT.format(bv.probability));
		
		clears = new SimpleIntegerProperty(bv.clears);

	}
	
	public StringProperty probabilityProperty() {
		return probability;
	}
	
	public IntegerProperty clearsProperty() {
		return clears;
	}
	
	public IntegerProperty valueProperty() {
		return value;
	}
}
