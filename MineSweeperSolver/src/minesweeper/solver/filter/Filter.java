package minesweeper.solver.filter;

import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;

public abstract class Filter {

	protected boolean filtered = false;
	
	public List<CandidateLocation> filter(List<CandidateLocation> input) {
		return filter(input, 0);
	};
	
	abstract public List<CandidateLocation> filter(List<CandidateLocation> input, int flags);

	public boolean didFilter() {
		return filtered;
	}
	
	protected void stampOutput(List<CandidateLocation> output, String stamp) {
		
        filtered = true;
        
        /*
        int count = output.size();
        
        String desc = "[" + stamp + ":" + count + "]";
        
        // mark them as min-max
        for (CandidateLocation l: output) {
        	l.appendDescription(desc);
        }
		*/
        
	}
	
}
