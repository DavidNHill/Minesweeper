/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import javafx.scene.image.Image;

/**
 *
 * @author David
 */
public class Graphics {
    
    public static final double SIZE = 50;
    
    
    private static final Image button;
    private static final Image mineBang;
    private static final Image flag;
    private static final Image[] number = new Image[9]; 
    private static final Image mine;
    
    
    static {
    
        button = clean(new Image(Graphics.class.getResource("resources/ms_button.png").toExternalForm(), SIZE, SIZE, true, true));
        mineBang = clean(new Image(Graphics.class.getResource("resources/ms_mine_bang.png").toExternalForm(), SIZE, SIZE, true, true));
        flag = clean(new Image(Graphics.class.getResource("resources/ms_flag.png").toExternalForm(), SIZE, SIZE, true, true));
        mine = clean(new Image(Graphics.class.getResource("resources/ms_mine.png").toExternalForm(), SIZE, SIZE, true, true));
        
        number[0] = clean(new Image(Graphics.class.getResource("resources/ms_zero.png").toExternalForm(), SIZE, SIZE, true, true));
        number[1] = clean(new Image(Graphics.class.getResource("resources/ms_one.png").toExternalForm(), SIZE, SIZE, true, true));
        number[2] = clean(new Image(Graphics.class.getResource("resources/ms_two.png").toExternalForm(), SIZE, SIZE, true, true));
        number[3] = clean(new Image(Graphics.class.getResource("resources/ms_three.png").toExternalForm(), SIZE, SIZE, true, true));
        number[4] = clean(new Image(Graphics.class.getResource("resources/ms_four.png").toExternalForm(), SIZE, SIZE, true, true));
        number[5] = clean(new Image(Graphics.class.getResource("resources/ms_five.png").toExternalForm(), SIZE, SIZE, true, true));
        number[6] = clean(new Image(Graphics.class.getResource("resources/ms_six.png").toExternalForm(), SIZE, SIZE, true, true));
        number[7] = clean(new Image(Graphics.class.getResource("resources/ms_seven.png").toExternalForm(), SIZE, SIZE, true, true));
        number[8] = clean(new Image(Graphics.class.getResource("resources/ms_eight.png").toExternalForm(), SIZE, SIZE, true, true));

          
    }    
    
    static public Image getNumber(int c) {
        
        return number[c];
        
    }
    
    static public Image getMineBang() {
        
        return mineBang;
        
    }

    static public Image getMine() {
        
        return mine;
        
    }
    
    static public Image getFlag() {
        
        return flag;
        
    }
    
    static public Image getButton() {
        
        return button;
        
    }    
    
    // in case we want to do some image manipulation
    static private Image clean(Image image) {

        return image;
        
    }
        
    
}
