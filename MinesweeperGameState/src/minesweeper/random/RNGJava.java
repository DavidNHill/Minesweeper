package minesweeper.random;

import java.util.Random;

public class RNGJava implements RNG {

	static String shortName = "Java RNG";
	
	private Random rng = new Random();
	
	public RNGJava() {
	}
	
	public RNGJava(long seed) {
		seed(seed);
	}
	
	@Override
	public void seed(long seed) {
		rng = new Random(seed);
	}

	@Override
	public long random(int in) {

		if (in == 0) {
			return rng.nextLong();
		} else {
			return rng.nextInt(in);
		}
		//return (long) Math.floor(rng.nextDouble() * in);
		
	}

	@Override
	public String name() {
		return "Standard Java random numbers";
	}
	
	@Override
	public String shortname() {
		return "Java RNG";
	}
	
	
}
