package minesweeper.explorer.main;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import minesweeper.explorer.gamestate.GameStateExplorer;
import minesweeper.explorer.main.Graphics.GraphicsSet;
import minesweeper.explorer.structure.Board;
import minesweeper.explorer.structure.Expander;
import minesweeper.explorer.structure.LedDigits;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Preferences;
import minesweeper.solver.Solver;
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class MainScreenController {
	
	public final static DecimalFormat EXPONENT_DISPLAY = new DecimalFormat("##0.###E0");
	public final static DecimalFormat NUMBER_DISPLAY = new DecimalFormat("#,##0");
	
	private class Indicator extends Rectangle {
		
		public Indicator(Action action) {
			super(action.x * 24, action.y * 24, 24d, 24d);

    		setMouseTransparent(true);    
			
			if (action.getAction() == Action.CLEAR) {
				this.setFill(Color.GREEN);
				if (!action.isCertainty()) {
					this.setStroke(Color.DARKRED);
					this.setStrokeWidth(5);
					this.setStrokeType(StrokeType.INSIDE);
				}
				this.setOpacity(0.5d);
			} else if (action.getAction() == Action.FLAG) {
				this.setFill(Color.RED);
				this.setOpacity(0.5d);
			} if (action.getAction() == Action.CLEARALL) {
				this.setFill(Color.BLUE);
				this.setOpacity(0.5d);
			}
			
		}
		
		public Indicator(Location action, Paint colour) {
			super(action.x * 24, action.y * 24, 24d, 24d);

    		setMouseTransparent(true);    
			
    		this.setFill(colour);
    		this.setOpacity(0.5d);
		}
	}
	
	@FXML private AnchorPane boardDisplayArea;
	@FXML private AnchorPane header;
	@FXML private Label messageLine;
	@FXML private Label solutionLine;
	@FXML private Button buttonExplore;
	@FXML private Button buttonCheckFlags;
	
	private TileValuesController tileValueController;
	private GraphicsSet graphicsSet;
	private Expander boardExpander = new Expander(0, 0, 6, Color.BLACK);
	private Board currentBoard;
	private LedDigits mineCount;
	private LedDigits minesPlaced;
	private List<Indicator> indicators = new ArrayList<>();

	@FXML
	void initialize() {
		System.out.println("Entered Main Screen Controller initialize method");
		
		tileValueController = TileValuesController.launch(null);

	}
	
	@FXML
	public void clearBoardToZero() {
		
		System.out.println("At clear board to zero");
		
		clearBoard(false);
	}
	
	@FXML
	public void clearBoardToHidden() {
		
		System.out.println("At clear board to hidden");
		
		clearBoard(true);
	}
	
	@FXML 
	public void newBeginnerBoard() {
		newBoard(9,9);
	}
	
	@FXML 
	public void newIntermediateBoard() {
		newBoard(16,16);
	}
	
	@FXML 
	public void newExpertBoard() {
		
		newBoard(30,16);
	}
	
	@FXML 
	public void newCustomBoard() {
		
		int boardWidth = (int) (boardExpander.getCenterX() / 24);
		int boardHeight = (int) (boardExpander.getCenterY() / 24);
		
		newBoard(boardWidth,boardHeight);
		
	}
	
	@FXML 
	public void checkFlagsButtonPressed() {
		System.out.println("Check flags button pressed");

		currentBoard.setGameInformation(null);
		GameStateModel gs = null;
		try {
			gs = GameStateExplorer.build(currentBoard, mineCount.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Solver solver = new Solver(gs, Preferences.VERY_LARGE_ANALYSIS, true);
		try {
			currentBoard.setGameInformation(solver.runProbabiltyEngine());
		} catch (Exception e) {
			e.printStackTrace();
			setSolutionLine("Unable to process:" + e.getMessage());
		}
		
		
	}
	
	@FXML 
	public void exploreButtonPressed() {
		System.out.println("Explore button pressed");
		
		GameStateModel gs = null;
		try {
			gs = GameStateExplorer.build(currentBoard, mineCount.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Solver solver = new Solver(gs, Preferences.VERY_LARGE_ANALYSIS, true);
		
		solver.start();
		
		Action[] actions = solver.getResult();
		
		if (actions.length == 0) {
			messageLine.setText("No suggestion returned by the solver");
		} else {
			Action a = actions[0];
			messageLine.setText(a.asString());
			
			currentBoard.getChildren().removeAll(indicators);
			indicators.clear();
			for (Action action: actions) {
				indicators.add(new Indicator(action));
			}
			
	    	List<EvaluatedLocation> els = solver.getEvaluatedLocations();
	    	if (els != null) {
	        	for (EvaluatedLocation el: els) {
	        		
	        		// don't show evaluated positions which are actually chosen to be played
	        		boolean ignore = false;
	        		for (Action action: actions) {
	        			if (el.equals(action)) {
	        				ignore = true;
	        			}
	        		}
	        		
	        		if (!ignore) {
	        			indicators.add(new Indicator(el, Color.ORANGE));
	        		}

	        		
	        	}    		
	    	}
			
	    	Area dead = solver.getDeadLocations();
	    	if (dead != null) {
	        	for (Location loc: dead.getLocations()) {
	        		
	        		// don't show evaluated positions which are actually chosen to be played
	        		boolean ignore = false;
	        		for (Action action: actions) {
	        			if (loc.equals(action)) {
	        				ignore = true;
	        			}
	        		}
	        		
	        		if (!ignore) {
	        			indicators.add(new Indicator(loc, Color.BLACK));
	        		}

	        	}    		
	    	}
			
			
			currentBoard.getChildren().addAll(indicators);
		}
		
	}
	
	private void clearBoard(boolean covered) {
		
		currentBoard.getChildren().removeAll(indicators);
		indicators.clear();
		
		this.currentBoard.clearBoard(covered);
	}
	
	private void newBoard(int width, int height) {
		
		// tidy up old details
		if (minesPlaced != null) {
			minesPlaced.removeValueListener();
		}
		
		
		// remove current board graphics
		getBoardDisplayArea().getChildren().clear();
		indicators.clear();
		
		// create new board
		currentBoard = new Board(this, width, height);

		boardExpander.setCenterX(width * 24);
		boardExpander.setCenterY(height * 24);
		
		getBoardDisplayArea().getChildren().addAll(currentBoard, boardExpander);

		mineCount = new LedDigits(3);
		mineCount.relocate(10, 5);
		mineCount.setBackground(Explorer.GREY_BACKGROUND);
		getHeader().getChildren().add(mineCount);
		
		minesPlaced = new LedDigits(3, true);
		minesPlaced.relocate(100, 5);
		minesPlaced.setBackground(Explorer.GREY_BACKGROUND);
		minesPlaced.setValueListener(currentBoard.getMinesPlacedProperty());
		getHeader().getChildren().add(minesPlaced);
		
		messageLine.setText("Build a board");
		solutionLine.setText("");
		
	}
	
	public void setCurrentBoard(Board board) {
		this.currentBoard = board;
	}
	
	public Board getCurrentBoard() {
		return this.currentBoard;
	}
	
	public int getGameMines() {
		return mineCount.getValue();
	}
	
	public void setGraphicsSet(GraphicsSet graphicsSet) {
		this.graphicsSet = graphicsSet;
	}
	
	public GraphicsSet getGraphicsSet() {
		return this.graphicsSet;
	}
	
	public AnchorPane getHeader() {
		return header;
	}
	
	public AnchorPane getBoardDisplayArea() {
		return boardDisplayArea;
	}

	public TileValuesController getTileValueController() {
		return this.tileValueController;
	}
	
	public void setSolutionLine(String text) {
		
		if (Platform.isFxApplicationThread()) {
			solutionLine.setText(text);
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					solutionLine.setText(text);
				}
			});
		}

	}

	
}
