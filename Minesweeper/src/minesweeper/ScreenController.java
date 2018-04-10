/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import java.math.BigDecimal;
import java.math.BigInteger;

import Monitor.AsynchMonitor;
import javafx.application.Platform;
import javafx.beans.binding.NumberBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import minesweeper.coach.HelperController;
import minesweeper.gamestate.Action;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;
import minesweeper.solver.Preferences;
import minesweeper.solver.Solver;

/**
 *
 * @author David
 */
public class ScreenController {
    
    @FXML
    private Pane myPane;
    
    @FXML
    private Button button;
    
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

    @FXML private RadioMenuItem gameTypeEasy;
    @FXML private RadioMenuItem gameTypeNormal;
    @FXML private RadioMenuItem gameTypeHard;
    
    
    
    @FXML
    private CheckMenuItem showMove;
    @FXML private CheckMenuItem acceptGuess;
    @FXML private CheckMenuItem showMines;
    @FXML private CheckMenuItem flagFree;
    @FXML private CheckMenuItem probHeatMap;
    
    @FXML
    private Circle highlight;
    
    private static final int IMAGE_SIZE = (int) Graphics.SIZE;
    
    //private static final int SOLVER = Solver.ANY_GUESSES;
    
    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_MEDIUM = 2;
    public static final int DIFFICULTY_HARD = 3;
    public static final int DEFER_TO_MINESWEEPERX = 99;
    public static final int DIFFICULTY_CUSTOM = 100;
    
    public static final int GAMETYPE_EASY = 1;
    public static final int GAMETYPE_NORMAL = 2;
    public static final int GAMETYPE_HARD = 3;
    
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    
    
    // What kind of solver should we use
    //private static final Preferences preferences = Preferences.NO_BRUTE_FORCE; 
    //private static final Preferences preferences = Preferences.MEDIUM_BRUTE_FORCE; 
    private static final Preferences preferences = Preferences.LARGE_BRUTE_FORCE; 
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
    
    private double combProb = 1;
    
    private int difficulty = DIFFICULTY_HARD;
    
    private int gameType = GAMETYPE_NORMAL;
    
    private Popup toolTip = new Popup();
    private Text popupText = new Text();

