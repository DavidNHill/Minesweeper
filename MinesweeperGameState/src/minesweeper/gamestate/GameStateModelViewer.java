package minesweeper.gamestate;

import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

/**
 * This contains a method to allow the viewer to see where the mines are
 * @author David
 *
 */
abstract public class GameStateModelViewer extends GameStateModel {

    public GameStateModelViewer(GameSettings gameSettings) {
		this(gameSettings, 0);
	}

    public GameStateModelViewer(GameSettings gameSettings, long seed) {
		super(gameSettings, seed);
	}
    
	// can be used by the display to get the mines
    abstract public int privilegedQuery(Location m, boolean showMines);


}
