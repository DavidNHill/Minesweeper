package minesweeper.explorer.main;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import minesweeper.explorer.gamestate.GameStateExplorer;
import minesweeper.explorer.main.Graphics.GraphicsSet;
import minesweeper.explorer.rollout.RolloutController;
import minesweeper.explorer.structure.Board;
import minesweeper.explorer.structure.Expander;
import minesweeper.explorer.structure.LedDigits;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Preferences;
import minesweeper.solver.RolloutGenerator;
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
	@FXML private Button buttonSolve;
	@FXML private Button buttonAnalyse;
	@FXML private Button buttonRollout;
	@FXML private CheckBox checkBoxLockMineCount;
	@FXML private RadioMenuItem rolloutStrong;
	@FXML private RadioMenuItem rolloutWeak;
	
	private TileValuesController tileValueController;
	private GraphicsSet graphicsSet;
	private Expander boardExpander = new Expander(0, 0, 6, Color.BLACK);
	private Board currentBoard;
	private LedDigits minesToFind;
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
		newBoard(9, 9, 10);
	}
	
	@FXML 
	public void newIntermediateBoard() {
		newBoard(16, 16, 40);
	}
	
	@FXML 
	public void newExpertBoard() {
		
		newBoard(30, 16, 99);
	}
	
	@FXML 
	public void newCustomBoard() {
		
		int boardWidth = (int) (boardExpander.getCenterX() / 24);
		int boardHeight = (int) (boardExpander.getCenterY() / 24);
		
		newBoard(boardWidth,boardHeight, 0);
		
	}
	
	@FXML 
	public void rolloutButtonPressed() {
		System.out.println("Rollout button pressed");

		GameStateModel gs = null;
		try {
			gs = GameStateExplorer.build(currentBoard, minesToFind.getValue() + minesPlaced.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Solver solver = new Solver(gs, Preferences.VERY_LARGE_ANALYSIS, true);
		
		Preferences preferences;
		if (rolloutWeak.isSelected()) {
			System.out.println("Weak selected");
			preferences = Preferences.SMALL_ANALYSIS.setTieBreak(false);
		} else {
			preferences = Preferences.SMALL_ANALYSIS;
		}
		
		try {
			RolloutGenerator gen = solver.getRolloutGenerator();
			
	        RolloutController.launch(boardDisplayArea.getScene().getWindow(), gen, preferences);
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	}
	
	@FXML 
	public void analyseButtonPressed() {
		System.out.println("Analyse button pressed");

		currentBoard.setGameInformation(null, 0);
		GameStateModel gs = null;
		try {
			gs = GameStateExplorer.build(currentBoard, minesToFind.getValue() + minesPlaced.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Solver solver = new Solver(gs, Preferences.SMALL_ANALYSIS, true);
		
		try {
			int hash = currentBoard.getHashValue();
			currentBoard.setGameInformation(solver.runTileAnalysis(), hash);
		} catch (Exception e) {
			e.printStackTrace();
			setSolutionLine("Unable to process:" + e.getMessage());
		}
		
		
	}
	
	@FXML 
	public void solveButtonPressed() {
		System.out.println("Solve button pressed");
		
		GameStateModel gs = null;
		try {
			gs = GameStateExplorer.build(currentBoard, minesToFind.getValue() + minesPlaced.getValue());
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
			
			//currentBoard.getChildren().removeAll(indicators);
			//indicators.clear();
			removeIndicators();
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
	
	protected void removeIndicators() {
		
		if (Platform.isFxApplicationThread()) {
			doRemoveIndicators();
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					doRemoveIndicators();
				}
			});
		}

	}
	
	private void doRemoveIndicators() {
		currentBoard.getChildren().removeAll(indicators);
		indicators.clear();
	}
	
	
	private void clearBoard(boolean covered) {
		
		removeIndicators();
		//currentBoard.getChildren().removeAll(indicators);
		//indicators.clear();
		
		if (checkBoxLockMineCount.isSelected()) {
			
		}
		if (checkBoxLockMineCount.isSelected() && covered) {
			minesToFind.setValue(minesToFind.getValue() + minesPlaced.getValue());
		} else {
			minesToFind.setValue(0);
		}
		
		this.currentBoard.clearBoard(covered);
	}
	
	private void newBoard(int width, int height, int mines) {
		
		// tidy up old details
		if (minesPlaced != null) {
			minesPlaced.removeValueListener();
		}
		
		
		// remove current board graphics
		getBoardDisplayArea().getChildren().clear();
		indicators.clear();
		
		// create new board
		currentBoard = new Board(this, width, height);

		currentBoard.clearBoard(true);  // all covered to start with
		checkBoxLockMineCount.setSelected(mines != 0);
	
		boardExpander.setCenterX(width * 24);
		boardExpander.setCenterY(height * 24);
		
		getBoardDisplayArea().getChildren().addAll(currentBoard, boardExpander);

		minesToFind = new LedDigits(3);
		minesToFind.relocate(10, 5);
		minesToFind.setBackground(Explorer.GREY_BACKGROUND);
		getHeader().getChildren().add(minesToFind);
		minesToFind.setValue(mines);
		
		minesPlaced = new LedDigits(3, true);
		minesPlaced.relocate(100, 5);
		minesPlaced.setBackground(Explorer.GREY_BACKGROUND);
		minesPlaced.setValueListener(currentBoard.getMinesPlacedProperty());
		getHeader().getChildren().add(minesPlaced);
		
		messageLine.setText("Build a board");
		solutionLine.setText("");

		Explorer.setSubTitle(width + " x " + height);
		
	}
	
	public void setCurrentBoard(Board board) {
		this.currentBoard = board;
	}
	
	public Board getCurrentBoard() {
		return this.currentBoard;
	}
	
	public int getTotalMines() {
		return 	minesToFind.getValue() + minesPlaced.getValue();
	}
	
	public LedDigits getMinesToFindController() {
		return minesToFind;
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
	
	public boolean mineCountLocked() {
		return checkBoxLockMineCount.isSelected();
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

	public void setButtonsEnabled(boolean enable) {
		
		if (Platform.isFxApplicationThread()) {
			buttonSolve.setDisable(!enable);
			buttonAnalyse.setDisable(!enable);
			buttonRollout.setDisable(!enable);
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					buttonSolve.setDisable(!enable);
					buttonAnalyse.setDisable(!enable);
					buttonRollout.setDisable(!enable);
				}
			});
		}

	}
}
