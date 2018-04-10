/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Monitor;

import Asynchronous.Asynchronous;

/**
 *
 * @author David
 */
public class AsynchMonitor {
    
    private class Process extends Thread {
        
        private final int index;
        private final Asynchronous item;
        
        private volatile boolean started = false;
        private volatile boolean completed = false;
 
        public Process(int index, Asynchronous item) {
            super("Process " + index);
            
            this.index = index;
            this.item = item;
            
        }
        
        @Override
        public void run() {
            
            if (started) {
                System.out.println("trying to start an already running task");
                return;
            }
            
            started = true;
            
            item.start();
            
            completed = true;
            
            taskCompleted(index);
            
        }
        
        public boolean isStarted() {
            return started;
        }
        public boolean isCompleted() {
            return completed;
        }
        
    }    
    
    private Asynchronous[] items;
    private Process[] process;
    private boolean finished = false;
    private boolean started = false;
    private Thread initThread;
    private int maxThreads = 100;
    private volatile int startedCount;  // volatile ensures it is updated fro other threads
    
    // create a monitor for the provided tasks
    public AsynchMonitor(Asynchronous... items) {
        
        this.items = items;
        this.process = new Process[items.length];
        //this.complete = new boolean[items.length];

        for (int i=0; i < items.length; i++) {
            
            process[i] = new Process(i, items[i]);
            
        }
        
    }
    
    // kick off each of the sub tasks
    public void start() throws Exception {
        
        if (started) {
            throw new Exception("Processes already started exception");
        }
        
        started = true;
        finished = false;
        
        startedCount = Math.min(maxThreads, items.length);
 
        // can't use started count because it might get updated by a quick finisher
        int stop = startedCount;
        
        // start the initial processes checking that they haven't already been started
        // by a quick finisher
        for (int i=0; i < stop; i++) {
            if (!process[i].isStarted()) {
                process[i].start();
            }
        }

    }
    
    public void setMaxThreads(int max) {
        this.maxThreads = max;
    }
    
    
    public void startAndWait() throws Exception {
        
        try {
            start();
        } catch (Exception ex) {
            throw ex;
        }
        
        suspend();
        
    }
    
    public void suspend() {
        
        initThread = Thread.currentThread();
        
        while (!finished) {
           try {
                 Thread.sleep(1000);
            } catch (InterruptedException e) {
                 //System.out.println("interrupted");
            }            
        }
        
        
    }
    
    
    private synchronized void  taskCompleted(int index) {
        
        //System.out.println("Sub task " + index + " finished");
        
        //complete[index] = true;
        
        
        // look for more to start up but domn't include the initial start ups
        for (int i=startedCount; i < process.length; i++) {
            if (!process[i].isStarted()) {
                //System.out.println("Starting process " + i + " out of " + process.length);
                startedCount = i + 1;
                process[i].start();
                break;
            }
        }
        
        /*
        for (Process process: process) {
            if (!process.isStarted()) {
                process.start();
                break;
            }
        }        
        */
        boolean allDone = true;
        for (Process process: process) {
            if (!process.isCompleted()) {
                allDone = false;
                break;
            }
        }
        
        // if we are all done then wake the requested thread
        if (allDone && !finished) {
            //System.out.println("All the sub tasks have completed");
            finished = true;
            if (initThread != null) {
                initThread.interrupt();
            }
        }
        
    }
    
    public boolean isFinished() {
        return finished;
    }
    

}
