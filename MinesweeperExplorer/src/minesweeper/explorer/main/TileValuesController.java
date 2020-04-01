package minesweeper.explorer.main;

import java.io.IOException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import minesweeper.solver.constructs.InformationLocation;
import minesweeper.solver.constructs.InformationLocation.ByValue;

public class TileValuesController {

	private final static String WINDOW_NAME = "Tile values";
	
	private Stage stage;
	private Scene scene;

	@FXML private TableView<TileValueData> resultsTable;
	@FXML private TableColumn<TileValueData, String> columnValue;
	@FXML private TableColumn<TileValueData, String> columnProbability;
	@FXML private TableColumn<TileValueData, String> columnClears;
	
	private boolean closed = false;
	
	private ObservableList<TileValueData> items = FXCollections.observableArrayList();
	
	
	@FXML
	void initialize() {
		System.out.println("Entered Tile Values Screen initialize method");

	}

	public static TileValuesController launch(Window owner) {


		if (TileValuesController.class.getResource("TileValuesScreen.fxml") == null) {
			System.out.println("TileValuesScreen.fxml not found");
		}

		// create the bulk runner screen
		FXMLLoader loader = new FXMLLoader(TileValuesController.class.getResource("TileValuesScreen.fxml"));

		Parent root = null;
		try {
			root = (Parent) loader.load();
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
		}

		TileValuesController custom = loader.getController();
		
		if (custom == null) {
			System.out.println("Custom is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		custom.scene = new Scene(root);

		custom.stage = new Stage();

		custom.stage.setScene(custom.scene);
		custom.stage.setTitle(WINDOW_NAME);
		
		custom.stage.getIcons().add(Graphics.ICON);
		
		custom.stage.setResizable(false);

		//custom.stage.initOwner(owner);
		//custom.stage.initModality(Modality.WINDOW_MODAL);
		
		custom.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler for Tile Values Screen");
				
				custom.closed = true;
				custom.items.clear();  // clear down the table to encourage the items to be garbage collected
				
			}
			
		});
		
		custom.resultsTable.setItems(custom.items);
		
		custom.resultsTable.getSelectionModel();
		
		custom.columnValue.setCellValueFactory(new PropertyValueFactory<TileValueData, String>("value"));
		custom.columnProbability.setCellValueFactory(new PropertyValueFactory<TileValueData, String>("probability"));
		custom.columnClears.setCellValueFactory(new PropertyValueFactory<TileValueData, String>("clears"));
		
		custom.getStage().show();

		//System.out.println("Columns = " + custom.resultsTable.getColumns().size());
		
		return custom;
	}

	public Stage getStage() {
		return this.stage;
	}
	

	
	public boolean update(final InformationLocation il) {

		if (closed) {  // if the window has been closed then let anyone who calls know
			return false;
		}
		
		Platform.runLater(new Runnable() {
			@Override public void run() {
				items.clear();

				if (il == null || il.getByValueData() == null) {
					return;
				}

				stage.setTitle(WINDOW_NAME + " (" + il.x + "," + il.y + ")");
				
				for (ByValue bv: il.getByValueData()) {
					items.add(new TileValueData(bv));
				}
				items.add(new TileValueData("Safe", Explorer.PERCENT.format(il.getProbability()), ""));
				items.add(new TileValueData("Progress", Explorer.PERCENT.format(il.getProgressProbability()), Explorer.TWO_DP.format(il.getExpectedClears())));
				items.add(new TileValueData("Weighting", Explorer.PERCENT.format(il.getWeighting()), ""));
			}
		});            

		return true;

	}

}
