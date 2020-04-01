package minesweeper.explorer.main;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import minesweeper.solver.constructs.InformationLocation.ByValue;

public class TileValueData {

	private final StringProperty value;
	private final StringProperty probability;
	private final StringProperty clears;
	
	public TileValueData(ByValue bv) {
		
		this.value = new SimpleStringProperty(String.valueOf(bv.value));
		this.probability = new SimpleStringProperty(Explorer.PERCENT.format(bv.probability));
		this.clears = new SimpleStringProperty(String.valueOf(bv.clears));

	}
	
	public TileValueData(String value, String prob, String clears) {
		
		this.value = new SimpleStringProperty(value);
		this.probability = new SimpleStringProperty(prob);
		this.clears = new SimpleStringProperty(clears);

	}
	
	public StringProperty probabilityProperty() {
		return probability;
	}
	
	public StringProperty clearsProperty() {
		return clears;
	}
	
	public StringProperty valueProperty() {
		return value;
	}
}
