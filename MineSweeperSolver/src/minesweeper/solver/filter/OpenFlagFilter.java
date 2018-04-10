package minesweeper.solver.filter;

import java.util.ArrayList;
import java.util.List;

import minesweeper.gamestate.Location;
import minesweeper.solver.constructs.CandidateLocation;
import minesweeper.solver.constructs.SubLocation;

public class OpenFlagFilter extends Filter {

	final private List<Location> openFlags;
	
	public OpenFlagFilter(List<Location> openFlags) {
		
		this.openFlags = openFlags;
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
        for (CandidateLocation l: input) {
            for (Location s: openFlags) {
                if (s.equals(l)) {
                    output.add(l);
                    found = true;
                    break;
                }
            }
        }     
	
        // if none are open flags then return them all back
        if (!found) {
        	return input;
        }
        
        stampOutput(output, "open flag");
        
		return output;
	}


}
