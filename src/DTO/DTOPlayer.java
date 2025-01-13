
package DTO;

public class DTOPlayer {
    private String userName;
    private String password;
    private String status;
    private int score;

    
    public DTOPlayer() { }

    public DTOPlayer(String userName, String password) {
        this.userName = userName;
        this.password = password;
        
    }
    
    public DTOPlayer(String userName, String password, String status, int score) {
        this.userName = userName;
        this.password = password;
        this.status = status;
        this.score = score;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

}
