package Controller;

import Model.History;
import Model.User;
import java.util.ArrayList;

public class HistoryService {

    private final SQLite db;

    public HistoryService(SQLite db) {
        this.db = db;
    }

    public static boolean isValidSearch(String query) {
        return Validator.search(query);
    }

    public ArrayList<History> view(User actor) {
        try {
            if (AccessControl.isAllowed(actor, AccessControl.HISTORY_VIEW_ALL)) {
                return db.getHistory();
            }
            if (AccessControl.check(actor, AccessControl.HISTORY_VIEW_OWN)) {
                return db.getHistoryByUser(actor.getUsername());
            }
        } catch (Exception e) {
        }
        return new ArrayList<History>();
    }

    public ArrayList<History> search(User actor, String query) {
        ArrayList<History> result = new ArrayList<History>();
        if (!isValidSearch(query)) {
            SecurityLogger.log("VALIDATION_FAIL", actor == null ? "unknown" : actor.getUsername(),
                    "History search rejected by validation");
            return result;
        }
        String q = query.trim().toLowerCase();
        for (History h : view(actor)) {
            if (q.isEmpty()
                    || h.getUsername().toLowerCase().contains(q)
                    || h.getName().toLowerCase().contains(q)) {
                result.add(h);
            }
        }
        return result;
    }
}
