package minesweeper.solver.constructs;

public class TranspositionTableOld {
    // 3 pieces of data per entry: [0] Zobrist Key, [1] Mixed Data, [2] Generation/Age
    private static final int ENTRY_SIZE = 2;
    
    // Bounds flags for Alpha-Beta pruning
    //private static final long CHECK_VALUE = 123;

    // only lock if two updates share the same part of the index
    private static final Object[] lock = new Object[16];
    static {
    	for (int i=0; i < lock.length; i++) {
    		lock[i] = new Object();
    	}
    }
    
    private final long[] table;
    private final int sizeBuckets;
    //private int currentAge = 0;
    
    private int usedBuckets = 0;

    /**
     * Initializes the table. Size must be a power of two for fast bitwise modulo.
     * @param megabytes Memory limit for the table.
     */
    public TranspositionTableOld(int megabytes) {
        long bytes = (long) megabytes * 1024 * 1024;
        long totalEntries = bytes / (ENTRY_SIZE * 8); // 8 bytes per long
        
        // Find highest power of two less than or equal to totalEntries
        int powerOfTwo = 1;
        while (powerOfTwo <= totalEntries / 2) {
            powerOfTwo <<= 1;
        }
        
        this.sizeBuckets = powerOfTwo;
        
        System.out.println("Entries allowed in cache " + this.sizeBuckets);
        
        this.table = new long[sizeBuckets * ENTRY_SIZE];
    }


    /**
     * Stores a position in the table. 
     * Overwrites if the new entry has a deeper search depth or belongs to a newer epoch.
     */
    public void store(long hash, int depth, int score) {
        // Fast bitwise modulo using power-of-two mask
        int index = (int) (hash & (sizeBuckets - 1)) * ENTRY_SIZE;

        int lockIndex = index & 0xF;
        
        //if (score > 1000000) {
        //	System.err.println("Excessive winning lines being stroe in the transition table " + score);
        //}
        
        long existingHash;
        long existingData;
        synchronized(lock[lockIndex]) {
            existingHash = table[index];
            existingData = table[index + 1];
        }
 
        // Unpack existing metadata to evaluate replacement strategy
        int existingDepth = (int) (existingData & 0xFFFFFFFF);

        // Replacement Strategy: Keep deeper evaluations or newer game phases
        if (existingHash != 0 && existingDepth < depth) {
            return; // Keep the existing higher entry
        }
        
        if (existingHash == 0) {
        	usedBuckets++;
        }

        // Pack score (32-bit), depth (16-bit), flag (4-bit), and move (12-bit) into one long
        long rawData = ((long) score << 32) 
                     | (long) depth;
  

        // Lockless protection: XOR the data with the hash to validate complete writes
        //long XORedData = rawData ^ hash;
        
        // Order of writes matters to minimize data corruption windows
        synchronized(lock[lockIndex]) {
            table[index] = hash;
            table[index + 1] = rawData;
        }
 
    }

    /**
     * Retrieves a stored position. Returns null if missing or corrupted.
     */
    public int probe(long hash) {
        int index = (int) (hash & (sizeBuckets - 1)) * ENTRY_SIZE;

        int lockIndex = index & 0xF;
        
        long storedHash;
        long rawData;
        synchronized(lock[lockIndex]) {
            storedHash = table[index];
            rawData = table[index + 1];
        }
        
        // 1. Verify the hash matches
        if (storedHash != hash) {
            return -1;
        }
       
        // 2. Unpack and verify the data payload using the XOR key
        //long rawData = XORedData;

        //long checkValue = rawData & 0xFFFF;
        //if (checkValue != CHECK_VALUE) {
        //	System.err.println("CHECK VALUE mismatch - transposition table collision prevented");
        //	return null;
        //}
        
        // 3. Re-verify the hash. If a thread overrode the index mid-read, 
        // the hash or the data calculation will fail consistency.
        //if (table[index] != hash) {
        //   return null;
        //}

        //TTEntry entry = new TTEntry();
        //entry.score = (int) (rawData >> 32);
        //entry.depth = (int) (rawData & 0xFFFFFFFF);

        
        return (int) (rawData >> 32);
    }

    public float size() {
    	return (float) usedBuckets / (float) sizeBuckets;
    }
    // Lightweight carrier class for probe results
    public static class TTEntry {
        public int score;
        public int depth;
    }
}

