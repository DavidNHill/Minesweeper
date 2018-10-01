package minesweeper.bulk;

import java.io.IOException;
import java.text.DecimalFormat;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import minesweeper.Graphics;
import minesweeper.Minesweeper;
import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.gamestate.GameStateModelViewer;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;

public class ResultsController {

	
	
	private Stage stage;
	private Scene scene;

	@FXML private TableView<TableData> resultsTable;
	@FXML private TableColumn<TableData, Integer> columnCount;
	@FXML private TableColumn<TableData, Long> columnSeed;
	@FXML private TableColumn<TableData, Integer> columnComplete;
	@FXML private TableColumn<TableData, String> columnResult;
	
	private GameSettings gameSettings;
	private GameType gameType;
	private int count = 0;
	private boolean closed = false;
	
	private ObservableList<TableData> items = FXCollections.observableArrayList();
	
	
	@FXML
	void initialize() {
		System.out.println("Entered Bulk Result Screen initialize method");


	}

	@FXML
	private void handlePlayButton(ActionEvent event) {
	
		System.out.println("handlePlayButton method entered");
		
		TableData selected = resultsTable.getSelectionModel().getSelectedItem();
		
		long seed = selected.seedProperty().longValue();
		
		System.out.println("Selected seed " + seed);
		
		GameStateModelViewer gs = GameFactory.create(gameType, gameSettings, seed);
		
		Minesweeper.playGame(gs);
		
	}


	public static ResultsController launch(Window owner, GameSettings gameSettings, GameType gameType) {


		if (ResultsController.class.getResource("BulkScreen.fxml") == null) {
			System.out.println("BulkScreen.fxml not found");
		}

		// create the bulk runner screen
		FXMLLoader loader = new FXMLLoader(ResultsController.class.getResource("BulkResults.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}

		ResultsController custom = loader.getController();
		
		if (custom == null) {
			System.out.println("Custom is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		custom.scene = new Scene(root);

		custom.stage = new Stage();

		custom.stage.setScene(custom.scene);
		custom.stage.setTitle("Bulk run results");
		
		custom.stage.getIcons().add(Graphics.getMine());
		
		custom.stage.setResizable(false);

		custom.stage.initOwner(owner);
		custom.stage.initModality(Modality.WINDOW_MODAL);
		
		custom.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler for Bulk Results Screen");
				
				custom.closed = true;
				custom.items.clear();  // clear down the table to encourage the items to be garbage collected
				
			}
			
		});
		
		custom.resultsTable.setItems(custom.items);
		
		custom.resultsTable.getSelectionModel();
		
		custom.columnCount.setCellValueFactory(new PropertyValueFactory<TableData, Integer>("count"));
		custom.columnSeed.setCellValueFactory(new PropertyValueFactory<TableData, Long>("seed"));
		custom.columnComplete.setCellValueFactory(new PropertyValueFactory<TableData, Integer>("complete"));
		custom.columnResult.setCellValueFactory(new PropertyValueFactory<TableData, String>("result"));
		
		custom.getStage().show();

		custom.gameType = gameType;
		custom.gameSettings = gameSettings;
		
		//System.out.println("Columns = " + custom.resultsTable.getColumns().size());
		
		return custom;
	}

	public Stage getStage() {
		return this.stage;
	}
	

	
	public boolean update(GameStateModel gs) {

		if (closed) {  // if the window has been closed then let anyone who calls know
			return false;
		}


		count++;
		final TableData td = new TableData(count, gs);

		Platform.runLater(new Runnable() {
			@Override public void run() {

				items.add(td);

			}
		});            

		return true;

	}

}
