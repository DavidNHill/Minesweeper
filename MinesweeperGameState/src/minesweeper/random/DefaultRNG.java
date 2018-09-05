package minesweeper.random;

public class DefaultRNG {

	static private Class<? extends RNG> defaultRNG = RNGJava.class;
	
	
	/**
	 * Set the default RNG implementation used when creating the mine sweeper boards
	 * @param rngClass
	 */
	public static void setDefaultRNGClass(Class<? extends RNG> rngClass) {
		defaultRNG = rngClass;
	}
	
	
	/**
	 * Get the default RNG implementation used when creating the mine sweeper boards
	 * @param rngClass
	 */
	public static Class<? extends RNG> getDefaultRNGClass() {
		return defaultRNG;
	}
	
	/**
	 * Return an instance of the default random number generator with seed
	 * @return
	 */
	public static RNG getRNG(long seed) {
		
		RNG rng = null;
		try {
			rng = defaultRNG.newInstance();
			rng.seed(seed);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return rng;
		
	}
	
}
