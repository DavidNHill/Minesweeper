/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver.iterator;

/**
 *
 * @author David
 */
public class RandomIterator extends Iterator {

    int max;
    int count=0;
    
    int[] shuffle;
    
    int[] sample;
    
    public RandomIterator(int n, int m, int max) {
        super(n,m);
        
        this.max = max;
        
        shuffle = new int[m];
        
        for (int i=0; i < shuffle.length; i++) {
            shuffle[i] = i;
        }
        
        sample = new int[n];
        
    }

    @Override
    public int[] getSample() {
 
        if (count >= max) {
            return null;
        }
        count++;
                
        int top = shuffle.length -1;
        
        // create a random sample
        for (int i=0; i < sample.length; i++) {
            
            int e = (int) Math.floor(Math.random()*top);
            
            sample[i] = shuffle[e];
            
            // swap shuffle[e] to the top off the array
            shuffle[e] = shuffle[top];
            shuffle[top] = sample[i];
            
            // reduce top by 1 so, shuffle[e] can't be picked again
            top--;
            
        }

       return sample; 
        
    }
    
}
