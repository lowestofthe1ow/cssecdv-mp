package Controller;

import Model.User;
import java.util.ArrayList;

public class UserService {

    public static final String DENIED = "Access denied.";
    public static final String GENERIC_ERROR = "Something went wrong. Please try again.";

    private final SQLite db;

    public UserService(SQLite db) {
        this.db = db;
    }

    private static boolean isSelf(User actor, String target) {
        return actor != null && actor.getUsername() != null && actor.getUsername().equalsIgnoreCase(target);
    }

    public ArrayList<User> list(User actor) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_VIEW)) {
                return new ArrayList<User>();
            }
            return db.getUsers();
        } catch (Exception e) {
            return new ArrayList<User>();
        }
    }

    public String changeRole(User actor, String target, String roleText) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_EDIT_ROLE)) {
                return DENIED;
            }
            if (!Validator.role(roleText)) {
                SecurityLogger.log("VALIDATION_FAIL", actor.getUsername(), "Role code rejected by validation");
                return "Role must be a number from 1 to 5.";
            }
            if (!AuthService.isValidUsername(target) || db.getUserByUsername(target) == null) {
                return "User not found.";
            }
            if (isSelf(actor, target)) {
                return "You cannot change your own role.";
            }
            db.setUserRole(db.getUserByUsername(target).getUsername(), Integer.parseInt(roleText));
            SecurityLogger.log("NOTICE", actor.getUsername(), "Role of " + target + " changed to " + roleText);
            return null;
        } catch (Exception e) {
            return GENERIC_ERROR;
        }
    }

    public String delete(User actor, String target) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_DELETE)) {
                return DENIED;
            }
            if (!AuthService.isValidUsername(target) || db.getUserByUsername(target) == null) {
                return "User not found.";
            }
            if (isSelf(actor, target)) {
                return "You cannot delete your own account.";
            }
            db.removeUser(db.getUserByUsername(target).getUsername());
            SecurityLogger.log("NOTICE", actor.getUsername(), "User deleted: " + target);
            return null;
        } catch (Exception e) {
            return GENERIC_ERROR;
        }
    }

    public String toggleLock(User actor, String target) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_LOCK)) {
                return DENIED;
            }
            if (!AuthService.isValidUsername(target)) {
                return "User not found.";
            }
            User t = db.getUserByUsername(target);
            if (t == null) {
                return "User not found.";
            }
            if (isSelf(actor, target)) {
                return "You cannot lock your own account.";
            }
            boolean currentlyLocked = t.getRole() == 1 || t.getLocked() == 1;
            db.setUserRole(t.getUsername(), currentlyLocked ? 2 : 1);
            SecurityLogger.log("NOTICE", actor.getUsername(),
                    (currentlyLocked ? "User unlocked (set to Client): " : "User locked: ") + t.getUsername());
            return null;
        } catch (Exception e) {
            return GENERIC_ERROR;
        }
    }
}
