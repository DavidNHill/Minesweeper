package minesweeper.solver.bulk;

public class BulkEvent {
	
	private int gamesToPlay;
	private int gamesPlayed;
	private int gamesWon;
	private int noGuessWins;
	
	private int totalGuesses;
	private double fairness;
	
	private int winStreak;
	private int mastery;
	
	private long timeSoFar;
	private long estimatedTimeLeft;
	
	private boolean isFinished = false;
	
	
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
	public long getTimeSoFar() {
		return timeSoFar;
	}
	public long getEstimatedTimeLeft() {
		return estimatedTimeLeft;
	}
	protected void setTimeSoFar(long timeSoFar) {
		this.timeSoFar = timeSoFar;
	}
	protected void setEstimatedTimeLeft(long estimatedTimeLeft) {
		this.estimatedTimeLeft = estimatedTimeLeft;
	}
	public boolean isFinished() {
		return isFinished;
	}
	protected void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}
	
}
