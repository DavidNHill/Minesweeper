/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper;

import java.io.File;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
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
import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateEasy;
import minesweeper.gamestate.GameStateHard;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.gamestate.GameStateReader;
import minesweeper.gamestate.msx.GameStateX;
import minesweeper.gamestate.msx.ScreenScanner;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;

/**
 *
 * @author David
 */
public class Minesweeper extends Application {
    
	public final static String VERSION = "1.03b";
	
	public static final String TITLE = "Minesweeper coach (" + VERSION + ")";
	
    private static GameStateModelViewer myGame;
    private static GameSettings gameSettings;
    
    private static Stage myStage = null;
    
    private static ScreenController myController;
    

    @Override
    public void start(Stage stage) throws Exception {
  
        myStage = stage;

        // this creates a hard game on start-up
        createNewGame(ScreenController.DIFFICULTY_EXPERT, GameType.STANDARD, null);

        System.out.println("creating root");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Screen.fxml"));
        
        Parent root = (Parent) loader.load();
        
  
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

        myController.newGame(ScreenController.DIFFICULTY_EXPERT);
        
        stage.setOnHidden(null);
        
        // actions to perform when a close request is received
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.out.println("Minesweeper window has received a close request");
                //event.consume();
                myController.kill();
                Platform.exit();
            }
        });                        
 
    }

    // use the difficulty set in the menu system
    static public GameStateModel newGame() {
 
        return createNewGame(myController.getDifficulty(), myController.getGameType(), null);
        
    }
    
    // force a difficulty setting
    static public GameStateModel createNewGame(int difficulty, GameType gameType, File fileSelected) {
        
    	long gameCode = 0;
    	gameSettings = GameSettings.EXPERT;
    	
    	switch (difficulty) {
    	case ScreenController.DIFFICULTY_BEGINNER: 
    		gameSettings = GameSettings.BEGINNER;
    		break;
    	case ScreenController.DIFFICULTY_ADVANCED: 
    		gameSettings = GameSettings.ADVANCED;
    		break;
    	case ScreenController.DIFFICULTY_EXPERT: 
    		gameSettings = GameSettings.EXPERT;
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
    		System.out.println("X = " + myGame.getWidth() + " Y =" + myGame.getHeight());
    		break;
    	case ScreenController.DIFFICULTY_FILE:
    		GameStateModelViewer game;
			try {
				game = GameStateReader.load(fileSelected);
				myGame = game;
			} catch (Exception e) {
    			Alert alert = new Alert(AlertType.ERROR, e.getLocalizedMessage());
    			Optional<ButtonType> result = alert.showAndWait();
				return null;
			}
    		break;
    	case ScreenController.DIFFICULTY_CUSTOM:
    		CustomController custom = CustomController.getCustomController();
    		gameSettings = custom.getGameSettings();
    		gameCode = custom.getGameCode();
    		
     		break;
    	default:
    		gameSettings = GameSettings.EXPERT;
    	}

    	// if we are shadowing minesweeperX then we don't need to do any more
    	if (difficulty == ScreenController.DEFER_TO_MINESWEEPERX || difficulty == ScreenController.DIFFICULTY_FILE) {
    		return myGame;
    	}
    	
    	
    	myGame = GameFactory.create(gameType, gameSettings, gameCode);
    	
    	/*
    	switch (gameType) {
    	case GameType.: 
    		
    		if (gameCode == 0) {
    			myGame = new GameStateEasy(gameSettings);
    		} else {
    			myGame = new GameStateEasy(gameSettings, gameCode);
    		}
    		break;
    	case ScreenController.GAMETYPE_NORMAL: 
    		if (gameCode == 0) {
    			myGame = new GameStateStandard(gameSettings);
    		} else {
    			myGame = new GameStateStandard(gameSettings, gameCode);
    		}
    		break;
    	case ScreenController.GAMETYPE_HARD: 
    		if (gameCode == 0) {
    			myGame = new GameStateHard(gameSettings);
    		} else {
    			myGame = new GameStateHard(gameSettings, gameCode);
    		}
    		break;    	
    	default:
    		if (gameCode == 0) {
    			myGame = new GameStateStandard(gameSettings);
    		} else {
    			myGame = new GameStateStandard(gameSettings, gameCode);
    		}
    	}    	
    	*/
    	
    	return myGame;        
        
    }
    
    
    static public GameStateModelViewer getGame() {
         return myGame;
    }
    
    static public void playGame(GameStateModelViewer gs) {
    	myGame = gs;
    	myController.newGame(gs);
    }
    
    
    static public GameSettings getGameSettings() {
    	return gameSettings;
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
