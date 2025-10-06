package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import minesweeper.solver.constructs.Box;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.LinkedLocation;
import minesweeper.solver.utility.BinomialCache;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

abstract public class ProbabilityEngineModel {
	
	abstract public void process();
	
	abstract protected long getDuration();
	abstract protected long getIndependentGroups();
	abstract Box getBox(Location l);
	abstract public BigDecimal getSafety(Location l);
	abstract protected List<CandidateLocation> getBestCandidates(BigDecimal freshhold, boolean excludeDead);
	abstract protected List<CandidateLocation> getProbableMines(BigDecimal freshhold);
	abstract protected List<Location> getFiftyPercenters();
	abstract protected BigInteger getSolutionCount();
	//abstract protected BigDecimal getBestOnEdgeProb();
	abstract protected BigDecimal getOffEdgeSafety();
	abstract protected BigInteger getOffEdgeTally();
	abstract protected boolean foundCertainty();
	abstract protected Area getDeadLocations();
	abstract boolean  allDead();
	abstract protected int getDeadValueDelta(Location l);
	abstract protected List<Location> getMines();
	abstract protected List<LinkedLocation> getLinkedLocations();
	abstract protected LinkedLocation getLinkedLocation(Location tile);
	abstract protected List<BruteForce> getIsolatedEdges();
	abstract protected boolean isBestGuessOffEdge();
	abstract protected int getLivingClearCount();
	abstract protected int getSafeTileCount();
	abstract protected int getTilesOffEdgeCount();
	abstract protected List<Box> getEmptyBoxes();

	abstract protected BigDecimal getBlendedSafety();
	abstract protected BigDecimal getBestSafety();
	abstract protected BigDecimal getBestLivingSafety();

	abstract protected Location getSingleSafestTile();

	abstract public void setBinomialCache(BinomialCache cache);
	
}
