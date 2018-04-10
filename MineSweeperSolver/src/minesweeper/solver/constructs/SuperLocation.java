package minesweeper.solver.constructs;

public class SuperLocation extends ProbabilityLocation {

	final int size;
	final int value;
	
	public SuperLocation(int x, int y, int size, int value) {
		super(x, y);

		this.size = size;
		this.value = value;
		
	}
	
	
	public int getSize() {
		return this.size;
	}
	
	
	public int getValue() {
		return this.value;
	}


	@Override
	public int[] getAdjacentFlagsRequired() {
		return  new int[] {this.value};
	}

	@Override
	public String display() {
		
		return "SuperLocation: " + super.display() + " value " + value + " size " + size;
	}
	
}
