package minesweeper.solver.bulk;

public class BulkEventMain {
	
	private int gamesToPlay;
	private int gamesPlayed;
	
	private long timeSoFar;
	private long estimatedTimeLeft;
	
	private BulkEventGame[] events;
	private boolean isFinished = false;
	
	public int getGamesToPlay() {
		return gamesToPlay;
	}
	public int getGamesPlayed() {
		return gamesPlayed;
	}

	protected void setGamesToPlay(int gamesToPlay) {
		this.gamesToPlay = gamesToPlay;
	}
	protected void setGamesPlayed(int gamesPlayed) {
		this.gamesPlayed = gamesPlayed;
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
	
	public BulkEventGame[] getGameEvents() {
		return events;
	}
	protected void setGameEvents(BulkEventGame[] events) {
		this.events = events;
	}
}
