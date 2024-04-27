package minesweeper.solver.bulk;

import java.math.BigDecimal;

import minesweeper.gamestate.GameStateModel;

public class BulkEventController {
	
	private final int gamesToPlay;
	
	private volatile int gamesPlayed = 0;
	
	private volatile int failedToStart = 0;
	private volatile int wins = 0;
	private volatile int guesses = 0;
	private volatile int noGuessWins = 0;
	
	private volatile int totalActions = 0;
	private volatile long total3BV = 0;
	private volatile long total3BVSolved = 0;
	private volatile BigDecimal fairness = BigDecimal.ZERO;
	private volatile int currentWinStreak = 0;
	private volatile int bestWinStreak = 0;
	private volatile int currentMastery = 0;
	private volatile int bestMastery = 0;
	
	private volatile boolean[] mastery = new boolean[100];
	
	//private volatile BulkEventGame event;
	private volatile BulkEventGame finalEvent;
	
	BulkEventController(int gamesToPlay) {
		this.gamesToPlay = gamesToPlay;
	}
	
	
	/**
	 * When the process is finished you can get the final results from here
	 */
	public BulkEventGame getResults() {
		return this.finalEvent;
	}
	

	
	void processGame(BulkRequest request, int index) {
		
		int masteryIndex = request.sequence % 100;
		
		BulkRequestGame game = request.getRequestGames()[index];
		
		gamesPlayed++;
		
		if (game.gs.getGameState() == GameStateModel.WON) {
			wins++;

			if (game.guesses == 0) {
				noGuessWins++;
			}
			
			currentWinStreak++;
			if (currentWinStreak > bestWinStreak) {
				bestWinStreak = currentWinStreak;
			}
			
			// if we lost 100 games ago then mastery is 1 more
			if (!mastery[masteryIndex]) {
				mastery[masteryIndex] = true;
				currentMastery++;
				if (currentMastery > bestMastery) {
					bestMastery = currentMastery;
				}
			}
			
			//double efficiency = 100 * ((double) game.gs.getTotal3BV() / (double) game.gs.getActionCount());

			
		} else {
			
			if (!game.startedOkay) {
				failedToStart++;
			}
			
			currentWinStreak = 0;
			
			// if we won 100 games ago, then mastery is now 1 less
			if (mastery[masteryIndex]) {
				mastery[masteryIndex] = false;
				currentMastery--;
			}
		}

		// accumulate the total actions taken
		totalActions = totalActions + game.gs.getActionCount();
		
		// accumulate 3BV in the game and how much was solved
		total3BV = total3BV + game.gs.getTotal3BV();
		total3BVSolved = total3BVSolved + game.gs.getCleared3BV();

		// accumulate total guesses made
		guesses = guesses + game.guesses;
		
		fairness = fairness.add(game.fairness);
			
	}
	
	BulkEventGame createEvent() {
		
		BulkEventGame event = new BulkEventGame();
		
		event.setGamesToPlay(gamesToPlay);
		event.setGamesPlayed(gamesPlayed);
		event.setGamesWon(wins);
		
		event.setTotalGuesses(guesses);
		event.setNoGuessWins(noGuessWins);
		if (guesses != 0) {
			event.setFairness(fairness.doubleValue() / guesses);
		} else {
			event.setFairness(0);
		}

		event.setMastery(bestMastery);
		event.setWinStreak(bestWinStreak);
		event.setTotalActions(totalActions);
		event.setFailedToStart(failedToStart);
		event.setTotal3BV(total3BV);
		event.setTotal3BVSolved(total3BVSolved);
		
		return event;
		
	}
	

	
}
