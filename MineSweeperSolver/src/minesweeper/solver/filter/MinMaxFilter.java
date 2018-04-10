package minesweeper.solver.filter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import minesweeper.solver.CrunchResult;
import minesweeper.solver.constructs.CandidateLocation;

public class MinMaxFilter extends Filter {

	public final static int NONE = 0;
	public final static int WINS_ONLY = 1;
	public final static int FIFTY_FIFTY = 2;
	
	
	final private CrunchResult[] minMax;
	
	public MinMaxFilter(CrunchResult... minMax) {
		
		this.minMax = minMax;
	}
	
	
	@Override
	public List<CandidateLocation> filter(List<CandidateLocation> input, int flags) {
		
		boolean limited;
		if (flags == 0) {
			limited = false;
		} else {
			limited = true;
		}
		
		
		List<CandidateLocation> output = new ArrayList<>();
		boolean found = false;
		
		int count = 0;
		BigInteger work = null;
        for (CandidateLocation l: input) {
            BigInteger size = work;
            int tCount = 0;
            
            outer: for (CrunchResult cr: minMax) {
                for (int i=0; i < cr.getSquare().size(); i++) {
                    if (cr.getSquare().get(i).equals(l)) {
                        size = cr.getBigMax()[i].multiply(cr.getWeight());
                        tCount = cr.getBigCount()[i];
                        //tTouching = cr.getBigMaxIndex()[i];
                        break outer;
                    }
                }
            }     
            /*
            if (work == null || tCount > count || (size.compareTo(work) < 0 && tCount == count)) {
                output.clear();
                output.add(l);
                work = size;
                count = tCount;
                found = true;
            } else if (size.compareTo(work) == 0 && tCount == count) {
                output.add(l);
            }
            */
            if (work == null || size.compareTo(work) < 0 || (size.compareTo(work) == 0 && tCount > count)) {
                output.clear();
                output.add(l);
                work = size;
                count = tCount;
                found = true;
            } else if (size.compareTo(work) == 0 && tCount == count) {
                output.add(l);
            }
        }
	
        // if none are min-max
        if (!found || (limited && work.compareTo(BigInteger.valueOf(flags)) > 0)) {
        	return input;
        }
        
        String desc;
        if (work.compareTo(BigInteger.ONE) == 0 ) {
        	desc = "solve or die";
        } else if (work.compareTo(BigInteger.valueOf(2)) == 0 ) {
        	desc = "fifty-fifty";
        } else {
        	if (limited) {
        		desc = "min-max-" + work;
        	} else {
        		desc = "min-max";
        	}
        	
        }
        
        stampOutput(output, desc);
        
		return output;
	}

	
	public CrunchResult[] getCrunchResults() {
		return this.minMax;
	}

}
