package minesweeper.solver.constructs;

import java.util.concurrent.atomic.AtomicLongArray;

public class TranspositionTable {
    // 3 pieces of data per entry: [0] Zobrist Key, [1] Mixed Data
    private static final int ENTRY_SIZE = 2;
    
    private static final long CHECK_VALUE = 34123 & 0xFFFFl;

    private final AtomicLongArray table;
    private final int sizeBuckets;
    //private int currentAge = 0;
    
    private int usedBuckets = 0;

    /**
     * Initializes the table. Size must be a power of two for fast bitwise modulo.
     * @param megabytes Memory limit for the table.
     */
    public TranspositionTable(int megabytes) {
        long bytes = (long) megabytes * 1024 * 1024;
        long totalEntries = bytes / (ENTRY_SIZE * 8); // 8 bytes per long
        
        // Find highest power of two less than or equal to totalEntries
        int powerOfTwo = 1;
        while (powerOfTwo <= totalEntries / 2) {
            powerOfTwo <<= 1;
        }
        
        this.sizeBuckets = powerOfTwo;
        
        System.out.println("Entries allowed in cache " + this.sizeBuckets + ", check value is " + CHECK_VALUE);
        
        this.table = new AtomicLongArray(sizeBuckets * ENTRY_SIZE);
    }


    /**
     * Stores a position in the table. 
     * Overwrites if the new entry has a deeper search depth or belongs to a newer epoch.
     */
    public void store(long hash, int depth, int score) {
        // Fast bitwise modulo using power-of-two mask
        int index = (int) (hash & (sizeBuckets - 1)) * ENTRY_SIZE;
        
        //if (score > 1000000) {
        //	System.err.println("Excessive winning lines being store in the transition table " + score);
        //}
        
        long existingHash = table.get(index);
        long existingData = table.get(index + 1);
 
        // Unpack existing metadata to evaluate replacement strategy
        int existingDepth = (int) (((existingData ^ hash) >> 20) & 0xFFFl);

        // Replacement Strategy: Keep earlier evaluations
        if (existingHash != 0 && existingDepth < depth) {
            return; // Keep the existing higher entry
        }
        
        if (existingHash == 0) {
        	usedBuckets++;
        }

        // Pack score (32-bit), depth (16-bit), flag (4-bit), and move (12-bit) into one long
        long rawData = ((long) score << 32) 
                     | ((long) depth << 20)
                     | CHECK_VALUE;
  

        // Lockless protection: XOR the data with the hash to validate complete writes
        long XORedData = rawData ^ hash;
        
        //if ((XORedData ^ hash) != rawData) {
        //	System.err.println("XORed data is not reversable !! " + rawData + " " + XORedData + " " + hash);
        //}
        
        table.set(index, hash);
        table.set(index + 1, XORedData);
        
    }

    /**
     * Retrieves a stored position. Returns null if missing or corrupted.
     */
    public int probe(long hash) {
        int index = (int) (hash & (sizeBuckets - 1)) * ENTRY_SIZE;
        
        long storedHash = table.get(index);
        long XORedData = table.get(index + 1);
        
        // 1. Verify the hash matches
        if (storedHash != hash) {
            return -1;
        }
       
        // 2. Unpack and verify the data payload using the XOR key
        long rawData = XORedData ^ hash;

        long checkValue = rawData & 0xFFFFF;
        if (checkValue != CHECK_VALUE) {
        	//System.err.println("CHECK VALUE mismatch - transposition table collision prevented");
        	return -1;
        }
        
        // 3. Re-verify the hash. If a thread overrode the index mid-read, 
        // the hash or the data calculation will fail consistency.
        if (table.get(index) != hash) {
        	System.err.println("Transposition table concurrent updated prevented");
            return -1;
        }

        //TTEntry entry = new TTEntry();
        //entry.score = (int) (rawData >> 32);
        //entry.depth = (int) (rawData & 0xFFFFFFFF);
        
        return (int) (rawData >> 32);
    }

    public float usedSizeRatio() {
    	return (float) usedBuckets / (float) sizeBuckets;
    }
    
    public long usedBuckets() {
    	return usedBuckets;
    }
    
    // Lightweight carrier class for probe results
    public static class TTEntry {
        public int score;
        public int depth;
    }
}

