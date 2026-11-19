package Controller;

import java.util.regex.Pattern;

public final class Validator {
    public static final int MAX_STOCK = 100000;
    public static final double MAX_PRICE = 1000000.00;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9]{3,20}$");
    private static final Pattern PRODUCT  = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ]{0,29}$");
    private static final Pattern SEARCH   = Pattern.compile("^[A-Za-z0-9 ]{0,30}$");
    private static final Pattern ROLE     = Pattern.compile("^[1-5]$");
    private static final Pattern WHOLE    = Pattern.compile("^(0|[1-9][0-9]{0,5})$");
    private static final Pattern MONEY    = Pattern.compile("^(0|[1-9][0-9]{0,6})(\\.[0-9]{1,2})?$");

    private Validator() {}

    /** Validates an alphanumeric username (3-20 characters). */
    public static boolean username(String s)    { return s != null && USERNAME.matcher(s).matches(); }
    /** Validates a product name (1-30 alphanumeric or space characters). */
    public static boolean productName(String s) { return s != null && PRODUCT.matcher(s).matches(); }
    /** Validates a search filter (0-30 alphanumeric or space characters). */
    public static boolean search(String s)      { return s != null && SEARCH.matcher(s).matches(); }
    /** Validates a role code string ('1' through '5'). */
    public static boolean role(String s)        { return s != null && ROLE.matcher(s).matches(); }
    /** Validates a whole-number stock count from 0 to MAX_STOCK. */
    public static boolean stock(String s) {
        return s != null && WHOLE.matcher(s).matches() && Integer.parseInt(s) <= MAX_STOCK;
    }
    /** Validates a whole-number purchase quantity from 1 to MAX_STOCK. */
    public static boolean quantity(String s) {
        return stock(s) && Integer.parseInt(s) >= 1;
    }
    /** Validates a positive currency amount up to MAX_PRICE with at most two decimals. */
    public static boolean price(String s) {
        if (s == null || !MONEY.matcher(s).matches()) return false;
        double p = Double.parseDouble(s);
        return p > 0 && p <= MAX_PRICE;
    }
    /** Checks that a password array is within the allowed min/max length bounds. */
    public static boolean passwordLength(char[] p) {
        return p != null && p.length >= PasswordPolicy.MIN_LENGTH && p.length <= PasswordPolicy.MAX_LENGTH;
    }
}