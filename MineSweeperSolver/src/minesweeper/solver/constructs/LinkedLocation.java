package minesweeper.solver.constructs;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import minesweeper.structure.Location;

public class LinkedLocation extends Location {

	private Set<Location> partners = new HashSet<>();
	
	private int links = 0;
	
	public LinkedLocation(int x, int y, List<? extends Location> partner) {
		super(x, y);

		incrementLinks(partner);
		
	}
	
	public void incrementLinks(List<? extends Location> partner) {
		
		for (Location p: partner) {
			if (partners.add(p)) {
				links++;		
			}
		}
	}
	
	public int getLinksCount() {
		return links;
	}
	
	public Set<Location> getLinkedLocations() {
		return partners;
	}
	
	static public final Comparator<LinkedLocation> SORT_BY_LINKS_DESC  = new Comparator<LinkedLocation>() {
		@Override
		public int compare(LinkedLocation o1, LinkedLocation o2) {
			return o2.links - o1.links;
		}
	};

}
