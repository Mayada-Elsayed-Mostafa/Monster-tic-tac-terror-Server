package tictactoegameitiserver;

import DB.DAO;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.sql.SQLException;
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

    public static ServerSocket server = null;

    public static boolean isFinished = true;

    public static boolean isClosed = true;

    private static Label numberOfAvailable;
    private static Label numberOfInGame;
    private static Label numberOfOffline;
    private static BorderPane pane;

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
        stopBtn.setDisable(true);
        numberOfAvailable = numberOfAvailableLabel;
        numberOfInGame = numberOfInGameLabel;
        numberOfOffline = numberOfOfflineLabel;
        pane = borderPane;
    }

    @FXML
    private void handleStartBtn(ActionEvent event) throws SQLException {
        ServerHandler.isFinished = false;
        DAO.setAllOff();
        startServer();
        isClosed = false;
        stopBtn.setDisable(false);
        startBtn.setDisable(true);
        updateLabels();
        drawPieChart();
    }

    @FXML
    private void handleStopBtn(ActionEvent event) throws IOException, SQLException {
        isClosed = true;
        stopBtn.setDisable(true);
        startBtn.setDisable(false);
        borderPane.setCenter(null);
        clearLabels();
        ServerHandler.closeServer();
        if (ServerUIController.server != null) {
            ServerUIController.server.close();
            server = null;
        }
        ServerHandler.isFinished = true;
    }

    public static void updateLabels() {
        try {
            if (!isClosed) {
                numberOfAvailable.setText(DAO.getAvailablePlayersForServer() + "");
                numberOfInGame.setText(DAO.getInGamePlayersForServer() + "");
                numberOfOffline.setText(DAO.getOfflinePlayersForServer() + "");
            }
        } catch (SQLException ex) {
            Logger.getLogger(ServerUIController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startServer() {
        try {
            server = new ServerSocket(5005);
        } catch (IOException ex) {
            Logger.getLogger(ServerUIController.class.getName()).log(Level.SEVERE, null, ex);
        }
        ServerUIController.isFinished = false;
        Thread start = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isFinished) {
                    try {
                        if (server != null) {
                            Socket s = server.accept();
                            ServerHandler.isFinished = false;
                            new ServerHandler(s);
                        }

                    } catch (IOException ex) {
                        server = null;
                    }
                }
            }
        });
        start.start();
    }

    public static void drawPieChart() {
        if (!isClosed) {
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Available", Integer.parseInt(numberOfAvailable.getText())),
                    new PieChart.Data("In-game", Integer.parseInt(numberOfInGame.getText())),
                    new PieChart.Data("Offline", Integer.parseInt(numberOfOffline.getText()))
            );
            PieChart pieChart = new PieChart(pieChartData);
            pieChart.setClockwise(true);
            pieChart.setLabelLineLength(50);
            pieChart.setLabelsVisible(true);
            pieChart.setStartAngle(180);
            pane.setCenter(pieChart);
        }
    }

    private void clearLabels() {
        numberOfAvailable.setText("#");
        numberOfInGame.setText("#");
        numberOfOffline.setText("#");
    }
}
