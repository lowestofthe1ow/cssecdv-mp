<div align="center">

CSSECDV Machine Project

<h1>"SECURITY Svcs"</h1>

</div>

## Setup

### Requirements

- JDK 8 or newer (target Java 1.8)
- Apache Ant or NetBeans

The only runtime dependency is `sqlite-jdbc-3.23.1.jar`, which is included in
the repository root and copied into `dist/lib/` by the Ant build. SQLite command-line
tools are optional and are not required to run the Java application.

### Build and run

With Ant:

```bash
ant clean jar
cd dist
java -jar CSSECDV.jar
```

The `dist/` directory is a runnable distribution and contains:

- `CSSECDV.jar` - executable application JAR
- `lib/sqlite-jdbc-3.23.1.jar` - SQLite JDBC driver referenced by the JAR manifest
- `database.db` - seeded demonstration database

Run the JAR with the `dist/` directory as the working directory so SQLite resolves
`database.db` beside the application. The application also creates missing tables and
seeds sample products/default accounts when starting with a new database.

The main class is in `src/Controller/Main.java`.

### SQLite

The application uses the SQLite JDBC driver directly; no SQLite server or command-line
binaries are required. A SQLite CLI may optionally be installed to inspect `database.db`.

## Demo accounts

Created automatically on first run (stored as salted PBKDF2 hashes in `database.db`).

| Username | Password | Role |
| :--- | :--- | :---: |
| admin | Admin#2026! | 5 (Administrator) |
| manager | Manager#2026! | 4 (Manager) |
| staff | Staff#2026! | 3 (Staff) |
| client1 | Client#2026! | 2 (Client) |
| client2 | Client#2027! | 2 (Client) |

Five consecutive failed logins move an account to Role 1 (Disabled).
