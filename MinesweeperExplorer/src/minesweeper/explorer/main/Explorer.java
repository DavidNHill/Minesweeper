package minesweeper.explorer.main;
	
import java.io.IOException;
import java.text.DecimalFormat;

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
	
	public static String APPLICATION_NAME = "Minesweeper explorer";
	public static String VERSION = "0.0";
	
	public static final Background BACKGROUND_PINK = new Background(new BackgroundFill(Color.PINK, null, null));
	public static final Background BACKGROUND_SILVER = new Background(new BackgroundFill(Color.SILVER, null, null));
	
	private final int tileSize = 24;
	
	public static final DecimalFormat PERCENT = new DecimalFormat("#0.000%");
	public static final DecimalFormat TWO_DP = new DecimalFormat("#0.00");
	public static final Background GREY_BACKGROUND = new Background(new BackgroundFill(Color.LIGHTGREY, null, null));

	private static Stage primaryStage;
	
	@Override
	public void start(Stage primaryStage) {
		
		System.out.println("Starting...");
		
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
		
		this.primaryStage = primaryStage;
		
		try {
			//BorderPane root = new BorderPane();
			Scene scene = new Scene(root,900,550);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle(APPLICATION_NAME);
			primaryStage.getIcons().add(Graphics.ICON);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Main screen running...");
		
		MainScreenController mainScreenController = loader.getController();
		
		if (mainScreenController == null) {
			System.out.println("MainScreenController not found");
		}
		
		mainScreenController.setGraphicsSet(graphicsSet);
		mainScreenController.newExpertBoard();
		
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
		
        double x = primaryStage.getX();
        double y = primaryStage.getY();
        
        mainScreenController.getTileValueController().getStage().setX(x + primaryStage.getWidth());
        mainScreenController.getTileValueController().getStage().setY(y);
        
        System.out.println("...Startup finished.");
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static void setSubTitle(String subTitle) {
		
		if (subTitle == null || subTitle.trim().isEmpty()) {
			primaryStage.setTitle(APPLICATION_NAME);
		} else {
			primaryStage.setTitle(APPLICATION_NAME + " - " + subTitle);
		}
		
		
	}
	
}
