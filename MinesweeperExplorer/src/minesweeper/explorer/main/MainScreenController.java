package minesweeper.explorer.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import minesweeper.explorer.busy.BusyController;
import minesweeper.explorer.busy.ParallelTask;
import minesweeper.explorer.gamestate.GameStateExplorer;
import minesweeper.explorer.main.Graphics.GraphicsSet;
import minesweeper.explorer.rollout.RolloutController;
import minesweeper.explorer.structure.Board;
import minesweeper.explorer.structure.Expander;
import minesweeper.explorer.structure.LedDigits;
import minesweeper.explorer.structure.Tile;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.RolloutGenerator;
import minesweeper.solver.Solver;
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.solver.settings.PlayStyle;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SettingsFactory.Setting;
import minesweeper.solver.settings.SolverSettings.GuessMethod;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.solver.utility.ProgressMonitor;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

public class MainScreenController {
	
	public final static DecimalFormat EXPONENT_DISPLAY = new DecimalFormat("##0.###E0");
	public final static DecimalFormat NUMBER_DISPLAY = new DecimalFormat("#,##0");
	
	private class Indicator extends Rectangle {
		
		public Indicator(Action action) {
			super(action.x * graphicsSet.getSize(), action.y * graphicsSet.getSize(), graphicsSet.getSize(), graphicsSet.getSize());

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
			super(action.x * graphicsSet.getSize(), action.y * graphicsSet.getSize(), graphicsSet.getSize(), graphicsSet.getSize());

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
	@FXML private RadioMenuItem rollout4000;
	@FXML private RadioMenuItem rollout400;
	@FXML private RadioMenuItem rollout40;
	@FXML private RadioMenuItem rolloutNoBF;
	
	@FXML private RadioMenuItem tileSize12;
	@FXML private RadioMenuItem tileSize16;
	@FXML private RadioMenuItem tileSize24;
	@FXML private RadioMenuItem tileSize32;
	
	@FXML private RadioMenuItem secondarySafetyProgress;
	@FXML private RadioMenuItem safetyProgress;
	@FXML private RadioMenuItem recursiveSafety;
	
	@FXML private CheckMenuItem useBruteForce;
	@FXML private CheckMenuItem useLongTermSafety;
	@FXML private CheckMenuItem use5050Detection;
	@FXML private CheckMenuItem useTestMode;
	
	@FXML private CheckMenuItem useLongTermSafetyRollout;
	@FXML private CheckMenuItem useTestModeRollout;
	
	private TileValuesController tileValueController;
	private Graphics graphics;
	private GraphicsSet graphicsSet;
	private Expander boardExpander = new Expander(0, 0, 6, 24, Color.BLACK);
	private Board currentBoard;
	private LedDigits minesToFind;
	private LedDigits minesPlaced;
	private List<Indicator> indicators = new ArrayList<>();
    private FileChooser fileChooser = new FileChooser();
    private File fileSelected = null;
    private RolloutController rolloutController;
    
    
	@FXML
	void initialize() {
		System.out.println("Entered Main Screen Controller initialize method");
		
        // set-up the filechooser
    	ExtensionFilter ef1 = new ExtensionFilter("All files", "*.*");
    	ExtensionFilter ef2 = new ExtensionFilter("Minesweeper board", "*.mine");
    	//ExtensionFilter ef3 = new ExtensionFilter("Minesweeper board", "*.board");
    	//ExtensionFilter ef4 = new ExtensionFilter("Minesweeper board", "*.mbf");
    	fileChooser.getExtensionFilters().addAll(ef1, ef2);
    	fileChooser.setSelectedExtensionFilter(ef2);        
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
		
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
	public void resizeBoard() {
		
		System.out.println("Resizing the board");
		
		if (tileSize12.isSelected()) {
			this.graphicsSet = new Graphics().getGraphicsSet(12);

		} else if (tileSize16.isSelected()) {
			this.graphicsSet = new Graphics().getGraphicsSet(16);

		} else if (tileSize24.isSelected()) {
			this.graphicsSet = new Graphics().getGraphicsSet(24);

		} else {
			this.graphicsSet = new Graphics().getGraphicsSet(32);
		}
		
		currentBoard.resizeBoard(graphicsSet);
		
		//remove, recreate and add the board expander
		getBoardDisplayArea().getChildren().remove(boardExpander);
		boardExpander = new Expander(0, 0, 6, graphicsSet.getSize(), Color.BLACK);
		boardExpander.setCenterX(currentBoard.getGameWidth() * graphicsSet.getSize());
		boardExpander.setCenterY(currentBoard.getGameHeight() * graphicsSet.getSize());
		getBoardDisplayArea().getChildren().add(boardExpander);
		
		// remove all the indicators
		removeIndicators();
		
	}
	
	@FXML 
	public void loadBoard() {
		
    	fileChooser.setTitle("Open game to analyse");
    	if (fileSelected != null) {
    		fileChooser.setInitialDirectory(fileSelected.getParentFile());
    	}
    	fileSelected = fileChooser.showOpenDialog(boardDisplayArea.getScene().getWindow());
    	
    	if (fileSelected != null) {
    		try {
				loadFromFile(fileSelected);
			} catch (Exception e) {
				setSolutionLine("Unable to load file: " + e.getMessage());
				e.printStackTrace();
			}
    	}
		
	}
	
	@FXML 
	public void saveBoard() {
		
    	fileChooser.setTitle("Save board position");
    	if (fileSelected != null) {
    		fileChooser.setInitialDirectory(fileSelected.getParentFile());
    	}
    	
     	fileSelected = fileChooser.showSaveDialog(boardDisplayArea.getScene().getWindow());

     	try {
			saveToFile(fileSelected);
		} catch (Exception e) {
			System.out.println("Error writing to output file");
			e.printStackTrace();
		}
		
	}
	
	@FXML 
	public void newCustomBoard() {
		
		int boardWidth = (int) (boardExpander.getCenterX() / this.graphicsSet.getSize());
		int boardHeight = (int) (boardExpander.getCenterY() / this.graphicsSet.getSize());
		
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

		int depth = 2;
		GuessMethod guessMethod;
		if (secondarySafetyProgress.isSelected()) {
			guessMethod = GuessMethod.SECONDARY_SAFETY_PROGRESS;
		} else if (recursiveSafety.isSelected()) {
			guessMethod = GuessMethod.RECURSIVE_SAFETY;
		} else {
			guessMethod = GuessMethod.SAFETY_PROGRESS;
		}
		
		SolverSettings settings;
		if (rolloutNoBF.isSelected()) {
			settings = SettingsFactory.GetSettings(Setting.NO_BRUTE_FORCE).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);
		} else if (rollout40.isSelected()) {
			settings = SettingsFactory.GetSettings(Setting.TINY_ANALYSIS).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);
		} else if (rollout400.isSelected()) {
			settings = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);
		} else {
			settings = SettingsFactory.GetSettings(Setting.LARGE_ANALYSIS).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);
		}
		
