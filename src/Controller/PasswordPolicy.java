package Controller;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private PasswordPolicy() {}

    
    /** Validates password length, complexity, and topology without username context. */
    public static String validate(char[] pw) {
        return validate(pw, null);
    }

    /**
     * Validates password length (8-64), complexity (upper, lower, digit, symbol), and topology.
     * @param pw candidate password characters
     * @param username optional username to reject passwords containing the account name
     */
    public static String validate(char[] pw, String username) {
        if (pw == null || pw.length < MIN_LENGTH || pw.length > MAX_LENGTH) {
            return "Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.";
        }
        boolean upper = false, lower = false, digit = false, symbol = false;
        int classRuns = 0;
        int prevClass = 0;
        for (char c : pw) {
            if (c < 0x21 || c > 0x7E) {
                return "Password may only use letters, digits and standard symbols (no spaces).";
            }
            int cls;
            if (c >= 'A' && c <= 'Z') {
                upper = true;
                cls = 1;
            } else if (c >= 'a' && c <= 'z') {
                lower = true;
                cls = 2;
            } else if (c >= '0' && c <= '9') {
                digit = true;
                cls = 3;
            } else {
                symbol = true;
                cls = 4;
            }
            if (cls != prevClass) {
                classRuns++;
                prevClass = cls;
            }
        }
        if (!(upper && lower && digit && symbol)) {
            return "Password must contain an uppercase letter, a lowercase letter, a digit and a symbol.";
        }
        if (classRuns <= 4) {
            return "Password uses a predictable pattern; mix character types instead of grouping them.";
        }
        if (hasWeakSequence(pw)) {
            return "Password cannot contain 3 or more repeated or sequential characters.";
        }
        if (containsIgnoreCase(pw, username)) {
            return "Password cannot contain your username.";
        }
        return null;
    }

    private static boolean hasWeakSequence(char[] pw) {
        for (int i = 0; i <= pw.length - 3; i++) {
            char a = pw[i], b = pw[i + 1], c = pw[i + 2];
            if (a == b && b == c) {
                return true;
            }
            if (isAlphaNum(a) && isAlphaNum(b) && isAlphaNum(c)) {
                char la = Character.toLowerCase(a);
                char lb = Character.toLowerCase(b);
                char lc = Character.toLowerCase(c);
                if ((lb - la == 1 && lc - lb == 1) || (la - lb == 1 && lb - lc == 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAlphaNum(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean containsIgnoreCase(char[] pw, String username) {
        if (username == null || username.length() < 3 || pw.length < username.length()) {
            return false;
        }
        int uLen = username.length();
        for (int i = 0; i <= pw.length - uLen; i++) {
            boolean match = true;
            for (int j = 0; j < uLen; j++) {
                if (Character.toLowerCase(pw[i + j]) != Character.toLowerCase(username.charAt(j))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }
}
