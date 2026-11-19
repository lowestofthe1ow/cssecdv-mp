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

    /** Returns all user accounts if the actor is authorized to view users. */
    public ArrayList<User> list(User actor) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_VIEW)) {
                return new ArrayList<User>();
            }
            return db.getUsers();
        } catch (Exception e) {
            ErrorHandler.handle("UserService.list", e);
            return new ArrayList<User>();
        }
    }

    /**
     * Updates another user's role code (1-5) after admin authorization and validation.
     * @param actor authenticated administrator
     * @param target username to modify (must differ from actor)
     * @param roleText new role code ("1"-"5")
     */
    public String changeRole(User actor, String target, String roleText) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_EDIT_ROLE)) {
                return DENIED;
            }
            if (!Validator.role(roleText)) {
                SecurityLogger.log("VALIDATION_FAIL", actor.getUsername(), "Role code rejected by validation");
                return "Role must be a number from 1 to 5.";
            }
            if (!AuthService.isValidUsername(target)) {
                SecurityLogger.log("VALIDATION_FAIL", actor.getUsername(), "Target username rejected by validation");
                return "User not found.";
            }
            if (db.getUserByUsername(target) == null) {
                return "User not found.";
            }
            if (isSelf(actor, target)) {
                return "You cannot change your own role.";
            }
            db.setUserRole(db.getUserByUsername(target).getUsername(), Integer.parseInt(roleText));
            SecurityLogger.log("NOTICE", actor.getUsername(), "Role of " + target + " changed to " + roleText);
            return null;
        } catch (Exception e) {
            ErrorHandler.handle("UserService.changeRole", e);
            return GENERIC_ERROR;
        }
    }

    /**
     * Deletes another user's account after admin authorization and validation.
     * @param actor authenticated administrator
     * @param target username to delete (must differ from actor)
     */
    public String delete(User actor, String target) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_DELETE)) {
                return DENIED;
            }
            if (!AuthService.isValidUsername(target)) {
                SecurityLogger.log("VALIDATION_FAIL", actor.getUsername(), "Target username rejected by validation");
                return "User not found.";
            }
            if (db.getUserByUsername(target) == null) {
                return "User not found.";
            }
            if (isSelf(actor, target)) {
                return "You cannot delete your own account.";
            }
            db.removeUser(db.getUserByUsername(target).getUsername());
            SecurityLogger.log("NOTICE", actor.getUsername(), "User deleted: " + target);
            return null;
        } catch (Exception e) {
            ErrorHandler.handle("UserService.delete", e);
            return GENERIC_ERROR;
        }
    }

    /**
     * Locks an active user (to Role 1) or unlocks a locked user back to their previous role.
     * @param actor authenticated administrator
     * @param target username to lock or unlock (must differ from actor)
     */
    public String toggleLock(User actor, String target) {
        try {
            if (!AccessControl.check(actor, AccessControl.USER_LOCK)) {
                return DENIED;
            }
            if (!AuthService.isValidUsername(target)) {
                SecurityLogger.log("VALIDATION_FAIL", actor.getUsername(), "Target username rejected by validation");
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
            if (currentlyLocked) {
                if (!db.unlockUser(t.getUsername())) {
                    return "Could not update the account.";
                }
                User restored = db.getUserByUsername(t.getUsername());
                SecurityLogger.log("NOTICE", actor.getUsername(),
                        "User unlocked: " + t.getUsername() + " (role " + (restored == null ? "unknown" : restored.getRole()) + ")");
            } else {
                db.setUserRole(t.getUsername(), 1);
                SecurityLogger.log("NOTICE", actor.getUsername(), "User locked: " + t.getUsername());
            }
            return null;
        } catch (Exception e) {
            ErrorHandler.handle("UserService.toggleLock", e);
            return GENERIC_ERROR;
        }
    }
}
