package minesweeper.solver.constructs;

public class SubLocation extends ProbabilityLocation {

	final int size;
	final int value;  // this is the value the location needs to come good as a sub location
	
	public SubLocation(int x, int y, int size, int value) {
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
		// TODO Auto-generated method stub
		return new int[] {this.value};
	}
		
	@Override
	public String display() {
		
		return "SubLocation: " + super.display() + " value " + value + " size " + size;
	}
		

}
