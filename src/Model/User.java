package Model;

public class User {
    private int id;
    private String username;
    private String password;
    private int role = 2;
    private int locked = 0;
    private String salt;
    private int failedAttempts = 0;
    private String lastAttempt;
    private String lastStatus;

    public User(String username, String password){
        this.username = username;
        this.password = password;
    }
    
    public User(int id, String username, String password, int role, int locked){
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.locked = locked;
    }
    
    public User(int id, String username, String password, int role, int locked,
            String salt, int failedAttempts, String lastAttempt, String lastStatus){
        this(id, username, password, role, locked);
        this.salt = salt;
        this.failedAttempts = failedAttempts;
        this.lastAttempt = lastAttempt;
        this.lastStatus = lastStatus;
    }
    
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public String getLastAttempt() { return lastAttempt; }
    public void setLastAttempt(String lastAttempt) { this.lastAttempt = lastAttempt; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getLocked() {
        return locked;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }
}
