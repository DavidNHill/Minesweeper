/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.gamestate;

/**
 * An abstract representation of a game of Minesweeper.
 * <p>Allows a player to query squares to discover if they are HIDDEN, FLAGGED or how many mines surround them.</p>
 * <p>Supports placing flags, removing flags and clearing squares</p>
 * 
 * @author David
 */
abstract public class GameStateModel {
    
    public final static int HIDDEN = -11;
    public final static int MINE = -10;
    public final static int FLAG = -12;
    public final static int EXPLODED_MINE = - 13;
    //public final static int CLEAR = - 14;
    
    public final static int NOT_STARTED = 0;
    public final static int STARTED = 1;
    public final static int LOST = 2;
    public final static int WON = 3;
    
    protected final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    protected final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    
    private long startTS = 0;
    private long finishTS = 0;
    
    protected final int x;
    protected final int y;
    protected final int mines;
    
    
    protected boolean allowEarlyFinish = true;
    
    /**
     * If this is true then the solver will place the remaining flags when the number of free squares = number of mines left
     */
    protected boolean doAutoComplete = true;    
    
    private int gameState = NOT_STARTED;
    
    private int squaresRevealed = 0;
    private int flagsPlaced = 0;
    

    private final boolean[][] flag;
    private final boolean[][] revealed;
    
    
    public GameStateModel(int x, int y, int mines) {
        
        this.x = x;
        this.y = y;
        this.mines = mines;
        
        //board = new int[x][y];
        
        flag = new boolean[x][y];
        
        revealed = new boolean[x][y];
        
        this.gameState = NOT_STARTED;
        
    }
    
    public boolean  doAction(Action a) {
        
        boolean result;
        
        if (a.getAction() == Action.CLEAR) {
            result = clearSquare(a);
        } else if (a.getAction() == Action.FLAG) {
            result = placeFlag(a);
        } else if (a.getAction() == Action.CLEARALL) {
            result = clearSurround(a);
        } else {
            result = false;
        }
        
        return result;
    }

    // returns false if the move is not allowed
    private boolean placeFlag(Location m) {

        // if the game is finished then nothing to do
        if (gameState > STARTED) {
            return false;
        }        
        
        // if the location is already revealed then nothing more to do
        if (query(m) != GameStateModel.HIDDEN && query(m) != GameStateModel.FLAG) {
            return false;
        }
        
        // otherwise toggle the flag
        flag[m.x][m.y] = !flag[m.x][m.y];
        
        if (flag[m.x][m.y]) {
            
            //if (board[m.x][m.y] != GameState.MINE) {
            //    System.out.println("DEBUG (" + m.x + "," + m.y + ") is not a mine!");
            //}
            flagsPlaced++;
        } else {
            flagsPlaced--;
        }

        // call this handle to allow extra logic to be added by the extending class
        placeFlagHandle(m);
        
        return true;
        
    }
    
    /**
     * to be overridden
     * @param m
     */
    abstract protected void placeFlagHandle(Location m);
    
    
    // returns false if the move is not allowed
    protected boolean clearSquare(Location m) {
        
       // if the game is finished then nothing to do
        if (gameState > GameStateModel.STARTED) {
            return false;
        }
        
        // can't reveal a location with a flag on it
        if (flag[m.x][m.y]) {
            return false;
        }
        
        // if the game isn't started yet, then start it
        if (gameState == GameStateModel.NOT_STARTED) {
            start(m);
        }
        
        // if the location is already revealed then nothing more to do
        if (query(m) != GameStateModel.HIDDEN) {
            return false;
        }
        
        /*
        // if the location is already revealed then nothing more to do
        if (revealed[m.x][m.y]) {
            return false;
        }
        */
        revealed[m.x][m.y] = true;
        
        squaresRevealed++;
 
        boolean mine = clearSquareHitMine(m);
        
        // if we have revealed a mine we have lost
        if (mine) {
            finish(GameStateModel.LOST);
            return true;
        }

        // if we have revealed enough locations without hitting a mine
        // we have won
        if (squaresRevealed == this.x * this.y - this.mines) {
            finishFlags();
            finish(GameStateModel.WON);
            return true;
        }

        return true;
        
    }
    
    /**
     * Checks whether we have won and performs any necessary actions
     * @return
     */
    protected boolean checkForWin() {
    	
        if (squaresRevealed == this.x * this.y - this.mines) {
            finishFlags();
            finish(GameStateModel.WON);
            return true;
        }    	
    	
        return false;
    	
    }
    
    /**
     *  to be overridden - returns true if the clear hits a mine
     * @param m
     * @return
     */
    abstract protected boolean clearSquareHitMine(Location m);
    
        
    // return false if the move is not allowed
    protected boolean clearSurround(Location m) {
        
        // if the game is finished then nothing to do
        if (gameState > GameStateModel.STARTED) {
            return false;
        }
        
        // can't clear around a flag
        if (flag[m.x][m.y]) {
            return false;
        }

        // if the square isn't revealed then we can't clear around it
        if (query(m) == GameStateModel.HIDDEN) {
            return false;
        }
               
        // if the number of flags is not complete then we can't clear
        if (countFlags(m.x, m.y) != query(m)) {
            return false;
        }
        
        clearSurroundHandle(m); 
        
        /*
        // otherwise, clear around this revealed square
        for (int j=0; j < DX.length; j++) {
            if (m.x + DX[j] >= 0 && m.x + DX[j] < this.x && m.y + DY[j] >= 0 && m.y + DY[j] < this.y) {
                clearSquare(new Location(m.x+DX[j], m.y+DY[j]));
            }
        }        
        */
        
        return true;
        
    }
    
