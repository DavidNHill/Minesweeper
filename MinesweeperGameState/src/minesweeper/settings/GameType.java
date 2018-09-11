package minesweeper.settings;

public enum GameType {

	/**
	 * Game starts with a guaranteed zero
	 */
	EASY("Easy"),
	
	/**
	 * Game starts with a guaranteed safe position (which could be a zero)
	 */	
	STANDARD("Standard"),
	
	/**
	 * No guaranteed safe start (could be a mine)
	 */
	HARD("Hard");
	
	
	public String name;
	
	private GameType(String name) {
		this.name = name;
	}
	
	
}
