package minesweeper.explorer.structure;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.HBox;

public class LedDigits extends HBox {

	ChangeListener VALUE_LISTENER = new ChangeListener() {
		@Override
		public void changed(ObservableValue observable, Object oldValue, Object newValue) {
			setValue((Integer) newValue);
		}
	};
	ReadOnlyIntegerProperty monitoredProperty;

	private final int numberOfDigits;
	private final LedDigit[] digits;

	private final int maxValue;
	
	private boolean locked;
	//private int value;

	public LedDigits(int numberOfDigits) {
		this(numberOfDigits, false);
	}

	public LedDigits(int numberOfDigits, boolean locked) {

		this.numberOfDigits = numberOfDigits;
		this.digits = new LedDigit[numberOfDigits];

		this.locked = locked;
	
		int size = 1;

		for (int i=0; i < numberOfDigits; i++) {
			digits[numberOfDigits - i - 1] = new LedDigit(this, size);
			size = size * 10;
		}

		this.maxValue = size - 1;
		//System.out.println(this.maxValue);
		
		// add the digits to the container
		this.getChildren().addAll(digits);

	}


	private void doDraw() {

	}

	public void setValueListener(ReadOnlyIntegerProperty valueProperty) {
		monitoredProperty = valueProperty;
		monitoredProperty.addListener(VALUE_LISTENER);
	}

	public void removeValueListener() {
		if (monitoredProperty == null) {
			return;
		}
		
		monitoredProperty.removeListener(VALUE_LISTENER);
		monitoredProperty = null;
	}

	public void setValue(int value) {

		if (value < 0) {
			value = 0;
		} else if (value > this.maxValue) {
			value = this.maxValue;
		}
		
		int work = value;
		for (int i=numberOfDigits - 1; i >= 0; i-- ) {
			int digitValue = work % 10;
			work = (work - digitValue) / 10;
			digits[i].setValue(digitValue);
		}

		doDraw();
	}

	public int getValue() {
		
		int work = 0;
		int exponent = 1;
		for (int i=numberOfDigits - 1; i >= 0; i-- ) {
			work = work + digits[i].getValue() * exponent;
			exponent = exponent * 10;
		}

		return work;
	}
	
	public boolean isLocked() {
		return locked;
	}

}
