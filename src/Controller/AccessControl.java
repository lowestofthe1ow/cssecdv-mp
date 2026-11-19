package Controller;

import Model.User;
import java.util.HashMap;
import java.util.Map;

public final class AccessControl {

    public static final String PANEL_ADMIN = "adminHomePnl";
    public static final String PANEL_MANAGER = "managerHomePnl";
    public static final String PANEL_STAFF = "staffHomePnl";
    public static final String PANEL_CLIENT = "clientHomePnl";

    public static final String SCREEN_USERS = "screen.users";
    public static final String SCREEN_PRODUCTS = "screen.products";
    public static final String SCREEN_HISTORY = "screen.history";
    public static final String SCREEN_LOGS = "screen.logs";

    public static final String PRODUCT_VIEW = "product.view";
    public static final String PRODUCT_PURCHASE = "product.purchase";
    public static final String PRODUCT_ADD = "product.add";
    public static final String PRODUCT_EDIT = "product.edit";
    public static final String PRODUCT_DELETE = "product.delete";

    public static final String HISTORY_VIEW_OWN = "history.view.own";
    public static final String HISTORY_VIEW_ALL = "history.view.all";

    public static final String USER_VIEW = "user.view";
    public static final String USER_EDIT_ROLE = "user.editrole";
    public static final String USER_DELETE = "user.delete";
    public static final String USER_LOCK = "user.lock";
    public static final String USER_CHANGE_PASSWORD = "user.changepassword";

    public static final String LOGS_VIEW = "logs.view";
    public static final String LOGS_CLEAR = "logs.clear";

    private static final Map<String, int[]> RULES = new HashMap<String, int[]>();

    static {
        RULES.put(PANEL_ADMIN, new int[]{5});
        RULES.put(PANEL_MANAGER, new int[]{4});
        RULES.put(PANEL_STAFF, new int[]{3});
        RULES.put(PANEL_CLIENT, new int[]{2});

        RULES.put(SCREEN_USERS, new int[]{5});
        RULES.put(SCREEN_PRODUCTS, new int[]{2, 3, 4});
        RULES.put(SCREEN_HISTORY, new int[]{2, 4});
        RULES.put(SCREEN_LOGS, new int[]{5});

        RULES.put(PRODUCT_VIEW, new int[]{2, 3, 4});
        RULES.put(PRODUCT_PURCHASE, new int[]{2});
        RULES.put(PRODUCT_ADD, new int[]{3, 4});
        RULES.put(PRODUCT_EDIT, new int[]{3, 4});
        RULES.put(PRODUCT_DELETE, new int[]{3, 4});

        RULES.put(HISTORY_VIEW_OWN, new int[]{2});
        RULES.put(HISTORY_VIEW_ALL, new int[]{4});

        RULES.put(USER_VIEW, new int[]{5});
        RULES.put(USER_EDIT_ROLE, new int[]{5});
        RULES.put(USER_DELETE, new int[]{5});
        RULES.put(USER_LOCK, new int[]{5});
        RULES.put(USER_CHANGE_PASSWORD, new int[]{5});

        RULES.put(LOGS_VIEW, new int[]{5});
        RULES.put(LOGS_CLEAR, new int[]{5});
    }

    private static SQLite db;

    private AccessControl() {}

    /** Binds the database handle used for fresh role lookups. */
    public static void init(SQLite sqlite) {
        db = sqlite;
    }

    /**
     * Returns true if the given role code is permitted to access the resource.
     * @param role role code (2-5)
     * @param resource resource constant from {@link AccessControl}
     */
    public static boolean roleAllowed(int role, String resource) {
        int[] allowed = RULES.get(resource);
        if (allowed == null) {
            return false;
        }
        for (int r : allowed) {
            if (r == role) {
                return true;
            }
        }
        return false;
    }

    /** Queries the database for the user's current active role (2-5), or 0 if inactive/invalid. */
    public static int currentRole(User user) {
        try {
            if (db == null || user == null || user.getUsername() == null) {
                return 0;
            }
            User fresh = db.getUserByUsername(user.getUsername());
            if (fresh == null) {
                return 0;
            }
            int role = fresh.getRole();
            return (role >= 2 && role <= 5) ? role : 0;
        } catch (Exception e) {
            ErrorHandler.handle("AccessControl.currentRole", e);
            return 0;
        }
    }

    /**
     * Checks whether the user currently holds a role allowed for the resource without logging.
     * @param user authenticated session user
     * @param resource resource constant from {@link AccessControl}
     */
    public static boolean isAllowed(User user, String resource) {
        try {
            return roleAllowed(currentRole(user), resource);
        } catch (Exception e) {
            ErrorHandler.handle("AccessControl.isAllowed", e);
            return false;
        }
    }

    /** Returns true if the user's account is active (role 2-5). */
    public static boolean isActive(User user) {
        return currentRole(user) != 0;
    }

    /**
     * Enforces access control for a resource and logs an ACCESS_DENIED event on failure.
     * @param user authenticated session user
     * @param resource resource constant from {@link AccessControl}
     */
    public static boolean check(User user, String resource) {
        boolean ok = isAllowed(user, resource);
        if (!ok) {
            SecurityLogger.log("ACCESS_DENIED", user == null ? "unknown" : user.getUsername(),
                    "Attempted to access " + resource + " without sufficient privileges");
        }
        return ok;
    }

    /** Resolves the home panel card name for the user's role, or logs and returns null if unauthorized. */
    public static String homePanelFor(User user) {
        int role = currentRole(user);
        String panel = null;
        if (role == 5) {
            panel = PANEL_ADMIN;
        } else if (role == 4) {
            panel = PANEL_MANAGER;
        } else if (role == 3) {
            panel = PANEL_STAFF;
        } else if (role == 2) {
            panel = PANEL_CLIENT;
        }
        if (panel == null) {
            SecurityLogger.log("ACCESS_DENIED", user == null ? "unknown" : user.getUsername(),
                    "No home panel available for this account");
        }
        return panel;
    }
}
