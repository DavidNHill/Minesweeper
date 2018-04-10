/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import javafx.scene.Node;

/**
 *
 * @author David
 */
public class Rotator implements Runnable {

    private Thread rotator;
    
    Node object;
    
    
    public Rotator(Node object) {
        
        this.object = object;
        
        rotator = new Thread(this, "Rotator");
        
    }

     public void start() {
        
        rotator.start();
        
    }    
    
    @Override
    public void run() {
        
        for (int i=0; i < 360; i=i+20) {
            
            this.object.setRotate(i);
            
            try {
                 Thread.sleep(20);
            } catch (InterruptedException e) {
                 System.out.println("interrupted");
            }
            
            
        }
        
        this.object.setRotate(0);
        
     }
    
    
    
}
