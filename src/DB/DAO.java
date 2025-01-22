package DB;

import DTO.DTOPlayer;
import org.apache.derby.jdbc.ClientDriver;
import java.sql.*;
import java.util.ArrayList;
import org.json.simple.JSONObject;
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

    public static boolean signup(DTOPlayer user) throws SQLException {
        String sql = "INSERT INTO PLAYER (username, password, STATUS) VALUES (?, ?, ?)";
        Connection connection = DriverManager.getConnection("jdbc:derby://localhost:1527/Player", "root", "root");
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getUserName());
        preparedStatement.setString(2, user.getPassword());
        preparedStatement.setString(3, MassageType.STATUS_OFFLINE);
        int rowsInserted = preparedStatement.executeUpdate();
        preparedStatement.close();
        connection.close();

        return rowsInserted > 0;

    }

    public static ArrayList<String> getavailablePlayersList(String username) throws SQLException {
        ArrayList<String> availablePlayersList = new ArrayList<>();
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select USERNAME, TOTAL_SCORE from PLAYER where STATUS = ? and USERNAME != ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_ONLINE);
        ps.setString(2, username);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            JSONObject onlinePlayer = new JSONObject();
            onlinePlayer.put("username", rs.getString("USERNAME"));
            onlinePlayer.put("score", rs.getInt("TOTAL_SCORE"));
            availablePlayersList.add(onlinePlayer.toJSONString());
        }
        return availablePlayersList;
    }

    public static int getAvailablePlayersForServer() throws SQLException {
        int number = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select COUNT(*) from PLAYER where STATUS = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_ONLINE);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            number = rs.getInt(1);
        }
        return number;
    }

    public static int getOfflinePlayersForServer() throws SQLException {
        int number = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select COUNT(*) from PLAYER where STATUS = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_OFFLINE);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            number = rs.getInt(1);
        }
        return number;
    }

    public static int getInGamePlayersForServer() throws SQLException {
        int number = 0;
        DriverManager.registerDriver(new ClientDriver());
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("select COUNT(*) from PLAYER where STATUS = ?",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        ps.setString(1, MassageType.STATUS_INGAME);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            number = rs.getInt(1);
        }
        return number;
    }

    public static int updateAvailable(DTO.DTOPlayer user) throws SQLException {  // This function is called when the user logs in
        DriverManager.registerDriver(new ClientDriver());                       // This function works properly when it returns 1
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("update player set status = ? where username = ?");
        ps.setString(1, MassageType.STATUS_ONLINE);
        ps.setString(2, user.getUserName());
        int rowsUpdated = ps.executeUpdate();
        ps.close();
        con.close();
        return rowsUpdated;
    }

    public static int updateInGame(DTO.DTOPlayer user) throws SQLException { // This function is called when the user enters a game
        DriverManager.registerDriver(new ClientDriver());                   // This function works properly when it returns 1
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("update player set status = ? where username = ?");
        ps.setString(1, MassageType.STATUS_INGAME);
        ps.setString(2, user.getUserName());
        int rowsUpdated = ps.executeUpdate();
        ps.close();
        con.close();
        return rowsUpdated;
    }

    public static int updateOffline(DTO.DTOPlayer user) throws SQLException {    // This function is called when the user logs out
        DriverManager.registerDriver(new ClientDriver());                       // This function works properly when it returns 1
        Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        PreparedStatement ps = con.prepareStatement("update player set status = ? where username = ?");
        ps.setString(1, MassageType.STATUS_OFFLINE);
        ps.setString(2, user.getUserName());
        int rowsUpdated = ps.executeUpdate();
        ps.close();
        con.close();
        return rowsUpdated;
    }

    public static void updateScore(DTO.DTOPlayer user) throws SQLException {
        DriverManager.registerDriver(new ClientDriver());
        Connection connection = DriverManager.getConnection("jdbc:derby://localhost:1527/Player",
                "root", "root");
        String selectQuery = "SELECT TOTAL_SCORE FROM PLAYER WHERE USERNAME = ?";
        String updateQuery = "UPDATE PLAYER SET TOTAL_SCORE = ? WHERE USERNAME = ?";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
            selectStmt.setString(1, user.getUserName());
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                int currentScore = rs.getInt("TOTAL_SCORE");
                int updatedScore = currentScore + 10;

                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, updatedScore);
                    updateStmt.setString(2, user.getUserName());
                    updateStmt.executeUpdate();
                }
            } else {
                throw new SQLException("User not found in the database.");
            }
        }
    }

}
