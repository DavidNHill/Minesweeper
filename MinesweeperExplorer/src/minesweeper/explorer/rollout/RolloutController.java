package minesweeper.explorer.rollout;

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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import minesweeper.explorer.main.Explorer;
import minesweeper.explorer.main.Graphics;
import minesweeper.solver.RolloutGenerator;
import minesweeper.solver.bulk.BulkEventGame;
import minesweeper.solver.bulk.BulkEventMain;
import minesweeper.solver.bulk.BulkListener;
import minesweeper.solver.bulk.BulkRollout;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.solver.utility.Timer;
import minesweeper.structure.Location;

public class RolloutController {

	private final static DecimalFormat PERCENT = new DecimalFormat("#0.000%");
	private final static int DEFAULT_ROLLOUTS = 10000;
	private final static String[] THREAD_DROPDOWN = {"on 1 Thread", "on 2 Threads", "on 3 Threads", "on 4 Threads", "on 6 Threads", "on 8 Threads"};
	private final static int[] THREAD_NUMBER = {1, 2, 3, 4, 6, 8};
	
	
	@FXML private AnchorPane window;

	@FXML private TextField gameCount;
	@FXML private TextField gameSeed;
	@FXML private Label winPercentage;
	@FXML private Label fairnessPercentage;
	@FXML private Label bestWinStreak;
	@FXML private Label bestMastery;
	@FXML private Label totalGuesses;
	
	@FXML private ProgressBar progressRun;
	@FXML private Label progressRunLabel;
	@FXML private TextField startLocX;
	@FXML private TextField startLocY;
	@FXML private CheckBox safeStart;
	
	@FXML private Label messageBox;
	@FXML private ChoiceBox<String> threadsCombo;
	
	private Stage stage;
	private Scene scene;

	private int gamesMax;
	private long gameGenerator;

	//private GameSettings gameSettings;
	//private GameType gameType;
	private Location startLocation;
	private SolverSettings preferences;
	
	private RolloutGenerator generator;
	
	//private ResultsController resultsController;
	
	//private BulkRunner bulkRunner;
	
	private BulkRollout bulkRunner;
	
	private boolean wasCancelled = false;


	@FXML
	void initialize() {
		System.out.println("Entered Rollout Screen initialize method");


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
			gamesMax = DEFAULT_ROLLOUTS;
		} else {
			try {
				gamesMax = Integer.parseInt(gameCount.getText().trim());
				if (gamesMax < 1) {
					gamesMax = DEFAULT_ROLLOUTS;
				}
			} catch (NumberFormatException e) {
				gamesMax = DEFAULT_ROLLOUTS;
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
				if (startX >= 0 && startX < generator.getWidth() && startY >= 0 && startY < generator.getHeight()) {
					startLocation = new Location(startX, startY);
					System.out.println("Start location set to " + startLocation.toString());
				} else {
					System.out.println("Start location out of bounds");
				}
				
			} catch (NumberFormatException e) {
				System.out.println("Start location can't be parsed");
			}
		} else {
			System.out.println("Start location is not populated");
		}

