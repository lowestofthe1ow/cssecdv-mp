package Controller;

import Model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.sql.SQLException;

public class AuthService {
    public static final int MAX_ATTEMPTS = 5;
    public static final String GENERIC_LOGIN_ERROR = "Invalid username and/or password.";
    public static final String GENERIC_ERROR = "Something went wrong. Please try again.";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SQLite db;

    public AuthService(SQLite db) {
        this.db = db;
    }

    public static class LoginResult {
        public final boolean success;
        public final User user;
        public final String lastAttempt;
        public final String lastStatus;

        LoginResult(boolean success, User user, String lastAttempt, String lastStatus) {
            this.success = success;
            this.user = user;
            this.lastAttempt = lastAttempt;
            this.lastStatus = lastStatus;
        }
    }

    private static final LoginResult DENIED = new LoginResult(false, null, null, null);

    private static String now() {
        return LocalDateTime.now().format(TS);
    }

    /** Checks whether a username matches the alphanumeric 3-20 character whitelist. */
    public static boolean isValidUsername(String u) {
        return Validator.username(u);
    }

    private static boolean isActiveUser(User u) {
        return u != null && u.getRole() >= 2 && u.getRole() <= 5 && u.getLocked() == 0;
    }

    /**
     * Authenticates a user, enforcing lockout, password verification, and audit logging.
     * @param username account username
     * @param password plaintext password characters (caller clears after use)
     */
    public LoginResult login(String username, char[] password) {
        try {
            // Validate inputs first
            boolean validUser = isValidUsername(username);
            if (!validUser || password == null
                    || password.length == 0 || password.length > PasswordPolicy.MAX_LENGTH) {
                PasswordUtil.dummyVerify();
                SecurityLogger.log("VALIDATION_FAIL", "unknown", "Login input rejected by validation");
                SecurityLogger.log("AUTH_FAIL", validUser ? username : "unknown", "Login failed: invalid input");
                if (validUser) {
                    countFailedLogin(db.getUserByUsername(username));
                }
                return DENIED;
            }

            // Check if user exists
            User u = db.getUserByUsername(username);
            if (u == null) {
                PasswordUtil.dummyVerify();
                SecurityLogger.log("AUTH_FAIL", username, "Login failed: unknown username");
                return DENIED;
            }

            boolean passwordOk = PasswordUtil.verify(password, u.getSalt(), u.getPassword());

            // Check if user is active/not disabled (in case of account lockouts)
            if (!isActiveUser(u)) {
                SecurityLogger.log("AUTH_FAIL", u.getUsername(), "Login blocked: account disabled, locked or invalid role");
                return DENIED;
            }

            // Validate password
            if (!passwordOk) {
                countFailedLogin(u);
                SecurityLogger.log("AUTH_FAIL", u.getUsername(), "Login failed: wrong password");
                return DENIED;
            }

            // Success
            db.recordLoginSuccess(u.getUsername(), now());
            SecurityLogger.log("AUTH_SUCCESS", u.getUsername(), "Login successful");
            u.setPassword(null);
            u.setSalt(null);
            return new LoginResult(true, u, u.getLastAttempt(), u.getLastStatus());
        } catch (Exception e) {
            SecurityLogger.log("AUTH_FAIL", "unknown", "Login failed: authentication error");
            ErrorHandler.handle("AuthService.login", e);
            return DENIED;
        }
    }

    /** Counts a failed authentication for an active account and logs ACCOUNT_LOCKED at the threshold. */
    private void countFailedLogin(User user) throws SQLException {
        if (!isActiveUser(user)) {
            return;
        }

        // Let SQLite lockout user if failures >= MAX_ATTEMPTS
        db.recordLoginFailure(user.getUsername(), now(), MAX_ATTEMPTS);
        if (user.getFailedAttempts() + 1 >= MAX_ATTEMPTS) {
            SecurityLogger.log("ACCOUNT_LOCKED", user.getUsername(),
                    "Account disabled after " + MAX_ATTEMPTS + " consecutive failed logins");
        }
    }

    private String validateNewPassword(String actorName, String targetName, char[] pw, char[] confirm, String mismatchMsg) {
        String policyError = PasswordPolicy.validate(pw, targetName);

        // NOTE: actorName can be different if an admin is changing someone's password

        if (policyError != null) {
            SecurityLogger.log("VALIDATION_FAIL", actorName, "Password rejected by policy");
            return policyError;
        }

        if (confirm == null || !Arrays.equals(pw, confirm)) {
            SecurityLogger.log("VALIDATION_FAIL", actorName, "Password confirmation mismatch");
            return mismatchMsg;
        }

        return null;
    }

