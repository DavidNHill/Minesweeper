/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeperbulk;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.MoveMethod;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.Solver;
import minesweeper.solver.bulk.BulkEvent;
import minesweeper.solver.bulk.BulkListener;
import minesweeper.solver.bulk.BulkPlayer;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SettingsFactory.Setting;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.solver.utility.Timer;
import minesweeper.structure.Action;
import minesweeper.structure.Location;
import minesweeperbulk.Recorder.Value;

/**
 *
 * @author David
 */
public class MinesweeperBulk {

	private final static int WON = 1;
	private final static int LOST = 2;
	private final static int IGNORE = 3;
	
	private static final DecimalFormat MASK = new DecimalFormat("#0.000");
	private static final DecimalFormat MASK5DP = new DecimalFormat("#0.000000");
	
	private static final BigDecimal BIG_HALF = new BigDecimal("0.5");

	private final static int MAX = 50000;
	private final static int STEP = MAX / 5000;

	private static int neverGuessed = 0;
	private static int[] guessMatrix = new int[10];
	private static double fairness = 0;
	private static int guesses = 0;
	private static int fairGuesses = 0;
	private static int losingGuesses = 0;
	private static int winningGuesses = 0;
	private static double totalProb = 1;
	private static int fiftyFiftyGuess = 0;
	private static int fiftyFiftyWon = 0;
	private static int fiftyFiftyLost = 0;

	private static boolean[] mastery = new boolean[100];
	private static int masteryCount = 0;
	private static int maxMasteryCount = 0;
	private static boolean masteryDone = false;
	
	private static int count8 = 0;
	
	public static void main(String[] args) {

		int wins=0;
		int losses=0;
		int ignored=0;
		int played=0;

		long start = System.currentTimeMillis();

		// pick a random seed or override with a previously used seed to play the same sequence of games again.
		long seed = (new Random()).nextInt();

		//seed = 82488337;
		//seed = 662429271;   // expert 10,000,000 run
		
		System.out.println("Seed is " + seed);
		Random seeder = new Random(seed);
		
		//GameSettings gameSettings = GameSettings.EXPERT;
		GameSettings gameSettings = GameSettings.create(50, 50, 600);
		
		SolverSettings settings = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS);
		//settings.setStartLocation(new Location(1,1));
		//settings.setTieBreak(false);
		
		final long bulkSeed = seed;
		BulkPlayer controller = new BulkPlayer(seeder, 50000, GameType.STANDARD, gameSettings, settings, 10, 10000);
		controller.setFlagFree(true);
		
		// click all 4 corners first
		List<Action> preactions = new ArrayList<>();
		preactions.add(new Action(0, 0, Action.CLEAR));
		preactions.add(new Action(0, gameSettings.height - 1, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, 0, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, Action.CLEAR));
		//controller.setPreActions(preactions);
		
		controller.registerListener(new BulkListener() {
			@Override
			public void intervalAction(BulkEvent event) {
				double p = (double) event.getGamesWon() / (double) event.getGamesPlayed();
				double err = Math.sqrt(p * ( 1- p) / (double) event.getGamesPlayed()) * 1.9599d;
				
				System.out.println("Seed: " + bulkSeed + ", Played " + event.getGamesPlayed() + " of " + event.getGamesToPlay() + ", won " + event.getGamesWon() + 
						", without guessing " + event.getNoGuessWins() + ", guesses " + event.getTotalGuesses() + ", actions " + event.getTotalActions() +
						", fairness " + MASK5DP.format(event.getFairness()) + ", win streak " + event.getWinStreak() + ", mastery " + event.getMastery() + 
						", win percentage " + MASK.format(p * 100) + " +/- " + MASK.format(err * 100) + ", Time left " + Timer.humanReadable(event.getEstimatedTimeLeft()) );
			}
			
		});
	
		controller.run();
		
		{
		BulkEvent event = controller.getResults();
		double p = (double) event.getGamesWon() / (double) event.getGamesPlayed();
		double err = Math.sqrt(p * ( 1- p) / (double) event.getGamesPlayed()) * 1.9599d;
		
		System.out.println("Seed: " + bulkSeed + " ==> Board " + gameSettings + " ==> Played " + event.getGamesPlayed() + ", won " + event.getGamesWon() + 
				", without guessing " + event.getNoGuessWins() + ", guesses " + event.getTotalGuesses() + ", actions " + event.getTotalActions() +
				", fairness " + MASK5DP.format(event.getFairness()) + ", win streak " + event.getWinStreak() + ", mastery " + event.getMastery() + 
				", win percentage " + MASK.format(p * 100) + " +/- " + MASK.format(err * 100) + ", Duration " + Timer.humanReadable(event.getTimeSoFar()) );
		
		
		}
		
