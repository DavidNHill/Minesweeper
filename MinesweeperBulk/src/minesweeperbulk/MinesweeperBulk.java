/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeperbulk;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Random;
import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameStateEasy;
import minesweeper.gamestate.GameStateStandard;
import minesweeper.gamestate.Location;
import minesweeper.gamestate.GameStateHard;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.solver.OpeningLocation;
import minesweeper.solver.Preferences;
import minesweeper.solver.Solver;

/**
 *
 * @author David
 */
public class MinesweeperBulk {

	private static final DecimalFormat MASK = new DecimalFormat("#0.000");
	private static final DecimalFormat MASK5DP = new DecimalFormat("#0.00000");
	
	private static final BigDecimal BIG_HALF = new BigDecimal("0.5");

	private final static int MAX = 100000;
	private final static int STEP = MAX / 5000;

	private static int neverGuessed = 0;
	private static int[] guessMatrix = new int[10];
	private static double fairness = 0;
	private static int guesses = 0;
	private static int losingGuesses = 0;
	private static int winningGuesses = 0;
	private static double totalProb = 1;
	private static int fiftyFiftyGuess = 0;
	private static int fiftyFiftyWon = 0;
	private static int fiftyFiftyLost = 0;

	public static void main(String[] args) {

		int wins=0;
		int losses=0;

		long start = System.currentTimeMillis();

		// pick a random seed or override with a previously used seed to play the same sequence of games again.
		long seed = (new Random()).nextInt();

		seed = 1652664258;

		//Seed -1502353305 played 50000 games Wins=26816 (53.632%) after 119688(56389,63299) guesses(winning, losing), fairness ratio=-0.00035, duration = 484985 milliseconds
		//Number of Never Guessed wins is 8036 (16.072%) modified win rate = 44.753%
		
		//Seed -834064616 played 100000 games Wins=39761 (39.761%) after 329403(137589,191814) guesses(winning, losing), fairness ratio=0.00221, duration = 470243 milliseconds
		//Number of Never Guessed wins is 5164 modified win rate = 36.481
		
		//Seed 1301985346 played [400 + 20000] 50000 games Wins=20096 (40.192%) after 162217(68817,93400) guesses(winning, losing), fairness ratio=-0.00417, duration = 412378 milliseconds
		//Number of Never Guessed wins is 2591 modified win rate = 36.923
		
		//Seed 1301985346 played [400 + 75000] 50000 games Wins=20158 (40.316%) after 162067(68509,93558) guesses(winning, losing), fairness ratio=-0.00134, duration = 471477 milliseconds
		//Number of Never Guessed wins is 2592 (5.184%) modified win rate = 37.053%
		
		//Seed 1301985346 played [100 + 5000] 50000 games Wins=20118 (40.236%) after 162327(68421,93906) guesses(winning, losing), fairness ratio=-0.00337, duration = 336992 milliseconds
		//Number of Never Guessed wins is 2592 (5.184%) mo dified win rate = 36.968%
		
		//Seed 1301985346 played [no brute force] 50000 games Wins=19792 (39.584%) after 163503(67454,96049) guesses(winning, losing), fairness ratio=-0.00410, duration = 308200 milliseconds
		//Number of Never Guessed wins is 2592 (5.184%) modified win rate = 36.281%
		
		//Seed -778220640 played 50000 games Wins=19925 (39.850%) after 161812(67190,94622) guesses(winning, losing), fairness ratio=-0.01167, duration = 305548 milliseconds
		//Number of Never Guessed wins is 2624 modified win rate = 36.518
			
		// start (3,2)
		//Seed 1652664258 played 100000 games Wins=88880 (88.880%) after 40412(25413,14999) guesses(winning, losing), fairness ratio=0.01630, duration = 200178 milliseconds
		//Number of Never Guessed wins is 71928 (71.928%) modified win rate = 60.388%
		// start (3,3)
		//Seed 1652664258 played 100000 games Wins=88684 (88.684%) after 40182(24983,15199) guesses(winning, losing), fairness ratio=0.01047, duration = 200371 milliseconds
		//Number of Never Guessed wins is 72070 (72.070%) modified win rate = 59.484%
		// start (2,2)
		//Seed 1652664258 played 100000 games Wins=88987 (88.987%) after 41751(26752,14999) guesses(winning, losing), fairness ratio=0.01335, duration = 200081 milliseconds
		//Number of Never Guessed wins is 71407 (71.407%) modified win rate = 61.484%
		// start (1,1)
		//Seed 1652664258 played 100000 games Wins=87728 (87.728%) after 51127(33160,17967) guesses(winning, losing), fairness ratio=0.00457, duration = 207854 milliseconds
		//Number of Never Guessed wins is 67318 (67.318%) modified win rate = 62.450%
		
		System.out.println("Seed is " + seed);
		Random seeder = new Random(seed);

		for (int i=0; i < MAX; i++) {

			//GameStateModel gs = new GameStateHard(30, 16, 99, seeder.nextLong()); // first move can be a mine
			//GameStateModel gs = new GameStateEasy(24, 30, 203, seeder.nextLong());  // first move is a zero
			GameStateModel gs = new GameStateStandard(30, 24, 203, seeder.nextLong());
			//GameStateModel gs = new GameStateStandard(9, 9, 10, seeder.nextLong());
			Solver solver = new Solver(gs, Preferences.SMALL_BRUTE_FORCE, false);
			//Solver solver = new Solver(gs, Preferences.TINY_BRUTE_FORCE, false);
			//Solver solver = new Solver(gs, Preferences.NO_BRUTE_FORCE, false);

			//solver.setFlagFree(true);
			
			//solver.overrideBookOpenings(new OpeningLocation(0, 0), new OpeningLocation(29,0),new OpeningLocation(29,15),new OpeningLocation(0,15));
			
			//solver.overrideBookOpenings(new OpeningLocation(3, 3), new OpeningLocation(29,0),new OpeningLocation(29,15), new OpeningLocation(0,15)); 
			
			//solver.overrideBookOpenings(new OpeningLocation(5,0), new OpeningLocation(10,0),new OpeningLocation(15,5), new OpeningLocation(15,10), new OpeningLocation(5,15), new OpeningLocation(10,15), new OpeningLocation(0,5), new OpeningLocation(0,10)); // 16x16

			if (i % STEP == 0) {
				double p = (double) wins / (double) i;
				System.out.println("played " + i + "/" + MAX + " games Wins=" + wins + " (" + MASK.format(p * 100) + "%). After "
						+ guesses + "(" + winningGuesses + "," + losingGuesses + ") guesses(winning, losing), fairness ratio=" + MASK5DP.format(fairness / guesses) + "." );
			}

			if (playGame(gs, solver)) {
				wins++;
			} else {
				losses++;
			}

		}

		long duration = System.currentTimeMillis() - start;

		double p = (double) wins / (double) MAX;
		System.out.println("Seed " + seed + " played " + MAX + " games Wins=" + wins + " (" + MASK.format(p * 100) + "%) after " + guesses + "(" + winningGuesses + "," + losingGuesses + ") guesses(winning, losing), fairness ratio=" + MASK5DP.format(fairness / guesses) + ", duration = " + duration + " milliseconds");

		double p1 = (double) (wins - neverGuessed) / (double) (MAX - neverGuessed);
		
		double p2 = (double) neverGuessed / (double) MAX;
		
		System.out.println("Number of Never Guessed wins is " + neverGuessed + " (" + MASK.format(p2 * 100) + "%) modified win rate = " + MASK.format(p1 * 100) + "%");
		for (int i=0; i <guessMatrix.length; i++) {
			System.out.println("Number of Less than " + (i+1)*10 + "% chance wins " + guessMatrix[i]);
		}
		
		System.out.println("Number of 50-50 guesses = " + fiftyFiftyGuess + " won " + fiftyFiftyWon + " lost " + fiftyFiftyLost);

	}

