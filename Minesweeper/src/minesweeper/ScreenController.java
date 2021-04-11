/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import java.io.File;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import Monitor.AsynchMonitor;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Popup;
import minesweeper.bulk.BulkController;
import minesweeper.coach.HelperController;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.gamestate.GameStateStandardWith8;
import minesweeper.gamestate.MoveMethod;
import minesweeper.random.DefaultRNG;
import minesweeper.random.RNGJSF;
import minesweeper.random.RNGJava;
import minesweeper.random.RNGKiss64;
import minesweeper.settings.GameType;
import minesweeper.solver.Solver;
import minesweeper.solver.constructs.EvaluatedLocation;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.solver.settings.SettingsFactory.Setting;
import minesweeper.solver.settings.SolverSettings.GuessMethod;
import minesweeper.structure.Action;
import minesweeper.structure.Area;
import minesweeper.structure.Location;

/**
 *
 * @author David
 */
public class ScreenController {
    
	private static final Background BG_GREEN = new Background(new BackgroundFill(Color.GREEN, null, null));
	private static final Background BG_GREY = new Background(new BackgroundFill(Color.GREY, null, null));
	
    @FXML
    private Pane myPane;
    
    @FXML private Button newGameButton;
    @FXML private Button automateButton;
    
    @FXML
    private AnchorPane window;
    
    @FXML
    private Label scoreLabel;
    
    @FXML
    private Label timeLabel;    
    
    @FXML private RadioMenuItem easyMode;
    @FXML private RadioMenuItem mediumMode;
    @FXML private RadioMenuItem hardMode;
    @FXML private RadioMenuItem customMode;    
    @FXML private RadioMenuItem msxMode;
    @FXML private RadioMenuItem fromFile;

    @FXML private RadioMenuItem gameTypeEasy;
    @FXML private RadioMenuItem gameTypeNormal;
    @FXML private RadioMenuItem gameTypeHard;
    
    @FXML private MenuItem saveBoard;
    
    @FXML private RadioMenuItem rngJava;
    @FXML private RadioMenuItem rngKiss64;
    
    @FXML private RadioMenuItem sol400;
    @FXML private RadioMenuItem sol4000;
    
    @FXML private RadioMenuItem secondarySafetyGuess;
    @FXML private RadioMenuItem standardGuess;
    
    @FXML private CheckMenuItem showMove;
    @FXML private CheckMenuItem showTooltips;
    @FXML private CheckMenuItem acceptGuess;
    @FXML private CheckMenuItem showMines;
    @FXML private CheckMenuItem flagFree;
    @FXML private CheckMenuItem useChords;
    @FXML private CheckMenuItem dumpTree;
    @FXML private CheckMenuItem probHeatMap;
    
    @FXML
    private Circle highlight;
    
    private static final int IMAGE_SIZE = (int) Graphics.SIZE;
    
    //private static final int SOLVER = Solver.ANY_GUESSES;
    
    public static final int DIFFICULTY_BEGINNER = 1;
    public static final int DIFFICULTY_ADVANCED = 2;
    public static final int DIFFICULTY_EXPERT = 3;
    public static final int DIFFICULTY_FILE = 98;
    public static final int DEFER_TO_MINESWEEPERX = 99;
    
    public static final int DIFFICULTY_CUSTOM = 100;
    
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    
    
    // What kind of solver should we use
    private static SolverSettings preferences; 
    private Solver solver;
    
    private WritableImage scr;
    private PixelWriter scrWriter;
    private NumberBinding rootWatch;
    private ImageView display;
    private int[][] lastScreen;
    
    private double scaleFactor = 25;

    private boolean automate = false;
    private int moveHighlighted = 0;
    
    private Action[] move = new Action[0];
    private int nextMove = 0;
    
    Animator animator;
    
    private BigDecimal combProb = BigDecimal.ONE;
    
    private int difficulty = DIFFICULTY_EXPERT;
    private RadioMenuItem lastValidDifficulty;
    private File fileSelected = null;
    
    private GameType gameType = GameType.STANDARD;
    
    private Popup toolTip = new Popup();
    private Text popupText = new Text();

    private FileChooser fileChooser = new FileChooser();
    
