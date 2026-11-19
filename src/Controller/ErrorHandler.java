package Controller;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class ErrorHandler {
    public static final String GENERIC_MESSAGE = "Something went wrong. Please try again.";
    private static volatile Runnable failsafe;

    private ErrorHandler() {}

    /** Registers the recovery callback invoked on unrecoverable uncaught exceptions. */
    public static void setFailsafe(Runnable r) { failsafe = r; }

    /**
     * Logs only the location and exception class name without leaking messages or stack traces.
     * @param where method or component identifier
     * @param t thrown exception or error
     */
    public static void handle(String where, Throwable t) {
        SecurityLogger.log("ERROR", "system", where + ": " + t.getClass().getSimpleName());
    }

    /**
     * Logs an error internally and displays a generic error dialog to the user.
     * @param parent parent Swing component for dialog positioning
     * @param where method or component identifier
     * @param t thrown exception or error
     */
    public static void report(java.awt.Component parent, String where, Throwable t) {
        handle(where, t);
        JOptionPane.showMessageDialog(parent, GENERIC_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Installs a global uncaught-exception handler that triggers the failsafe and shows a generic dialog. */
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