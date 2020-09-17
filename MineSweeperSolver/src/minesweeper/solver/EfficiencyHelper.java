package minesweeper.solver;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import minesweeper.gamestate.MoveMethod;
import minesweeper.solver.constructs.ChordLocation;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class EfficiencyHelper {
	
	private BoardState board;
	private WitnessWeb wholeEdge;
	private List<Action> actions;
	
	public EfficiencyHelper(BoardState board, WitnessWeb wholeEdge, List<Action> actions)  {
		
		this.board = board;
		this.actions = actions;
		this.wholeEdge = wholeEdge;
		
	}

	
	public List<Action> process() {
		
		if (actions.size() < 2) {
			return actions;
		}
		
		List<Action> result = new ArrayList<>();
		List<ChordLocation> chordLocations = new ArrayList<>();
		
		boolean[][] processed = new boolean[board.getGameWidth()][board.getGameHeight()];
		
		for (Location loc: board.getAllLivingWitnesses()) {
			
			// if the witness is satisfied how many clicks will it take to clear the area by chording
			if (board.getWitnessValue(loc) == board.countAdjacentConfirmedFlags(loc)) {
				int benefit = board.countAdjacentUnrevealed(loc);
				int cost = board.getWitnessValue(loc) - board.countAdjacentFlagsOnBoard(loc);
				
				if (benefit - cost > 1) {
					chordLocations.add(new ChordLocation(loc.x, loc.y, benefit - cost));
				}
			}

		}

		// sort the most beneficial chords to the top
		Collections.sort(chordLocations, ChordLocation.SORT_BY_BENEFIT_DESC);
		
		for (ChordLocation cl: chordLocations) {
			
			for (Location l: board.getAdjacentSquaresIterable(cl)) {
				// flag not yet on board
				if (!processed[l.x][l.y] && board.isConfirmedFlag(l) && !board.isFlagOnBoard(l)) {
					result.add(new Action(l, Action.FLAG, MoveMethod.TRIVIAL, "Place flag", BigDecimal.ONE, 0));
				}
				// flag on board in error
				if (!processed[l.x][l.y] && !board.isConfirmedFlag(l) && board.isFlagOnBoard(l)) {
					result.add(new Action(l, Action.FLAG, MoveMethod.CORRECTION, "Remove flag", BigDecimal.ONE, 0));
				}
				processed[l.x][l.y] = true;
			}
			// now add the clear all
			result.add(new Action(cl, Action.CLEARALL, MoveMethod.TRIVIAL, "Clear All", BigDecimal.ONE, 1));	
				
			break;

		}
		
		if (result.isEmpty()) {
			
			Action bestAction = null;
			BigInteger highest = BigInteger.ZERO;
			int currentReward = 0;
			
			for (Action act: actions) {

				if (act.getAction() == Action.CLEAR) {
					int adjMines = board.countAdjacentConfirmedFlags(act);
					int adjFlags = board.countAdjacentFlagsOnBoard(act);
					int hidden = board.countAdjacentUnrevealed(act);
					
					int reward = hidden - adjMines + adjFlags;  // tiles adjacent - ones which are mines - mines which aren't flagged yet
					
					SolutionCounter counter = board.getSolver().validateLocationUsingSolutionCounter(wholeEdge, act, adjMines, Area.EMPTY_AREA);
					
					BigInteger current = counter.getSolutionCount().multiply(BigInteger.valueOf(reward));
					
					if (current.compareTo(highest) > 0) {
					//if (reward > currentReward) {
						highest = current;
						bestAction = act;
						currentReward = reward;
					}
					
				}
				
			}		
			
			if (bestAction != null) {
				result.add(bestAction);
			}

		}
		
		// now add the the actions that haven't been resolved by a chord play
		/*
		for (Action act: actions) {
			if (!processed[act.x][act.y]) {
				processed[act.x][act.y] = true;
				result.add(act);
			}
		}
		*/
		
		if (result.isEmpty()) {
			return actions;
		} else {
			return result;
		}
		

	}
	
	
	
}
