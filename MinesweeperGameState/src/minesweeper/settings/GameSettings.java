package minesweeper.settings;

import java.util.Arrays;
import java.util.List;

public class GameSettings {

	/**
	 * 9x9/10
	 */
	final static public GameSettings BEGINNER = new GameSettings(9, 9, 10, "Beginner");
	/**
	 * 16x6/40
	 */
	final static public GameSettings ADVANCED = new GameSettings(16, 16, 40, "Advanced");
	/**
	 * 30x16/99
	 */
	final static public GameSettings EXPERT = new GameSettings(30, 16, 99, "Expert");
	
	final static private List<GameSettings> standardSettings = Arrays.asList(BEGINNER, ADVANCED, EXPERT);
	
	final public int width;
	final public int height;
	final public int mines;
	final public String name;
	
	private GameSettings(int width, int height, int mines) {
		this(width, height, mines, "Custom");
	}
	
	private GameSettings(int width, int height, int mines, String name) {
		this.width = width;
		this.height = height;
		this.mines = mines;
		this.name = name;
	}	
	
	public static GameSettings create(int width, int height, int mines) {
		
		for (GameSettings game: standardSettings) {
			if (game.width == width && game.height == height && game.mines == mines) {
				return game;
			}
		}
		
		return new GameSettings(width, height, mines, "Custom");
		
	}
	
	public String description() {
		
		return name + " (" + width + "," + height + "," + mines + ")";
		
	}
	
}
