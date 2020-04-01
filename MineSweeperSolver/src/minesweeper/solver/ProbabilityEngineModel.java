package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.LinkedLocation;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

abstract public class ProbabilityEngineModel {
	
	abstract public void process();
	
	abstract protected long getDuration();
	abstract protected long getIndependentGroups();
	abstract public BigDecimal getProbability(Location l);
	abstract protected List<CandidateLocation> getBestCandidates(BigDecimal freshhold);
	abstract protected BigInteger getSolutionCount();
	abstract protected BigDecimal getBestOnEdgeProb();
	abstract protected BigDecimal getOffEdgeProb();
	abstract protected boolean foundCertainty();
	abstract protected Area getDeadLocations();
	abstract protected List<Location> getMines();
	abstract protected List<LinkedLocation> getLinkedLocations();
	abstract protected LinkedLocation getLinkedLocation(Location tile);
	abstract protected List<BruteForce> getIsolatedEdges();

}