    //TODO finish this ...
    private EventHandler<MouseEvent> me = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent event) {
			
			//System.out.println(event.getX() + "," + event.getY());
			
			toolTip.setX(event.getScreenX() + 10);
			toolTip.setY(event.getScreenY());
			
			Point2D p = getSquare(event.getX(), event.getY());
			
			BigDecimal prob = null;
			if (p.getX() >= 0 && p.getX() <= solver.getGame().getx() && p.getY() >= 0 && p.getY() <= solver.getGame().gety() && solver.getGame().query(new Location((int) p.getX(), (int) p.getY())) == GameStateModel.HIDDEN) {
				prob = solver.getProbability((int) p.getX(), (int) p.getY());
				if (prob == null) {
					popupText.setText("?");
				} else {
					popupText.setText(Action.FORMAT_2DP.format(prob.multiply(ONE_HUNDRED)) + "%");
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
    
    @FXML
    private void handleDifficulty(ActionEvent event) {
        
    	int prevDiff = difficulty;
    	
        if (easyMode.isSelected()) {
            difficulty = DIFFICULTY_EASY;
        } else if (mediumMode.isSelected()) {
            difficulty = DIFFICULTY_MEDIUM;
        } else if (hardMode.isSelected()) {
            difficulty = DIFFICULTY_HARD;
        } else if (msxMode.isSelected()) {
            difficulty = DEFER_TO_MINESWEEPERX;
        } else if (customMode.isSelected()) {
            difficulty = DIFFICULTY_CUSTOM;
        }

        if (difficulty == DIFFICULTY_CUSTOM) {
        	System.out.println("At custom Menu");
        	CustomController custom = CustomController.launch(window.getScene().getWindow(), Minesweeper.getGame());
        	
            custom.getStage().showAndWait();
            
            if (custom.wasCancelled()) {
            	difficulty = prevDiff;
            }
            
            System.out.println("At custom menu finish");
        }
        
        
        System.out.println("Menu difficulty option " + difficulty + " picked");
        
    }
    
    @FXML
    private void handleGameType(ActionEvent event) {
        
        if (gameTypeEasy.isSelected()) {
            gameType = GAMETYPE_EASY;
        } else if (gameTypeNormal.isSelected()) {
            gameType = GAMETYPE_NORMAL;
        } else if (gameTypeHard.isSelected()) {
            gameType = GAMETYPE_HARD;
        }

    }    
    
    @FXML
    private void handleNewGameButton(ActionEvent event) {
        
        //System.out.println("You clicked me!");
        
        // rotate the button
        new Rotator((Node) event.getSource()).start();
        
       
        newGame();
        
    }

    @FXML
    private void handleAutomateButton(ActionEvent event) {
        
        //System.out.println("You clicked me!");
        
        // rotate the button
        new Rotator((Node) event.getSource()).start();
        
        automate = !automate;
        
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
            action = new Action(m, Action.CLEAR);
        } else if (event.isSecondaryButtonDown()) {
            action = new Action(m, Action.FLAG);
        }
 
        doMove(action);

        //if (Minesweeper.getGame().getGameState() == GameState.LOST) {
        //   display.setRotate(5);
        //}
        
        move = getMoves();

        highlightMove(0);

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
    private void probHeatMapToggled(ActionEvent event) {
        
    	
    	
    }   
    
    @FXML
    void initialize() {
        assert button != null : "fx:id=\"button\" was not injected: check your FXML file 'Screen.fxml'.";
        assert myPane != null : "fx:id=\"myPane\" was not injected: check your FXML file 'Screen.fxml'.";
        
        // get some details about the game
        GameStateModel gs = Minesweeper.getGame();
        int x = gs.getx();
        int y = gs.gety();

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
        
        if (result) {
            updateScreen();
        }
        
        return result;
    }
    
    protected void moveCheck() {
        
        if (!automate) {
            return;
        }
        
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
                return;
            }

            // if we aren't accepting guesses then stop the automated processing when we come to one
            if (!acceptGuess.isSelected() && !move[nextMove].isCertainty()) {
                automate = false;
                highlightMove(nextMove);
                return;
            }
            
            // play the move
            success = doMove(move[nextMove]);

            if (move[nextMove].getProb() > 0 && move[nextMove].getProb() < 1) {
                System.out.println(move[nextMove].asString());
                combProb = combProb * move[nextMove].getProb();
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
        
        double x2 = display.getFitWidth() * x1 / (double) gs.getx() + myPane.getLayoutX();
        double y2 = display.getFitHeight() * y1 / (double) gs.gety() + myPane.getLayoutY();
        
        double d = Math.max(display.getFitWidth() / gs.getx(), display.getFitHeight() / gs.gety());
        
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

        long startTime = System.currentTimeMillis();
        
        ImageView result = new ImageView();
        result.setPreserveRatio(false);
        result.fitHeightProperty().bind(myPane.heightProperty());
        result.fitWidthProperty().bind(myPane.widthProperty());   

        GameStateModel gs = Minesweeper.getGame();
        
        int x = gs.getx();
        int y = gs.gety();
        
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
 
        long duration = System.currentTimeMillis() - startTime;
        
        result.setImage(scr);
        

        
        //System.out.println("Screen refreshed in " + duration + " milliseconds");
        
        return result;
        
    }
    /*
    private void update(Location m) {
        
        int result = Minesweeper.getGame().query(m);
        
        System.out.println("result from query is " + result);
        
        PixelReader pr;
        
        if (result == GameState.MINE) {
            pr = Graphics.getMineBang().getPixelReader();
        } else {
            pr = Graphics.getNumber(result).getPixelReader();
        }

        addImage(scr.getPixelWriter(), m.x * IMAGE_SIZE, m.y * IMAGE_SIZE, pr);
        
        System.out.println("error flag is " + scr.isError());
         
    }
    */
    private void updateScreen() {
        
        ImageView temp = refreshScreen();
        
        myPane.getChildren().add(temp);
        myPane.getChildren().remove(display);
       
        display = temp;

        display.setOnMouseMoved(me);
        display.setOnMouseEntered(me);
        display.setOnMouseExited(me);
        
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
        } else if (query == GameStateModel.FLAG) {
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
        
        double x1 = gs.getx();
        double y1 = gs.gety();
        
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
        
    public int getGameType() {
        return gameType;
    }
    
    
    // pick the difficulty up from the menu option
    private void newGame() {
        
        newGame(this.difficulty);

    }

    // stop the current game and start a new one
    protected void newGame(int difficulty) {
        
        // create a new game state
        GameStateModel gs = Minesweeper.createNewGame(difficulty, gameType);

        // create a memory of the last screen - set to full refresh
        lastScreen = new int[gs.getx()][gs.gety()];
        for (int i=0; i < gs.getx(); i++) {
            for (int j=0; j < gs.gety(); j++) {
                lastScreen[i][j] = -1;
            }
        }

        if (solver != null) {
            solver.kill();
        }

        // if the screen image doesn't exist or is the wrong size then create a new one
        if (scr == null || IMAGE_SIZE * gs.getx() != scr.getWidth() || IMAGE_SIZE * gs.gety() != scr.getHeight()) {
            System.out.println("Creating a new Screen Image");
            scr = new WritableImage(IMAGE_SIZE * gs.getx(), IMAGE_SIZE * gs.gety());
        }
        
        updateScreen();
        
        // create a new solver
        solver = new Solver(gs, preferences, HelperController.launch(), true);
        solver.setFlagFree(flagFree.isSelected());
        combProb = 1;
 
        // forget any moves we have stored up
        move = new Action[0];
        nextMove = 0;
        
        
        // garbage collection
        System.gc();
        
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
