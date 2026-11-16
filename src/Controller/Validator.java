package Controller;

import java.util.regex.Pattern;

public final class Validator {
    public static final int MAX_STOCK = 100000;
    public static final double MAX_PRICE = 1000000.00;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9]{3,20}$");
    private static final Pattern PRODUCT  = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ]{0,29}$");
    private static final Pattern SEARCH   = Pattern.compile("^[A-Za-z0-9 ]{0,30}$");
    private static final Pattern ROLE     = Pattern.compile("^[1-5]$");
    private static final Pattern WHOLE    = Pattern.compile("^[0-9]{1,6}$");
    private static final Pattern MONEY    = Pattern.compile("^[0-9]{1,7}(\\.[0-9]{1,2})?$");

    private Validator() {}

    public static boolean username(String s)    { return s != null && USERNAME.matcher(s).matches(); }
    public static boolean productName(String s) { return s != null && PRODUCT.matcher(s).matches(); }
    public static boolean search(String s)      { return s != null && SEARCH.matcher(s).matches(); }
    public static boolean role(String s)        { return s != null && ROLE.matcher(s).matches(); }
    public static boolean stock(String s) {
        return s != null && WHOLE.matcher(s).matches() && Integer.parseInt(s) <= MAX_STOCK;
    }
    public static boolean quantity(String s) {
        return stock(s) && Integer.parseInt(s) >= 1;
    }
    public static boolean price(String s) {
        if (s == null || !MONEY.matcher(s).matches()) return false;
        double p = Double.parseDouble(s);
        return p > 0 && p <= MAX_PRICE;
    }
    public static boolean passwordLength(char[] p) {
        return p != null && p.length >= PasswordPolicy.MIN_LENGTH && p.length <= PasswordPolicy.MAX_LENGTH;
    }
}