package minesweeper.explorer.busy;

import java.io.IOException;
import java.text.DecimalFormat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import minesweeper.explorer.main.Graphics;
import minesweeper.solver.utility.ProgressMonitor;

public class BusyController {

	private final static DecimalFormat PERCENT = new DecimalFormat("#0.000%");

	@FXML private AnchorPane window;

	@FXML private ProgressBar progressRun;

	private Stage stage;
	private Scene scene;
	private Thread myThread;
	private boolean finished = false;
	private boolean wasCancelled = false;
	private MonitorTask monitorTask;


	@FXML
	void initialize() {
		System.out.println("Entered Busy Screen initialize method");


	}

	@FXML
	private void handleOkayButton(ActionEvent event) {
	
		System.out.println("handleOkayButton method entered");
		
		
	}

	@FXML
	private void handleCancelButton(ActionEvent event) {

		System.out.println("handleCancelButton method entered");
		
		stage.close();

	}


	public static BusyController launch(Window owner, ParallelTask runnable, ProgressMonitor monitor) {

		if (BusyController.class.getResource("BusyScreen.fxml") == null) {
			System.out.println("BusyScreen.fxml not found");
		}

		// create the bulk runner screen
		FXMLLoader loader = new FXMLLoader(BusyController.class.getResource("BusyScreen.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}

		BusyController busyController = loader.getController();
		
		if (busyController == null) {
			System.out.println("Busy controller is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		busyController.scene = new Scene(root);

		busyController.stage = new Stage();

		busyController.stage.setScene(busyController.scene);
		busyController.stage.setTitle("Busy");
		
		busyController.stage.getIcons().add(Graphics.ICON);
		
		busyController.stage.setResizable(false);

		busyController.stage.initOwner(owner);
		busyController.stage.initModality(Modality.WINDOW_MODAL);
		//busyController.stage.setOpacity(0.9);
		
		busyController.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler");
				
			}
			
		});

		busyController.progressRun.setProgress(0d);
		//busyController.progressMonitor = monitor;
		
		// this will update the progress bar
		if (monitor != null) {
			busyController.monitorTask = new MonitorTask(monitor, busyController);
			new Thread(busyController.monitorTask).start();
		}
		
		busyController.myThread = Thread.currentThread();

		runnable.setController(busyController);
		new Thread(runnable).start();
		
		// see if the task finishes in less than a second
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		if (!busyController.finished) {
			busyController.stage.show();
		}
		
		return busyController;
	}

	public Stage getStage() {
		return this.stage;
	}
	
	protected void finished() {
		
		finished = true;

		if (monitorTask != null) {
			monitorTask.stop();  // cancel the progress monitor
		}
		
		myThread.interrupt();  // wake the thread
		
		if (Platform.isFxApplicationThread()) {
			stage.hide();
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					stage.hide();
				}
			});
		}
	}
	
	public boolean wasCancelled() {
		return wasCancelled;
	}
	
	public void update(ProgressMonitor progress) {
		
		if (progress.getMaxProgress() == 0) {
			return;
		}
		
        Platform.runLater(new Runnable() {
            @Override public void run() {
            	double prog = (double) progress.getProgress() / (double) progress.getMaxProgress();
            	progressRun.setProgress(prog);
            }
      });            
		
		
	}
	

}
