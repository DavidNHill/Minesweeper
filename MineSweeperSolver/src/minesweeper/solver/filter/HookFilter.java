package minesweeper.solver.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import minesweeper.gamestate.Location;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.HookLocation;

public class HookFilter extends Filter {

	final private List<HookLocation> hooks = new ArrayList<>();
	
	public HookFilter(List<HookLocation> hooksOnEdge, List<HookLocation> hooksOffEdge) {
		
		this.hooks.addAll(hooksOnEdge);
		this.hooks.addAll(hooksOffEdge);
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
		BigDecimal hwm = BigDecimal.ZERO;
		
		
		// only keep the best hooks based on probability of it happening (got from brute force analysis)
        for (CandidateLocation l: input) {
            for (HookLocation s: hooks) {
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
        
        stampOutput(output, "hook");
        
		return output;
	}


}
