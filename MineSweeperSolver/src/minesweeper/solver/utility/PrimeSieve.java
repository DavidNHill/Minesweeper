package minesweeper.solver.utility;
import java.util.Iterator;

public class PrimeSieve {
	
    // iterator for prime numbers
    private class Primes implements Iterable<Integer>, Iterator<Integer> {
    	
    	private int index = 0;
    	private int stop;
    	private int nextPrime;
    	
    	private Primes(int start, int stop) {
    		this.index = start;
    		this.stop = stop;
    		this.nextPrime = findNext();
    	}

		@Override
		public Iterator<Integer> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return (nextPrime != -1);
		}

		@Override
		public Integer next() {

			int result = nextPrime;
			nextPrime = findNext();
			
			return result;
		}
		
		private int findNext() {

			int next = -1;
			while (index <= stop && next == -1) {
	            if (!composite[index]) {
	            	next = index;
	            }		
	            index++;
			}

			return next;

		}

    }
	
	
	private final boolean[] composite;
	private final int max;
	
	public PrimeSieve(int n) {
		
		if (n < 2) {
			max = 2;
		} else {
			max = n;
		}
		
		composite = new boolean[max + 1];

		final int rootN = (int) Math.floor(Math.sqrt(n));
		
		for (int i=2; i < rootN; i++) {
			
			// if this is a prime number (not composite) then sieve the array
			if (!composite[i]) {
				int index = i + i;
				while (index <= max) {
					composite[index] = true;
					index = index + i;
				}
			}
		}

	}

	
	
	public boolean isPrime(int n) throws Exception {
		if (n <= 1 || n > max) {
			throw new Exception("Test value " + n + " is out of range 2 - " + max);
		}
		
		return !composite[n];
	}
	
    protected Iterable<Integer> getPrimesIterable(int start, int stop) throws Exception {
    	
    	if (start > stop) {
    		throw new Exception("start " + start + " must be <= to stop " + stop);
    	}
		if (start <= 1 || start > max) {
			throw new Exception("Start value " + start + " is out of range 2 - " + max);
		}
		if (stop <= 1 || stop > max) {
			throw new Exception("Stop value " + stop + " is out of range 2 - " + max);
		}
    	
    	return new Primes(start, stop);
    }
	
}
