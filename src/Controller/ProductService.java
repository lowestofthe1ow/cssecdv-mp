package Controller;

import Model.Product;
import Model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ProductService {

    public static final String DENIED = "Access denied.";
    public static final String GENERIC_ERROR = "Something went wrong. Please try again.";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final SQLite db;

    public ProductService(SQLite db) {
        this.db = db;
    }

    private static String who(User u) {
        return u == null ? "unknown" : u.getUsername();
    }

    private String validateProduct(User actor, String name, String stockText, String priceText) {
        if (!Validator.productName(name)) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product name rejected by validation");
            return "Product name must be 1-30 letters, digits or spaces.";
        }
        if (!Validator.stock(stockText)) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product stock rejected by validation");
            return "Stock must be a whole number from 0 to " + Validator.MAX_STOCK + ".";
        }
        if (!Validator.price(priceText)) {
            SecurityLogger.log("VALIDATION_FAIL", who(actor), "Product price rejected by validation");
            return "Price must be greater than 0, at most " + (long) Validator.MAX_PRICE
                    + ", with up to 2 decimals.";
        }
        return null;
    }

    /** Returns the product catalogue if the actor is authorized to view products. */
    public ArrayList<Product> list(User actor) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_VIEW)) {
                return new ArrayList<Product>();
            }
            return db.getProduct();
        } catch (Exception e) {
            ErrorHandler.handle("ProductService.list", e);
            return new ArrayList<Product>();
        }
    }

    /**
     * Validates and executes a client product purchase, recording stock deduction and history.
     * @param actor authenticated client user
     * @param productName product name to purchase
     * @param qtyText quantity text (1 to MAX_STOCK)
     */
    public String purchase(User actor, String productName, String qtyText) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_PURCHASE)) {
                return DENIED;
            }
            if (!Validator.productName(productName)) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Purchase product name rejected by validation");
                return "Invalid product.";
            }
            if (!Validator.quantity(qtyText)) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Purchase quantity rejected by validation");
                return "Quantity must be a whole number from 1 to " + Validator.MAX_STOCK + ".";
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
            ErrorHandler.handle("ProductService.purchase", e);
            return GENERIC_ERROR;
        }
    }

    /**
     * Validates and adds a new product when requested by authorized staff or managers.
     * @param actor authenticated staff or manager
     * @param name unique product name
     * @param stockText initial stock count
     * @param priceText positive unit price
     */
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
            ErrorHandler.handle("ProductService.addProduct", e);
            return "Could not add the product. The name may already exist.";
        }
    }

    /**
     * Validates and updates an existing product's name, stock, and price.
     * @param actor authenticated staff or manager
     * @param oldName current product name
     * @param name updated product name
     * @param stockText updated stock count
     * @param priceText updated positive unit price
     */
    public String editProduct(User actor, String oldName, String name, String stockText, String priceText) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_EDIT)) {
                return DENIED;
            }
            if (!Validator.productName(oldName)) {
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
            ErrorHandler.handle("ProductService.editProduct", e);
            return "Could not update the product. The name may already exist.";
        }
    }

    /**
     * Deletes a product by name after checking authorization and input validity.
     * @param actor authenticated staff or manager
     * @param name product name to delete
     */
    public String deleteProduct(User actor, String name) {
        try {
            if (!AccessControl.check(actor, AccessControl.PRODUCT_DELETE)) {
                return DENIED;
            }
            if (!Validator.productName(name)) {
                SecurityLogger.log("VALIDATION_FAIL", who(actor), "Delete product name rejected by validation");
                return "Invalid product.";
            }
            if (!db.deleteProduct(name)) {
                return "Product not found.";
            }
            SecurityLogger.log("NOTICE", actor.getUsername(), "Product deleted: " + name);
            return null;
        } catch (Exception e) {
            ErrorHandler.handle("ProductService.deleteProduct", e);
            return GENERIC_ERROR;
        }
    }
}