		System.exit(0);

		//DefaultRNG.setDefaultRNGClass(RNGJSF.class);
		gameSettings = GameSettings.EXPERT;
		//GameSettings gameSettings = GameSettings.create(30,16,154);
		GameType gameType = GameType.STANDARD;
		//GameType gameType = GameType.EASY;
		
		Recorder record3BV = new Recorder();
		
		while (played < MAX) {

			GameStateModel gs = GameFactory.create(gameType, gameSettings, Math.abs(seeder.nextLong() & 0xFFFFFFFFFFFFFl));
			//GameStateModel gs = new GameStateStandardWith8(gameSettings, seeder.nextLong());
			Solver solver = new Solver(gs, settings, false);
			//solver.setFlagFree(true);
			//solver.setPlayChords(true);
			//solver.setTestMode();
			//solver.setStartLocation(new Location(1, 1));
			//Solver solver = new Solver(gs, Preferences.MEDIUM_BRUTE_FORCE, false);
			//Solver solver = new Solver(gs, Preferences.NO_BRUTE_FORCE, false);

			if (played % STEP == 0) {
				double p = (double) wins / (double) played;
				double err = Math.sqrt(p * ( 1- p) / (double) played) * 1.9599d;
				System.out.println("played " + played + "/" + MAX + " games, Wins " + wins + " (" + MASK.format(p * 100) + " +/- " + MASK.format(err * 100) +  "%). After "
						+ guesses + "(" + winningGuesses + "," + losingGuesses + ") guesses(winning, losing), fairness ratio=" + MASK5DP.format(fairness / fairGuesses) + "." );
			}

			//System.out.println(gs.getSeed());
			int result = playGame(gs, solver);
			
			int masteryIndex = played % 100;
			if (mastery[masteryIndex]) {
				masteryCount--;
			}
			
			
			if (result == WON) {
				//System.out.println(gs.getSeed());
				mastery[masteryIndex] = true;
				masteryCount++;
				maxMasteryCount = Math.max(masteryCount, maxMasteryCount);
				if (maxMasteryCount == 52 && !masteryDone) {
					masteryDone = true;
					System.out.println("got to master " + maxMasteryCount + " after " + (played + 1) + " games played");
				}
				wins++;
				record3BV.add(gs.get3BV(), true);
			} else if (result == LOST) {
				mastery[masteryIndex] = false;
				losses++;
				record3BV.add(gs.get3BV(), false);
			} else {
				mastery[masteryIndex] = false;
				ignored++;
				continue;
			}

			played++;
		}

		long duration = System.currentTimeMillis() - start;

		List<Value> histogram = record3BV.getValues();
		
		Collections.reverse(histogram);
		int totalPlayed = 0;
		int totalWon = 0;
		for (Value value: histogram) {
			totalPlayed = totalPlayed + value.played;
			totalWon = totalWon + value.won;
			double totalWinRate = (double) totalWon * 100d / (double) totalPlayed;
			double winRate = (double) value.won * 100d / (double) value.played;
			//System.out.println("3BV " + value.key + ", played " + value.played + ", won " + value.won + ", win " + MASK.format(winRate) + "%, win accumulated " + MASK.format(totalWinRate)
			//  + " after " + totalPlayed + " games");
			System.out.println(value.key + "," + value.played + "," + value.won + "," + winRate + "," + totalWinRate + "," + totalPlayed);
		}
		
		
		double p = (double) wins / (double) MAX;
		double err = Math.sqrt(p * ( 1- p) / (double) played) * 1.9599d;
		System.out.println("Seed " + seed + " played " + MAX + " games, Wins " + wins + " (" + MASK.format(p * 100) + " +/- " + MASK.format(err * 100) + "%) after " 
		+ guesses + "(" + winningGuesses + "," + losingGuesses + ") guesses(winning, losing), fairness ratio=" + MASK5DP.format(fairness / fairGuesses) 
		+ ", duration = " + duration + " milliseconds");

