/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bulkCompare;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.bulk.BulkEventGame;
import minesweeper.solver.bulk.BulkEventMain;
import minesweeper.solver.bulk.BulkListener;
import minesweeper.solver.bulk.BulkPlayer;
import minesweeper.solver.bulk.GamePostListener;
import minesweeper.solver.bulk.StaticCounter;
import minesweeper.solver.bulk.StaticCounter.SCType;
import minesweeper.solver.settings.PlayStyle;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SettingsFactory.Setting;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.solver.utility.Timer;
import minesweeper.structure.Action;

/**
 *
 * @author David
 */
public class BulkCompare {

	
	private static final DecimalFormat MASK = new DecimalFormat("#0.000");
	private static final DecimalFormat MASK5DP = new DecimalFormat("#0.000000");
	
	public static void main(String[] args) {


		// pick a random seed or override with a previously used seed to play the same sequence of games again.
		long seed = (new Random()).nextInt();

		//seed = 1373631748;
		seed = 462440595;
		//seed = -60442780;   // expert 10,000,000 run
		
		System.out.println("Seed is " + seed);
		Random seeder = new Random(seed);
		
		GameSettings gameSettings = GameSettings.EXPERT;
		//GameSettings gameSettings = GameSettings.create(14, 12, 64);
		
		SolverSettings settings0 = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS);
		settings0.setSingleThread(true);
		//settings.setGuessMethod(GuessMethod.RECURSIVE_SAFETY);
		//settings.setRecursiveSafetyDepth(1);
		//settings.setStartLocation(new Location(2,2));
		//settings.set5050Check(false);
		//settings.setEarly5050Check(true);
		//settings.setTieBreak(false);
		//settings.setTestMode(true);
		//settings.setLongTermSafety(false);
		//settings.setProgressContribution(new BigDecimal("0.052"));
		//settings.setSafetyWeights(1, 0);
		
