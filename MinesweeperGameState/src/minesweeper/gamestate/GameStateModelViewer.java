package minesweeper.gamestate;

/**
 * This contains a method to allow the viewer to see where the mines are
 * @author David
 *
 */
abstract public class GameStateModelViewer extends GameStateModel {

    public GameStateModelViewer(int x, int y, int mines) {
		super(x, y, mines);
	}

	// can be used by the display to get the mines
    abstract public int privilegedQuery(Location m, boolean showMines);


}
