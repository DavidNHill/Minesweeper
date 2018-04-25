/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import java.io.File;
import java.util.Optional;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import minesweeper.gamestate.GameStateStandard;
import minesweeper.gamestate.GameStateEasy;
import minesweeper.gamestate.GameStateHard;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.gamestate.GameStateReader;
import minesweeper.gamestate.msx.GameStateX;
import minesweeper.gamestate.msx.ScreenScanner;

/**
 *
 * @author David
 */
public class Minesweeper extends Application {
    
	public static final String TITLE = "Minesweeper coach";
	
    private static GameStateModelViewer myGame;
    
    private static Stage myStage = null;
    
    private static ScreenController myController;
    

    @Override
    public void start(Stage stage) throws Exception {
  
        myStage = stage;

        // this creates a hard game on start-up
        createNewGame(ScreenController.DIFFICULTY_HARD, ScreenController.GAMETYPE_NORMAL, null);

        System.out.println("creating root");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Screen.fxml"));
        
        Parent root = (Parent) loader.load();
        
        //Parent root = FXMLLoader.load(getClass().getResource("Screen.fxml"));
        
        myController = loader.getController();
        
        System.out.println("root created");
        
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.setTitle(TITLE);

        stage.getIcons().add(Graphics.getMine());
        
        stage.setX(50);
        stage.setY(50);
  
        stage.setWidth(1000);
        stage.setHeight(650);
        
        stage.show();
  
        //stage.setResizable(false);

        myController.newGame(ScreenController.DIFFICULTY_HARD);
        
        stage.setOnHidden(null);
        
        // actions to perform when a close rquest is received
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.out.println("Minesweeper window has received a close request");
                //event.consume();
                myController.kill();
            }
        });                        
 
    }

    // use the difficulty set in the menu system
    static public GameStateModel newGame() {
 
        return createNewGame(myController.getDifficulty(), myController.getGameType(), null);
        
    }
    
    // force a difficulty setting
    static public GameStateModel createNewGame(int difficulty, int gameType, File fileSelected) {
        
    	int width = 30;
    	int height = 16;
    	int mines = 99;
    	long gameCode = 0;
    	
    	
    	switch (difficulty) {
    	case ScreenController.DIFFICULTY_EASY: 
    		width = 9;
    		height = 9;
    		mines = 10;
    		break;
    	case ScreenController.DIFFICULTY_MEDIUM: 
    		width = 16;
    		height = 16;
    		mines = 40;
    		break;
    	case ScreenController.DIFFICULTY_HARD: 
    		width = 30;
    		height = 16;
    		mines = 99;
    		break;
    	case ScreenController.DEFER_TO_MINESWEEPERX:
    		ScreenScanner scanner = new ScreenScanner("Minesweeper X");
    		if (!scanner.isValid()) {
    			
    			System.out.println("MinsweeperX not found");
    			Alert alert = new Alert(AlertType.ERROR, "MinesweeperX can't be found: ensure it is maximised and unobstructed");
    			Optional<ButtonType> result = alert.showAndWait();
    			if (result.isPresent() && result.get() == ButtonType.OK) {
    			     return myGame;   // old game
    			}
    			
    		}
    		myGame = new GameStateX(scanner);
    		System.out.println("X = " + myGame.getx() + " Y =" + myGame.gety());
    		break;
    	case ScreenController.DIFFICULTY_FILE:
    		myGame = GameStateReader.load(fileSelected);
    		break;
    	case ScreenController.DIFFICULTY_CUSTOM:
    		CustomController custom = CustomController.getCustomController();
    		width = custom.getWidth();
    		height = custom.getHeight();
    		mines = custom.getMines();
    		
    		gameCode = custom.getGameCode();
    		
     		break;
    	default:
    		width = 30;
    		height = 16;
    		mines = 99;
    	}

    	// if we are shadowing minesweeperX then we don't need to do any more
    	if (difficulty == ScreenController.DEFER_TO_MINESWEEPERX || difficulty == ScreenController.DIFFICULTY_FILE) {
    		return myGame;
    	}
    	
    	
    	switch (gameType) {
    	case ScreenController.GAMETYPE_EASY: 
    		
    		if (gameCode == 0) {
    			myGame = new GameStateEasy(width, height, mines);
    		} else {
    			myGame = new GameStateEasy(width, height, mines, gameCode);
    		}
    		break;
    	case ScreenController.GAMETYPE_NORMAL: 
    		if (gameCode == 0) {
    			myGame = new GameStateStandard(width, height, mines);
    		} else {
    			myGame = new GameStateStandard(width, height, mines, gameCode);
    		}
    		break;
    	case ScreenController.GAMETYPE_HARD: 
    		if (gameCode == 0) {
    			myGame = new GameStateHard(width, height, mines);
    		} else {
    			myGame = new GameStateHard(width, height, mines, gameCode);
    		}
    		break;    	
    	default:
    		if (gameCode == 0) {
    			myGame = new GameStateStandard(width, height, mines);
    		} else {
    			myGame = new GameStateStandard(width, height, mines, gameCode);
    		}
    	}    	
    	
    	return myGame;        
        
    }
    
    
    static public GameStateModelViewer getGame() {
        
        return myGame;
        
    }
    
    static public Stage getStage() {
        
        return myStage;
        
    }
    
    @Override
    public void stop() {
      
        myController.stop();
        
    }
    
    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
