package minesweeper.solver;

import java.util.Collection;
import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

public interface LocationEvaluator {
	
	abstract public Action[] bestMove();
	abstract public List<EvaluatedLocation> getEvaluatedLocations();
	abstract public void evaluateLocations();
	abstract public void showResults();
	
	abstract public void evaluateOffEdgeCandidates(List<Location> allUnrevealedSquares);
	abstract public void addLocations(Collection<CandidateLocation> tiles);
}
