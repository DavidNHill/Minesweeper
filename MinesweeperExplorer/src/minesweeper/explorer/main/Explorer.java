package minesweeper.explorer.main;
	
import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import minesweeper.explorer.main.Graphics.GraphicsSet;

public class Explorer extends Application {
	
	private final int tileSize = 24;
	
	public static final Background GREY_BACKGROUND = new Background(new BackgroundFill(Color.LIGHTGREY, null, null));

	@Override
	public void start(Stage primaryStage) {
		
		if (Explorer.class.getResource("MainScreen.fxml") == null) {
			System.out.println("MainScreen.fxml not found");
		}

		Graphics graphics = new Graphics();
		GraphicsSet graphicsSet = graphics.getGraphicsSet(tileSize);
		
		// create the helper screen
		FXMLLoader loader = new FXMLLoader(Explorer.class.getResource("MainScreen.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}
		
		try {
			//BorderPane root = new BorderPane();
			Scene scene = new Scene(root,800,600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Minesweeper explorer");
			primaryStage.getIcons().add(Graphics.ICON);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		MainScreenController mainScreenController = loader.getController();
		
		mainScreenController.setGraphicsSet(graphicsSet);
		mainScreenController.newIntermediateBoard();
		
		BoardMonitor monitor = new BoardMonitor(mainScreenController);
		
		Thread monitorThread = new Thread(monitor, "Monitor");
		monitorThread.setDaemon(true);
		monitorThread.start();
		
        // actions to perform when a close request is received
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.out.println("Minesweeper explorer has received a close request");
                 //mainScreenController.kill();
                Platform.exit();
            }
        });                        
		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	
	
}
