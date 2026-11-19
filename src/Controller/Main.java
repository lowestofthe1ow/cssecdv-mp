package Controller;

import View.Frame;

public class Main {

    public SQLite sqlite;

    public static void main(String[] args) {
        ErrorHandler.install();
        new Main().init();
    }

    public void init() {
        initializeDatabase();
        Frame frame = new Frame();
        frame.init(this);
    }

    /** Initializes the local database without discarding existing application data. */
    public void initializeDatabase() {
        sqlite = new SQLite();
        sqlite.createNewDatabase();
        sqlite.createLogsTable();
        SecurityLogger.init(sqlite);
        AccessControl.init(sqlite);

        sqlite.initUsersSchema();
        sqlite.createProductTable();
        sqlite.createHistoryTable();
        sqlite.seedSampleProducts();
    }
}
