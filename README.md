<div align="center">

CSSECDV Machine Project

<h1>"SECURITY Svcs"</h1>

</div>

## Setup

### Requirements

- JDK 8 or newer (target Java 1.8)
- Apache Ant or NetBeans

The only dependency is `sqlite-jdbc-3.23.1.jar`, which is included in the repo
root (same file provided in the Canvas assignment)

### Build and run

With Ant:

```bash
ant clean jar
java -jar dist/CSSECDV.jar    # or: ant run
```

The main class is in `src/Controller/Main.java`.

### SQLite

If on Windows or Mac, grab the SQLite binaries from the Canvas assignment.
On Linux (Fedora), run the following command:

```bash
sudo dnf install sqlite sqlite-tools sqlite-analyzer
```

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
