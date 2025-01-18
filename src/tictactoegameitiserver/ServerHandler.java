package tictactoegameitiserver;

import DB.DAO;
import DTO.DTOPlayer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ServerHandler extends Thread {

    DataInputStream messageIn;
    DataOutputStream messageOut;
    static Vector<ServerHandler> clients = new Vector<ServerHandler>();
    static HashMap<String, ServerHandler> availableClients = new HashMap<>();
    static HashMap<String, ServerHandler> inGameClients = new HashMap<>();
    static HashMap<String, String> gameRequests = new HashMap<>();

    JSONObject response;
    String username = null;
    boolean inGame = false;
    ServerHandler currentOpponent = null;
    boolean isFinished = false;
    Socket currentSocket;

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
                } else if (msgType.equals(MassageType.CHALLENGE_ACCESSEPT_MSG)) {
                    acceptHandler(msg);
                } else if (msgType.equals(MassageType.CLIENT_CLOSE_MSG)) {
                    clientClose();
                } else if (msgType.equals(MassageType.LOGOUT_MSG)) {
                    logout();
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
                sendUsernamesToAvailable();

                username = user.getUserName();
                loginData.put("type", MassageType.LOGINSUCCESS_MSG);
                loginData.put("data", DAO.getavailablePlayersList(username));
            } else {
                loginData.put("type", MassageType.LOGINFAIL_MSG);
                loginData.put("data", null);
            }
            messageOut.writeUTF(loginData.toJSONString());
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void clientClose() throws SQLException, IOException {
        if (inGame) {

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
    }

    public void logout() throws SQLException, IOException {
        DAO.updateOffline(new DTOPlayer(username, ""));
        availableClients.remove(username);
        sendUsernamesToAvailable();
        username = null;
    }

    public static void sendToAll(String s) throws IOException {
        for (ServerHandler client : clients) {
            client.messageOut.writeUTF(s);
        }
    }

    public void sendUsernamesToAvailable() throws IOException, SQLException {
        for (ServerHandler handler : availableClients.values()) {
            ArrayList<String> availablePlayersList = DAO.getavailablePlayersList(handler.username);
            JSONObject availablePlayers = new JSONObject();
            availablePlayers.put("type", MassageType.UPDATE_LIST_MSG);
            availablePlayers.put("data", availablePlayersList);
            handler.messageOut.writeUTF(availablePlayers.toJSONString());
        }
    }

    private void requestHandler(String msg) {
        //JSONObject challengeRequest = (JSONObject) JSONValue.parse((String) response.get("data"));
        String opponentUsername = (String) response.get("data");

        if (availableClients.containsKey(opponentUsername)) {
            try {
                gameRequests.put(username, opponentUsername);
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

    public void acceptHandler(String msg) throws IOException {
        //JSONObject acceptResponse = (JSONObject) JSONValue.parse((String) response.get("data"));
        String opponentUsername = (String) response.get("data");

        if (availableClients.containsKey(opponentUsername)) {
            ServerHandler opponentHandler = availableClients.get(opponentUsername);

            JSONObject responseMessage = new JSONObject();
            if (response.get("type").equals(MassageType.CHALLENGE_ACCESSEPT_MSG)) {
                responseMessage.put("type", MassageType.CHALLENGE_START_MSG);
                responseMessage.put("data", username);
                opponentHandler.messageOut.writeUTF(responseMessage.toJSONString());
            } else if (response.get("type").equals(MassageType.CHALLENGE_REJECT_MSG)) {
                responseMessage.put("type", MassageType.CHALLENGE_FAIL_MSG);
                responseMessage.put("data", "Challenge rejected");
                opponentHandler.messageOut.writeUTF(responseMessage.toJSONString());
            }
        }
    }
}
