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

	private boolean locked;
	//private int value;

	public LedDigits(int numberOfDigits) {
		this(numberOfDigits, false);
	}

	public LedDigits(int numberOfDigits, boolean locked) {

		this.numberOfDigits = numberOfDigits;
		this.digits = new LedDigit[numberOfDigits];

		this.locked = locked;

		for (int i=0; i < numberOfDigits; i++) {
			digits[i] = new LedDigit(this);
		}

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
		//this.value = value;

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
