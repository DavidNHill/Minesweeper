/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.gamestate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;


/**
 *
 * @author David
 */
public class Action extends Location {
  
    public static final int CLEAR = 1;
    public static final int CLEARALL = 2;
    public static final int FLAG = 3;
    
    public static final DecimalFormat FORMAT_2DP = new DecimalFormat("00.00");
    
    private final static String[] ACTION = {"", "Clear", "Clear around", "Place flag"};
    private final static BigDecimal MINUS_ONE = new BigDecimal("-1");
    
    static long globalUID = 0;
    
    private final long myUID;
    //private final Location l;
    private final int action;
    private final double prob;
    private final BigDecimal bigProb;
    
    private final boolean certainty;
    
    private int type = 0;
    
    String comment;
    
    // used by the human player
    public Action(Location l, int a) {
        this(l, a, 1, "", MINUS_ONE);
    }
    
    // used by the computer coach
    public Action(Location l, int a, int type, String comment, BigDecimal bigProb) {
    	this(l, a, type, comment, bigProb, globalUID++);
        
    }    
    
    // used by the computer coach to force a move to be earlier in the list - e.g. when we need to remove a flag placed by the human player before we can clear the square
    public Action(Location l, int a, int type, String comment, BigDecimal bigProb, long uid) {
        super(l.x, l.y);
        
        //this.l = l;
        this.action = a;
        this.comment = comment;
        this.bigProb = bigProb;
        this.prob = this.bigProb.doubleValue();
        this.type = type;
        
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
    
    //public Location getLocation() {
    //    return this.l;
    //}    
    
    /**
     * Consider moving to getBigProb which returns the big decimal version of the probability
     */
    @Deprecated
    public double getProb() {
       return prob;
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
    
    public int getType() {
    	return this.type;
    }
    
    public String asString() {
        
        String result = Action.ACTION[this.action] + " at " + super.display() + ": " + comment; 
        
        if (prob > 0 && prob < 1) {
            result = result + " with a probability of " + FORMAT_2DP.format(prob * 100) + "%";
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
