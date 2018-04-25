package minesweeper.gamestate.msx;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

import window.controller.GetWindowRect;

public class ScreenScanner {
	
	public static final int EXPLODED_BOMB = -4;
	public static final int BOMB = -2;
	public static final int HIDDEN = 0;
	public static final int EMPTY = -1;
	public static final int FLAG = -3;

	private final static int[] digitCheckX = new int[] {6, 2, 10, 6, 2, 10, 6};
	private final static int[] digitCheckY = new int[] {3, 6, 6, 10, 16, 16, 20};
	
	private class DigitCheck {
		
		private boolean checks[]; 
		private int value;
		
		private DigitCheck(int value, boolean... checks) {  
			this.value = value;
			this.checks = checks;
		}
		
		private boolean matches(boolean[] test) {
			for (int i = 0; i < checks.length; i++ ) {
				if (checks[i] != test[i]) {
					return false;
				}
			}
			
			return true;
		}
		
	}
	
	private final DigitCheck DIGIT_ZERO = new DigitCheck(0, true, true, true, false, true, true, true);
	private final DigitCheck DIGIT_ONE = new DigitCheck(1, false, false, true, false, false, true, false);
	private final DigitCheck DIGIT_TWO = new DigitCheck(2, true, false, true, true, true, false, true);	
	private final DigitCheck DIGIT_THREE = new DigitCheck(3, true, false, true, true, false, true, true);
	private final DigitCheck DIGIT_FOUR = new DigitCheck(4, false, true, true, true, false, true, false);
	private final DigitCheck DIGIT_FIVE = new DigitCheck(5, true, true, false, true, false, true, true);
	private final DigitCheck DIGIT_SIX = new DigitCheck(6, true, true, false, true, true, true, true);
	private final DigitCheck DIGIT_SEVEN = new DigitCheck(7, true, false, true, false, false, true, false);
	private final DigitCheck DIGIT_EIGHT = new DigitCheck(8, true, true, true, true, true, true, true);
	private final DigitCheck DIGIT_NINE = new DigitCheck(9, true, true, true, true, false, true, true);
	
	private final DigitCheck[] DIGIT_LIST = new DigitCheck[] {DIGIT_ZERO, DIGIT_ONE, DIGIT_TWO, DIGIT_THREE, DIGIT_FOUR, DIGIT_FIVE, DIGIT_SIX, DIGIT_SEVEN, DIGIT_EIGHT, DIGIT_NINE};
	
	
	private String windowName;
	private ScreenLocation location;
	
	final int offsetX=15;
	final int offsetY=-101;
	
	final int botOffsetX=15;
	final int botOffsetY=15;
	
	private Robot clicker;
	
	
	private int[][] field;
	private int rows;
	private int columns;
	private int mines;
	
	private int numHidden;
	private int numEmpty;
	
	private boolean dead = false;
	