		if (startLocation == null) {
			startLocX.setBackground(Explorer.BACKGROUND_PINK);
			startLocY.setBackground(Explorer.BACKGROUND_PINK);
		} else {
			startLocX.setBackground(Explorer.BACKGROUND_SILVER);
			startLocY.setBackground(Explorer.BACKGROUND_SILVER);
			
			gameSeed.setText(String.valueOf(gameGenerator));
			
			String dropdown = threadsCombo.getValue();
			int threads = 2;
			for (int i = 0; i < THREAD_DROPDOWN.length; i++) {
				if (dropdown.equals(THREAD_DROPDOWN[i])) {
					threads = THREAD_NUMBER[i];
					break;
				} 
			}
			
			bulkRunner = new BulkRollout(new Random(gameGenerator), gamesMax, generator, startLocation, safeStart.isSelected(), preferences, threads);
			bulkRunner.registerEventListener(new BulkListener() {
				@Override
				public void intervalAction(BulkEventMain event) {
					update(event);
				}
				
			});
			
			messageBox.setText("Starting...");
			new Thread(bulkRunner, "Bulk Run").start();
			

			
		}
		
	}

	@FXML
	private void handleCancelButton(ActionEvent event) {

		System.out.println("handleCancelButton method entered");
		
		stage.close();

	}

	public void show(RolloutGenerator generator, SolverSettings preferences ) {
		
		this.generator = generator;
		this.preferences = preferences;
		
		this.stage.setTitle("Rollout - " + generator + " - " + preferences.getGuessMethod().name);
		
		this.stage.show();
		
	}

	public static RolloutController launch(Window owner, RolloutGenerator generator, SolverSettings preferences ) {

		if (RolloutController.class.getResource("RolloutScreen.fxml") == null) {
			System.out.println("RolloutScreen.fxml not found");
		}

		// create the bulk runner screen
		FXMLLoader loader = new FXMLLoader(RolloutController.class.getResource("RolloutScreen.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}

		RolloutController custom = loader.getController();
		
		if (custom == null) {
			System.out.println("Custom is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		//custom.generator = generator;
		//custom.preferences = preferences;
		
		custom.scene = new Scene(root);

		custom.stage = new Stage();

		custom.stage.setScene(custom.scene);
		custom.stage.setTitle("Rollout");
		
		custom.stage.getIcons().add(Graphics.ICON);
		
		custom.stage.setResizable(false);

		custom.stage.initOwner(owner);
		custom.stage.initModality(Modality.WINDOW_MODAL);
		
		custom.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler");
				
				if (custom.bulkRunner != null && !custom.bulkRunner.isFinished()) {
					custom.bulkRunner.stop();
				}
				
				System.gc();
			
			}
			
		});
		
		custom.gameCount.setText(String.valueOf(DEFAULT_ROLLOUTS));
		custom.progressRun.setProgress(0d);
		custom.progressRunLabel.setText("");
		
		custom.threadsCombo.getItems().addAll(THREAD_DROPDOWN);
		custom.threadsCombo.setValue(THREAD_DROPDOWN[1]);
		
		//custom.getStage().show();

		return custom;
	}

	public Stage getStage() {
		return this.stage;
	}
	
	public boolean wasCancelled() {
		return wasCancelled;
	}
	
	private void update(BulkEventMain mainEvent) {
		
		final BulkEventGame gameEvent = mainEvent.getGameEvents()[0];
		
        Platform.runLater(new Runnable() {
            @Override public void run() {
            	double prog = (double) gameEvent.getGamesPlayed() / (double) gameEvent.getGamesToPlay();
            	progressRun.setProgress(prog);
            	
            	progressRunLabel.setText(gameEvent.getGamesPlayed() + "(" + gameEvent.getGamesWon() + ") /" + gameEvent.getGamesToPlay());
            	
            	double winPerc = (double) gameEvent.getGamesWon() / (double) gameEvent.getGamesPlayed();
            	
            	double err = Math.sqrt(winPerc * ( 1- winPerc) / (double) gameEvent.getGamesPlayed()) * 1.9599d;
            	
            	winPercentage.setText(PERCENT.format(winPerc) + " +/- " + PERCENT.format(err));
            	
            	totalGuesses.setText(String.valueOf(gameEvent.getTotalGuesses()));
            	
            	String fairnessText = PERCENT.format(gameEvent.getFairness());
            	
            	fairnessPercentage.setText(fairnessText);
            	bestWinStreak.setText(String.valueOf(gameEvent.getWinStreak()));
            	bestMastery.setText(String.valueOf(gameEvent.getMastery()));
            	
            	if (gameEvent.getGamesPlayed() == gameEvent.getGamesToPlay()) {
            		messageBox.setText("Duration " + Timer.humanReadable(mainEvent.getTimeSoFar()));
            	} else if (mainEvent.isFinished()) {
            		messageBox.setText("Stopped after " + Timer.humanReadable(mainEvent.getTimeSoFar()));
            	} else {
                	messageBox.setText("Time left " + Timer.humanReadable(mainEvent.getEstimatedTimeLeft()));
            	}
            	
        }
      });            
		
		
	}
	
	/*
	public void update(int steps, int maxSteps, int wins, int guesses, double fairness, int winStreak, int mastery) {
		
        Platform.runLater(new Runnable() {
            @Override public void run() {
            	double prog = (double) steps / (double) maxSteps;
            	progressRun.setProgress(prog);
            	
            	progressRunLabel.setText(steps + "(" + wins + ") /" + maxSteps);
            	
            	double winPerc = (double) wins / (double) steps;
            	
            	double err = Math.sqrt(winPerc * ( 1- winPerc) / (double) steps) * 1.9599d;
            	
            	winPercentage.setText(PERCENT.format(winPerc) + " +/- " + PERCENT.format(err));
            	
            	totalGuesses.setText(String.valueOf(guesses));
            	
            	String fairnessText;
            	if (guesses == 0) { 
            		fairnessText = "--";
            	} else {
            		fairnessText = PERCENT.format(fairness / guesses);
            	}
            	
            	fairnessPercentage.setText(fairnessText);
            	bestWinStreak.setText(String.valueOf(winStreak));
            	bestMastery.setText(String.valueOf(mastery));

            	
        }
      });            
		
		
	}
	*/

}
