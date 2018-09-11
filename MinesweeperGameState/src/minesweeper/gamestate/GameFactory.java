package minesweeper.gamestate;

import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;

public class GameFactory {
	
	private GameFactory() {
		
	}

	public static GameStateModelViewer create(GameType type, GameSettings settings, long gameSeed) {
		
		GameStateModelViewer gsm;
		
		if (type == GameType.EASY) {
			if (gameSeed != 0) {
				gsm = new GameStateEasy(settings, gameSeed);
			} else {
				gsm = new GameStateEasy(settings);
			}
		} else if (type == GameType.STANDARD) {
			if (gameSeed != 0) {
				gsm = new GameStateStandard(settings, gameSeed);
			} else {
				gsm = new GameStateStandard(settings);
			}
		} else if (type == GameType.HARD) {
			if (gameSeed != 0) {
				gsm = new GameStateHard(settings, gameSeed);
			} else {
				gsm = new GameStateHard(settings);
			}
		} else {
			throw new RuntimeException("Unexpected values in Game generation");
		}
		
		return gsm;
		
	}
	
}
