package minesweeper.random;

public interface RNG {

	public void seed(long seed);
	
	public long random(int in);
	
	public String name();
	
	public String shortname();
	
}
