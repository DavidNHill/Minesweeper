package minesweeper.random;


public class RNGJSF implements RNG {
	
	
	private int[] s;
	
	public RNGJSF() {
	}
	
	public RNGJSF(long seed) {
		seed(seed);
	}
	
	public void seed(long seed) {
	
	    int seed1 = (int) Math.abs(seed);
	    int seed2 = (int) ((Math.abs(seed) >>> 32) & 0x001FFFFFl);
	    
	    //System.out.println(seed1 + " " + seed2);
	    
	    s = new int[] {0xf1ea5eed, seed1, seed2, seed1};
	    for (int i = 0; i < 20; i++) random(1);
	
	}
	
	@Override
	public long random(int in) {
		
        int e = s[0] - (s[1] << 27 | s[1] >>> 5);
        
        s[0] = s[1] ^ (s[2] << 17 | s[2] >>> 15);
        s[1] = s[2] + s[3];
        s[2] = s[3] + e;
        s[3] = s[0] + e;
        
        //System.out.println(e + " " + s[0] + " " + s[1] + " " + s[2] + " " + s[3]);
        
        return ((s[3] & 0xFFFFFFFFl) * in) >>> 32;
			
	}

	private long bit32(long x) {
		return x & 0xFFFFFFFFl;
	}
	
	@Override
	public String name() {
		return "JSF random numbers";
	}

	@Override
	public String shortname() {
		return "JSF";
	}

	
}
