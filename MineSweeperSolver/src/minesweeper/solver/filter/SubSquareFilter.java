package minesweeper.solver.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.SubLocation;

public class SubSquareFilter extends Filter {

	final private List<SubLocation> subLocations;
	
	public SubSquareFilter(List<SubLocation> subLocations) {
		
		this.subLocations = subLocations;
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
		/*
        int bestSize = -1;
        for (CandidateLocation l: input) {
            int size = -1;
            for (SubLocation s: subLocations) {
                if (s.equals(l)) {
                    size = s.getSize();
                    break;
                }
            }
            if (size > bestSize) {
                bestSize = size;
                output.clear();
                output.add(l);
                found = true;
            } else if (size == bestSize) {
            	output.add(l);
            }
        }     
		*/
		
        int bestSize = -1;
        BigDecimal hwm = BigDecimal.ZERO;
        
        for (CandidateLocation l: input) {
             for (SubLocation s: subLocations) {
                if (s.equals(l)) {
                    found = true;
                	int compare = s.getProbability().compareTo(hwm);
                	if (compare > 0) {
                        hwm = s.getProbability();
                        bestSize = s.getSize();
                		output.clear();
                        output.add(l);
                	} else if (compare == 0) {
                        if (s.getSize() > bestSize) {
                            bestSize = s.getSize();
                            output.clear();
                            output.add(l);
                        } else if (s.getSize() == bestSize) {
                        	output.add(l);
                        }
                	}
                    break;            
                }
            }
        }     
		
        // if none are sub squares then return them all back
        if (!found) {
        	return input;
        }
        
        stampOutput(output, "sub square");
        
		return output;
	}


}
