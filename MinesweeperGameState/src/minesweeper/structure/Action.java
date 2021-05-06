/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.structure;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;

import minesweeper.gamestate.MoveMethod;


/**
 *
 * @author David
 */
public class Action extends Location {
  
    public static final int CLEAR = 1;
    public static final int CLEARALL = 2;
    public static final int FLAG = 3;
    
    public static final DecimalFormat FORMAT_2DP = new DecimalFormat("#0.00");
    
    private final static String[] ACTION = {"", "Clear", "Clear around", "Place flag"};
    private final static BigDecimal MINUS_ONE = new BigDecimal("-1");
    private final static BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    
    static long globalUID = 0;
    
    private final long myUID;
    private final int action;
    private final BigDecimal bigProb;
    
    private final boolean certainty;
    
    //private int type = 0;
    private final MoveMethod moveMethod;
    
    String comment;
    
    // used by the human player
    public Action(int x, int y, int a) {
        this(new Location(x, y), a);
    }
    
    // used by the human player
    public Action(Location l, int a) {
        this(l, a, MoveMethod.HUMAN, "", MINUS_ONE);
    }
    
    // used by the computer coach
    public Action(Location l, int a, MoveMethod moveMethod, String comment, BigDecimal bigProb) {
    	this(l, a, moveMethod, comment, bigProb, globalUID++);
        
    }    
    
    // used by the computer coach to force a move to be earlier in the list - e.g. when we need to remove a flag placed by the human player before we can clear the square
    public Action(Location l, int a, MoveMethod moveMethod, String comment, BigDecimal bigProb, long uid) {
        super(l.x, l.y);
        
        this.action = a;
        this.comment = comment;
        this.bigProb = bigProb;
        this.moveMethod = moveMethod;
        
        this.myUID = uid;
        if (bigProb.compareTo(BigDecimal.ONE) == 0) {
        	this.certainty = true;
        } else {
        	this.certainty = false;
        }
         
    }    
    
    public int getAction() {
        return this.action;
    }
    
    /**
     * Returns true when this action is 100% certain
     */
    public boolean isCertainty() {
    	return this.certainty;
    }
    
    public BigDecimal getBigProb() {
        return bigProb;
    }
    
    public MoveMethod getMoveMethod() {
    	return this.moveMethod;
    }
    
    @Override
    public String toString() {
        
        String result = Action.ACTION[this.action] + " at " + super.toString() + " by " + moveMethod.description + " " + comment; 
        
        if (bigProb.compareTo(BigDecimal.ONE) < 0) {
            result = result + " with a probability of " + FORMAT_2DP.format(bigProb.multiply(ONE_HUNDRED)) + "%";
        }
        
        return result;
        
    }

    
	/**
	 * sort by the UID field which is a sequence of when the move was found
	 */
	static public final Comparator<Action> SORT_BY_MOVE_NUMBER  = new Comparator<Action>() {
		@Override
		public int compare(Action o1, Action o2) {
			
			return (int) (o1.myUID - o2.myUID);
		
		}
	};
	
}
