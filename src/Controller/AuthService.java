package Controller;

import Model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class AuthService {

    public static final int MAX_ATTEMPTS = 5;
    public static final String GENERIC_LOGIN_ERROR = "Invalid username and/or password";
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

    public static boolean isValidUsername(String u) {
        return Validator.username(u);
    }

    
    public LoginResult login(String username, char[] password) {
        try {
            if (!isValidUsername(username) || password == null
                    || password.length == 0 || password.length > PasswordPolicy.MAX_LENGTH) {
                PasswordUtil.dummyVerify();
                SecurityLogger.log("VALIDATION_FAIL", "unknown", "Login input rejected by validation");
                return DENIED;
            }

            User u = db.getUserByUsername(username);
            if (u == null) {
                PasswordUtil.dummyVerify();
                SecurityLogger.log("AUTH_FAIL", username, "Login failed: unknown username");
                return DENIED;
            }

            boolean passwordOk = PasswordUtil.verify(password, u.getSalt(), u.getPassword());
            int role = u.getRole();

            if (role < 2 || role > 5) {
                SecurityLogger.log("AUTH_FAIL", u.getUsername(), "Login blocked: account disabled or invalid role");
                return DENIED;
            }
            if (!passwordOk) {
                db.recordLoginFailure(u.getUsername(), now(), MAX_ATTEMPTS);
                SecurityLogger.log("AUTH_FAIL", u.getUsername(), "Login failed: wrong password");
                User after = db.getUserByUsername(u.getUsername());
                if (after != null && after.getRole() == 1) {
                    SecurityLogger.log("ACCOUNT_LOCKED", u.getUsername(),
                            "Account disabled after " + MAX_ATTEMPTS + " consecutive failed logins");
                }
                return DENIED;
            }

            db.recordLoginSuccess(u.getUsername(), now());
            SecurityLogger.log("AUTH_SUCCESS", u.getUsername(), "Login successful");
            return new LoginResult(true, u, u.getLastAttempt(), u.getLastStatus());
        } catch (Exception e) {
            return DENIED;
        }
    }

    
    public String register(String username, char[] password, char[] confirm) {
        try {
            if (!isValidUsername(username)) {
                SecurityLogger.log("VALIDATION_FAIL", "unknown", "Registration username rejected");
                return "Username must be 3-20 characters, letters and digits only.";
            }
            String policyError = PasswordPolicy.validate(password);
            if (policyError != null) {
                SecurityLogger.log("VALIDATION_FAIL", username, "Registration password rejected by policy");
                return policyError;
            }
            if (confirm == null || !Arrays.equals(password, confirm)) {
                SecurityLogger.log("VALIDATION_FAIL", username, "Registration password confirmation mismatch");
                return "Passwords do not match.";
            }
            if (db.getUserByUsername(username) != null) {
                SecurityLogger.log("VALIDATION_FAIL", username, "Registration rejected: username unavailable");
                return "Registration failed. Please choose a different username.";
            }
            String salt = PasswordUtil.newSalt();
            db.addUser(username, PasswordUtil.hash(password, salt), salt, 2);
            SecurityLogger.log("NOTICE", username, "User registration successful");
            return null;
        } catch (Exception e) {
            return "Registration failed. Please try again.";
        }
    }

    
    public String changePasswordFor(User actor, String targetUsername, char[] actorPw, char[] newPw, char[] confirm) {
        try {
            if (actor == null || actorPw == null || targetUsername == null) {
                return GENERIC_ERROR;
            }
            User a = db.getUserByUsername(actor.getUsername());
            if (a == null || a.getRole() < 2 || a.getRole() > 5) {
                return GENERIC_ERROR;
            }
            if (!AccessControl.check(a, AccessControl.USER_CHANGE_PASSWORD)) {
                return "Access denied.";
            }
            if (!Validator.passwordLength(actorPw)) {
                return GENERIC_ERROR;
            }
            if (!PasswordUtil.verify(actorPw, a.getSalt(), a.getPassword())) {
                db.recordLoginFailure(a.getUsername(), now(), MAX_ATTEMPTS);
                SecurityLogger.log("AUTH_FAIL", a.getUsername(), "Re-authentication failed during password change");
                return "Your current password is incorrect.";
            }
            User t = db.getUserByUsername(targetUsername);
            if (t == null) {
                return GENERIC_ERROR;
            }
            String policyError = PasswordPolicy.validate(newPw);
            if (policyError != null) {
                SecurityLogger.log("VALIDATION_FAIL", a.getUsername(), "New password rejected by policy");
                return policyError;
            }
            if (confirm == null || !Arrays.equals(newPw, confirm)) {
                SecurityLogger.log("VALIDATION_FAIL", a.getUsername(), "Password change confirmation mismatch");
                return "New passwords do not match.";
            }
            if (a.getUsername().equalsIgnoreCase(t.getUsername()) && Arrays.equals(newPw, actorPw)) {
                return "New password must be different from the current password.";
            }
            String salt = PasswordUtil.newSalt();
            db.updatePassword(t.getUsername(), PasswordUtil.hash(newPw, salt), salt);
            SecurityLogger.log("NOTICE", a.getUsername(),
                    "Password of " + t.getUsername() + " changed after re-authentication");
            return null;
        } catch (Exception e) {
            return GENERIC_ERROR;
        }
    }

    public boolean isAccountDisabled(String username) {
        try {
            User u = db.getUserByUsername(username);
            return u == null || u.getRole() < 2 || u.getRole() > 5;
        } catch (Exception e) {
            return true;
        }
    }

    
    public static void seedDefaultAccountsIfEmpty(SQLite db) {
        try {
            if (db.countUsers() > 0) {
                return;
            }
            String[][] accounts = {
                {"admin",    "Admin#2026!",   "5"},
                {"manager",  "Manager#2026!", "4"},
                {"staff",    "Staff#2026!",   "3"},
                {"client1",  "Client#2026!",  "2"},
                {"client2",  "Client#2027!",  "2"},
            };
            for (String[] a : accounts) {
                String salt = PasswordUtil.newSalt();
                db.addUser(a[0], PasswordUtil.hash(a[1].toCharArray(), salt), salt, Integer.parseInt(a[2]));
            }
        } catch (Exception e) {
        }
    }
}