		settings.setLongTermSafety(useLongTermSafetyRollout.isSelected());
		settings.setTestMode(useTestModeRollout.isSelected());
		
		Solver solver = new Solver(gs, settings, true);
		
		try {
			RolloutGenerator gen = solver.getRolloutGenerator();
			
			if (rolloutController == null) {
		        rolloutController = RolloutController.launch(boardDisplayArea.getScene().getWindow(), gen, settings);
			}
			rolloutController.show(gen, settings);
			
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

		SolverSettings settings = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS);
		Solver solver = new Solver(gs, settings, false);
		solver.setPlayStyle(PlayStyle.NO_FLAG);
		ProgressMonitor pm = new ProgressMonitor();
		
		// run this task in parallel while locking the screen against any other actions
		ParallelTask<Boolean> pt = new ParallelTask<Boolean>() {
			@Override
			public void doExecute() {
				
				try {
					int hash = currentBoard.getHashValue();
					currentBoard.setGameInformation(solver.runTileAnalysis(pm), hash);
				} catch (Exception e) {
					e.printStackTrace();
					setSolutionLine("Unable to process:" + e.getMessage());
				}
				
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						
					}
				});
				
			}

			@Override
			public Boolean getResult() {
				return true;
			}

			@Override
			public int getMaxProgress() {
				return pm.getMaxProgress();
			}

			@Override
			public int getProgress() {
				return pm.getProgress();
			}
		};

		BusyController.launch(boardDisplayArea.getScene().getWindow(), pt, pm);
		
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

		GuessMethod guessMethod;
		if (secondarySafetyProgress.isSelected()) {
			guessMethod = GuessMethod.SECONDARY_SAFETY_PROGRESS;
		} else if (recursiveSafety.isSelected()) {
			guessMethod = GuessMethod.RECURSIVE_SAFETY;
		} else {
			guessMethod = GuessMethod.SAFETY_PROGRESS;
		}
		
		int depth = 4;
		SolverSettings settings;
		if (this.useBruteForce.isSelected()) {
			//settings = SettingsFactory.GetSettings(Setting.VERY_LARGE_ANALYSIS).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);
			settings = SettingsFactory.GetSettings(Setting.MAX_ANALYSIS).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);
		} else {
			settings = SettingsFactory.GetSettings(Setting.NO_BRUTE_FORCE).setGuessMethod(guessMethod).setRecursiveSafetyDepth(depth);;			
		}
		
		settings.setLongTermSafety(this.useLongTermSafety.isSelected());
		settings.setTestMode(this.useTestMode.isSelected());
		settings.set5050Check(this.use5050Detection.isSelected());
		
		//SolverSettings settings = SettingsFactory.GetSettings(Setting.MAX_ANALYSIS).setGuessMethod(guessMethod);
		Solver solver = new Solver(gs, settings, true);
		
		
		// run this task in parallel while locking the screen against any other actions
		ParallelTask<Action[]> pt = new ParallelTask<Action[]>() {
			@Override
			public void doExecute() {
				solver.start();
				
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						
						Action[] actions = solver.getResult();
						
						if (actions.length == 0) {
							messageLine.setText("No suggestion returned by the solver");
						} else {
							Action a = actions[0];
							messageLine.setText(a.toString());
							
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
				});
				
			}

			@Override
			public Action[] getResult() {
				return solver.getResult();
			}

			@Override
			public int getMaxProgress() {
				return 0;
			}

			@Override
			public int getProgress() {
				return 0;
			}
		};

		BusyController.launch(boardDisplayArea.getScene().getWindow(), pt, null);
		
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
		
	
		// create new board
		Board newBoard = new Board(this, width, height);
		newBoard.clearBoard(true);  // all covered to start with

		setNewBoard(newBoard, mines);
		
	}
	
	public void loadFromFile(File file) throws Exception {
		
    	int width;
    	int height;
    	int mines;
    	
    	int minesCount = 0;
    	
    	Board result;

		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
				BufferedReader reader  = new BufferedReader(isr)
		){
			
			String data = reader.readLine();
			
			if (data == null) {
				throw new Exception("File is empty!");
			}
			
			String[] header = data.trim().split("x");
			if (header.length != 3) {
				throw new Exception("Header (" + data + ") doesn't contain width, height, mine separated by 'x'");
			}
			
			try {
				width = Integer.parseInt(header[0]);
				height = Integer.parseInt(header[1]);
				mines = Integer.parseInt(header[2]);
			} catch (Exception e) {
				throw new Exception("Unable to parse the values in the header (" + data + ")");
			}

			result = new Board(this, width, height);

			data = reader.readLine();
			int cy=0;
			
			while (data != null) {
				
				if (data.trim().length() != width) {
					throw new Exception("Detail row is not the same width as the header's width value");
				}
				
				int cx = 0;
				for (char c: data.trim().toCharArray()) {
					
					Tile tile = result.getTile(cx,  cy);
					
					if (c == 'M' || c == 'F') {
						
						//System.out.println("Set mine " + tile.asText());
						minesCount++;
						
						result.setFlag(tile, true);
						
					} else if (c == 'm') {
						
						//System.out.println("unfound mine " + tile.asText());
						tile.setCovered(true);
					
					}else if (c != 'H' && c != 'h') {
						int val = Character.getNumericValue(c);
						//System.out.println("Set value " + tile.asText() + " to " + val);
						tile.setCovered(false);
						tile.setValue(val);
					
					} else {
						//System.out.println("Set covered " + tile.asText());
						tile.setCovered(true);
					}
					cx++;
				}
			
				cy++;
				data = reader.readLine();	
				
				if (cy == height) {
					break;
				}
				
			};

			if (cy != height) {
				throw new Exception("Not enough rows in the file for the game defined in the header");
			}
		
		} catch (Exception e) {
			throw e;
		}	

		int minesLeft = Math.max(0, mines - minesCount);
		
		setNewBoard(result, minesLeft);
		
	}
	
	private void setNewBoard(Board board, int minesLeft) {
		
		// tidy up old details
		if (minesPlaced != null) {
			minesPlaced.removeValueListener();
		}

		// remove current board graphics
		getBoardDisplayArea().getChildren().clear();
		indicators.clear();
		
		// create new board
		currentBoard = board;

		checkBoxLockMineCount.setSelected(minesLeft != 0);
	
		boardExpander.setCenterX(board.getGameWidth() * graphicsSet.getSize());
		boardExpander.setCenterY(board.getGameHeight() * graphicsSet.getSize());
		
		getBoardDisplayArea().getChildren().addAll(currentBoard, boardExpander);

		minesToFind = new LedDigits(4);
		minesToFind.relocate(10, 5);
		minesToFind.setBackground(Explorer.GREY_BACKGROUND);
		getHeader().getChildren().add(minesToFind);
		minesToFind.setValue(minesLeft);
		
		minesPlaced = new LedDigits(4, true);
		minesPlaced.relocate(120, 5);
		minesPlaced.setBackground(Explorer.GREY_BACKGROUND);
		minesPlaced.setValueListener(currentBoard.getMinesPlacedProperty());
		minesPlaced.setValue(currentBoard.getFlagsPlaced());
		getHeader().getChildren().add(minesPlaced);
		
		messageLine.setText("Build a board");
		solutionLine.setText("");

		Explorer.setSubTitle(board.getGameWidth() + " x " + board.getGameHeight());
		
	}
	
    private void saveToFile(File file) throws Exception {
    	
    	if (file == null) {
    		return;
    	}
    	

    	int width = currentBoard.getGameWidth();
    	int height = currentBoard.getGameHeight();
    	int mines = currentBoard.getFlagsPlaced() + minesToFind.getValue();
    	
    	List<String> records = new ArrayList<>();
    	
    	String header =  width + "x" + height + "x" + mines;
    	records.add(header);
    	
    	for (int y=0; y < height; y++) {
    		
    		StringBuilder record = new StringBuilder();
        	for (int x=0; x < width; x++) {
        		
        		
        		Tile tile = currentBoard.getTile(x, y);

        		if (tile.isFlagged()) {
                	record.append("M");
                } else if (tile.isCovered()) {
                	 record.append("h");
                } else {
                	record.append(String.valueOf(tile.getValue()));
                }
        	}   		
    		
        	records.add(record.toString());
    		
    	}

    	records.add("Game created by Minesweeper Explorer vsn " + Explorer.VERSION);
    	
    	try (PrintStream output = new PrintStream(file)) {
    	   	for (String record: records) {
        		output.println(record);
        	}    		
    	} catch (Exception e) {
    		throw e;
    	}
    	
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
	
	public void setGraphicsSet(Graphics graphics) {
		this.graphics = graphics;
		this.graphicsSet = graphics.getGraphicsSet(24);
		
		boardExpander = new Expander(0, 0, 6, graphicsSet.getSize(), Color.BLACK);
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
