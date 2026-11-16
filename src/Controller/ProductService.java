package Controller;

import Model.Product;
import Model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ProductService {

    public static final String DENIED = "Access denied.";
    public static final String GENERIC_ERROR = "Something went wrong. Please try again.";

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ]{0,29}$");
    private static final Pattern WHOLE = Pattern.compile("^[0-9]{1,6}$");
    private static final Pattern MONEY = Pattern.compile("^[0-9]{1,7}(\\.[0-9]{1,2})?$");
    private static final int MAX_STOCK = 100000;
    private static final double MAX_PRICE = 1000000.00;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final SQLite db;

    public ProductService(SQLite db) {
        this.db = db;
    }

    private static String who(User u) {
        return u == null ? "unknown" : u.getUsername();
    }

    private String validateProduct(User actor, String name, String stockText, String priceText) {
        if (name == null || !NAME.matcher(name).matches()) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product name rejected by validation");
            return "Product name must be 1-30 letters, digits or spaces.";
        }
        if (stockText == null || !WHOLE.matcher(stockText).matches() || Integer.parseInt(stockText) > MAX_STOCK) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product stock rejected by validation");
            return "Stock must be a whole number from 0 to " + MAX_STOCK + ".";
        }
        if (priceText == null || !MONEY.matcher(priceText).matches()) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product price rejected by validation");
            return "Price must be a number with up to 2 decimals.";
        }
        double price = Double.parseDouble(priceText);
        if (price <= 0 || price > MAX_PRICE) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product price out of range");
            return "Price must be greater than 0 and at most " + (long) MAX_PRICE + ".";
        }
        return null;
    }

    public ArrayList<Product> list(User actor) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_VIEW)) {
                return new ArrayList<Product>();
            }
            return db.getProduct();
        } catch (Exception e) {
            return new ArrayList<Product>();
        }
    }

    public String purchase(User actor, String productName, String qtyText) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_PURCHASE)) {
                return DENIED;
            }
            if (productName == null || !NAME.matcher(productName).matches()) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Purchase product name rejected by validation");
                return "Invalid product.";
            }
            if (qtyText == null || !WHOLE.matcher(qtyText).matches()
                    || Integer.parseInt(qtyText) < 1 || Integer.parseInt(qtyText) > MAX_STOCK) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Purchase quantity rejected by validation");
                return "Quantity must be a whole number from 1 to " + MAX_STOCK + ".";
            }
            int qty = Integer.parseInt(qtyText);
            if (db.getProduct(productName) == null) {
                return "Product not found.";
            }
            if (!db.purchaseProduct(actor.getUsername(), productName, qty, LocalDateTime.now().format(TS))) {
                return "Not enough stock available.";
            }
            SecurityLogger.log("NOTICE", actor.getUsername(), "Purchased " + qty + " x " + productName);
            return null;
        } catch (Exception e) {
            return GENERIC_ERROR;
        }
    }

    public String addProduct(User actor, String name, String stockText, String priceText) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_ADD)) {
                return DENIED;
            }
            String error = validateProduct(actor, name, stockText, priceText);
            if (error != null) {
                return error;
            }
            db.addProduct(name, Integer.parseInt(stockText), Double.parseDouble(priceText));
            SecurityLogger.log("NOTICE", actor.getUsername(), "Product added: " + name);
            return null;
        } catch (Exception e) {
            return "Could not add the product. The name may already exist.";
        }
    }

    public String editProduct(User actor, String oldName, String name, String stockText, String priceText) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_EDIT)) {
                return DENIED;
            }
            if (oldName == null || !NAME.matcher(oldName).matches()) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Edit product name rejected by validation");
                return "Invalid product.";
            }
            String error = validateProduct(actor, name, stockText, priceText);
            if (error != null) {
                return error;
            }
            if (!db.updateProduct(oldName, name, Integer.parseInt(stockText), Double.parseDouble(priceText))) {
                return "Product not found.";
            }
            SecurityLogger.log("NOTICE", actor.getUsername(), "Product edited: " + oldName);
            return null;
        } catch (Exception e) {
            return "Could not update the product. The name may already exist.";
        }
    }

    public String deleteProduct(User actor, String name) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_DELETE)) {
                return DENIED;
            }
            if (name == null || !NAME.matcher(name).matches()) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Delete product name rejected by validation");
                return "Invalid product.";
            }
            if (!db.deleteProduct(name)) {
                return "Product not found.";
            }
            SecurityLogger.log("NOTICE", actor.getUsername(), "Product deleted: " + name);
            return null;
        } catch (Exception e) {
            return GENERIC_ERROR;
        }
    }
}