		SolverSettings settings1 = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS);
		settings1.setSingleThread(true);
		
		// can have an arbitrary number of settings in the array
		SolverSettings[] solverSettings = new SolverSettings[] {settings0, settings1};
		
		final long bulkSeed = seed;
		BulkPlayer controller = new BulkPlayer(seeder, 20000, GameType.STANDARD, gameSettings, solverSettings, 10, 10000);
		controller.setPlayStyle(PlayStyle.NO_FLAG);
		
		// this is executed before the game is passed to the solver
		// Optionally a listener for each solver settings.  
		//controller.registerPreGameListener(0, new StartStrategy(twoCornerStart(gameSettings), 5));
		controller.registerPreGameListener(1, new StartStrategy(fourCornerStart(gameSettings), 5));
		
		GamePostListener monitor = new CompareMonitor();
		controller.registerPostGameListener(monitor);
		
		// this acts as a heart beat for the processing
		controller.registerEventListener(new BulkListener() {
			@Override
			public void intervalAction(BulkEventMain mainEvent) {
				
				BulkEventGame event0 = mainEvent.getGameEvents()[0];  // get the statistics for solver settings 0
				BulkEventGame event1 = mainEvent.getGameEvents()[1];  // get the statistics for solver settings 1
				
				for (int i=0; i < mainEvent.getGameEvents().length; i++) {
					String stats = statisticsToString(mainEvent, i);
					System.out.println("Seed: " + bulkSeed + " Run: " + i + " ==> " + stats);
				}

			}
			
		});
	
		controller.run();
		// 4394503940621334  3495601381446703  2878438628482118
		
		// 3bv 5 ==> 3507948847220847 2378685257559362 732083917567661 3393825076821824 1423288336267551
		{
		BulkEventMain mainEvent = controller.getResults();
		BulkEventGame event = mainEvent.getGameEvents()[0];
		
		double p = (double) event.getGamesWon() / (double) event.getGamesPlayed();
		double err = Math.sqrt(p * ( 1- p) / (double) event.getGamesPlayed()) * 1.9599d;
		
		int apw;
		if (event.getGamesWon() != 0) {
			apw = event.getTotalActions() / event.getGamesWon();
		} else {
			apw = 0;
		}
		
		double bpw;
		if (event.getGamesWon() != 0) {
			bpw = (double) event.getTotal3BVSolved() / (double) event.getGamesWon();
		} else {
			bpw = 0;
		}
		
		double average3BV = (double) event.getTotal3BV() / (double) event.getGamesPlayed();

		System.out.println("Seed: " + bulkSeed + " ==> Board " + gameSettings + " ==> Played " + event.getGamesPlayed() + ", failed to start " + event.getFailedToStart() + ", won " + event.getGamesWon() + 
				", without guessing " + event.getNoGuessWins() + ", guesses " + event.getTotalGuesses() + ", actions " + event.getTotalActions() + ", apw " + apw + 
				", avg 3BV " + MASK.format(average3BV) + ", Solved 3BV/win " + MASK.format(bpw) + 
				", fairness " + MASK5DP.format(event.getFairness()) + ", win streak " + event.getWinStreak() + ", mastery " + event.getMastery() + 
				", win percentage " + MASK.format(p * 100) + " +/- " + MASK.format(err * 100) + ", Duration " + Timer.humanReadable(mainEvent.getTimeSoFar()) );
		
		}
		
		monitor.postResults();
		//random.displayTable();

	}

	// expand the statistics
	static private String statisticsToString(BulkEventMain mainEvent, int index) {
		
		BulkEventGame event = mainEvent.getGameEvents()[index];
		
		double p = (double) event.getGamesWon() / (double) event.getGamesPlayed();
		double err = Math.sqrt(p * ( 1- p) / (double) event.getGamesPlayed()) * 1.9599d;
		
		return "Played " + event.getGamesPlayed() + " of " + event.getGamesToPlay() + ", failed to start " + event.getFailedToStart() + ", won " + event.getGamesWon() +
				", without guessing " + event.getNoGuessWins() + ", guesses " + event.getTotalGuesses() + ", actions " + event.getTotalActions() + ", solved 3BV " + event.getTotal3BVSolved() +
				", fairness " + MASK5DP.format(event.getFairness()) + ", win streak " + event.getWinStreak() + ", mastery " + event.getMastery() + 
				", win percentage " + MASK.format(p * 100) + " +/- " + MASK.format(err * 100) + ", Time left " + Timer.humanReadable(mainEvent.getEstimatedTimeLeft());
		
	}
	
	static private List<Action> twoCornerStart(GameSettings gameSettings) {
		
		List<Action> preactions = new ArrayList<>();
		preactions.add(new Action(0, 0, Action.CLEAR));
		preactions.add(new Action(0, gameSettings.height - 1, Action.CLEAR));
		//preactions.add(new Action(gameSettings.width - 1, 0, Action.CLEAR));
		//preactions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, Action.CLEAR));
		
		return preactions;
		
	}
	
	static private List<Action> threeCornerStart(GameSettings gameSettings) {
		
		List<Action> preactions = new ArrayList<>();
		preactions.add(new Action(0, 0, Action.CLEAR));
		preactions.add(new Action(0, gameSettings.height - 1, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, 0, Action.CLEAR));
		//preactions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, Action.CLEAR));
		
		return preactions;
		
	}
	
	static private List<Action> fourCornerStart(GameSettings gameSettings) {
		
		List<Action> preactions = new ArrayList<>();
		preactions.add(new Action(0, 0, Action.CLEAR));
		preactions.add(new Action(0, gameSettings.height - 1, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, 0, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, Action.CLEAR));
		
		return preactions;
		
	}
	
	static private List<Action> middle4CornerStart(GameSettings gameSettings) {
		
		List<Action> preactions = new ArrayList<>();
		preactions.add(new Action(gameSettings.width / 2, gameSettings.height / 2, Action.CLEAR));
		preactions.add(new Action(0, 0, Action.CLEAR));
		preactions.add(new Action(0, gameSettings.height - 1, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, 0, Action.CLEAR));
		preactions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, Action.CLEAR));
		
		return preactions;
		
	}

	static private List<Action> flagClickChord(GameSettings gameSettings) {
		
		List<Action> preactions = new ArrayList<>();
		preactions.add(new Action(1, 3, Action.FLAG));
		preactions.add(new Action(1, 2, Action.CLEAR));
		preactions.add(new Action(1, 2, Action.CLEARALL));
		preactions.add(new Action(1, 3, Action.FLAG));
		
		return preactions;
		
	}

	
	
}
