/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.solver;

import java.math.BigInteger;
import java.util.List;

import minesweeper.structure.Location;

/**
 *
 * @author David
 */
public class CrunchResult {
    
        protected List<Location> square;
        protected Location witness[];
        
        protected int originalNumMines;
        
        // information about witnesses
        protected int[] witnessGood;
        protected boolean[] witnessRestFlags;
        protected boolean[] witnessRestClear;
        protected int currentFlags[];
        protected boolean alwaysSatisfied[];

        //protected int[] hookMines;
        
        /**
         * The weight is used to scale up the values so they represent the whole solution. 
         */
        private BigInteger weight;
        protected BigInteger bigGoodCandidates = BigInteger.ZERO;
        
         /**
         * The number of candidate solutions that put a mine in this square.
         * if the value is zero a mine is never present, if equals bigGoodCandidates then a mine is always present
         */
        protected BigInteger[] bigTally;
        
       
        /**
         * <p>bigDistribution[square][3] = 102, means 102 candidate solutions have 3 mines surrounding this square</p>
         */
        protected BigInteger[][] bigDistribution;
        
        /**
         * The largest number of candidate solutions within the bigDistribution for this square. This is the maximum number of solutions that can
         * remain if we choose to guess here.
         */
        //private BigInteger[] bigMax;  // the maximum count for each square
        //private int[] bigMaxIndex;
        
        /**
         * Number of different values this square can have
         */
        private int[] bigCount; // number of different values this square can have
        private int maxBigCount = 0;
        

        // merge crunch results to give a single view of the situation
        protected static CrunchResult bigMerge(CrunchResult... cr) {
       
            CrunchResult result = new CrunchResult();
            
            // if all the results have the same number of original mines then store that
            int originalMines = cr[0].originalNumMines;
            for (CrunchResult r: cr) {
                if (r.originalNumMines != originalMines) {
                    originalMines = 0;
                    break;
                }
            }
            result.originalNumMines = originalMines;
            
            result.witness = cr[0].witness;
            result.square = cr[0].getSquare();
            
            result.setWeight(BigInteger.ONE);
            
            BigInteger[][] distribution;
            BigInteger[] bigMax;
            BigInteger[] bigCount;
            if (cr[0].bigDistribution == null) {
                distribution = null;
            } else {
                distribution = new BigInteger[result.getSquare().size()][9];
                bigMax = new BigInteger[result.getSquare().size()];
                bigCount = new BigInteger[result.getSquare().size()];
                for (int i = 0; i < result.getSquare().size(); i++) {
                    for (int j = 0; j < 9; j++) {
                        distribution[i][j] = BigInteger.ZERO;
                    }
                }
            }
            
            BigInteger[] tally = new BigInteger[cr[0].bigTally.length];
            BigInteger candidates = BigInteger.ZERO;
            for (int i=0; i < tally.length; i++) {
                tally[i] = BigInteger.ZERO;
            }
            for (int j=0; j < cr.length; j++) { 
                for (int k=0; k < tally.length; k++) {
                    tally[k] = tally[k].add(cr[j].getWeight().multiply(cr[j].bigTally[k]));
                    if (distribution != null) {
                        for (int l=0; l < 9; l++) {
                            distribution[k][l] = distribution[k][l].add(cr[j].getWeight().multiply(cr[j].bigDistribution[k][l]));
                        }
                    }
                }
                candidates = candidates.add(cr[j].getWeight().multiply(cr[j].bigGoodCandidates));
            }
            result.bigTally = tally;
            result.bigGoodCandidates = candidates;
            result.bigDistribution = distribution;
            
            //result.calculateMinMax();
            
            
            // merge the witness information
            result.witnessRestFlags = new boolean[cr[0].witnessRestFlags.length];
            result.witnessRestClear = new boolean[cr[0].witnessRestClear.length];
            
            for (int i=0; i < result.witnessRestFlags.length; i++) {
            	result.witnessRestFlags[i] = true;
            	result.witnessRestClear[i] = true;
            	for (int j=0; j < cr.length; j++) {
            		result.witnessRestFlags[i] = result.witnessRestFlags[i] & cr[j].witnessRestFlags[i];
            		result.witnessRestClear[i] = result.witnessRestClear[i] & cr[j].witnessRestClear[i];
            	}
            }
            
            // witness good information should be the same for all the merged data
            result.witnessGood = cr[0].witnessGood;
            
            return result;

        }
        
        /*
        protected void calculateMinMax() {
            
            if (this.bigDistribution == null) {
                return;
            }
            
            this.setBigCount(new int[this.getSquare().size()]);
            this.setBigMax(new BigInteger[this.getSquare().size()]);
            this.setBigMaxIndex(new int[this.getSquare().size()]);

            for (int i=0; i < this.getSquare().size(); i++) {
                BigInteger max = BigInteger.ZERO;
                int maxIndex = 0;
                for (int j=0; j < this.bigDistribution[i].length; j++) {
                    if (this.bigDistribution[i][j].signum() != 0) {
                        this.bigCount[i]++;
                    }
                    if (this.bigDistribution[i][j].compareTo(max) > 0) {
                        max = this.bigDistribution[i][j];
                        maxIndex = j;
                    }
                }
                this.getBigMax()[i] = max;
                this.getBigMaxIndex()[i] = maxIndex;
            }

            for (int i=0; i < this.bigCount.length; i++) {
            	maxBigCount = Math.max(maxBigCount, bigCount[i]);
            }
            
            
            
        }
		*/
        
		public List<Location> getSquare() {
			return square;
		}

		public void setSquare(List<Location> square) {
			this.square = square;
		}

		//public BigInteger[] getBigMax() {
		//	return bigMax;
		//}

		//public void setBigMax(BigInteger[] bigMax) {
		//	this.bigMax = bigMax;
		//}

		public BigInteger getWeight() {
			return weight;
		}

		public void setWeight(BigInteger weight) {
			this.weight = weight;
		}

		public int[] getBigCount() {
			return bigCount;
		}

		public void setBigCount(int[] bigCount) {
			this.bigCount = bigCount;
		}

		//public int[] getBigMaxIndex() {
		//	return bigMaxIndex;
		//}

		//public void setBigMaxIndex(int[] bigMaxIndex) {
		//	this.bigMaxIndex = bigMaxIndex;
		//}
        
		//public int getMaxCount() {
		//	return this.maxBigCount;
		//}
}