    /**
     * Validates and registers a new Role 2 account with a salted PBKDF2 hash.
     * @param username desired username (3-20 alphanumeric chars)
     * @param password password characters
     * @param confirm confirmation password characters
     */
    public String register(String username, char[] password, char[] confirm) {
        // WARNING: This is only for generating Role 2 accounts. Admins can elevate roles afterward.

        try {
            // Validate password
            if (!isValidUsername(username)) {
                SecurityLogger.log("VALIDATION_FAIL", "unknown", "Registration username rejected");
                return "Username must be 3-20 characters, letters and digits only.";
            }

            // Check if "Confirm password" matches
            String pwError = validateNewPassword(username, username, password, confirm, "Passwords do not match.");

            if (pwError != null) {
                return pwError;
            }

            // Check for username availability
            if (db.getUserByUsername(username) != null) {
                SecurityLogger.log("VALIDATION_FAIL", username, "Registration rejected: username unavailable");
                return "Registration failed. Please choose a different username.";
            }

            String salt = PasswordUtil.newSalt();
            db.addUser(username, PasswordUtil.hash(password, salt), salt, 2);
            SecurityLogger.log("NOTICE", username, "User registration successful");

            return null;
        } catch (Exception e) {
            ErrorHandler.handle("AuthService.register", e);
            return "Registration failed. Please try again.";
        }
    }

    /**
     * Changes a target user's password after verifying the actor's role and current password.
     * @param actor authenticated user performing the change
     * @param targetUsername username whose password is being updated
     * @param actorPw actor's current password for re-authentication
     * @param newPw new password characters
     * @param confirm confirmation of the new password
     */
    public String changePasswordFor(User actor, String targetUsername, char[] actorPw, char[] newPw, char[] confirm) {
        try {
            // Everything has generic messages only

            if (actor == null || actor.getUsername() == null || actorPw == null || targetUsername == null) {
                SecurityLogger.log("VALIDATION_FAIL", actor == null ? "unknown" : actor.getUsername(),
                        "Password change required input missing");
                return GENERIC_ERROR;
            }

            User a = db.getUserByUsername(actor.getUsername());
            if (!isActiveUser(a)) {
                return GENERIC_ERROR;
            }
            if (!AccessControl.check(a, AccessControl.USER_CHANGE_PASSWORD)) {
                return "Access denied.";
            }
            if (!Validator.passwordLength(actorPw)) {
                SecurityLogger.log("VALIDATION_FAIL", a.getUsername(), "Current password length rejected by validation");
                SecurityLogger.log("AUTH_FAIL", a.getUsername(), "Re-authentication failed: invalid input");
                return GENERIC_ERROR;
            }
            if (!PasswordUtil.verify(actorPw, a.getSalt(), a.getPassword())) {
                countFailedLogin(a);
                SecurityLogger.log("AUTH_FAIL", a.getUsername(), "Re-authentication failed during password change");
                return "Your current password is incorrect.";
            }

            SecurityLogger.log("AUTH_SUCCESS", a.getUsername(), "Re-authentication succeeded for password change");

            if (!isValidUsername(targetUsername)) {
                SecurityLogger.log("VALIDATION_FAIL", a.getUsername(), "Password change target username rejected by validation");
                return GENERIC_ERROR;
            }

            User t = db.getUserByUsername(targetUsername);
            if (t == null) {
                return GENERIC_ERROR;
            }

            String pwError = validateNewPassword(a.getUsername(), t.getUsername(), newPw, confirm, "New passwords do not match.");
            if (pwError != null) {
                return pwError;
            }
            if (a.getUsername().equalsIgnoreCase(t.getUsername()) && Arrays.equals(newPw, actorPw)) {
                SecurityLogger.log("VALIDATION_FAIL", a.getUsername(), "New password must differ from current password");
                return "New password must be different from the current password.";
            }

            String salt = PasswordUtil.newSalt();
            db.updatePassword(t.getUsername(), PasswordUtil.hash(newPw, salt), salt);
            SecurityLogger.log("NOTICE", a.getUsername(),
                    "Password of " + t.getUsername() + " changed after re-authentication");

            return null;
        } catch (Exception e) {
            ErrorHandler.handle("AuthService.changePasswordFor", e);
            return GENERIC_ERROR;
        }
    }

    /** Returns true if the account does not exist or holds an inactive/disabled role. */
    public boolean isAccountDisabled(String username) {
        try {
            return !isActiveUser(db.getUserByUsername(username));
        } catch (Exception e) {
            ErrorHandler.handle("AuthService.isAccountDisabled", e);
            return true;
        }
    }
}
