package DB;

import DTO.DTOPlayer;
import org.apache.derby.jdbc.ClientDriver;
import java.sql.*;
import tictactoegameitiserver.MassageType;

public class DAO {

    public static boolean logIn(DTOPlayer user) throws SQLException {
        boolean exists = false;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select * from PLAYER where USERNAME = ? AND PASSWORD = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, user.getUserName());
        ps.setString(2, user.getPassword());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            exists = true;
        }
        con.close();
        ps.close();
        return exists;
    }
    public static int getAvailablePlayersForServer() throws SQLException{
        int number=0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select COUNT(*) from PLAYER where STAUS = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_ONLINE);
        ResultSet rs = ps.executeQuery();
        if(rs.next()){
            number=rs.getInt(1);
        }
        return number;
    }
    
    public static int getOfflinePlayersForServer() throws SQLException{
        int number=0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select COUNT(*) from PLAYER where STAUS = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_OFFLINE);
        ResultSet rs = ps.executeQuery();
        if(rs.next()){
            number=rs.getInt(1);
        }
        return number;
    }
    
    public static int getInGamePlayersForServer() throws SQLException{
        int number=0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select COUNT(*) from PLAYER where STAUS = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_INGAME);
        ResultSet rs = ps.executeQuery();
        if(rs.next()){
            number=rs.getInt(1);
        }
        return number;
    }
}
