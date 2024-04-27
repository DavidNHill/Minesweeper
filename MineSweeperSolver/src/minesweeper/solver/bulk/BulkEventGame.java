package minesweeper.solver.bulk;

public class BulkEventGame {
	
	private int gamesToPlay;
	private int gamesPlayed;
	private int gamesWon;
	private int noGuessWins;
	private int totalActions;
	
	private long total3BV;
	private long total3BVSolved;
	
	private int totalGuesses;
	private double fairness;
	
	private int winStreak;
	private int mastery;
	
	private int failedToStart;   // this is when the game didn't survive the start sequence
	
	
	public int getFailedToStart() {
		return failedToStart;
	}
	public int getGamesToPlay() {
		return gamesToPlay;
	}
	public int getGamesPlayed() {
		return gamesPlayed;
	}
	public int getGamesWon() {
		return gamesWon;
	}
	public int getNoGuessWins() {
		return noGuessWins;
	}
	public int getTotalGuesses() {
		return totalGuesses;
	}
	public double getFairness() {
		return fairness;
	}
	public int getWinStreak() {
		return winStreak;
	}
	public int getMastery() {
		return mastery;
	}
	public int getTotalActions() {
		return totalActions;
	}

	public long getTotal3BV() {
		return total3BV;
	}
	public long getTotal3BVSolved() {
		return total3BVSolved;
	}
	
	protected void setFailedToStart(int failedToStart) {
		this.failedToStart = failedToStart;
	}
	protected void setGamesToPlay(int gamesToPlay) {
		this.gamesToPlay = gamesToPlay;
	}
	protected void setGamesPlayed(int gamesPlayed) {
		this.gamesPlayed = gamesPlayed;
	}
	protected void setGamesWon(int gamesWon) {
		this.gamesWon = gamesWon;
	}
	protected void setNoGuessWins(int noGuessWins) {
		this.noGuessWins = noGuessWins;
	}
	protected void setTotalGuesses(int totalGuesses) {
		this.totalGuesses = totalGuesses;
	}
	protected void setFairness(double fairness) {
		this.fairness = fairness;
	}
	protected void setWinStreak(int winStreak) {
		this.winStreak = winStreak;
	}
	protected void setMastery(int mastery) {
		this.mastery = mastery;
	}
	protected void setTotalActions(int actions) {
		this.totalActions = actions;
	}
	protected void setTotal3BV(long total3bv) {
		total3BV = total3bv;
	}
	protected void setTotal3BVSolved(long total3bvSolved) {
		total3BVSolved = total3bvSolved;
	}

	
}
