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
