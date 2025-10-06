package minesweeper.solver.utility;

import java.math.BigInteger;
import java.util.Arrays;

public class BinomialCache  {

	private class BinomialEntry implements Comparable<BinomialEntry> {
		final private int n;
		final private int k;
		final private BigInteger bco;
		
		private long lastUsed;
		
		private BinomialEntry(int k, int n, BigInteger bco) {
			this.k = k;
			this.n = n;
			this.bco = bco;
		}

		@Override
		public int compareTo(BinomialEntry o) {
			if (lastUsed < o.lastUsed) {
				return -1;
			} else if (lastUsed > o.lastUsed) {
				return 1;
			} else {
				return 0;
			}
		}
		
	}
	private BinomialEntry[] cache;
	private int start = -1;
	private long useCount = 0;

	private int cacheHits = 0;
	private int cacheStored = 0;
	private int sumDepth = 0;
	private int nearMissHits = 0;
	private int fullCalc = 0;
	
	private final int cacheSize;
	private final int cacheFreshold;
	private final int compressRemoval;
	
	private final Binomial binomialEngine;
	
	public BinomialCache(int cacheSize, int cacheFreshold, Binomial binomialEngine) {
		
		this.cacheSize = cacheSize;
		this.cacheFreshold = cacheFreshold;
		this.binomialEngine = binomialEngine;
		this.compressRemoval = cacheSize / 2;
		
		this.cache = new BinomialEntry[this.cacheSize];
		
	}

	public BigInteger getBinomial(int k, int n) {
		
		// if the binomial is below the size freshold then just go get it
		if (n <= cacheFreshold) {
			try {
				return binomialEngine.generate(k, n);
			} catch (Exception e) {
				e.printStackTrace();
				return BigInteger.ONE;
			}
		}
		
		useCount++;
		
		// if there are some details in the cache then search it
		BinomialEntry nearMissK = null;
		BinomialEntry nearMissN = null;
		if (start != -1) {
			int tally = 0;
			for (int i = start; i >= 0; i--) {
				BinomialEntry entry = cache[i];
				tally++;
				if (entry.n == n && entry.k == k) {
					entry.lastUsed = useCount;
					cacheHits++;
					sumDepth = sumDepth + tally;
					
					//if (cacheHits % 250 == 0) {
					//	Arrays.sort(cache, 0, start);
					//}
					
					return entry.bco;
				}
				if (entry.n == n && entry.k == k + 1) {
					nearMissK = entry;
				}
				if (entry.n == n + 1 && entry.k == k) {
					nearMissN = entry;
				}
			}
		}

		BigInteger b;
		if (nearMissK != null) {  // one below a cached entry we can do quickly
			b = nearMissK.bco.multiply(BigInteger.valueOf(nearMissK.k)).divide(BigInteger.valueOf(nearMissK.n - nearMissK.k + 1));
			nearMissHits++;
			
		} else if (nearMissN != null) {  // one below a cached entry we can do quickly
			b = nearMissN.bco.multiply(BigInteger.valueOf(nearMissN.n - nearMissN.k)).divide(BigInteger.valueOf(nearMissN.n));
			nearMissHits++;
			
		} else {  		// not in the cache, so generate it
			try {
				b = binomialEngine.generate(k, n);
				fullCalc++;
			} catch (Exception e) {
				e.printStackTrace();
				b = BigInteger.ONE;
			}			
		}
		
		if (start == cacheSize - 1) {
			compressCache();
		} 
		//else if (useCount % 50 == 0) {
		//	Arrays.sort(cache, 0, start);
		//}

		start++;
		BinomialEntry be = new BinomialEntry(k, n, b);
		be.lastUsed = useCount;
		cacheStored++;
		
		cache[start] = be;
		
		return b;
	}
	
	// remove the least used binomials from the cache
	private void compressCache() {
		
		//System.out.print(Thread.currentThread().getName() + " Cache compressing...");
		
		Arrays.sort(cache);
		
		if (cache[0].lastUsed > cache[1].lastUsed) {
			System.out.println("Sort order wrong!");
		}
		
		for (int i=0; i < this.cacheSize - this.compressRemoval; i++) {
			cache[i] = cache[i + this.compressRemoval];
		}

		this.start = this.start - this.compressRemoval;
		//System.out.println("Cache compressed ..." + this.start);
	}

	public void showStats() {
		
		int avgDepth = 0;
		if (cacheHits != 0) {
			avgDepth = sumDepth / cacheHits;
		}
		
		
		System.out.println(Thread.currentThread().getName() + " Cache stored: " + cacheStored + ", cache Hits: " + cacheHits + ", Near Miss Hits: " + nearMissHits 
				+ ", Full Calculation: " + fullCalc + ", current entries: " + (start + 1) + ", avg depth: " + avgDepth);
	}

	
}
