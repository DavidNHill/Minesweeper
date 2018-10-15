/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.gamestate;

/**
 *
 * @author David
 */
public class Location implements Comparable<Location> {
    
    public final int x;
    public final int y;

    protected final int sortOrder;
    
    public Location(int x, int y) {
        
        this.x = x;
        this.y = y;
        
        this.sortOrder = y + x * 10000;
        
    }
    
    public String display() {
        return "(" + x + "," + y + ")";
    }
    
    /**
     * target is one if the 8 squares surrounding this location
     * @param target
     * @return
     */
    public boolean isAdjacent(Location target) {
        
        int dx = Math.abs(this.x - target.x);
        int dy = Math.abs(this.y - target.y);
        
        if (dx > 1 || dy > 1 || (dx == 0 && dy == 0)) {
            return false;
        } else {
            return true;
        }        

    }
    
    @Override
    /**
     * Returns true if m describes the same location
     */
    public boolean equals(Object m) {
        
        if (m instanceof Location) {
        	if (this.x == ((Location) m).x && this.y == ((Location) m).y) {
        		return true;
        	} else {
        		return false;
        	}
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {
    	return sortOrder;
    }
    
	@Override
	public int compareTo(Location arg0) {
		return this.sortOrder - arg0.sortOrder;
	}
    
}
