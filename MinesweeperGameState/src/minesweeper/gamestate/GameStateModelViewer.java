package minesweeper.gamestate;

/**
 * This contains a method to allow the viewer to see where the mines are
 * @author David
 *
 */
abstract public class GameStateModelViewer extends GameStateModel {

    public GameStateModelViewer(int x, int y, int mines) {
		this(x, y, mines, 0);
	}

    public GameStateModelViewer(int x, int y, int mines, long seed) {
		super(x, y, mines, seed);
	}
    
	// can be used by the display to get the mines
    abstract public int privilegedQuery(Location m, boolean showMines);


}
