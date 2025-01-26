package tictactoegameitiserver;

import DB.DAO;
import DTO.DTOPlayer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ServerHandler extends Thread {

    DataInputStream messageIn;
    DataOutputStream messageOut;
    static Vector<ServerHandler> clients = new Vector<ServerHandler>();
    static ConcurrentHashMap<String, ServerHandler> availableClients = new ConcurrentHashMap<>();
    JSONObject response;
    String username = null;
    boolean inGame = false;
    ServerHandler currentOpponent = null;
    static boolean isFinished = false;
    Socket currentSocket;
    boolean isBetweenGame = false;

    public ServerHandler(Socket s) throws IOException {
        currentSocket = s;
        messageIn = new DataInputStream(s.getInputStream());
        messageOut = new DataOutputStream(s.getOutputStream());
        ServerHandler.clients.add(this);
        start();
    }

    @Override
    public void run() {
        while (!isFinished) {
            try {
                String msg = messageIn.readUTF();
                response = (JSONObject) JSONValue.parse(msg);
                String msgType = (String) response.get("type");
                if (msgType.equals(MassageType.LOGIN_MSG)) {
                    login(msg);
                } else if (msgType.equals(MassageType.REGISTER_MSG)) {
                    signup(msg);
                } else if (msgType.equals(MassageType.CHALLENGE_REQUEST_MSG)) {
                    requestHandler(msg);
                } else if (msgType.equals(MassageType.CHALLENGE_ACCEPT_MSG)) {
                    startGame();
                } else if (msgType.equals(MassageType.CLIENT_CLOSE_MSG)) {
                    clientClose();
                } else if (msgType.equals(MassageType.LOGOUT_MSG)) {
                    logout();
                } else if (msgType.equals(MassageType.PLAY_MSG)) {
                    play(msg);
                } else if (msgType.equals(MassageType.RESTART_REQUEST_MSG)) {
                    restartRequest();
                } else if (msgType.equals(MassageType.END_GAME_MSG)) {
                    endGame();
                } else if (msgType.equals(MassageType.RESTART_ACCEPT_MSG)) {
                    restartGame();
                } else if (msgType.equals(MassageType.RESTART_REJECT_MSG)) {
                    endGame();
                } else if (msgType.equals(MassageType.WITHDRAW_GAME_MSG)) {
                    withdraw(msg);
                } else if (msgType.equals(MassageType.IN_BETWEEN_GAME_MSG)) {
                    handleInBetweenGame(msg);
                }
            } catch (IOException ex) {
                try {
                    clientClose();
                } catch (SQLException ex1) {
                    Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (IOException ex1) {
                    Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex1);
                }

            } catch (SQLException ex) {
                Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void login(String msg) throws SQLException {
        try {
            JSONObject player = (JSONObject) JSONValue.parse((String) response.get("data"));
            DTOPlayer user = new DTOPlayer((String) player.get("username"), (String) player.get("password"));
            JSONObject loginData = new JSONObject();
            boolean isSuccessful = DAO.logIn(user);
            if (isSuccessful) {
                availableClients.put(user.getUserName(), this);
                DAO.updateAvailable(user);
                username = user.getUserName();
                sendUsernamesToAvailable();
                JSONObject data = new JSONObject();
                data.put("username", user.getUserName());
                data.put("score", DAO.getTotalScore(user.getUserName()));
                data.put("players", DAO.getavailablePlayersList(user.getUserName()));

                loginData.put("type", MassageType.LOGINSUCCESS_MSG);
                loginData.put("data", data);
            } else {
                loginData.put("type", MassageType.LOGINFAIL_MSG);
                loginData.put("data", null);
            }
            messageOut.writeUTF(loginData.toJSONString());
            updateUI();
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void handleInBetweenGame(String msg) throws SQLException {
        isBetweenGame = true;
        currentOpponent.isBetweenGame = true;
        JSONObject object = (JSONObject) JSONValue.parse(msg);
        JSONObject result = (JSONObject) JSONValue.parse((String) object.get("data"));
        if (((String) result.get("result")).equals("win")) {
            String winner = (String) result.get("winner");
            DAO.updateScore(new DTOPlayer(winner, ""));
        }
    }

    private void signup(String msg) {
        try {
            JSONObject signResponse = new JSONObject();
            try {
                JSONObject object = (JSONObject) JSONValue.parse((String) response.get("data"));
                DTOPlayer user = new DTOPlayer((String) object.get("username"), (String) object.get("password"));
                boolean isSuccessful = DAO.signup(user);
                if (isSuccessful) {
                    signResponse.put("type", MassageType.REGISTER_SUCCESS_MSG);
                } else {
                    signResponse.put("type", MassageType.REGISTER_FAIL_MSG);
                }

            } catch (SQLException ex) {
                signResponse.put("type", MassageType.REGISTER_FAIL_MSG);

            }
            messageOut.writeUTF(signResponse.toJSONString());
            updateUI();
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clientClose() throws SQLException, IOException {
        if (inGame) {
            if (!isBetweenGame) {
                withdrawFromGameInCloseClient();
            } else {
                endGameInCloseClient();
            }

        } else if (username != null) {
            DAO.updateOffline(new DTOPlayer(username, ""));
            availableClients.remove(username);
            clients.remove(this);
            sendUsernamesToAvailable();
            username = null;
            isFinished = true;
            messageIn.close();
            messageOut.close();
            currentSocket.close();
        } else {
            clients.remove(this);
            isFinished = true;

            messageIn.close();
            messageOut.close();
            currentSocket.close();
        }
        updateUI();
    }

    public void logout() throws SQLException, IOException {
        DAO.updateOffline(new DTOPlayer(username, ""));
        availableClients.remove(username);
        sendUsernamesToAvailable();
        username = null;
        updateUI();
    }

    public void startGame() throws IOException, SQLException {
        String opponent = (String) response.get("data");
        JSONObject game = new JSONObject();
        game.put("type", MassageType.CHALLENGE_START_MSG);
        JSONObject player1 = new JSONObject();
        player1.put("player1", username);
        player1.put("player2", opponent);
        player1.put("isStarted", true);
        JSONObject player2 = new JSONObject();
        player2.put("player1", username);
        player2.put("player2", opponent);
        player2.put("isStarted", false);
        ServerHandler p1 = availableClients.get(username);// depend on how will be player 1 the sender or the receiver
        ServerHandler p2 = availableClients.get(opponent);
        if (p1 != null && p2 != null) {
            p1.currentOpponent = p2;
            p1.inGame = true;
            p1.isBetweenGame = false;
            availableClients.remove(username);
            DAO.updateInGame(new DTOPlayer(username, ""));
            p2.currentOpponent = p1;
            p2.inGame = true;
            p2.isBetweenGame = false;
            availableClients.remove(opponent);
            DAO.updateInGame(new DTOPlayer(opponent, ""));
            game.put("data", player1.toJSONString());
            p1.messageOut.writeUTF(game.toJSONString());
            game.put("data", player2.toJSONString());
            p2.messageOut.writeUTF(game.toJSONString());
            sendUsernamesToAvailable();
            updateUI();

        }
    }

    public static void sendToAll(String s) throws IOException {
        for (ServerHandler client : clients) {
            client.messageOut.writeUTF(s);
        }
    }

    public void sendUsernamesToAvailable() throws IOException, SQLException {
        for (ServerHandler handler : availableClients.values()) {
            JSONObject availablePlayers = new JSONObject();
            JSONObject data = new JSONObject();
            data.put("username", handler.username);
            data.put("score", DAO.getTotalScore(handler.username));
            data.put("players", DAO.getavailablePlayersList(handler.username));
            availablePlayers.put("type", MassageType.UPDATE_LIST_MSG);
            availablePlayers.put("data", data);
            handler.messageOut.writeUTF(availablePlayers.toJSONString());
        }
    }

    private void play(String msg) throws IOException {
        currentOpponent.messageOut.writeUTF(msg);
    }

    private void withdraw(String msg) throws IOException, SQLException {
        if (!isBetweenGame) {
            JSONObject withdraw = new JSONObject();
            withdraw.put("type", MassageType.WITHDRAW_GAME_MSG);
            currentOpponent.messageOut.writeUTF(withdraw.toJSONString());
            DAO.updateScore(new DTOPlayer(currentOpponent.username, ""));
            currentOpponent.inGame = false;
            currentOpponent.isBetweenGame = false;
            currentOpponent.currentOpponent = null;
            availableClients.put(username, this);
            inGame = false;
            isBetweenGame = false;
            DAO.updateAvailable(new DTOPlayer(username, ""));
            sendUsernamesToAvailable();
            availableClients.put(currentOpponent.username, currentOpponent);
            DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
            try {
                Thread.sleep(10000);
                sendUsernamesToAvailable();
            } catch (InterruptedException ex) {
                Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            currentOpponent = null;
            updateUI();
        } else {
            JSONObject withdraw = new JSONObject();
            withdraw.put("type", MassageType.END_GAME_MSG);
            currentOpponent.messageOut.writeUTF(withdraw.toJSONString());
            currentOpponent.inGame = false;
            currentOpponent.currentOpponent = null;
            availableClients.put(currentOpponent.username, currentOpponent);
            DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
            currentOpponent = null;
            inGame = false;
            availableClients.put(username, this);
            DAO.updateAvailable(new DTOPlayer(username, ""));
            sendUsernamesToAvailable();
            updateUI();
        }
    }

    public void restartRequest() throws IOException {
        JSONObject restartRequestMsg = new JSONObject();
        restartRequestMsg.put("type", MassageType.RESTART_REQUEST_MSG);
        currentOpponent.messageOut.writeUTF(restartRequestMsg.toJSONString());
    }

    public void restartGame() throws IOException {
        isBetweenGame = false;
        currentOpponent.isBetweenGame = false;
        JSONObject continueGameMsg = new JSONObject();
        continueGameMsg.put("type", MassageType.CONTINUE_GAME_MSG);
        messageOut.writeUTF(continueGameMsg.toJSONString());
        currentOpponent.messageOut.writeUTF(continueGameMsg.toJSONString());
    }

    public void endGame() throws IOException, SQLException {
        JSONObject end = new JSONObject();
        end.put("type", MassageType.END_GAME_MSG);
        currentOpponent.messageOut.writeUTF(end.toJSONString());
        currentOpponent.currentOpponent = null;
        currentOpponent.inGame = false;
        currentOpponent.isBetweenGame = false;
        DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
        availableClients.put(currentOpponent.username, currentOpponent);
        currentOpponent = null;
        inGame = false;
        isBetweenGame = false;
        DAO.updateAvailable(new DTOPlayer(username, ""));
        availableClients.put(username, this);
        sendUsernamesToAvailable();
        updateUI();

    }

    private void requestHandler(String msg) {
        String opponentUsername = (String) response.get("data");

        if (availableClients.containsKey(opponentUsername)) {
            try {
                ServerHandler opponentHandler = availableClients.get(opponentUsername);

                JSONObject challengeMsg = new JSONObject();
                challengeMsg.put("type", MassageType.CHALLENGE_REQUEST_MSG);
                challengeMsg.put("data", username);

                opponentHandler.messageOut.writeUTF(challengeMsg.toJSONString());
            } catch (IOException ex) {
                Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                JSONObject responseMessage = new JSONObject();
                responseMessage.put("type", MassageType.CHALLENGE_FAIL_MSG);
                responseMessage.put("data", "Opponent not available");
                messageOut.writeUTF(responseMessage.toJSONString());
            } catch (IOException ex) {
                Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void withdrawFromGameInCloseClient() throws IOException, SQLException {
        JSONObject withdraw = new JSONObject();
        withdraw.put("type", MassageType.WITHDRAW_GAME_MSG);
        currentOpponent.messageOut.writeUTF(withdraw.toJSONString());
        DAO.updateScore(new DTOPlayer(currentOpponent.username, ""));
        currentOpponent.inGame = false;
        currentOpponent.currentOpponent = null;
        availableClients.put(currentOpponent.username, currentOpponent);
        DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
        try {
            Thread.sleep(10000);
            sendUsernamesToAvailable();
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        currentOpponent = null;
        inGame = false;
        DAO.updateOffline(new DTOPlayer(username, ""));
        clients.remove(this);
        username = null;
        isFinished = true;
        updateUI();
        messageIn.close();
        messageOut.close();
        currentSocket.close();
    }

    private void endGameInCloseClient() throws IOException, SQLException {
        JSONObject endGame = new JSONObject();
        endGame.put("type", MassageType.END_GAME_MSG);
        currentOpponent.messageOut.writeUTF(endGame.toJSONString());
        currentOpponent.inGame = false;
        currentOpponent.isBetweenGame = false;
        currentOpponent.currentOpponent = null;
        availableClients.put(currentOpponent.username, currentOpponent);
        DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
        sendUsernamesToAvailable();
        currentOpponent = null;
        inGame = false;
        DAO.updateOffline(new DTOPlayer(username, ""));
        clients.remove(this);
        username = null;
        isFinished = true;
        updateUI();
        messageIn.close();
        messageOut.close();
        currentSocket.close();
    }

    private void updateUI() {
        Platform.runLater(() -> {
            ServerUIController.updateLabels();
            ServerUIController.drawPieChart();
        });
    }

    public static void closeServer() throws IOException, SQLException {
        for (int i=0;i<clients.size();i++) {
            clients.get(i).currentSocket.close();
            
        }
        DAO.setAllOff();
        clients.clear();
        availableClients.clear();
    }
}
