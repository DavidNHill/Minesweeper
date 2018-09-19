package minesweeper.random;


public class RNGKiss64 implements RNG {
	
	
	private long kiss64_x = 1234567890987654321l;
	private long kiss64_c = 123456123456123456l;
	private long kiss64_y = 362436362436362436l;
	private long kiss64_z = 1066149217761810l;
	private long kiss64_t = 0;
	
	public RNGKiss64() {
	}
	
	public RNGKiss64(long seed) {
		seed(seed);
	}
	
	public void seed(long seed) {
	
		kiss64_x = seed | 1;
		kiss64_c = seed | 2;
		kiss64_y = seed | 4;
		kiss64_z = seed | 8;
		kiss64_t = 0;
	
	}
	
	@Override
	public long random(int in) {
		
		// multiply with carry
		kiss64_t = (kiss64_x << 58) + kiss64_c;
		kiss64_c = (kiss64_x >>> 6);   // unsigned right shift
		kiss64_x += kiss64_t;
		
		
		//kiss64_c += (kiss64_x < kiss64_t)?1l:0l;
		
		kiss64_c += Long.compareUnsigned(kiss64_x, kiss64_t) < 0 ? 1l:0l;
		
		// XOR shift
		kiss64_y ^= (kiss64_y << 13);
		kiss64_y ^= (kiss64_y >>> 17);  // unsigned right shift
		kiss64_y ^= (kiss64_y << 43);
		
		// Congruential
		kiss64_z = 6906969069l * kiss64_z + 1234567l;
		long rand = kiss64_x + kiss64_y + kiss64_z;
		
		if (in == 0) {
			return rand;
		} else {
			return ((rand & 0xFFFFFFFFl) * in) >>> 32;  // unsigned right shift;		
		}
			
	}

	@Override
	public String name() {
		return "KISS64 random numbers";
	}

	@Override
	public String shortname() {
		return "KISS64";
	}

	
}
