package minesweeper.solver.filter;

import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.LinkedLocation;

public class ContraLinkedFilter extends Filter {

	final private List<LinkedLocation> links;
	
	public ContraLinkedFilter(List<LinkedLocation> links) {
		
		this.links = links;
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		// the links are stored in descending order 
		int numLinks = 0;
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
        for (CandidateLocation l: input) {
            for (LinkedLocation s: links) {
                if (s.equals(l)) {
                	if (s.getLinksCount() > numLinks) {
                    	numLinks = s.getLinksCount();
                        output.clear();
                    	output.add(l);
                        found = true;
                        break;                		
                	} else if (s.getLinksCount() == numLinks) {
                		output.add(l);
                	}

                }
            }
        }     
	
        // if none are links then return them all back
        if (!found) {
        	return input;
        }
        
        stampOutput(output, "==>flag");
        
		return output;
	}


}
