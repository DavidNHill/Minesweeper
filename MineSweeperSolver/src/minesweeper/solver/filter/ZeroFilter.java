package minesweeper.solver.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.ZeroLocation;

public class ZeroFilter extends Filter {

	final private List<ZeroLocation> zeros;
	
	public ZeroFilter(List<ZeroLocation> zeros) {
		
		this.zeros = zeros;
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
		BigDecimal hwm = BigDecimal.ZERO;
		
		// only keep the best zero locations based on probability of it happening (got from brute force analysis)
        for (CandidateLocation l: input) {
            for (ZeroLocation s: zeros) {
                if (s.equals(l)) {
                	int compare = s.getProbability().compareTo(hwm);
                	if (compare > 0) {
                		output.clear();
                        output.add(l);
                        hwm = s.getProbability();
                        found = true;
                	} else if (compare == 0) {
                        output.add(l);
                        found = true;                		
                	}
                    break;            
                }
            }
        }     
	
        // if none are hooks then return them all back
        if (!found) {
        	return input;
        }
        
        stampOutput(output, "zero");
        
		return output;
	}


}
