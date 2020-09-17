package minesweeper.explorer.main;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

		TileValuesController tileValuesController = loader.getController();
		
		if (tileValuesController == null) {
			System.out.println("Custom is null");
		}

		if (root == null) {
			System.out.println("Root is null");
		}

		tileValuesController.scene = new Scene(root);

		tileValuesController.stage = new Stage();

		tileValuesController.stage.setScene(tileValuesController.scene);
		tileValuesController.stage.setTitle(WINDOW_NAME);
		
		tileValuesController.stage.getIcons().add(Graphics.ICON);
		
		tileValuesController.stage.setResizable(false);

		//custom.stage.initOwner(owner);
		//custom.stage.initModality(Modality.WINDOW_MODAL);
		
		tileValuesController.stage.setOnCloseRequest(new EventHandler<WindowEvent>() {

			@Override
			public void handle(WindowEvent event) {
				System.out.println("Entered OnCloseRequest handler for Tile Values Screen");
				
				tileValuesController.closed = true;
				tileValuesController.items.clear();  // clear down the table to encourage the items to be garbage collected
				
			}
			
		});
		
		tileValuesController.resultsTable.setItems(tileValuesController.items);
		
		tileValuesController.resultsTable.getSelectionModel();
		
		tileValuesController.columnValue.setCellValueFactory(new PropertyValueFactory<TileValueData, String>("value"));
		tileValuesController.columnProbability.setCellValueFactory(new PropertyValueFactory<TileValueData, String>("probability"));
		tileValuesController.columnClears.setCellValueFactory(new PropertyValueFactory<TileValueData, String>("clears"));
		
		tileValuesController.getStage().show();

		System.out.println("Tile values screen running...");
		
		//System.out.println("Columns = " + custom.resultsTable.getColumns().size());
		
		return tileValuesController;
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
				
				BigDecimal riskReward = null;
				if (il.getProbability().compareTo(BigDecimal.ONE) != 0) {
					riskReward = il.getProgressProbability().divide(BigDecimal.ONE.subtract(il.getProbability()), 2, RoundingMode.HALF_UP);
				}
				
				for (ByValue bv: il.getByValueData()) {
					items.add(new TileValueData(bv));
				}
				items.add(new TileValueData("Safe", Explorer.PERCENT.format(il.getProbability()), ""));
				items.add(new TileValueData("Progress", Explorer.PERCENT.format(il.getProgressProbability()), Explorer.TWO_DP.format(il.getExpectedClears())));
				items.add(new TileValueData("Score", Explorer.PERCENT.format(il.getWeighting()), ""));
				items.add(new TileValueData("Exp Sol redn", Explorer.PERCENT.format(il.getExpectedSolutionSpaceReduction()), ""));
				if (riskReward != null) {
					items.add(new TileValueData("Prog/Risk", riskReward.toPlainString(), ""));
				}


			}
		});            

		return true;

	}

}