	static private boolean playGame(GameStateModel gs, Solver solver) {

		int state;

		boolean certain = true;
		double probability = 1;

		int gameGuesses = 0;

		play: while (true) {

			Action[] moves;
			try {
				solver.start();
				moves = solver.getResult();
			} catch (Exception e) {
				System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
				e.printStackTrace();
				return false;
			}

			if (moves.length == 0) {
				System.err.println("No moves returned by the solver");
				System.exit(1);
			}            

			// play all the moves until all done, or the game is won or lost
			for (int i=0; i < moves.length; i++) {

				int type = moves[i].getType();
				double prob = moves[i].getProb();

				if (prob <= 0 || prob > 1) {
					System.out.println("Game (" + gs.showGameKey() + ") move with probability of " + prob + "! - " + moves[i].asString());
				} else if (prob != 1d) {
					certain = false;
					probability = probability * prob;
				}

				//System.out.print(moves[i].asString());
				boolean result = gs.doAction(moves[i]);
				//System.out.println("... with result: " + result);
				//if (gs.getMinesLeft() < 0) {
				//    System.out.println(moves[i].asString() + " results in mines left < 0 ... with result: " + result);
				//} else {
				//    System.out.println(moves[i].asString() + " ... with result: " + result);
				//}

				state = gs.getGameState();

				if (moves[i].getBigProb().compareTo(BIG_HALF) == 0) { 
					fiftyFiftyGuess++;
					if (state == GameStateModel.STARTED || state == GameStateModel.WON) {
						fiftyFiftyWon++;
					} else {
						fiftyFiftyLost++;
					}
				}
				
				
				// only monitor good guesses (brute force, probability engine, zonal, opening book and hooks)
				//if (type == 4 || type == 5 || type == 10 || type == 11 || type == 8 || type == 7) {
					if (state == GameStateModel.STARTED || state == GameStateModel.WON) {
						if (prob > 0d && prob < 1d) {
							gameGuesses++;
							guesses++;
							fairness = fairness + 1;
						}
					} else { // otherwise the guess resulted in a loss
						if (prob > 0d && prob < 1d) {
							gameGuesses++;
							guesses++;
							fairness = fairness - prob / (1d -prob);
						}                    
					}
				//} else {
				//	if (prob > 0d && prob < 1d) {
				//		//System.out.println("Game (" + gs.showGameKey() + ") " + moves[i].asString());
				//	}
				//}


				if (state == GameStateModel.LOST && prob == 1d) {
					System.out.println("Game (" + gs.showGameKey() + ") lost on move with probablity = " + prob + " :" + moves[i].asString());
				}

				if (state == GameStateModel.LOST || state == GameStateModel.WON) {
					break play;
				}
			}            
		}


		if (state == GameStateModel.LOST) {
			losingGuesses = losingGuesses + gameGuesses;
			return false;
		} else {
			winningGuesses = winningGuesses + gameGuesses;
			if (certain) {
				neverGuessed++;
			} else if (probability > 1) {
				System.out.println("Game (" + gs.showGameKey() + ") has a total probablity = " + probability);
			} else {
				int i = (int) (probability * 10);
				guessMatrix[i]++;
			}
			return true;
		}


	}


}
