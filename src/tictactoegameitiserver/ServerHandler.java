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
import java.util.Random;
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

    DataInputStream messageIn;
    DataOutputStream messageOut;
    static Vector<ServerHandler> clients = new Vector<ServerHandler>();
    static HashMap<String, ServerHandler> availableClients = new HashMap<>();
    JSONObject response;
    String username = null;
    boolean inGame = false;
    ServerHandler currentOpponent = null;
    boolean isFinished = false;
    Socket currentSocket;

    public ServerHandler(Socket s) throws IOException {
        currentSocket=s;
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
                }
                else if(msgType.equals(MassageType.CLIENT_CLOSE_MSG)){
                    clientClose();
                }
                else if(msgType.equals(MassageType.LOGOUT_MSG)){
                    logout();
                }
                else if(msgType.equals(MassageType.CHALLENGE_ACCESSEPT_MSG)){
                    startGame();
                }
                else if(msgType.equals(MassageType.PLAY_MSG)){
                    play(msg);
                }
                else if(msgType.equals(MassageType.WITHDRAW_GAME_MSG)){
                    withdraw(msg);
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
            JSONObject player =(JSONObject) JSONValue.parse((String) response.get("data"));
            DTOPlayer user = new DTOPlayer((String)player.get("username"),(String)player.get("password"));
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
                JSONObject object =(JSONObject) JSONValue.parse((String)response.get("data"));
                DTOPlayer user = new DTOPlayer((String)object.get("username"),(String)object.get("password"));
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
    
    public void clientClose() throws SQLException, IOException{
        if(inGame){
            JSONObject withdraw=new JSONObject();
            withdraw.put("type", MassageType.WITHDRAW_GAME_MSG);
            currentOpponent.messageOut.writeUTF(withdraw.toJSONString());
            // Update score for both players -->> Mayada hasn't finished this function
            currentOpponent.inGame = false;
            currentOpponent.currentOpponent = null;
            availableClients.put(currentOpponent.username, currentOpponent);
            DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
            sendUsernamesToAvailable();
            currentOpponent = null;
            inGame=false;
            DAO.updateOffline(new DTOPlayer(username, ""));
            clients.remove(this);
            username=null;
            isFinished=true;
            messageIn.close();
            messageOut.close();
            currentSocket.close();
            
        }
        else if(username!=null){
            DAO.updateOffline(new DTOPlayer(username, ""));
            availableClients.remove(username);
            clients.remove(this);
            sendUsernamesToAvailable();
            username=null;
            isFinished=true;
            messageIn.close();
            messageOut.close();
            currentSocket.close();
        }
        else{
            clients.remove(this);
            isFinished=true;
            messageIn.close();
            messageOut.close();
            currentSocket.close();
        }
    }
    
    public void logout() throws SQLException, IOException {
        DAO.updateOffline(new DTOPlayer(username, ""));
        availableClients.remove(username);
        sendUsernamesToAvailable();
        username=null;
    }
    
    public void startGame() throws IOException, SQLException{
        JSONObject data=(JSONObject) JSONValue.parse((String)response.get("data"));
        JSONObject game=new JSONObject();
        game.put("type", MassageType.START_GAME_MSG);
        JSONObject player1=new JSONObject();
        player1.put("player1",username);
        player1.put("player2",(String) data.get("opponent"));
        Random r=new Random();
        boolean isStarted=r.nextBoolean();
        player1.put("isStarted", isStarted);
        JSONObject player2=new JSONObject();
        player2.put("player1",username);
        player2.put("player2",(String) data.get("opponent"));
        player2.put("isStarted", !isStarted);
        ServerHandler p1=availableClients.get(username);// depend on how will be player 1 the sender or the receiver
        ServerHandler p2=availableClients.get((String) data.get("opponent"));
        if(p1!=null && p2!=null){
            p1.currentOpponent=p2;
            p1.inGame=true;
            availableClients.remove(username);
            DAO.updateInGame(new DTOPlayer(username, ""));
            p2.currentOpponent=p1;
            p2.inGame=true;
            availableClients.remove((String) data.get("opponent"));
            DAO.updateInGame(new DTOPlayer((String) data.get("opponent"), ""));
            game.put("data", player1.toJSONString());
            p1.messageOut.writeUTF(game.toJSONString());
            game.put("data", player2.toJSONString());
            p2.messageOut.writeUTF(game.toJSONString());
        }
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

    private void play(String msg) throws IOException {
        currentOpponent.messageOut.writeUTF(msg);
    }

    private void withdraw(String msg) throws IOException, SQLException {  
        currentOpponent.messageOut.writeUTF(msg);
        // Update score for both players -->> Mayada hasn't finished this function
        currentOpponent.inGame = false;
        currentOpponent.currentOpponent = null;
        availableClients.put(currentOpponent.username, currentOpponent);
        DAO.updateAvailable(new DTOPlayer(currentOpponent.username, ""));
        currentOpponent = null;
        inGame=false;
        availableClients.put(username, this);
        DAO.updateAvailable(new DTOPlayer(username, ""));
        sendUsernamesToAvailable();
    }
}
