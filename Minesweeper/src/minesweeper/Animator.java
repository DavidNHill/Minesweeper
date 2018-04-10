/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import javafx.application.Platform;

/**
 *
 * @author David
 */
public class Animator implements Runnable{

    private Thread animator;
    
    private ScreenController scon;
    
    private boolean stopped = false;
    private long gameTime = 0;
    
    public Animator(ScreenController scon) {
        
        this.scon = scon;
        
        // start animating the display pane - see the run method
        animator = new Thread(this, "Animator");

    }
    
    public void start() {
        
        animator.start();
        
    }
    
    public void stop() {
        
        stopped = true;
        
    }
    
    
    @Override
    public void run() {
        
        long timeDiff, sleep, timeDelay;
        
        timeDelay = 50;
        
        gameTime = System.currentTimeMillis();

        while (!stopped) {
            
           Platform.runLater(new Runnable() {
                  @Override public void run() {
                  scon.updateTime();
                  scon.moveCheck();
                  scon.highlightMove();
              }
            });            
            

            // calculate how long the work took
            timeDiff = System.currentTimeMillis() - gameTime;
            
            // pause for the ramainder of timeDelay
            sleep = timeDelay - timeDiff;

            if (sleep < 0) {
                sleep = 2;
            }

            try {
                 Thread.sleep(sleep);
            } catch (InterruptedException e) {
                 System.out.println("interrupted");
            }
            
            gameTime += timeDelay;
            
        } 
  
         System.out.println("Animator thread stopped");
        
    }
    
}
