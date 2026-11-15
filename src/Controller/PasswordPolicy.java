package Controller;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private PasswordPolicy() {}

    
    public static String validate(char[] pw) {
        if (pw == null || pw.length < MIN_LENGTH || pw.length > MAX_LENGTH) {
            return "Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.";
        }
        boolean upper = false, lower = false, digit = false, symbol = false;
        for (char c : pw) {
            if (c < 0x21 || c > 0x7E) {
                return "Password may only use letters, digits and standard symbols (no spaces).";
            }
            if (c >= 'A' && c <= 'Z') upper = true;
            else if (c >= 'a' && c <= 'z') lower = true;
            else if (c >= '0' && c <= '9') digit = true;
            else symbol = true;
        }
        if (!(upper && lower && digit && symbol)) {
            return "Password must contain an uppercase letter, a lowercase letter, a digit and a symbol.";
        }
        return null;
    }
}
