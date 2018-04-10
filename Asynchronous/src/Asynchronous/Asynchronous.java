/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Asynchronous;

/**
 *
 * This interface defines an object which can be run asynchronously
 */
public interface Asynchronous<V> {
    
    public void start();
            
    public void requestStop();
    
    public V getResult();
    
}
