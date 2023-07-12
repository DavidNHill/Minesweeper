package minesweeper.gamestate.msx;

import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

public class GameStateX extends GameStateModelViewer {

	private ScreenScanner scanner;
	
	public GameStateX(ScreenScanner scanner) {
        super(GameSettings.create(scanner.getColumns(), scanner.getRows(), scanner.getMines()));
        
        this.scanner = scanner;
        
        doAutoComplete = false; // minesweeperX will do this itself
        
	}
	
	
	@Override
	protected void placeFlagHandle(Location m) {
		scanner.flag(m.x, m.y);
		
		// give it time to set the flag
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			scanner.updateField();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	protected boolean clearSquareHitMine(Location m) {

		scanner.clear(m.x, m.y);
		
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			scanner.updateField();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// since the click could have expanded more areas we need to allow for that
		for (int x=0; x < scanner.getColumns(); x++) {
			for (int y=0; y < scanner.getRows(); y++) {
				if (scanner.getValue(x, y) != ScreenScanner.HIDDEN && scanner.getValue(x, y) != ScreenScanner.FLAG) {
					setRevealed(x,y);
				}
				
				// if a flag has been revealed by minesweeperX then place that in the model (this happens at the end of a game)
				if (scanner.getValue(x, y) == ScreenScanner.FLAG && this.query(new Location(x,y)) == HIDDEN) {
					setFlag(x,y);
				}
			}
		}
		
		return scanner.isGameLost();
	}

	@Override
	protected void startHandle(Location m) {
		
	}

	@Override
	protected int queryHandle(int x, int y) {
		
		int value = scanner.getValue(x, y);
		
		if (value == ScreenScanner.HIDDEN) {
			System.out.println("value at (" + x + "," + y + ") is hidden and yet queried!");
		}
		if (value == ScreenScanner.FLAG) {
			System.out.println("value at (" + x + "," + y + ") is a flag and yet queried!");
		}
		
		
		if (value == ScreenScanner.HIDDEN) {
			return GameStateModel.HIDDEN;
		} else if (value == ScreenScanner.EMPTY) {
			return 0;
		} else if (value == ScreenScanner.FLAG) {
			return GameStateModel.HIDDEN;
		} else if (value == ScreenScanner.BOMB) {
			return GameStateModel.MINE;
		} else if (value == ScreenScanner.EXPLODED_BOMB) {
			return GameStateModel.EXPLODED_MINE;
		} else {
			return value;
		}
	}


	@Override
	/**
	 * No privileged access since MinesweeperX can't show it
	 */
	public int privilegedQuery(Location m, boolean showMines) {
		return query(m);
	}

	@Override
	protected boolean clearSurroundHandle(Location m) {
		
		scanner.clearAll(m.x, m.y);
		
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			scanner.updateField();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// since the click could have expanded more areas we need to allow for that
		for (int x=0; x < scanner.getColumns(); x++) {
			for (int y=0; y < scanner.getRows(); y++) {
				if (scanner.getValue(x, y) != ScreenScanner.HIDDEN && scanner.getValue(x, y) != ScreenScanner.FLAG) {
					setRevealed(x,y);
				}
				
				// if a flag has been revealed by minesweeperX then place that in the model (this happens at the end of a game)
				if (scanner.getValue(x, y) == ScreenScanner.FLAG && this.query(new Location(x,y)) == HIDDEN) {
					setFlag(x,y);
				}
			}
		}
        
        return true;
	}


	@Override
	public String showGameKey() {
		return "Minesweeper X";
	}
	
	
}
