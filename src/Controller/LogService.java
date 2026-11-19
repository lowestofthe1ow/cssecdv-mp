package Controller;

import Model.Logs;
import Model.User;
import java.util.ArrayList;

/** Applies central access-control decision before exposing audit records. */
public class LogService {

    private final SQLite db;

    public LogService(SQLite db) {
        this.db = db;
    }

    /** Admin-only log management. A fresh authorization check precedes each clear. */
    public boolean clear(User actor) {
        try {
            if (!AccessControl.check(actor, AccessControl.LOGS_CLEAR)) {
                return false;
            }
            db.clearLogs();
            SecurityLogger.log("LOGS_CLEARED", actor.getUsername(), "Security logs cleared by administrator");
            return true;
        } catch (Exception ex) {
            ErrorHandler.handle("LogService.clear", ex);
            return false;
        }
    }

    /** Returns audit log entries only when the actor is authorized to view logs. */
    public ArrayList<Logs> view(User actor) {
        try {
            if (!AccessControl.check(actor, AccessControl.LOGS_VIEW)) {
                return new ArrayList<Logs>();
            }
            return db.getLogs();
        } catch (Exception ex) {
            ErrorHandler.handle("LogService.view", ex);
            return new ArrayList<Logs>();
        }
    }
}
