package minesweeper.solver.filter;

import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;

public class NullFilter extends Filter {


	public NullFilter() {
		
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
        	return input;

	}


}
