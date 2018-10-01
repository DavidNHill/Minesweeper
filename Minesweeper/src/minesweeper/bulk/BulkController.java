package minesweeper.bulk;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import minesweeper.Graphics;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.Location;
import minesweeper.random.DefaultRNG;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;

public class BulkController {

	private final static DecimalFormat PERCENT = new DecimalFormat("#0.000%");
	
	@FXML private AnchorPane window;

	@FXML private TextField gameCount;
	@FXML private TextField gameSeed;
	@FXML private Label winPercentage;
	@FXML private ProgressBar progressRun;
	@FXML private Label progressRunLabel;
	@FXML private TextField startLocX;
	@FXML private TextField startLocY;
	@FXML private CheckBox showGames;
	
	private Stage stage;
	private Scene scene;

	private int gamesMax;
	private long gameGenerator;

	private GameSettings gameSettings;
	private GameType gameType;
	private Location startLocation;
	
	//private ResultsController resultsController;
	
	private BulkRunner bulkRunner;
	
	private boolean wasCancelled = false;


	@FXML
	void initialize() {
		System.out.println("Entered Bulk Screen initialize method");


	}

	@FXML
	private void handleNewSeedButton(ActionEvent event) {
		gameSeed.setText(String.valueOf(new Random().nextLong()));
	}
	
	@FXML
	private void handleOkayButton(ActionEvent event) {
	
		System.out.println("handleOkayButton method entered");
		
		if (bulkRunner != null && !bulkRunner.isFinished()) {
			System.out.println("Previous bulk run still running");
			return;
		}
		
		if (gameCount.getText().trim().isEmpty()) {
			gamesMax = 1000;
		} else {
			try {
				gamesMax = Integer.parseInt(gameCount.getText().trim());
				if (gamesMax < 1) {
					gamesMax = 1000;
				}
			} catch (NumberFormatException e) {
				gamesMax = 1000;
			}
		}
		
		gameCount.setText(String.valueOf(gamesMax));
		
		if (gameSeed.getText().trim().isEmpty()) {
			gameGenerator = new Random().nextLong();
		} else {
			try {
				gameGenerator = Long.parseLong(gameSeed.getText().trim());
			} catch (NumberFormatException e) {
				gameGenerator = new Random().nextLong();
				//gameSeed.setText("");
			}
		}
		
		startLocation = null;
		if (!startLocX.getText().trim().isEmpty() && !startLocY.getText().trim().isEmpty()) {
			try {
				int startX = Integer.parseInt(startLocX.getText().trim());
				int startY = Integer.parseInt(startLocY.getText().trim());
				if (startX >= 0 && startX < gameSettings.width && startY >= 0 && startY < gameSettings.height) {
					startLocation = new Location(startX, startY);
					System.out.println("Start location set to " + startLocation.display());
				}
				
			} catch (NumberFormatException e) {
			}
		}

		gameSeed.setText(String.valueOf(gameGenerator));
		
		bulkRunner = new BulkRunner(this, gamesMax, gameSettings, gameType, gameGenerator, startLocation, showGames.isSelected());
		
		new Thread(bulkRunner, "Bulk Run").start();
		

	}

	@FXML
	private void handleCancelButton(ActionEvent event) {

		System.out.println("handleCancelButton method entered");
		
		stage.close();

	}


	public static BulkController launch(Window owner, GameSettings gameSettings, GameType gameType) {

		if (BulkController.class.getResource("BulkScreen.fxml") == null) {
			System.out.println("BulkScreen.fxml not found");
		}

		// create the bulk runner screen
		FXMLLoader loader = new FXMLLoader(BulkController.class.getResource("BulkScreen.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}

		BulkController custom = loader.getController();
		
		if (custom == null) {
			System.out.println("Custom is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		custom.gameSettings = gameSettings;
		custom.gameType = gameType;
		
		custom.scene = new Scene(root);

		custom.stage = new Stage();

		custom.stage.setScene(custom.scene);
		custom.stage.setTitle("Bulk run - " + gameSettings.description() + ", " + gameType.name + ", " + DefaultRNG.getRNG(1).shortname());
		
		custom.stage.getIcons().add(Graphics.getMine());
		
		custom.stage.setResizable(false);

		custom.stage.initOwner(owner);
		custom.stage.initModality(Modality.WINDOW_MODAL);
		
		custom.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler");
				
				if (custom.bulkRunner != null) {
					custom.bulkRunner.forceStop();
				}		
				
				System.gc();
			
			}
			
		});
		
		custom.gameCount.setText("1000");
		custom.progressRun.setProgress(0d);
		custom.progressRunLabel.setText("");
		
		//custom.resultsController = ResultsController.launch(null, gameSettings, gameType);
		
		
		custom.getStage().show();

		return custom;
	}

	public Stage getStage() {
		return this.stage;
	}
	
	public boolean wasCancelled() {
		return wasCancelled;
	}
	
	public void update(int steps, int maxSteps, int wins) {
		
        Platform.runLater(new Runnable() {
            @Override public void run() {
            	double prog = (double) steps / (double) maxSteps;
            	progressRun.setProgress(prog);
            	
            	progressRunLabel.setText(steps + "/" + maxSteps);
            	
            	double winPerc = (double) wins / (double) steps;
            	
            	double err = Math.sqrt(winPerc * ( 1- winPerc) / (double) steps) * 1.9599d;
            	
            	winPercentage.setText(PERCENT.format(winPerc) + " +/- " + PERCENT.format(err));
        }
      });            
		
		
	}
	
	/*
	public void storeResult(GameStateModel gs) {
		
		resultsController.update(gs);
		
	}
	*/
}
