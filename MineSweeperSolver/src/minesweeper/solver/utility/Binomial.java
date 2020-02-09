package minesweeper.solver.utility;
import java.math.BigInteger;

public class Binomial {

	private final int max;
	private final PrimeSieve ps;
	
	private final BigInteger[][] binomialLookup;
	private final int lookupLimit;
	
	public Binomial(int max, int lookup) {
		
		this.max = max;
		
		ps = new PrimeSieve(this.max);
		
		if (lookup < 10) {
			lookup = 10;
		}
		this.lookupLimit = lookup;
		
		final int lookup2 = lookup / 2;
		
		binomialLookup = new BigInteger[lookup + 1][lookup2 + 1];
		
		for (int total = 1; total <= lookup; total++) {
			for (int choose = 0; choose <= total / 2; choose++) {
				try {
					binomialLookup[total][choose] = generate(choose, total);
					//System.out.println("Binomial " + total + " choose " + choose + " is " + binomialLookup[total][choose]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
		}
		
	}
	
	
	public BigInteger generate(int k, int n) throws Exception {

		if (n == 0 && k == 0) {
			return BigInteger.ONE;
		}

		if (n < 1 || n > max) {
			throw new Exception("Binomial: 1 <= n and n <= max required, but n was " + n + " and max was " + max);
		}
		
		if (0 > k || k > n) {
			throw new Exception("Binomial: 0 <= k and k <= n required, but n was " + n + " and k was " + k);
		}
		
		int choose = Math.min(k, n-k);
		
		if (n <= lookupLimit && binomialLookup[n][choose] != null) {
			return binomialLookup[n][choose];
		} else if (choose < 125) {
			return combination(choose, n);
		} else {
			return combinationLarge(choose, n);
		}
	
	}
	
    private static BigInteger combination(int mines, int squares) {
        
        BigInteger top = BigInteger.ONE;
        BigInteger bot = BigInteger.ONE;
        
        int range = Math.min(mines, squares - mines);
        
        // calculate the combination. 
        for (int i = 0; i < range; i++) {
            top = top.multiply(BigInteger.valueOf(squares - i));
            bot = bot.multiply(BigInteger.valueOf(i+1));
        }
        
        BigInteger result = top.divide(bot);
        
        return result;
        
    }    
	
	
	private BigInteger combinationLarge(int k, int n) throws Exception {

		if ((k == 0) || (k == n)) return BigInteger.ONE;
		
		int n2 = n / 2;
		
		if (k > n2) {
			k = n - k; 
		}

		int nk = n - k;

		int rootN = (int) Math.floor(Math.sqrt(n));

		BigInteger result = BigInteger.ONE;


		for (int prime : ps.getPrimesIterable(2, n)) {

			if (prime > nk) {
				result = result.multiply(BigInteger.valueOf(prime));
				continue;
			}

			if (prime > n2) {
				continue;
			}

			if (prime > rootN) {
				if (n % prime < k % prime) {
					result = result.multiply(BigInteger.valueOf(prime));
				}
				continue;
			}

			int r = 0, N = n, K = k, p = 1;

			while (N > 0) {
				r = (N % prime) < (K % prime + r) ? 1 : 0;
				if (r == 1)
				{
					p *= prime;
				}
				N /= prime;
				K /= prime;
			}
			if (p > 1) {
				result = result.multiply(BigInteger.valueOf(p));
			}
		}

		return result;
	}

}
