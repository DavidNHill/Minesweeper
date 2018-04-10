package minesweeper.solver.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.SuperLocation;

public class SuperSquareFilter extends Filter {

	final private List<SuperLocation> superLocations;
	
	public SuperSquareFilter(List<SuperLocation> superLocations) {
		
		this.superLocations = superLocations;
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
        int bestSize = 10;
        BigDecimal hwm = BigDecimal.ZERO;
        
        for (CandidateLocation l: input) {
             for (SuperLocation s: superLocations) {
                if (s.equals(l)) {
                    found = true;
                	int compare = s.getProbability().compareTo(hwm);
                	if (compare > 0) {
                        hwm = s.getProbability();
                        bestSize = s.getSize();
                		output.clear();
                        output.add(l);
                	} else if (compare == 0) {
                        if (s.getSize() < bestSize) {
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
            /*

            */
        }     
        
        /*
        if (!found) {
            display("found a guess which is also a super square and has " + bestSize + " extra squares");
            bestLoc.appendDescription(" [super-square]");
        }
		*/
		
        // if none are super squares then return them all back
        if (!found) {
        	return input;
        }
        
        stampOutput(output, "super square");
        
		return output;
	}


}
