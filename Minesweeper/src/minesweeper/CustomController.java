package minesweeper;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import minesweeper.coach.HelperController;
import minesweeper.gamestate.GameStateModel;
import minesweeper.settings.GameSettings;

public class CustomController {

	@FXML
	private AnchorPane window;

	@FXML private TextField heightText;
	@FXML private TextField widthText;
	@FXML private TextField minesText;
	@FXML private TextField gameCodeText;


	private Stage stage;
	private Scene scene;

	private int height;
	private int width;
	private int mines;
	private GameSettings gameSettings;

	private long gameCode;
	
	private static CustomController custom;
	
	private boolean wasCancelled = false;

	/**
	 * Initializes the controller class.
	 */
	/*
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        System.out.println("Entered Custom Control initialize method");
     }    
	 */

	@FXML
	void initialize() {
		System.out.println("Entered Custom Control initialize method");

		if (heightText == null) {
			System.out.println("heightText is null");
		}

	}

	@FXML
	private void handleOkayButton(ActionEvent event) {

		this.mines = StringToInteger(minesText.getText(), 1, 9999, this.mines);
		this.width = StringToInteger(widthText.getText(), 4, 999, this.width);
		this.height = StringToInteger(heightText.getText(), 4, 999, this.height);
		
		try {
			this.gameCode = Long.parseLong(gameCodeText.getText());
		} catch (NumberFormatException e) {
			this.gameCode = 0;
		}
		
		if (mines > width * height - 10) {
			mines = width * height - 10;
		}
		
		gameSettings = GameSettings.create(width, height, mines);
		
		stage.close();
		
	}

	@FXML
	private void handleCancelButton(ActionEvent event) {

		wasCancelled = true;
		stage.close();

	}


	public static CustomController launch(Window owner, GameStateModel game) {


		
		// if we have already created it then show it and return
		if (custom != null) {
			//custom.stage.show();
			custom.wasCancelled = false;
			return custom;
		}

		if (CustomController.class.getResource("Custom.fxml") == null) {
			System.out.println("Custom.fxml not found");
		}

		// create the helper screen
		FXMLLoader loader = new FXMLLoader(CustomController.class.getResource("Custom.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}

		custom = loader.getController();

		//helperController = loader.getController();

		if (custom == null) {
			System.out.println("Custom is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		custom.scene = new Scene(root);

		custom.stage = new Stage();

		custom.stage.setScene(custom.scene);
		custom.stage.setTitle("Custom board");
		
		custom.stage.getIcons().add(Graphics.getMine());
		
		custom.stage.setResizable(false);

		custom.stage.initOwner(owner);
		custom.stage.initModality(Modality.WINDOW_MODAL);

		custom.width = game.getWidth();
		custom.height = game.getHeight();
		custom.mines = game.getMines();

		custom.widthText.setText(String.valueOf(custom.width));
		custom.heightText.setText(String.valueOf(custom.height));
		custom.minesText.setText(String.valueOf(custom.mines));

		//Stage st = Minesweeper.getStage();
		//custom.stage.setX(st.getX()+ st.getWidth());
		//custom.stage.setY(st.getY());

		custom.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler");
				custom.wasCancelled = true;
			
			}
			
		});
		
		
		return custom;
	}

	private int StringToInteger(String text, int min, int max, int dflt) {
		
		int val = dflt;
		try {
			val = Integer.parseInt(text);
		} catch (NumberFormatException e) {
		}

		val = Math.max(val, min);
		val = Math.min(val, max);
		
		return val;
		
	}
	
	
	public static CustomController getCustomController() {
		return custom;
	}
	
	public Stage getStage() {
		return this.stage;
	}
	
	public GameSettings getGameSettings() {
		return this.gameSettings;
	}
	
	
	public int getMines() {
		return this.mines;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public long getGameCode() {
		return this.gameCode;
	}
	
	public boolean wasCancelled() {
		return wasCancelled;
	}

}
