package minesweeper.settings;

public enum GameType {

	/**
	 * Game starts with a guaranteed zero
	 */
	EASY("Zero"),
	
	/**
	 * Game starts with a guaranteed safe position (which could be a zero)
	 */	
	STANDARD("Safe"),
	
	/**
	 * No guaranteed safe start (could be a mine)
	 */
	HARD("Unsafe");
	
	
	public final String name;
	
	private GameType(String name) {
		this.name = name;
	}
	
	
}
