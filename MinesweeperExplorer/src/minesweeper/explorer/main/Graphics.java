/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.explorer.main;

import java.net.URL;

import javafx.scene.image.Image;

/**
 *
 * @author David
 */
public class Graphics {
    
	public static final double[] SUPPORTED_SIZES = {12, 16, 24, 32, 48};
	
    public class GraphicsSet {
    	
    	private double size;
    	
        private Image hidden;
        private Image mineBang;
        private Image flag;
        private Image[] number = new Image[9]; 
        private Image mine;
        
        public double getSize() {
        	return this.size;
        }
        
        public Image getLed(int c) {
            return led[c];
        }
        
        public Image getSmallLed(int c) {
            return smallLED[c];
        }
        
        public Image getNumber(int c) {
             return number[c];
         }
        
        public Image getExploded() {
             return mineBang;
         }

        public Image getMine() {
            return mine;
        }
        
        public Image getFlag() {
            return flag;
        }
        
        public Image getHidden() {
             return hidden;
         }    
    }
    
    private static Image[] led = new Image[10];
    private static Image[] smallLED = new Image[10];

    public static final Image ICON;
    static {
    	ICON = clean("resources/images/flagged.png", 24, 24);
    }
    
    private GraphicsSet[] graphicsSets;
    
    public Graphics() {

    	graphicsSets = new GraphicsSet[SUPPORTED_SIZES.length];
    	
    	int index=0;
    	for (double size: SUPPORTED_SIZES) {
    		GraphicsSet gs = new GraphicsSet();
    		
    		gs.size = size;
            gs.hidden = clean("resources/images/hidden.png", size);
            gs.mineBang = clean("resources/images/exploded.png", size);
            gs.flag = clean("resources/images/flagged.png", size);
            gs.mine = clean("resources/images/mine.png", size);
            
            gs.number[0] = clean("resources/images/0.png", size);
            gs.number[1] = clean("resources/images/1.png", size);
            gs.number[2] = clean("resources/images/2.png", size);
            gs.number[3] = clean("resources/images/3.png", size);
            gs.number[4] = clean("resources/images/4.png", size);
            gs.number[5] = clean("resources/images/5.png", size);
            gs.number[6] = clean("resources/images/6.png", size);
            gs.number[7] = clean("resources/images/7.png", size);
            gs.number[8] = clean("resources/images/8.png", size);
    		graphicsSets[index] = gs;
    		index++;
    	}

        led[0] = clean("resources/images/led0.png", 24, 40);
        led[1] = clean("resources/images/led1.png", 24, 40);
        led[2] = clean("resources/images/led2.png", 24, 40);
        led[3] = clean("resources/images/led3.png", 24, 40);
        led[4] = clean("resources/images/led4.png", 24, 40);
        led[5] = clean("resources/images/led5.png", 24, 40);
        led[6] = clean("resources/images/led6.png", 24, 40);
        led[7] = clean("resources/images/led7.png", 24, 40);
        led[8] = clean("resources/images/led8.png", 24, 40);
        led[9] = clean("resources/images/led9.png", 24, 40);

        smallLED[0] = clean("resources/images/led0.png", 12, 20);
        smallLED[1] = clean("resources/images/led1.png", 12, 20);
        smallLED[2] = clean("resources/images/led2.png", 12, 20);
        smallLED[3] = clean("resources/images/led3.png", 12, 20);
        smallLED[4] = clean("resources/images/led4.png", 12, 20);
        smallLED[5] = clean("resources/images/led5.png", 12, 20);
        smallLED[6] = clean("resources/images/led6.png", 12, 20);
        smallLED[7] = clean("resources/images/led7.png", 12, 20);
        smallLED[8] = clean("resources/images/led8.png", 12, 20);
        smallLED[9] = clean("resources/images/led9.png", 12, 20);
          
    }    
    
    public static Image getLed(int c) {
        return led[c];
    }
    
    public static Image getSmallLed(int c) {
        return smallLED[c];
    }
    
    public GraphicsSet getGraphicsSet(double size) {
    	
    	for (GraphicsSet gs: graphicsSets) {
    		if (gs.getSize() == size) {
    			return gs;
    		}
    	}
    	
    	System.out.println("No graphics set with size " + size + " - defaulting to 24");
    	
    	for (GraphicsSet gs: graphicsSets) {
    		if (gs.getSize() == 24) {
    			return gs;
    		}
    	}
    	
    	System.err.println("No graphics set with size 24");
    	return null;
    }
    
    // in case we want to do some image manipulation
    static private Image clean(String resourceName, double size) {

    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
    	
    	URL url = cl.getResource(resourceName);
    	
    	if (url == null) {
    		System.out.println(resourceName + " not found");
    	}
    	
    	return new Image(url.toExternalForm(), size, size, true, true);

    }
    
    // in case we want to do some image manipulation
    static private Image clean(String resourceName, int width, int height) {

    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
    	
    	URL url = cl.getResource(resourceName);
    	
    	if (url == null) {
    		System.out.println(resourceName + " not found");
    	}
    	
    	return new Image(url.toExternalForm(), width, height, true, true);

    }
        
    
}
