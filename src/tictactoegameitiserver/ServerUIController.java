package tictactoegameitiserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    public static ServerSocket server=null;
    
    public static boolean isFinished=true;
    
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
        try {
            server=new ServerSocket(5005);
        } catch (IOException ex) {
            Logger.getLogger(ServerUIController.class.getName()).log(Level.SEVERE, null, ex);
        }
        ServerUIController.isFinished=false;
        Thread start=new Thread(new Runnable() {
            @Override
            public void run() {
                while(!isFinished){
                    try {
                        
                        Socket s=server.accept();
                        new ServerHandler(s);
                    } catch (IOException ex) {
                        Logger.getLogger(ServerUIController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        start.start();
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
