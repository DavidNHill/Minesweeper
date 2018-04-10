package minesweeper.solver.constructs;

import java.util.Comparator;

import minesweeper.gamestate.Location;

public class LinkedLocation extends Location {

	private int links = 0;
	
	public LinkedLocation(int x, int y) {
		super(x, y);

		links = 1;
		
	}
	
	public void incrementLinks() {
		links++;
	}
	
	public int getLinks() {
		return links;
	}
	
	
	static public final Comparator<LinkedLocation> SORT_BY_LINKS_DESC  = new Comparator<LinkedLocation>() {
		@Override
		public int compare(LinkedLocation o1, LinkedLocation o2) {
			return o2.links - o1.links;
		}
	};

}
