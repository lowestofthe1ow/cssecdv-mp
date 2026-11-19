package Controller;

import Model.History;
import Model.Logs;
import Model.Product;
import Model.User;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SQLite {
    
    String driverURL = "jdbc:sqlite:" + "database.db";
    
    public void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection(driverURL)) {
            if (conn != null) {
                conn.getMetaData();
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.createNewDatabase", ex);
        }
    }
    
    public void createHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS history (\n"
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + " username TEXT NOT NULL,\n"
            + " name TEXT NOT NULL,\n"
            + " stock INTEGER DEFAULT 0,\n"
            + " timestamp TEXT NOT NULL,\n"
            + " unit_price REAL NOT NULL DEFAULT 0.00\n"
            + ");";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            boolean hasUnitPrice = false;
            try (PreparedStatement columns = conn.prepareStatement("PRAGMA table_info(history)");
                 ResultSet rs = columns.executeQuery()) {
                while (rs.next()) {
                    if ("unit_price".equalsIgnoreCase(rs.getString("name"))) {
                        hasUnitPrice = true;
                        break;
                    }
                }
            }

            if (!hasUnitPrice) {
                stmt.execute("ALTER TABLE history ADD COLUMN unit_price REAL NOT NULL DEFAULT 0.00");
                // Legacy records did not store a purchase price. This is a one-time best-effort
                // backfill; all purchases made after this migration store an exact price snapshot.
                try (PreparedStatement migrate = conn.prepareStatement(
                        "UPDATE history SET unit_price = COALESCE((SELECT price FROM product "
                      + "WHERE product.name = history.name), 0.00)")) {
                    migrate.executeUpdate();
                }
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.createHistoryTable", ex);
        }
    }
    
    public void createLogsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS logs (\n"
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + " event TEXT NOT NULL,\n"
            + " username TEXT NOT NULL,\n"
            + " desc TEXT NOT NULL,\n"
            + " timestamp TEXT NOT NULL\n"
            + ");";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.createLogsTable", ex);
        }
    }
     
    public void createProductTable() {
        String sql = "CREATE TABLE IF NOT EXISTS product (\n"
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + " name TEXT NOT NULL UNIQUE,\n"
            + " stock INTEGER DEFAULT 0,\n"
            + " price REAL DEFAULT 0.00\n"
            + ");";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.createProductTable", ex);
        }
    }
     
    /** Seeds the demonstration catalogue only when the product table is empty. */
    public void seedSampleProducts() {
        String insertSql = "INSERT OR IGNORE INTO product(name, stock, price) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL)) {
            int productCount = 0;
            try (PreparedStatement count = conn.prepareStatement("SELECT COUNT(*) FROM product");
                 ResultSet rs = count.executeQuery()) {
                if (rs.next()) {
                    productCount = rs.getInt(1);
                }
            }
            if (productCount > 0) {
                return;
            }
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setString(1, "Antivirus");
                insert.setInt(2, 5);
                insert.setDouble(3, 500.00);
                insert.addBatch();

                insert.setString(1, "Firewall");
                insert.setInt(2, 3);
                insert.setDouble(3, 1000.00);
                insert.addBatch();

                insert.setString(1, "Scanner");
                insert.setInt(2, 10);
                insert.setDouble(3, 100.00);
                insert.addBatch();
                insert.executeBatch();
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.seedSampleProducts", ex);
        }
    }

    public void createUserTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (\n"
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + " username TEXT NOT NULL UNIQUE,\n"
            + " password TEXT NOT NULL,\n"
            + " role INTEGER DEFAULT 2,\n"
            + " previous_role INTEGER NOT NULL DEFAULT 2,\n"
            + " locked INTEGER DEFAULT 0,\n"
            + " salt TEXT,\n"
            + " failed_attempts INTEGER DEFAULT 0,\n"
            + " last_attempt TEXT,\n"
            + " last_status TEXT\n"
            + ");";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.createUserTable", ex);
        }
    }
    
    public void dropHistoryTable() {
        String sql = "DROP TABLE IF EXISTS history;";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.dropHistoryTable", ex);
        }
    }
    
    public void dropLogsTable() {
        String sql = "DROP TABLE IF EXISTS logs;";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.dropLogsTable", ex);
        }
    }
    
    public void dropProductTable() {
        String sql = "DROP TABLE IF EXISTS product;";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.dropProductTable", ex);
        }
    }
    
    public void dropUserTable() {
        String sql = "DROP TABLE IF EXISTS users;";

        try (Connection conn = DriverManager.getConnection(driverURL);
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.dropUserTable", ex);
        }
    }
    
    public void addHistory(String username, String name, int stock, String timestamp) {
        String sql = "INSERT INTO history(username,name,stock,timestamp,unit_price) "
                   + "SELECT ?, ?, ?, ?, COALESCE((SELECT price FROM product WHERE product.name = ?), 0.00)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setInt(3, stock);
            pstmt.setString(4, timestamp);
            pstmt.setString(5, name);
            pstmt.executeUpdate();
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.addHistory", ex);
        }
    }

    public void addHistory(String username, String name, int stock, String timestamp, double unitPrice)
            throws SQLException {
        String sql = "INSERT INTO history(username,name,stock,timestamp,unit_price) VALUES(?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, name);
            pstmt.setInt(3, stock);
            pstmt.setString(4, timestamp);
            pstmt.setDouble(5, unitPrice);
            pstmt.executeUpdate();
        }
    }

    /** Removes logs only when requested through the role-checked LogService. */
    void clearLogs() throws SQLException {
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM logs")) {
            pstmt.executeUpdate();
        }
    }

    public void addLogs(String event, String username, String desc, String timestamp) {
        String sql = "INSERT INTO logs(event,username,desc,timestamp) VALUES(?,?,?,?)";
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, event);
            pstmt.setString(2, username);
            pstmt.setString(3, desc);
            pstmt.setString(4, timestamp);
            pstmt.executeUpdate();
        } catch (Exception ex) {
            // Intentionally silent: ErrorHandler.handle() -> SecurityLogger.log() -> addLogs().
            // Calling ErrorHandler here would recurse forever if the log write itself keeps failing.
        }
    }
    
    public void addProduct(String name, int stock, double price) throws SQLException {
        String sql = "INSERT INTO product(name,stock,price) VALUES(?,?,?)";
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, name);
            pstmt.setInt(2, stock);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
        }
    }
    
    public boolean updateProduct(String oldName, String newName, int stock, double price) throws SQLException {
        try (Connection conn = DriverManager.getConnection(driverURL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement p = conn.prepareStatement("UPDATE product SET name = ?, stock = ?, price = ? WHERE name = ?")) {
                p.setString(1, newName);
                p.setInt(2, stock);
                p.setDouble(3, price);
                p.setString(4, oldName);
                if (p.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                // Historical rows are immutable snapshots and retain their original product name/price.
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }
    
    public boolean deleteProduct(String name) throws SQLException {
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM product WHERE name = ?")){
            pstmt.setString(1, name);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Atomically deducts product stock and inserts a price-snapshotted history record.
     * @param username buyer username
     * @param name product name
     * @param qty positive quantity to purchase
     * @param timestamp formatted purchase timestamp
     */
    public boolean purchaseProduct(String username, String name, int qty, String timestamp) throws SQLException {
        try (Connection conn = DriverManager.getConnection(driverURL)) {
            conn.setAutoCommit(false);
            try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE product SET stock = stock - ? WHERE name = ? AND stock >= ?");
                 PreparedStatement history = conn.prepareStatement(
                        "INSERT INTO history(username,name,stock,timestamp,unit_price) "
                      + "SELECT ?, name, ?, ?, price FROM product WHERE name = ?")) {
                update.setInt(1, qty);
                update.setString(2, name);
                update.setInt(3, qty);
                if (update.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                history.setString(1, username);
                history.setInt(2, qty);
                history.setString(3, timestamp);
                history.setString(4, name);
                if (history.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    public ArrayList<History> getHistory(){
        String sql = "SELECT id, username, name, stock, timestamp, unit_price FROM history ORDER BY id DESC LIMIT 1000";
        ArrayList<History> histories = new ArrayList<History>();
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){
            
            while (rs.next()) {
                histories.add(new History(rs.getInt("id"),
                                   rs.getString("username"),
                                   rs.getString("name"),
                                   rs.getInt("stock"),
                                   rs.getString("timestamp"),
                                   rs.getDouble("unit_price")));
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.getHistory", ex);
        }
        return histories;
    }
    
    public ArrayList<History> getHistoryByUser(String username) throws SQLException {
        String sql = "SELECT id, username, name, stock, timestamp, unit_price FROM history WHERE username = ? COLLATE NOCASE";
        ArrayList<History> histories = new ArrayList<History>();
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    histories.add(new History(rs.getInt("id"),
                                       rs.getString("username"),
                                       rs.getString("name"),
                                       rs.getInt("stock"),
                                       rs.getString("timestamp"),
                                       rs.getDouble("unit_price")));
                }
            }
        }
        return histories;
    }
    
    ArrayList<Logs> getLogs(){
        String sql = "SELECT id, event, username, desc, timestamp FROM logs ORDER BY id DESC LIMIT 1000";
        ArrayList<Logs> logs = new ArrayList<Logs>();
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){
            
            while (rs.next()) {
                logs.add(new Logs(rs.getInt("id"),
                                   rs.getString("event"),
                                   rs.getString("username"),
                                   rs.getString("desc"),
                                   rs.getString("timestamp")));
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.getLogs", ex);
        }
        return logs;
    }
    
    public ArrayList<Product> getProduct(){
        String sql = "SELECT id, name, stock, price FROM product";
        ArrayList<Product> products = new ArrayList<Product>();
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){
            
            while (rs.next()) {
                products.add(new Product(rs.getInt("id"),
                                   rs.getString("name"),
                                   rs.getInt("stock"),
                                   rs.getDouble("price")));
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.getProduct", ex);
        }
        return products;
    }
    
    private User mapUser(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getInt("role"),
                        rs.getInt("locked"),
                        rs.getString("salt"),
                        rs.getInt("failed_attempts"),
                        rs.getString("last_attempt"),
                        rs.getString("last_status"));
    }
    
    private static final String USER_COLS =
        "id, username, password, role, locked, salt, failed_attempts, last_attempt, last_status";
    
    public ArrayList<User> getUsers(){
        String sql = "SELECT " + USER_COLS + " FROM users";
        ArrayList<User> users = new ArrayList<User>();
        
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()){
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.getUsers", ex);
        }
        return users;
    }
    
    /** Fetches a user record by username using case-insensitive matching. */
    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT " + USER_COLS + " FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }
    
    public int countUsers() throws SQLException {
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM users");
            ResultSet rs = pstmt.executeQuery()){
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    public void addUser(String username, String passwordHash, String salt, int role) throws SQLException {
        String sql = "INSERT INTO users(username,password,salt,role,previous_role) VALUES(?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, salt);
            pstmt.setInt(4, role);
            pstmt.setInt(5, role);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Increments failed_attempts and locks the account at Role 1 once maxAttempts is reached.
     * @param username target username
     * @param timestamp formatted attempt timestamp
     * @param maxAttempts failure threshold before locking
     */
    public void recordLoginFailure(String username, String timestamp, int maxAttempts) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = failed_attempts + 1, "
                   + "previous_role = CASE WHEN failed_attempts + 1 >= ? AND role BETWEEN 2 AND 5 "
                   + "THEN role ELSE previous_role END, "
                   + "last_attempt = ?, last_status = 'FAILED', "
                   + "role = CASE WHEN failed_attempts + 1 >= ? THEN 1 ELSE role END, "
                   + "locked = CASE WHEN failed_attempts + 1 >= ? THEN 1 ELSE locked END "
                   + "WHERE username = ? COLLATE NOCASE";
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, maxAttempts);
            pstmt.setString(2, timestamp);
            pstmt.setInt(3, maxAttempts);
            pstmt.setInt(4, maxAttempts);
            pstmt.setString(5, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Resets failed_attempts and records the latest successful login timestamp.
     * @param username target username
     * @param timestamp formatted login timestamp
     */
    public void recordLoginSuccess(String username, String timestamp) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, last_attempt = ?, last_status = 'SUCCESS' WHERE username = ? COLLATE NOCASE";
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, timestamp);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }
    
    public void updatePassword(String username, String passwordHash, String salt) throws SQLException {
        String sql = "UPDATE users SET password = ?, salt = ? WHERE username = ? COLLATE NOCASE";
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, passwordHash);
            pstmt.setString(2, salt);
            pstmt.setString(3, username);
            pstmt.executeUpdate();
        }
    }
    
    public void reenableUser(String username, int role) throws SQLException {
        setUserRole(username, role);
    }

    /** Restores a locked/disabled account to its previous_role and clears lockout counters. */
    public boolean unlockUser(String username) throws SQLException {
        String sql = "UPDATE users SET role = CASE WHEN previous_role BETWEEN 2 AND 5 "
                   + "THEN previous_role ELSE 2 END, locked = 0, failed_attempts = 0 "
                   + "WHERE username = ? COLLATE NOCASE AND (role = 1 OR locked = 1)";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void removeUser(String username) {
        String sql = "DELETE FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.removeUser", ex);
        }
    }
    
    /** Initializes and migrates the users table and case-insensitive username index safely. */
    public void initUsersSchema() {
        boolean hasSalt = false;
        boolean hasPreviousRole = false;
        boolean schemaChecked = false;
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement("PRAGMA table_info(users)");
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String column = rs.getString("name");
                if ("salt".equalsIgnoreCase(column)) {
                    hasSalt = true;
                } else if ("previous_role".equalsIgnoreCase(column)) {
                    hasPreviousRole = true;
                }
            }
            schemaChecked = true;
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.initUsersSchema", ex);
        }
        if (schemaChecked && !hasSalt) {
            dropUserTable();
        }
        createUserTable();

        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement idx = conn.prepareStatement(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_nocase ON users(username COLLATE NOCASE)")) {
            idx.execute();
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.initUsersSchema.index", ex);
        }

        if (hasSalt && !hasPreviousRole) {
            try (Connection conn = DriverManager.getConnection(driverURL);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN previous_role INTEGER NOT NULL DEFAULT 2");
                try (PreparedStatement migrate = conn.prepareStatement(
                        "UPDATE users SET previous_role = role WHERE role BETWEEN 2 AND 5")) {
                    migrate.executeUpdate();
                }
            } catch (Exception ex) {
                ErrorHandler.handle("SQLite.initUsersSchema.previousRole", ex);
            }
        }
    }

    public Product getProduct(String name){
        String sql = "SELECT name, stock, price FROM product WHERE name = ?";
        Product product = null;
        try (Connection conn = DriverManager.getConnection(driverURL);
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    product = new Product(rs.getString("name"),
                                           rs.getInt("stock"),
                                           rs.getDouble("price"));
                }
            }
        } catch (Exception ex) {
            ErrorHandler.handle("SQLite.getProduct(String)", ex);
        }
        return product;
    }
    
    /**
     * Updates a user's role, syncing locked status and preserving previous_role when disabling.
     * @param username target username
     * @param role new role code (1-5)
     */
    public boolean setUserRole(String username, int role) throws SQLException {
        String sql = "UPDATE users SET "
                   + "previous_role = CASE WHEN ? = 1 THEN "
                   + "CASE WHEN role BETWEEN 2 AND 5 THEN role ELSE previous_role END ELSE ? END, "
                   + "role = ?, locked = ?, failed_attempts = 0 WHERE username = ? COLLATE NOCASE";
        try (Connection conn = DriverManager.getConnection(driverURL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, role);
            pstmt.setInt(2, role);
            pstmt.setInt(3, role);
            pstmt.setInt(4, role == 1 ? 1 : 0);
            pstmt.setString(5, username);
            return pstmt.executeUpdate() > 0;
        }
    }
}
