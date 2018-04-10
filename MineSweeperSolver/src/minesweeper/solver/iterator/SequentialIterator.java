/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.iterator;

/**
 *
 * @author David
 */
public class SequentialIterator extends Iterator {

    final private int[] sample;

    private boolean more = true;
    
    private int index;

    
    // a sequential iterator that puts n-balls in m-holes once in each possible way
    public SequentialIterator(int n, int m) {
        super(n,m);

        sample = new int[n];
        
        index = n - 1;
        
        for (int i=0; i < sample.length; i++) {
            sample[i] = i;
        }
        
        // reduce the iterator by 1, since the first getSample() will increase it
        // by 1 again
        sample[index]--;
        
    }
    
    
    
    @Override
    public int[] getSample(int start) {
        
        if (!more) {
            System.err.println("trying to iterate after the end");
            return null;
        }
        
        index = start;
        
        // add on one to the iterator
        sample[index]++;
        
        // if we have rolled off the end then move backwards until we can fit
        // the next iteration
        while (sample[index] >= numberHoles - numberBalls + 1 + index) {
            if (index == 0) {
                more = false;
                return null;
            } else {
                index--;
                sample[index]++;
            }
        }
        
        // roll forward 
        while (index != numberBalls - 1) {
            index++;
            sample[index] = sample[index-1] + 1;
        }
        
        return sample;
        
    }
     
}