    private final List<Node> heatMapNodes = new ArrayList<>();
    
    private EventHandler<MouseEvent> me = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {
			
			//System.out.println(event.getX() + "," + event.getY());
			
			if (!showTooltips.isSelected()) {
				return;
			}
			
			toolTip.setX(event.getScreenX() + 10);
			toolTip.setY(event.getScreenY() - 10);
			
			Point2D p = getSquare(event.getX(), event.getY());
			
			BigDecimal prob = null;
			if (p.getX() >= 0 && p.getX() <= solver.getGame().getWidth() && p.getY() >= 0 && p.getY() <= solver.getGame().getHeight() && solver.getGame().query(new Location((int) p.getX(), (int) p.getY())) == GameStateModel.HIDDEN) {
				prob = solver.getProbability((int) p.getX(), (int) p.getY());
				if (prob == null) {
					popupText.setText("?");
				} else if (prob.compareTo(BigDecimal.ZERO) == 0) {
					popupText.setText("Mine!");
				} else if (prob.compareTo(BigDecimal.ONE) == 0) {
					popupText.setText("Safe");
					
				} else {
					popupText.setText(Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED)) + "% safe");
				}
			} else {
				popupText.setText("");
			}
			
			//toolTip.hide();
			if (event.getEventType() == MouseEvent.MOUSE_EXITED) {
				toolTip.hide();
			} else if (event.getEventType() == MouseEvent.MOUSE_ENTERED) {
				toolTip.show(window.getScene().getWindow());
			}
			
			
		}
    	
    };
    
	private class Indicator extends Rectangle {
		
		//private final Action action; 
		
		public Indicator(DoubleBinding xBind, DoubleBinding yBind, DoubleBinding widthBind, DoubleBinding heightBind, Action action) {
			super();

    		heightProperty().bind(heightBind);
    		widthProperty().bind(widthBind);
    		xProperty().bind(xBind);
    		yProperty().bind(yBind);   
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

	}
    
	private class Placeholder extends Rectangle {
		
		public Placeholder(DoubleBinding xBind, DoubleBinding yBind, DoubleBinding widthBind, DoubleBinding heightBind, Color colour) {
			super();

    		heightProperty().bind(heightBind);
    		widthProperty().bind(widthBind);
    		xProperty().bind(xBind);
    		yProperty().bind(yBind);    
    		setMouseTransparent(true);    

    		this.setFill(colour);
			this.setOpacity(0.5d);

			
		}

	}
	
    @FXML
    private void handleDifficulty(ActionEvent event) {
        
    	int prevDiff = difficulty;
    	
        if (easyMode.isSelected()) {
        	lastValidDifficulty = easyMode;
            difficulty = DIFFICULTY_BEGINNER;
            newGame();
        } else if (mediumMode.isSelected()) {
        	lastValidDifficulty = mediumMode;
            difficulty = DIFFICULTY_ADVANCED;
            newGame();
        } else if (hardMode.isSelected()) {
        	lastValidDifficulty = hardMode;
            difficulty = DIFFICULTY_EXPERT;
            newGame();
        } else if (msxMode.isSelected()) {
        	lastValidDifficulty = fromFile;
            difficulty = DEFER_TO_MINESWEEPERX;
            newGame();
        } else if (customMode.isSelected()) {
            difficulty = DIFFICULTY_CUSTOM;
        } else if (fromFile.isSelected()) {
            difficulty = DIFFICULTY_FILE;
            
        	//FileChooser fileChooser = new FileChooser();
        	
        	fileChooser.setTitle("Open game to analyse");
        	if (fileSelected != null) {
        		fileChooser.setInitialDirectory(fileSelected.getParentFile());
        	}
        	fileSelected = fileChooser.showOpenDialog(Minesweeper.getStage());
        	
        	if (fileSelected == null) {
        		lastValidDifficulty.setSelected(true);
        		difficulty = prevDiff;
        	} else {
        		lastValidDifficulty = fromFile;
        		newGame();
        	}
        	
        }

        System.out.println("Menu difficulty option " + difficulty + " picked");
        
        if (difficulty == DIFFICULTY_CUSTOM) {
        	System.out.println("At custom Menu");
        	CustomController custom = CustomController.launch(window.getScene().getWindow(), Minesweeper.getGame());
        	
            custom.getStage().showAndWait();
            
            System.out.println("At custom menu finish");
            
            if (custom.wasCancelled()) {
            	lastValidDifficulty.setSelected(true);
            	difficulty = prevDiff;
            } else {
            	lastValidDifficulty = customMode;
            	newGame();
            } 
            
        }
        
        
        
        
    }
    
    @FXML
    private void saveBoardHandle(ActionEvent event) {
        
    	//FileChooser fileChooser = new FileChooser();
    	
    	fileChooser.setTitle("Save board position");
    	if (fileSelected != null) {
    		fileChooser.setInitialDirectory(fileSelected.getParentFile());
    	}
    	
     	fileSelected = fileChooser.showSaveDialog(Minesweeper.getStage());

     	try {
			saveGame(fileSelected);
		} catch (Exception e) {
			System.out.println("Error writing to output file");
			e.printStackTrace();
		}
    	
    	
    }    
    
    @FXML
    private void exitGameHandle(ActionEvent event) {
        
    	Platform.exit();

    }    
    
    @FXML
    private void handleGameType(ActionEvent event) {
        
        if (gameTypeEasy.isSelected()) {
            gameType = GameType.EASY;
        } else if (gameTypeNormal.isSelected()) {
            gameType = GameType.STANDARD;
        } else if (gameTypeHard.isSelected()) {
            gameType = GameType.HARD;
        }

    }    
    
    @FXML
    private void handleNewGameButton(ActionEvent event) {
        
        newGame();
        
    }

    @FXML
    private void handleNewBulkRun(ActionEvent event) {
        
        if (sol4000.isSelected()) {
        	preferences = SettingsFactory.GetSettings(Setting.LARGE_ANALYSIS);
        } else {
        	preferences = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS);
        }
    	
    	
        BulkController.launch(window.getScene().getWindow(), Minesweeper.getGameSettings(), gameType, preferences);
        
    }

    
    
    @FXML
    private void handleCopyToClipboard(ActionEvent event) {
        
    	//System.out.println("copy to clipboard");
    	
        // store the seed in the clipboard
        ClipboardContent content = new ClipboardContent();
        content.putString(String.valueOf(Minesweeper.getGame().getSeed()));
        Clipboard.getSystemClipboard().setContent(content);
        
    }
    
    
    @FXML
    private void handleAutomateButton(ActionEvent event) {
        
        //System.out.println("You clicked me!");
        
        // rotate the button
        //new Rotator((Node) event.getSource()).start();
        
    	//Button button = (Button) event.getSource();
    	
        automate = !automate;
        
        if (automate) {
        	automateButton.setText("Stop");
        } else {
        	automateButton.setText("Automate");
        }
        
    }    
    
    @FXML
    void mouseDown(MouseEvent event) {
        //System.out.println("mouse Down on the pane " + event.getButton());
        
        if (automate) {
            return;
        }
        
        Point2D p = getSquare(event.getX(), event.getY());
        
        //System.out.println(" at " + p.getX() + " by " + p.getY());
        
        Location m = new Location((int) p.getX(), (int) p.getY());
        Action action = null;

        if (event.isPrimaryButtonDown() && event.isSecondaryButtonDown()) {
            action = new Action(m, Action.CLEARALL);
        } else if (event.isPrimaryButtonDown()) {
        	if (solver.getGame().query(m) > 0) {
        		action = new Action(m, Action.CLEARALL);  // modern chording
        	} else {
        		action = new Action(m, Action.CLEAR);
        	}
 
        } else if (event.isSecondaryButtonDown()) {
            action = new Action(m, Action.FLAG);
        }
 
        doMove(action);

        //if (Minesweeper.getGame().getGameState() == GameState.LOST) {
        //   display.setRotate(5);
        //}
        
        window.setCursor(Cursor.WAIT);

        move = getMoves();
        

        if (!automate) {
        	createHeatMap(move);
        }
        
        highlightMove(0);

        window.setCursor(Cursor.DEFAULT);
        
        nextMove = 0;
        //update((int) p.getX(), (int) p.getY());
        
    }
    
    @FXML
    private void mouseUp(MouseEvent event) {
        //System.out.println("mouse Up on the pane");
    	
    	/*
        if (automate) {
            return;
        }
        
        Point2D p = getSquare(event.getX(), event.getY());
        
        //System.out.println(" at " + p.getX() + " by " + p.getY());
        
        Location m = new Location((int) p.getX(), (int) p.getY());
        Action action = null;

        MouseButton b = event.getButton();
        System.out.println("button = " + b);
        
        if (event.isPrimaryButtonDown() && event.isSecondaryButtonDown()) {
            action = new Action(m, Action.CLEARALL);
        } else if (event.isPrimaryButtonDown()) {
            action = new Action(m, Action.CLEAR);
        } else if (event.isSecondaryButtonDown()) {
            action = new Action(m, Action.FLAG);
        }
 
        doMove(action);

        //if (Minesweeper.getGame().getGameState() == GameState.LOST) {
        //    display.setRotate(5);
        //}
        
        move = getMoves();

        highlightMove(0);

        nextMove = 0;
        */
    }   
    
    @FXML
    private void showMinesToggled(ActionEvent event) {
        refreshScreen();
    }   
    
    @FXML
    private void flagFreeToggled(ActionEvent event) {
        
    	if (solver != null) {
    		solver.setFlagFree(flagFree.isSelected());
    	}
    	
    }   
    
    @FXML
    private void useChordsToggled(ActionEvent event) {
        
    	if (solver != null) {
    		solver.setPlayChords(useChords.isSelected());
    	}
    	
    }   
    
    @FXML
    private void dumpTreeToggled(ActionEvent event) {
        
    	if (solver != null) {
    		solver.setShowProbabilityTree(dumpTree.isSelected());
    	}
    	
    }   
    
    @FXML
    private void probHeatMapToggled(ActionEvent event) {
        
    	createHeatMap(move);
    	
    }   
    
    @FXML
    void initialize() {
        //assert button != null : "fx:id=\"button\" was not injected: check your FXML file 'Screen.fxml'.";
        assert myPane != null : "fx:id=\"myPane\" was not injected: check your FXML file 'Screen.fxml'.";
        
        window.setBackground(BG_GREY);
        
        // get some details about the game
        GameStateModel gs = Minesweeper.getGame();
        int x = gs.getWidth();
        int y = gs.getHeight();

        //Scene sc = window.getScene();
        //System.out.println("Scene size is " + sc.getX() + " by " + sc.getY());
        
        window.setMinSize(1, 1);
        window.setMaxSize(10000, 10000);

        // resize the window to the size of the board
        window.setPrefSize(scaleFactor * x + 20, scaleFactor*y + 70);
        //window.resize(scaleFactor*x + 20, scaleFactor*y + 70);
        
        // resize the pane to the size of the window
        window.layout();

        display = new ImageView();
        display.setPreserveRatio(false);
        //display.setImage(scr);

        myPane.getChildren().add(display);
       
        display.fitHeightProperty().bind(myPane.heightProperty());
        display.fitWidthProperty().bind(myPane.widthProperty());
        
        toolTip.getContent().addAll(popupText);
        popupText.setText("Test");
        popupText.setFont(new Font(20));
        
        // set-up the filechooser
    	ExtensionFilter ef1 = new ExtensionFilter("All files", "*.*");
    	ExtensionFilter ef2 = new ExtensionFilter("Minesweeper board", "*.mine");
    	ExtensionFilter ef3 = new ExtensionFilter("Minesweeper board", "*.board");
    	ExtensionFilter ef4 = new ExtensionFilter("Minesweeper board", "*.mbf");
    	fileChooser.getExtensionFilters().addAll(ef1, ef2, ef3, ef4);
    	fileChooser.setSelectedExtensionFilter(ef2);        
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
    	
        probHeatMap.setDisable(false);
        probHeatMap.setSelected(true);
        showMove.setSelected(false);
        
        lastValidDifficulty = hardMode;
        
        animator = new Animator(this);
        animator.start();
        
    }
    
    // call the solver asynchronously 
    private Action[] getMoves() {
        
        //solver.setApproach(0);
        
        AsynchMonitor am = new AsynchMonitor(solver);
        try {
            am.startAndWait();
        } catch (Exception ex) {
            System.err.println("Error in Asynchronous processing: " + ex.getMessage());
            Platform.exit();
        }
        
        Action[] result = solver.getResult();
 
        return result;

    }
    
    
    protected boolean doMove(Action action) {
        
        boolean result = Minesweeper.getGame().doAction(action);
        
        if (Minesweeper.getGame().getGameState() == GameStateModel.STARTED) {
        	saveBoard.setDisable(false);
        }
        
        if (result) {
            updateScreen();
        }
        
        return result;
    }
    
    protected void moveCheck() {
     	
        if (!automate) {
        	window.setCursor(Cursor.DEFAULT);
            return;
        }
        
        window.setCursor(Cursor.WAIT);
        
        // play the moves until one is successful
        // this is done because some moves become obsolete once previous ones
        // have been made
        boolean success = false;
        while (!success) {

            // if we have played all the current moves then get some more
            if (nextMove >= move.length) {
                move = getMoves();
                nextMove = 0;
            }

            // if the solver can't find any moves then stop automating
            if (move.length == 0) {
                automate = false;
            	automateButton.setText("Automate");
            	window.setCursor(Cursor.DEFAULT);
                return;
            }

            // if we aren't accepting guesses then stop the automated processing when we come to one
            if (!acceptGuess.isSelected() && !move[nextMove].isCertainty()) {
                automate = false;
            	automateButton.setText("Automate");
            	window.setCursor(Cursor.WAIT);
                highlightMove(nextMove);
                createHeatMap(move);
                return;
            }
            
            // play the move
            success = doMove(move[nextMove]);

            if (move[nextMove].getBigProb().signum() > 0 && move[nextMove].getBigProb().compareTo(BigDecimal.ONE) < 0) {
                System.out.println(move[nextMove].toString());
                combProb = combProb.multiply(move[nextMove].getBigProb());
                System.out.println("Combined probability is " + combProb);
            }
            
            // audit the move
            //System.out.println(move[nextMove].asString() + " ... success: " + success);

            // line up the next move
            nextMove++;
            
        }
        
        // high light the move we just made
        highlightMove(nextMove - 1);
        
    }
    
    // re-highlight the last position
    public void highlightMove() {
        
         if (move == null || move.length <= moveHighlighted) {
            highlight.setVisible(false);
            return;
         }        
         
         // high light the first move on the list
         highlightMove(moveHighlighted);
         
    }
    
    // high light this position
    public void highlightMove(int i) {
        
        if (move == null || move.length <= i) {
            highlight.setVisible(false);
            return;
        } 
        
        // remember the move we are high lightinh
        moveHighlighted = i;
        
        Action a = move[i];
        
        GameStateModel gs = Minesweeper.getGame();
         
        double x1 = a.x;
        double y1 = a.y;
        
        double x2 = display.getFitWidth() * x1 / (double) gs.getWidth() + myPane.getLayoutX();
        double y2 = display.getFitHeight() * y1 / (double) gs.getHeight() + myPane.getLayoutY();
        
        double d = Math.max(display.getFitWidth() / gs.getWidth(), display.getFitHeight() / gs.getHeight());
        
        // set the colours for the move indicator
        if (a.getAction() == Action.FLAG) {
            highlight.setFill(Color.LIGHTPINK);
        } else {
            highlight.setFill(Color.LIGHTGREEN);
        }
        if (a.isCertainty()) {
            highlight.setStroke(Color.BLACK);
        } else {
            highlight.setStroke(Color.DARKRED);
        }
        
        highlight.setRadius(d);
        highlight.relocate(x2 - d/2, y2 - d/2);
        highlight.setVisible(showMove.isSelected());
    }
     
    /*
    private WritableImage buildScreen() {
        
        long startTime = System.currentTimeMillis();
        
        GameStateModel gs = Minesweeper.getGame();
        
        int x = gs.getx();
        int y = gs.gety();
        
        WritableImage result = new WritableImage(IMAGE_SIZE * x, IMAGE_SIZE * y);
        
        PixelWriter pw = result.getPixelWriter();
        
        PixelReader pr;
        
        for (int i=0; i < x; i++) {
            for (int j=0; j < y; j++) {
                
                pr = getImage(i,j);
                
                if (pr != null) {
                    addImage(pw, i*IMAGE_SIZE, j*IMAGE_SIZE, pr);
                }
            }
        }
 
        //System.out.println("Image is width " + result.getWidth() + " by height " + result.getHeight());
        
        long duration = System.currentTimeMillis() - startTime;
        
        //System.out.println("Screen generated in " + duration + " milliseconds");
        
        return result;
        
    }
    */
    private ImageView refreshScreen() {

        //long startTime = System.currentTimeMillis();
        
        ImageView result = new ImageView();
        result.setPreserveRatio(false);
        result.fitHeightProperty().bind(myPane.heightProperty());
        result.fitWidthProperty().bind(myPane.widthProperty());   

        GameStateModel gs = Minesweeper.getGame();
        
        int x = gs.getWidth();
        int y = gs.getHeight();
        
        // show mines if it has been selected or the game is lost
        boolean revealMines = (gs.getGameState() == GameStateModel.LOST) || this.showMines.isSelected();
            
        PixelWriter pw = scr.getPixelWriter();
        
        PixelReader pr;
        
        for (int i=0; i < x; i++) {
            for (int j=0; j < y; j++) {
                
                pr = getImage(i,j, revealMines);
                
                if (pr != null) {
                    addImage(pw, i*IMAGE_SIZE, j*IMAGE_SIZE, pr);
                }
            }
        }
 
        //long duration = System.currentTimeMillis() - startTime;
        
        result.setImage(scr);
        

        
        //System.out.println("Screen refreshed in " + duration + " milliseconds");
        
        return result;
        
    }

    private void createHeatMap(Action[] moves) {

    	// clear the old data
    	myPane.getChildren().removeAll(heatMapNodes);
    	heatMapNodes.clear();
    	
    	// are we doing heat maps?
    	if (!probHeatMap.isSelected()) {
    		return;
    	}
    	
    	GameStateModel gs = Minesweeper.getGame();
    	
    	//display.fitHeightProperty().bind(myPane.heightProperty().divide(gs.getHeight()));
    	
    	DoubleBinding heightBind = myPane.heightProperty().divide(gs.getHeight());
    	DoubleBinding widthBind = myPane.widthProperty().divide(gs.getWidth());
    	
    	for (Action a: moves) {
   		
        	DoubleBinding yBind = myPane.heightProperty().multiply(a.y).divide(gs.getHeight());
        	DoubleBinding xBind = myPane.widthProperty().multiply(a.x).divide(gs.getWidth());
    		
    		Indicator indicator = new Indicator(xBind, yBind, widthBind, heightBind, a);
    		indicator.setMouseTransparent(true);
    		
    		heatMapNodes.add(indicator);
    	}
    	
    	List<EvaluatedLocation> els = solver.getEvaluatedLocations();
    	if (els != null) {
        	for (EvaluatedLocation el: els) {
        		
        		// don't show evaluated positions which are actually chosen to be played
        		boolean ignore = false;
        		for (Action a: moves) {
        			if (el.equals(a)) {
        				ignore = true;
        			}
        		}
        		
        		if (!ignore) {
                	DoubleBinding yBind = myPane.heightProperty().multiply(el.y).divide(gs.getHeight());
                	DoubleBinding xBind = myPane.widthProperty().multiply(el.x).divide(gs.getWidth());
            		
            		Placeholder indicator = new Placeholder(xBind, yBind, widthBind, heightBind, Color.ORANGE);
            		heatMapNodes.add(indicator);       			
        		}

        		
        	}    		
    	}
    	
    	Area dead = solver.getDeadLocations();
    	if (dead != null) {
        	for (Location loc: dead.getLocations()) {
        		
        		// don't show evaluated positions which are actually chosen to be played
        		boolean ignore = false;
        		for (Action a: moves) {
        			if (loc.equals(a)) {
        				ignore = true;
        			}
        		}
        		
        		if (!ignore) {
                	DoubleBinding yBind = myPane.heightProperty().multiply(loc.y).divide(gs.getHeight());
                	DoubleBinding xBind = myPane.widthProperty().multiply(loc.x).divide(gs.getWidth());
            		
            		Placeholder indicator = new Placeholder(xBind, yBind, widthBind, heightBind, Color.BLACK);
            		heatMapNodes.add(indicator);       			
        		}

        		
        	}    		
    	}
    	
    	myPane.getChildren().addAll(heatMapNodes);
    	
    	
    	
    }
    
    private void updateScreen() {
        
        ImageView temp = refreshScreen();
        
        myPane.getChildren().add(temp);
        myPane.getChildren().remove(display);
        
        display = temp;

        display.setOnMouseMoved(me);
        display.setOnMouseEntered(me);
        display.setOnMouseExited(me);
        
        //if (solver != null) {
        //   createHeatMap(move);       	
        //}

        
        //highlightMove();
        
        //System.gc();
        
    }
    
    private void addImage(PixelWriter canvase, int x, int y, PixelReader image) {
        
        canvase.setPixels(x, y, IMAGE_SIZE, IMAGE_SIZE, image, 0, 0);

    }

    private PixelReader getImage(int x, int y, boolean showMines) {
        
        Image result;
        
        //int query = Minesweeper.getGame().query(new Location(x, y));
        
        int query = Minesweeper.getGame().privilegedQuery(new Location(x, y), showMines);
        
        // if the square on the screen hasn't changed then don't update it
        if (lastScreen[x][y] == query) {
            return null;
        }
        
        lastScreen[x][y] = query;
        
        if (query == GameStateModel.MINE) {
            result = Graphics.getMine();
        } else if (query == GameStateModel.HIDDEN) {
            result = Graphics.getButton();
        } else if (query == GameStateModel.FLAG || query == GameStateModel.BAD_FLAG) {
            result = Graphics.getFlag();
        } else if (query == GameStateModel.EXPLODED_MINE) {
            result = Graphics.getMineBang();
        } else {
            result = Graphics.getNumber(query);
        }
        
        return result.getPixelReader();
        
    }
    
    private Point2D getSquare(double x, double y) {
        
        GameStateModel gs = Minesweeper.getGame();
        
        double x1 = gs.getWidth();
        double y1 = gs.getHeight();
        
        double x2 = x1 * x / display.getFitWidth();
        double y2 = y1 * y / display.getFitHeight();
        
        // if we click right on the edge we can get an out of bounds error, so prevent it
        if (x2 >= x1) {
            x2 = x1 - 1;
        }
        if (y2 >= y1) {
            y2 = y1 - 1;
        }
        
        return new Point2D(x2, y2);
        
    }
    
    public void updateTime() {
        
        int time = (int) Minesweeper.getGame().getGameTime();
        
        timeLabel.setText(formatXXX(time));
        
        int mines = Minesweeper.getGame().getMinesLeft();
        
        scoreLabel.setText(formatXXX(mines));
        
    }
    
    private String formatXXX(int i) {
        
        if (i < 0) {
            return "000";
        }
        
        if (i < 10) {
            return "00" + String.valueOf(i);
        }
        
        if (i < 100) {
            return "0" + String.valueOf(i);
        }
        
        if (i < 1000) {
            return String.valueOf(i);
        }
        
        return "999";
        
    }
    
    public int getDifficulty() {
        return difficulty;
    }
        
    public GameType getGameType() {
        return gameType;
    }
    
    
    // pick the difficulty up from the menu option
    private void newGame() {
        
        newGame(this.difficulty);

    }

    // stop the current game and start a new one
    protected void newGame(int difficulty) {
        
    	if (rngKiss64.isSelected()) {
    		DefaultRNG.setDefaultRNGClass(RNGKiss64.class);
    		//DefaultRNG.setDefaultRNGClass(RNGJSF.class);
    	} else {
    		DefaultRNG.setDefaultRNGClass(RNGJava.class);
    	}
    	
        // create a new game state
        GameStateModel gs = Minesweeper.createNewGame(difficulty, gameType, fileSelected);
         
        newGame(gs);
        
       
    }
    
    // stop the current game and start a new using this gameState
    protected void newGame(GameStateModel gs) {
        
        if (gs == null) {
        	System.out.println("new Game state has not been created!");
        	return;
        }

        if (gs.getGameState() == GameStateModel.NOT_STARTED) {
        	saveBoard.setDisable(true);  // can't save a game which isn't started since that sets the board layout
        }
        
        // create a memory of the last screen - set to full refresh
        lastScreen = new int[gs.getWidth()][gs.getHeight()];
        for (int i=0; i < gs.getWidth(); i++) {
            for (int j=0; j < gs.getHeight(); j++) {
                lastScreen[i][j] = -1;
            }
        }

        if (solver != null) {
            solver.kill();
        }

        // if the screen image doesn't exist or is the wrong size then create a new one
        if (scr == null || IMAGE_SIZE * gs.getWidth() != scr.getWidth() || IMAGE_SIZE * gs.getHeight() != scr.getHeight()) {
            System.out.println("Creating a new Screen Image");
            scr = new WritableImage(IMAGE_SIZE * gs.getWidth(), IMAGE_SIZE * gs.getHeight());
        }
        
        updateScreen();
        
        Double offsetX = (IMAGE_SIZE * gs.getWidth() - newGameButton.getWidth())/ 2d;
         
        newGameButton.setLayoutX(offsetX);
        
		GuessMethod guessMethod;
		if (secondarySafetyGuess.isSelected()) {
			guessMethod = GuessMethod.SECONDARY_SAFETY_PROGRESS;
		} else {
			guessMethod = GuessMethod.SAFETY_PROGRESS;
		}
        
        if (sol4000.isSelected()) {
        	preferences = SettingsFactory.GetSettings(Setting.LARGE_ANALYSIS).setGuessMethod(guessMethod);
        } else {
        	preferences = SettingsFactory.GetSettings(Setting.SMALL_ANALYSIS).setGuessMethod(guessMethod);
        }
        
        // create a new solver
        solver = new Solver(gs, preferences, HelperController.launch(), true);
        solver.setFlagFree(flagFree.isSelected());
        solver.setPlayChords(useChords.isSelected());
        solver.setShowProbabilityTree(dumpTree.isSelected());
        
        // don't play the opening move if the game is loaded from a file
        solver.setPlayOpening(difficulty != DIFFICULTY_FILE);
        
        combProb = BigDecimal.ONE;
 
        // forget any moves we have stored up
        move = new Action[0];
        nextMove = 0;
        
        Minesweeper.getStage().setTitle(Minesweeper.TITLE + " - Game " + gs.showGameKey());
        
       
    }
    
    private void saveGame(File file) throws Exception {
    	
    	if (file == null) {
    		return;
    	}
    	
    	GameStateModelViewer gs = Minesweeper.getGame();
    	
    	if (gs == null || gs.getGameState() == GameStateModel.NOT_STARTED) {
    		return;
    	}
    	
    	int width = gs.getWidth();
    	int height = gs.getHeight();
    	
    	List<String> records = new ArrayList<>();
    	
    	String header =  width + "x" + height + "x" + gs.getMines();
    	records.add(header);
    	
    	for (int y=0; y < height; y++) {
    		
    		StringBuilder record = new StringBuilder();
        	for (int x=0; x < width; x++) {
        		
        		Location l = new Location(x, y);
        		
                int query = Minesweeper.getGame().privilegedQuery(l, true);
             
                if (query == GameStateModel.MINE) {
                     record.append("m");
                } else if (query == GameStateModel.HIDDEN) {
                	 record.append("h");
                } else if (query == GameStateModel.FLAG) {
                	record.append("M");
                }  else if (query == GameStateModel.BAD_FLAG) {   // a flag but not a mine underneath
                	record.append("h");
                } else if (query == GameStateModel.EXPLODED_MINE) {
                	record.append("m");
                } else {
                	record.append(String.valueOf(query));
                }
        	}   		
    		
        	records.add(record.toString());
    		
    	}

    	records.add("Game created by Minesweeper Coach vsn " + Minesweeper.VERSION);
    	records.add(gs.showGameKey());
    	
    	try (PrintStream output = new PrintStream(file)) {
    	   	for (String record: records) {
        		output.println(record);
        	}    		
    	} catch (Exception e) {
    		throw e;
    	}
    	
    }
    
    public void kill() {
        
        if (solver != null) {
            solver.kill();
        }
        
        solver = null;
        
    }
    
    
    public void stop() {
        
        animator.stop();
        
    }
    
 }
