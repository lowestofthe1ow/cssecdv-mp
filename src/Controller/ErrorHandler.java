package Controller;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class ErrorHandler {
    public static final String GENERIC_MESSAGE = "Something went wrong. Please try again.";
    private static volatile Runnable failsafe;

    private ErrorHandler() {}

    public static void setFailsafe(Runnable r) { failsafe = r; }

    // Internal record only: location + exception class, never message or stack trace.
    public static void handle(String where, Throwable t) {
        SecurityLogger.log("ERROR", "system", where + ": " + t.getClass().getSimpleName());
    }

    // For view handlers: log, show a generic dialog, return to a safe state.
    public static void report(java.awt.Component parent, String where, Throwable t) {
        handle(where, t);
        JOptionPane.showMessageDialog(parent, GENERIC_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((th, ex) -> {
            handle("uncaught", ex);
            SwingUtilities.invokeLater(() -> {
                try {
                    Runnable r = failsafe;
                    if (r != null) r.run();
                    JOptionPane.showMessageDialog(null, GENERIC_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Throwable ignored) { }
            });
        });
    }
}