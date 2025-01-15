/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tictactoegameitiserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue; 

/**
 *
 * @author HAZEM-LAB
 */
public class ServerHandler extends Thread{
        DataInputStream massageIn;
        DataOutputStream massageOut;
        static Vector<ServerHandler> clients=new Vector<ServerHandler>();
        static HashMap<String,ServerHandler> avaliableClients=new HashMap<>();
        static HashMap<String,ServerHandler> inGameClients=new HashMap<>();
        String username=null;
        boolean inGame= false;
        ServerHandler currentOpponent=null;
        boolean isFinished=false;
        
        public ServerHandler(Socket s) throws IOException{
            massageIn=new DataInputStream(s.getInputStream());
            massageOut=new DataOutputStream(s.getOutputStream());
            ServerHandler.clients.add(this);
            start();
        }
        
        public void run(){
            while(!isFinished){
                
                try {
                    String msg=massageIn.readUTF();
                    JSONObject respone=(JSONObject) JSONValue.parse(msg);
                    String msgType=(String) respone.get("type");
                    if(msgType.equals(MassageType.LOGIN_MSG)){
                        
                        avaliableClients.put(msg, this);
                        sendUsernamesToAvailable();
                        
                    }
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                    
                }
                
            }
        }
        
        public void sendToAll(String s) throws IOException {
            for(ServerHandler client:clients){
                client.massageOut.writeUTF(s);
            }
        }
        public void sendUsernamesToAvailable() throws IOException{
            String userUpdate="&*update*&";
            for(ServerHandler handler: avaliableClients.values()){
                for(String username: avaliableClients.keySet()){
                    if(this!=avaliableClients.get(username))
                        userUpdate+=username+"\n";
                }
                handler.massageOut.writeUTF(userUpdate);
            }
        }
        
        
}