    /**
     *  to be overridden - determines how a clear surround should be handled
     * @param m
     * @return
     */
    abstract protected boolean clearSurroundHandle(Location m);
    
    
    // count how many flags are placed around this square
    private int countFlags(int x, int y) {
        
         int result = 0;
        
         for (int j=0; j < DX.length; j++) {
            if (x + DX[j] >= 0 && x + DX[j] < this.x && y + DY[j] >= 0 && y + DY[j] < this.y) {
                if (flag[x+DX[j]][y+DY[j]]) {
                    result++;
                }
            }
         }                       
        
         return result;
    }
    
    
    // put flags in the remaining squares if required
    protected void finishFlags() {

    	if (!doAutoComplete) {
    		return;
    	}
    	
        for (int i=0; i < x; i++) {
            for (int j=0; j < y; j++) {
                Location l = new Location(i,j);
                if (query(l) == GameStateModel.HIDDEN) {
                    doAction(new Action(l, Action.FLAG));
                }
            }
        }        
 
    }
    
    
    
    // set up the board with the mines
    private void start(Location m) {
        
        gameState = GameStateModel.STARTED;
        
        startHandle(m);
    
        startTS = System.currentTimeMillis();
        
    }

    /**
     * to be overridden - allows the board to be set-up on first clear click
     */
    abstract protected void startHandle(Location m);
    
    
    private void finish(int outcome) {
        
        finishTS = System.currentTimeMillis();
        
        gameState = outcome;

    }
    
    // returns the board value at this location
    final public int query(Location m) {
        
        if (!revealed[m.x][m.y]) {
            if (flag[m.x][m.y]) {
                return GameStateModel.FLAG;
            } else {
                return GameStateModel.HIDDEN;
            }
        }

        return queryHandle(m);
        
    }
    
    /**
     * to be overridden - returns the number of mines around the square at this location.
     * or GameStateModel.HIDDENED, .MINE, .FLAG .EXPLODED_MINE
     */
    abstract protected int queryHandle(Location m);
    

    // allows a sub class to check what has been opened
    protected boolean isHidden(int x, int y) {
    	return !revealed[x][y] && !flag[x][y];
    }
    
    
    // allows the sub class to confirm what has been revealed to it
    protected void setRevealed(int x, int y) {
     	
        if (!revealed[x][y]) {
        	//System.out.println("Auto Reveal at (" + x + "," + y + ")");
            revealed[x][y] = true;
            squaresRevealed++;
        }

    }
    
    // allows the sub class to set a square as hidden again (this is useful when moves are being echoed to an external 
    // board like minesweeper X and it is noticed that for some reason the clear hasn't been honoured by the external board)
    protected void setHidden(int x, int y) {
     	
        if (revealed[x][y]) {
        	//System.out.println("Auto Reveal at (" + x + "," + y + ")");
            revealed[x][y] = false;
            squaresRevealed--;
        }

    }
    
    
    // allows a sub class to check what has been marked with a flag
    protected boolean isFlag(int x, int y) {
    	return flag[x][y];
    }
    
    
    // allows the sub class to confirm that a flag has been revealed to it
    protected void setFlag(int x, int y) {
        
        if (!flag[x][y]) {
        	//System.out.println("Auto flag set at (" + x + "," + y + ")");
        	flag[x][y] = true;
            flagsPlaced++;
        }

    }
    
    // allows the sub class to remove a flag stealthily (this is useful when moves are being echoed to an external 
    // board like minesweeper X and it is noticed that a flag has been removed external to the solver)
    protected void removeFlag(int x, int y) {
        
        if (flag[x][y]) {
        	//System.out.println("Auto flag set at (" + x + "," + y + ")");
        	flag[x][y] = false;
            flagsPlaced--;
        }

    }
    
    /**
     * Returns the recommended initial start location for this game type.
     * Defaults to the top left corner.
     * @return
     */
    public Location getStartLocation() {
    	return new Location(0,0);
    }
    
    
    /**
     * This can be overriden to show the key used to generate the game if one is available
     * @return
     */
    public String showGameKey() {
    	
    	return "no key defined";
    	
    }
    
    /**
     * @return the width of the game board
     */
    final public int getx() {
        return x;
    }
    
    /**
     * @return the height of the game board
     */
    final public int gety() {
        return y;
    }    
    
    /**
     * @return the original number of mines in the game 
     */
    final public int getMines() {
        return mines;
     }    
    
    /**
     * @return the game state: NOT_STARTED, STARTED, LOST, WON
     */
    final public int getGameState() {
        return gameState;
    }
    
    /**
     * @return the number of non-flagged mines remaining in the game
     */
    
    final public int getMinesLeft() {
         return this.mines - this.flagsPlaced;
    }
    
    /**
     * Returns the number of squares hidden
     */
    final public int getHidden() {
    	return this.x * this.y - this.squaresRevealed;
    }
    
    /**
     * @return the length of time since the game started in seconds
     */
    final public long getGameTime() {
        
        if (gameState == GameStateModel.NOT_STARTED) {
            return 0;
        }
        
        if (gameState == GameStateModel.STARTED) {
            
            return (System.currentTimeMillis() - startTS) / 1000;
        }
        
        return (finishTS - startTS) / 1000;
        
    }
    
    
}
