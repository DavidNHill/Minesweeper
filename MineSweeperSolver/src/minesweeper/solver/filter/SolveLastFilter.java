package minesweeper.solver.filter;

import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.Zone;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.structure.Location;

public class SolveLastFilter extends Filter {

	final private List<Location> solveLast = new ArrayList<>();
	
	public SolveLastFilter(List<Zone> zones) {
		
		// load up all the locations inside zones which are solve last
		for (Zone z: zones) {
			if (z.isSolveLast()) {
				for (Location l: z.getInterior()) {
					solveLast.add(l);
				}				
			}
		}
		
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		// if there is nothing to check against then return the input
		if (solveLast.isEmpty()) {
			return input;
		}
		
		List<CandidateLocation> output = new ArrayList<>();
		
        for (CandidateLocation l: input) {
        	boolean match = false;
            for (Location s: solveLast) {
                if (s.equals(l)) {
                    match = true;
                    break;
                }
            }
        	if (!match) {
                output.add(l);            		
        	}
        }     
	
        // if everything is 'solve last' and been rejected then return the input
        if (output.isEmpty()) {
        	return input;
        }
        
        stampOutput(output, "okay");
        
		return output;
	}


}
