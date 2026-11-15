package Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class SecurityLogger {

    private static SQLite db;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private SecurityLogger() {}

    public static void init(SQLite sqlite) {
        db = sqlite;
    }

    public static void log(String event, String username, String desc) {
        try {
            if (db != null) {
                db.addLogs(event, username == null ? "unknown" : username, desc, LocalDateTime.now().format(FMT));
            }
        } catch (Exception ignored) {
        }
    }
}
