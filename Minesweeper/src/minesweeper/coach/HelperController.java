/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package minesweeper.coach;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import minesweeper.Minesweeper;
import minesweeper.solver.coach.CoachModel;

/**
 * FXML Controller class
 *
 * @author David
 */
public class HelperController implements CoachModel, Initializable {

    private static final Background BG_Red = new Background(new BackgroundFill(Color.RED, null, null));
    private static final Background BG_Green = new Background(new BackgroundFill(Color.GREEN, null, null));
    private static final Background BG_Orange = new Background(new BackgroundFill(Color.ORANGE, null, null));
	
    @FXML
    private TextArea text;
    
    //private HelperController helperController;
    public Stage stage;
    public Scene scene;
    
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        System.out.println("Entered Helper Control initialize method");
        // TODO
    }    
    
    /*
    public TextArea getTextArea() {
        return text;
    }
    */
    
    public void kill() {
        
    	stage.close();
    	
        System.out.println("Killing the Helper Control Object");        
        
    }
    
    public static HelperController launch() {
    	
            // create the helper screen
            FXMLLoader loader = new FXMLLoader(HelperController.class.getResource("Helper.fxml"));

            Parent root = null;
            try {
                root = (Parent) loader.load();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }

            HelperController coach = loader.getController();
            
            //helperController = loader.getController();

            coach.scene = new Scene(root);

            coach.stage = new Stage();

            coach.stage.setScene(coach.scene);
            coach.stage.setTitle("Minesweeper Coach");

            coach.writeLine("Minesweeper coach dedicated to Annie");

            //((AnchorPane) scene.getRoot()).setBackground(BG_Green);
            coach.setOkay();
            Stage st = Minesweeper.getStage();
            coach.stage.setX(st.getX()+ st.getWidth());
            coach.stage.setY(st.getY());

            coach.align();

            coach.stage.show();  
            
            return coach;
    }
    
    final public void align() {
        
        Stage st = Minesweeper.getStage();
        
        if (st != null) {
        	stage.setX(st.getX()+ st.getWidth());
        	stage.setY(st.getY());
        }

        stage.setHeight(500);
        
    }
    
    @Override
    final public void setOkay() {
    	setColor(BG_Green);
    }
    
    @Override
    final public void setWarn() {
    	setColor(BG_Orange);
    }
    
    @Override
    final public void setError() {
    	setColor(BG_Red);
    }
    
    private void setColor(Background c) {

    	((AnchorPane) scene.getRoot()).setBackground(c);

    }

	@Override
	public void clearScreen() {
		
		Platform.runLater(new Runnable() {
			@Override public void run() {
				text.clear();
			}
		});            
		
	}

	@Override
	public void writeLine(String line) {

		Platform.runLater(new Runnable() {
			@Override public void run() {
				String textLine = text.getText();
				if (textLine.equals("")) {
					textLine = line;
				} else {
					textLine = textLine + "\n" + line;
				}
				text.setText(textLine);
			}
		});            

	}

	@Override
	public boolean analyseFlags() {
		return true;
	}

}
