package minesweeper.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Collection of locations and some operations 
 * @author David
 *
 */
public class Area {

	public final static Area EMPTY_AREA = new Area(new HashSet<>());
	
	private final Set<Location> area;
	private final Set<Location> readOnlyArea;
	
	/**
	 * Build an area based on a collection of locations
	 */
	public Area(Collection<Location> area) {
		this.area = new HashSet<>(area);
		this.readOnlyArea = Collections.unmodifiableSet(this.area);
	}
	
	/**
	 * wrap a pre-built set 
	 */
	public Area(Set<Location> area) {
		this.area = area;
		this.readOnlyArea = Collections.unmodifiableSet(this.area);
	}
	
	/**
	 * Returns true if this area contains the location
	 */
	public boolean contains(Location loc) {
		return area.contains(loc);
	}
	
	/**
	 * returns true if this area contains all the locations in the subset
	 */
	public boolean supersetOf(Area subset) {
		return area.containsAll(subset.area);
	}
	
	public Collection<Location> getLocations() {
		return readOnlyArea;
	}
	
	public int size() {
		return area.size();
	}
	
	public Area add(Location add) {
		if (area.contains(add)) {
			return this;
		}
		Set<Location> result = new HashSet<>(this.size());
		result.addAll(area);
		result.add(add);
		return new Area(result);		
	}
	
	public Area remove(Location remove) {
		if (!area.contains(remove)) {
			return this;
		}
		Set<Location> result = new HashSet<>(this.size());
		result.addAll(area);
		result.remove(remove);
		return new Area(result);		
	}
	
	public Area merge(Area with) {
		Set<Location> result = new HashSet<>(this.size() + with.size());
		result.addAll(area);
		result.addAll(with.area);
		return new Area(result);
	}
	
}