	public ScreenScanner(String windowName) {
		this.windowName = windowName;
		
		try {
			clicker = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		//System.out.println("Locate");
		locate();
		
		// if we can't find the minesweeperX game then return
		if (!location.found) {
			return;
		}
		
		//System.out.println("Start MSX " + columns + " by " + rows);
		startMinesweeperX();  // get focus
		startMinesweeperX();  // start
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//System.out.println("Get mines");
    	try {
			mines = getMineCount();
		} catch (AWTException e) {
			System.out.println("Failed to parse the mine counter");
	    	location.found = false;
	    	return;
		}

    	//System.out.println("update field");
		try {
			updateField();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	private void locate() {
		
		location = new ScreenLocation();
		
		//System.out.println("Locating minesweeper window... \nPlease do not move the window.");
		
	    int[] rect;

	    try {
	    	rect = GetWindowRect.getRect(windowName);
	    	//System.out.println("corner locations for minesweeper: \n"+ windowName+ Arrays.toString(rect));
	    	location.topX = rect[0];
	    	location.topY = rect[1];
	    	location.botX = rect[2];
	    	location.botY = rect[3];
	    	
			int height = (location.botY - botOffsetY) - (location.topY - offsetY);
			int width = (location.botX - botOffsetX) - (location.topX + offsetX);
			
			rows = height/16;
			columns = width/16;
			
			if (rows < 1 || columns < 1) {
				location.found = false;
				return;
			}
			

	    } catch (GetWindowRect.WindowNotFoundException e){
	    	System.out.println("Please open minesweeper and try again");
	    	location.found = false;
	    	return;
	    } catch (GetWindowRect.GetWindowRectException e){
	    	System.out.println(e.getMessage());
	    	location.found = false;
	    	return;
	    }
	    
	    location.found = true;
	    
	}
	
	private int getMineCount() throws AWTException
	{
		int height = 22;
		int width = 38;
		
		//System.out.println("window of size: " +width +"x" + height + "found");
		
		Rectangle window = new Rectangle(location.topX + 20, location.topY + 62, width, height);
		Robot robot = new Robot();
		
		BufferedImage screenShot = robot.createScreenCapture(window);
		
		int d1 = getDigit(screenShot, 0);
		int d2 = getDigit(screenShot, 12);		
		int d3 = getDigit(screenShot, 26);
		
		int mines = d1 * 100 + d2 * 10 + d3;
		
		System.out.println("mines = " + mines);
		
		return mines;
		
	}
	
	private int getDigit(BufferedImage screenShot, int position) {
		
		boolean[] results = new boolean[7];
		for (int i=0; i < digitCheckX.length; i++) {
			
			int red = getRed(screenShot, position + digitCheckX[i], digitCheckY[i]);
			if (red == 255) {
				results[i] = true;
			} else {
				results[i] = false;
			}
			//System.out.println(" at " + digitCheckX[i] + ", " + digitCheckY[i] + " red is " + getRed(screenShot, digitCheckX[i], digitCheckY[i]));			
		}
		
		int integer = -1;
		for (DigitCheck dc: DIGIT_LIST) {
			if (dc.matches(results)) {
				integer = dc.value;
			}
		}
		
		//System.out.println(integer);
		
		return integer;
		
	}
	
	
	private int getRed(BufferedImage screenShot, int x, int y) {
		
		int rgb = screenShot.getRGB(x, y);
		final int red = (rgb & 0x00ff0000) >> 16;
		//final int green = (rgb & 0x0000ff00) >> 8;
		//final int blue = (rgb & 0x000000ff);	
		
		return red;
		
	}
	
	private BufferedImage getFieldImage() throws AWTException
	{
		int height = (location.botY - botOffsetY) - (location.topY - offsetY);
		int width = (location.botX - botOffsetX) - (location.topX + offsetX);
		
		//System.out.println("window of size: " +width +"x" + height + "found");
		
		Rectangle window = new Rectangle(location.topX + offsetX, location.topY - offsetY, width, height);
		Robot robot = new Robot();
		
		BufferedImage screenShot = robot.createScreenCapture(window);
		
		//rows = screenShot.getHeight()/16;
		//columns = screenShot.getWidth()/16;
		
		field = new int[columns][rows];
	
		return screenShot;
		
	}
	
	public void updateField() throws Exception
	{
		numHidden=0;
		//System.out.println("-UPDATING FIELD-");
		
		BufferedImage screenShot = getFieldImage();
		
		for(int x=0; x < columns; x++)
			for(int y=0; y < rows; y++)
			{
				//System.out.println("row: "+y+" of " + rows + ", column: "+x + " of " +columns);
				if(field[x][y]==HIDDEN || field[x][y] == FLAG) {
					field[x][y]=readTile(screenShot, x , y);
					if(field[x][y]==HIDDEN) {
						numHidden++;
					}
				} else if(field[x][y]==EMPTY)
					numEmpty++;
			}

	}
	
	public void printField()
	{
		for(int y=rows+1;y>=0;y--)
		{
			System.out.print("[");
			for(int x=0;x<columns+2;x++)
			{
				//System.out.println("column: "+x+" row: "+y);
				System.out.print(field[x][y]+", ");
			}
			System.out.println("]");
		}
	}
	
	private int readTile(BufferedImage image, int x, int y)
	{
		int tileType=0;//0=unknown,-1=empty -2=bomb, 1-8 are numbers
		
		//System.out.println("tile to read: " + x + ", " + y);
		
		int pixelX=(x)*16;
		//int pixelY=(rows-(y+1))*16;
		int pixelY=(y)*16;
		
		//System.out.println("checking pixel color at: " + pixelX + ", "+pixelY);
		
		String color="";
		
		int n=1;
		do
		{
			color=getPixelColor(image, pixelX+8,pixelY+n);
			switch(color)
			{
			case("white"): {tileType=HIDDEN; n=16;} break;
			case("flag"): {tileType=FLAG; n=16;} break;
			case("blue"): {tileType=1; n=16;} break;
			case("green"): {tileType=2; n=16;} break;
			case("red"): {tileType=3; n=16;} break;
			case("purple"): {tileType=4; n=16;} break;
			case("brown"): {tileType=5; n=16;} break;
			case("teal"): {tileType=6; n=16;} break;
			case("black"): {tileType=7; n=16;} break;
			case("gray"): {tileType=8; n=16;} break;
			case("bomb"): {tileType=BOMB; n=16;} break;
			case("exp_bomb"): {tileType=EXPLODED_BOMB; n=16;} break;
			default: {tileType=EMPTY;} break;
			}
			n++;
			
		}while(n<15);
		
		//System.out.println("checking pixel color at: " + pixelX + ", "+pixelY + "  x = " + x + " y = " + y + " colour " + color + " tiletype = " + tileType);
		
		return tileType;
	}
	
	private String getPixelColor(BufferedImage image, int x, int y)//get pixel color
	{
		String color = "empty";
		
		final int clr = image.getRGB(x, y);
		final int red = (clr & 0x00ff0000) >> 16;
		final int green = (clr & 0x0000ff00) >> 8;
		final int blue = (clr & 0x000000ff);

		if(red==255 && green==255 && blue==255) {
			//int flagCheckColor = (image.getRGB(x-2, y+12) & 0x00ffffff);
			//System.out.println("flag check = " + getRed(image, x, y+8));
			if (getRed(image, x, y+8) == 0) { // flag
				color =  "flag";
			} else {
				color = "white";	
			}
		}

		else if(blue==255 && red==0 && green==0)
			color = "blue";
		else if(green==128 && red==0 && blue==0)
			color = "green";
		else if(red==255 && green==0 && blue==0)
		{
			color = "red";
			int deadCheckColor = (image.getRGB(x-4, y+8) & 0x00ffffff);
			if(deadCheckColor==0)
			{
				dead=true;
				color="exp_bomb";
			}
		}
		else if(blue==128 && red==0 && green==0)
			color = "purple";
		else if(red==128 && green==0 && blue==0)
			color = "brown";
		else if(red==0 && green==128 && blue==128)
			color = "teal";
		else if(red==0 && green==0 && blue==0)
		{
			color = "black";
			
			int deadCheckColor = (image.getRGB(x-4, y+8) & 0x00ffffff);
			
			//System.out.println(deadCheckColor);
			if(deadCheckColor==0)
			{
				dead=true;
				color="bomb";
			}
		}
		else if(red==128 && green==128 && blue==128)
			color = "gray";
		
		return color;
	}
	

	private void click(int x, int y, int mask)
	{
		//int mask;
		
		Point p = MouseInfo.getPointerInfo().getLocation();
		
		//System.out.println("CLICKING - "+ x +", "+y);
		
		/*
		if(flag)
			mask = InputEvent.BUTTON1_DOWN_MASK;
		else
			mask = InputEvent.BUTTON3_DOWN_MASK;
		*/
		
		// release the mouse (to remove the human click if there was one)
		clicker.mouseRelease(InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK);
		
		int pixelX=(x*16)+8;
		//int pixelY=Math.abs((rows-y - 1 )*16)+8;
		int pixelY=(y*16)+8;
		
		pixelX+= location.topX + offsetX;
		pixelY+= location.topY - offsetY;

		clicker.mouseMove(pixelX, pixelY);
		
		clicker.mousePress(mask);
		
		clicker.mouseRelease(mask);
		
	
		// move mouse back again
		clicker.mouseMove(p.x, p.y);
		
	}
	
	public boolean isValid() {
		return this.location.found;
	}
	
	public int getColumns() {
		return columns;
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getMines() {
		return mines;
	}
	
	public int getValue(int x, int y) {
		return field[x][y];
	}
	
	public void clear(int x, int y) {
		click(x, y, InputEvent.BUTTON1_DOWN_MASK);
	}
	
	public void flag(int x, int y) {
		click(x, y, InputEvent.BUTTON3_DOWN_MASK);
	}
	
	public void clearAll(int x, int y) {
		click(x, y, InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK);
	}
	
	public int getHiddenCount() {
		return numHidden;
	}
	
	public boolean isGameLost() {
		return dead;
	}
	
	public void startMinesweeperX() {
		
		int x = columns / 2;
		int y = - 2;
		
		click(x, y, InputEvent.BUTTON1_DOWN_MASK);
		
	}
}