		double p1 = (double) (wins - neverGuessed) / (double) (MAX - neverGuessed);
		
		double p2 = (double) neverGuessed / (double) MAX;
		
		System.out.println("Number of Never Guessed wins is " + neverGuessed + " (" + MASK.format(p2 * 100) + "%) modified win rate = " + MASK.format(p1 * 100) + "%");
		for (int i=0; i <guessMatrix.length; i++) {
			System.out.println("Number of Less than " + (i+1)*10 + "% chance wins " + guessMatrix[i]);
		}
		
		System.out.println("Number of games ignored = " + ignored);
		System.out.println("Number of 50-50 guesses = " + fiftyFiftyGuess + " won " + fiftyFiftyWon + " lost " + fiftyFiftyLost);
		System.out.println("Mastery " + maxMasteryCount);

	}

	static private int playGame(GameStateModel gs, Solver solver) {

		int state;

		boolean certain = true;
		double probability = 1;

		int gameGuesses = 0;
		
		boolean has8 = false;


		play: while (true) {

			Action[] moves;
			try {
				solver.start();
				moves = solver.getResult();
			} catch (Exception e) {
				System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
				e.printStackTrace();
				return IGNORE;
			}

			if (moves.length == 0) {
				System.err.println(gs.getSeed() + " - No moves returned by the solver");
				System.exit(1);
			}            

			// play all the moves until all done, or the game is won or lost
			for (int i=0; i < moves.length; i++) {

				MoveMethod method = moves[i].getMoveMethod();
				//double prob = moves[i].getProb();
				BigDecimal prob = moves[i].getBigProb();
				
				// only count games without guesses
				//if (moves[i].getAction() == Action.CLEAR && !moves[i].isCertainty()) {
				//	return LOST;
				//}

				if (prob.compareTo(BigDecimal.ZERO) <= 0 || prob.compareTo(BigDecimal.ONE) > 0) {
					System.out.println("Game (" + gs.showGameKey() + ") move with probability of " + prob + "! - " + moves[i].toString());
				} else if (!moves[i].isCertainty()) {
					certain = false;
					probability = probability * prob.doubleValue();
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

				if (prob.compareTo(BIG_HALF) == 0 && method != MoveMethod.UNAVOIDABLE_GUESS) { // unavoidable guesses have an unreliable probability
					fiftyFiftyGuess++;
					if (state == GameStateModel.STARTED || state == GameStateModel.WON) {
						fiftyFiftyWon++;
					} else {
						fiftyFiftyLost++;
					}
				}
				
				
				// only monitor good guesses (brute force, probability engine, zonal, opening book and hooks)
				if (state == GameStateModel.STARTED || state == GameStateModel.WON) {
					if (!moves[i].isCertainty() ) { 
						gameGuesses++;
						guesses++;
						if (method != MoveMethod.UNAVOIDABLE_GUESS) {  // unavoidable guesses have an inacuurate probability
							fairGuesses++;
							fairness = fairness + 1d;
						}
					}
				} else { // otherwise the guess resulted in a loss
					if (!moves[i].isCertainty()) {
						gameGuesses++;
						guesses++;
						if (method != MoveMethod.UNAVOIDABLE_GUESS) {  // unavoidable guesses have an inacuurate probability
							fairGuesses++;
							fairness = fairness - prob.doubleValue() / (1d - prob.doubleValue());
						}
					}                    
				}

				if (state == GameStateModel.LOST && moves[i].isCertainty()) {
					System.out.println("Game (" + gs.showGameKey() + ") lost on move with probablity = " + prob + " :" + moves[i].toString());
				}

				if (state == GameStateModel.LOST || state == GameStateModel.WON) {
					break play;
				}
			}            
		}

		/*
		if (state == GameStateModel.WON) {
			for (int x=0; x < gs.getWidth(); x++) {
				for (int y=0; y < gs.getHeight(); y++) {
					
					Location l = new Location(x,y);
					int q = gs.query(l);
					if (q == 8) {
						System.out.println(gs.getSeed() + " has an 8");
					}
				}
			}
		}
		*/
		
		if (state == GameStateModel.LOST) {
			losingGuesses = losingGuesses + gameGuesses;
			return LOST;
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
			return WON;
		}


	}


}
