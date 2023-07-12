package minesweeper.solver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minesweeper.structure.Location;

public class BoardStateCache {

	private final static int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
	private final static int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
	
	protected class Cache {
		
		private int width;
		private int height;
		
		protected Location[][] locations;
		
		protected AdjacentSquares[][] adjacentLocations1;
		protected AdjacentSquares[][] adjacentLocations2;
		
		protected Location getLocation(int x, int y) {
			return locations[x][y];
		}
		
	}
	
	// iterator for adjacent squares
	protected class AdjacentSquares implements Iterable<Location> {

		private Location loc;
		private final int size;
		private List<Location> locations;

		AdjacentSquares(Cache cache, Location l, int width, int height, int size) {
			this.loc = l;
			this.size = size;
			
			if (size == 1) {
				locations = new ArrayList<>(8);
				for (int i=0; i < DX.length; i++) {
					if (loc.x + DX[i] >= 0 && loc.x + DX[i] < width && loc.y + DY[i] >= 0 && loc.y + DY[i] < height) {
						locations.add(cache.getLocation(loc.x + DX[i], loc.y + DY[i]));
					}		
					
				}
			} else {
				int startX = Math.max(0, loc.x - this.size);
				int endX = Math.min(width - 1, loc.x + this.size);
				
				int startY = Math.max(0, loc.y - this.size);
				int endY = Math.min(height - 1, loc.y + this.size);
				
				locations = new ArrayList<>((this.size * 2 - 1) * (this.size * 2 - 1));
				for (int i=startX; i <= endX; i++) {
					for (int j=startY; j <= endY; j++) {
						if (i == loc.x && j == loc.y) {
							// don't send back the central location
						} else {
							locations.add(cache.getLocation(i,j));
						}
						
					}
				}				
			}

		
		}

		@Override
		public Iterator<Location> iterator() {
			return locations.iterator();
		}

	
	}
	
	private static List<Cache> cacheAdjSqu = new ArrayList<>();
	private static BoardStateCache me;
	
	
	public synchronized Cache getAdjacentSquares1(int width, int height) {
		
		for (Cache cache: cacheAdjSqu) {
			if (cache.height == height && cache.width == width) {
				return cache;
			}
		}
		
		Cache cache = new Cache();
		
		cache.height = height;
		cache.width = width;
		
		cache.locations = new Location[width][height];
		
		// Create a Location for each entry yon the board
		for (int x=0; x < width; x++) {
			for (int y=0; y < height; y++) {
				cache.locations[x][y] = new Location(x,y);
			}
		}
		
		cache.adjacentLocations1 = new AdjacentSquares[width][height];
		cache.adjacentLocations2 = new AdjacentSquares[width][height];

		//  set up how many adjacent locations there are to each square - they are all unrevealed to start with
		for (int x=0; x < width; x++) {
			for (int y=0; y < height; y++) {

				cache.adjacentLocations1[x][y] = new AdjacentSquares(cache, cache.getLocation(x,y), width, height, 1);
				cache.adjacentLocations2[x][y] = new AdjacentSquares(cache, cache.getLocation(x,y), width, height, 2);

			}
		}
		
		cacheAdjSqu.add(cache);
		
		return cache;
	}
	
	public static synchronized BoardStateCache getInstance() {
		
		if (me == null) {
			me = new BoardStateCache();
		}
		
		return me;
	}
	
	
}
