package Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SecurityLogger {

    private static SQLite db;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private SecurityLogger() {}

    /** Binds the database handle used to persist security log entries. */
    public static void init(SQLite sqlite) {
        db = sqlite;
    }

    /**
     * Records a timestamped security event in the audit log without throwing on failure.
     * @param event event category (e.g., AUTH_FAIL, ACCESS_DENIED)
     * @param username actor username, or "unknown" if null
     * @param desc human-readable event description
     */
    public static void log(String event, String username, String desc) {
        try {
            if (db != null) {
                db.addLogs(event, username == null ? "unknown" : username, desc, LocalDateTime.now().format(FMT));
            }
        } catch (Exception ignored) {
        }
    }
}
