package tictactoegameitiserver;

import DB.DAO;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class TicTacToeGameITIServer extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/Assets/icon.jpg")));
        Parent root = FXMLLoader.load(getClass().getResource("ServerUI.fxml"));
        Scene scene = new Scene(root);
        stage.setResizable(false);
        stage.setWidth(600);
        stage.setHeight(400);
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            try {
                DAO.setAllOff();
            } catch (SQLException ex) {
                Logger.getLogger(TicTacToeGameITIServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
    
}