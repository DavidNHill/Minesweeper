package minesweeperbulk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import minesweeper.gamestate.GameStateModel;
import minesweeper.settings.GameSettings;
import minesweeper.solver.bulk.GamePreListener;
import minesweeper.structure.Action;

public class RandomGuesser extends GamePreListener {

	final private GameSettings settings;
	final private int border = 1;
	
	final List<Action> interior = new ArrayList<>();
	final Action[][] board;
	
	private int eights = 0;
	private int clicks = 0;
	private int played = 0;
	
	public RandomGuesser(GameSettings settings) {
		
		this.settings = settings;
		
		board = new Action[settings.width][settings.height];
		
		for (int i=border; i < settings.width - border; i++) {
			for (int j=border; j < settings.height - border; j++) {
				Action a = new Action(i,j, Action.CLEAR);
				interior.add(a);
				board[i][j] = a;
				
			}
		}

	}
	
	
	@Override
	public void preAction(GameStateModel game) {
		
		played++;
		
		// tiles we can guess shuffled
		List<Action> available = new ArrayList<>(interior);
		Collections.shuffle(available);
		
		boolean[][] processed = new boolean[settings.width][settings.height];
		
		// first guess
		Action guess = available.get(0);
		
		// while we have moves and the game hasn't blasted
		while (true) {
			
			clicks++;
			game.doAction(guess);
			
			// if we've lost stop
			if (game.getGameState() == GameStateModel.LOST) {
				break;
			}
			
			if (game.query(guess) == 8) {
				eights++;
			}
			
			// remove guesses near cleared tiles
			for (int i=border; i < settings.width - border; i++) {
				for (int j=border; j < settings.height - border; j++) {
					if (!processed[i][j]) {
						if (game.query(board[i][j]) != GameStateModel.HIDDEN) {
							
							//System.out.println(i + " " + j + " is not hidden");
							
							processed[i][j] = true;
							
							int offset;
							if (game.query(guess) < 3) {
								offset = 2;
							} else {
								offset = 1;
							}
							
							// remove tiles adjacent to an open area
							Iterator<Action> iterator = available.iterator();
							while (iterator.hasNext()) {
								Action a = iterator.next();
								if (distance(a.x, a.y, i, j) <= offset) {
									iterator.remove();
								}
							}
						}
					}
				
				}
			}
			
			if (available.isEmpty()) {
				//System.out.println("No guesses left");
				break;
			}
			
			// next guess
			guess = available.get(0);
			
		}
		
	} 

	private int distance(int x1, int y1, int x2, int y2) {
		
		int dx = Math.abs(x1 - x2);
		int dy = Math.abs(y1 - y2);
		
		return Math.max(dx,  dy);
		
	}
	
	
	public void displayTable() {
		
		System.out.println(eights + " eights out of " + played + " played and " + clicks + " clicks");
	
	}
	
}
