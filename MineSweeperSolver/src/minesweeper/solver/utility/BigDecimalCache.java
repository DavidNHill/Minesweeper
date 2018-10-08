package minesweeper.solver.utility;

import java.math.BigDecimal;

public class BigDecimalCache {
	
	private static BigDecimal[] cache = new BigDecimal[4000];
	static {
		for (int i=0; i < cache.length; i++) {
			cache[i] = BigDecimal.valueOf(i);
		}
	}
	
	
	public static BigDecimal get(int i) {
		
		if (i < cache.length) {
			return cache[i];
		} else {
			return BigDecimal.valueOf(i);
		}
		
		
	}
}
