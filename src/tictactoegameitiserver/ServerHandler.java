/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

/**
 *
 * @author HAZEM-LAB
 */
public class ServerHandler extends Thread {

    DataInputStream massageIn;
    DataOutputStream massageOut;
    static Vector<ServerHandler> clients = new Vector<ServerHandler>();
    static HashMap<String, ServerHandler> avaliableClients = new HashMap<>();
    static HashMap<String, ServerHandler> inGameClients = new HashMap<>();
    JSONObject response;
    String username = null;
    boolean inGame = false;
    ServerHandler currentOpponent = null;
    boolean isFinished = false;

    public ServerHandler(Socket s) throws IOException {
        massageIn = new DataInputStream(s.getInputStream());
        massageOut = new DataOutputStream(s.getOutputStream());
        ServerHandler.clients.add(this);
        start();
    }

    @Override
    public void run() {
        while (!isFinished) {
            try {
                String msg = massageIn.readUTF();
                response = (JSONObject) JSONValue.parse(msg);
                String msgType = (String) response.get("type");
                if (msgType.equals(MassageType.LOGIN_MSG)) {
                    login(msg);
                } else if (msgType.equals(MassageType.REGISTER_MSG)) {
                    signup(msg);
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            } catch (SQLException ex) {
                Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void login(String msg) throws SQLException {
        try {
            DTOPlayer user = (DTOPlayer) response.get("data");
            boolean isSuccessful = DAO.logIn(user);
            JSONObject loginResponse = new JSONObject();
            if (isSuccessful) {
                avaliableClients.put(msg, this);
                sendUsernamesToAvailable();
                DAO.updateAvailable(user);
                sendUsernamesToAvailable();
                loginResponse.put("type", MassageType.LOGINSUCCESS_MSG);
                loginResponse.put("data", DAO.getavailablePlayersList(username));
                username = user.getUserName();
            } else {
                loginResponse.put("type", MassageType.LOGINFAIL_MSG);
                loginResponse.put("data", null);
            }
            massageOut.writeUTF(loginResponse.toJSONString());
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void signup(String msg) {
        try {
            JSONObject signResponse = new JSONObject();

            try {
                DTOPlayer user = (DTOPlayer) response.get("data");
                boolean isSuccessful = DAO.signup(user);
                if (isSuccessful) {
                    signResponse.put("type", MassageType.REGISTER_SUCCESS_MSG);
                } else {
                    signResponse.put("type", MassageType.REGISTER_FAIL_MSG);
                }

            } catch (SQLException ex) {
                signResponse.put("type", MassageType.REGISTER_FAIL_MSG);

            }
            massageOut.writeUTF(signResponse.toJSONString());
        } catch (IOException ex) {
            Logger.getLogger(ServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendToAll(String s) throws IOException {
        for (ServerHandler client : clients) {
            client.massageOut.writeUTF(s);
        }
    }

    public void sendUsernamesToAvailable() throws IOException, SQLException {
        for (ServerHandler handler : avaliableClients.values()) {
            ArrayList<String> availablePlayersList = DAO.getavailablePlayersList(handler.username);
            JSONObject availablePlayers = new JSONObject();
            availablePlayers.put("type", MassageType.UPDATE_LIST_MSG);
            availablePlayers.put("data", availablePlayersList);
            handler.massageOut.writeUTF(availablePlayers.toJSONString());
        }
    }

}
