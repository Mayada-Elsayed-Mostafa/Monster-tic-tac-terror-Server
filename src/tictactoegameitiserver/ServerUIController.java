package tictactoegameitiserver;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class ServerUIController implements Initializable {
    
    private Label label;
    @FXML
    private BorderPane borderPane;
    @FXML
    private FlowPane buttonsFlowPane;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Label availableLabel;
    @FXML
    private Label numberOfAvailableLabel;   // This number changes dynamically
    @FXML
    private Label inGameLabel;
    @FXML
    private Label numberOfInGameLabel;      // This number changes dynamically
    @FXML
    private Label offlineLabel;
    @FXML
    private Label numberOfOfflineLabel;     // This number changes dynamically, when the server starts, it should have the number of users stored in the database
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        // Fetch the numbers from the database and assign them to the labels
    }    

    @FXML
    private void handleStartBtn(ActionEvent event){
        // This button is clickable by default and when we click on it, it becomes unclickable
        // We should write the logic of Start Button Here -->> the server starting to listen to connections
        // When we click on this button, the pie chart appears
        // This pieChart should be updated once a client signed in, entered a game, or signed out
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Available", 10),
                new PieChart.Data("In-game", 4),
                new PieChart.Data("Offline", 20)
        );
        
        PieChart pieChart = new PieChart(pieChartData);
        pieChart.setClockwise(true);
        pieChart.setLabelLineLength(50);
        pieChart.setLabelsVisible(true);
        pieChart.setStartAngle(180);
        borderPane.setCenter(pieChart);
        
        
    }

    @FXML
    private void handleStopBtn(ActionEvent event){
        // This button is not clickable by default and when we click on the Start button, it becomes clickable
        // We should write the logic of Stop Button Here
        // When we click on it, the pieChart disappears
    }
}
