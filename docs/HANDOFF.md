> **Historical baseline note (November 2026):** This handoff records findings from an earlier repository state. Several items below have since been addressed; use the current source and README for the latest implementation and build instructions.

> [!NOTE]
> This handoff document was generated with Gemini 3.8 Flash (High) with the
> Antigravity CLI (1.5.0).

### 1. Build and Environment

* **JDK Target Version**:
  * In [`nbproject/project.properties#L46-L47`](../nbproject/project.properties#L46-L47), the project targets **Java 1.8** (`javac.source=1.8`, `javac.target=1.8`).
  * **Newer JDK requirements**: Nothing in the codebase uses post-Java 8 syntax (no `var`, records, switch expressions, text blocks, or newer collections APIs). The project compiles cleanly under `--release 8`. However:
    1. Compiling with modern JDKs (e.g., JDK 21/25) emits compiler warnings that source/target value `1.8` is obsolete and bootstrap classpaths are not set.
    2. [`sqlite-jdbc-3.23.1.jar`](../sqlite-jdbc-3.23.1.jar) was released in 2018 and lacks native bindings for Apple Silicon (`Mac/aarch64`). It will fail on Apple Silicon M-series Macs unless run via Rosetta or upgraded to `sqlite-jdbc` 3.36.0+.

* **Classpath & Library References**:
  * In [`nbproject/project.properties#L35`](../nbproject/project.properties#L35): `file.reference.sqlite-jdbc-3.23.1.jar=sqlite-jdbc-3.23.1.jar`
  * In [`nbproject/project.properties#L40-L41`](../nbproject/project.properties#L40-L41): `javac.classpath=\ ${file.reference.sqlite-jdbc-3.23.1.jar}`
  * In [`nbproject/project.properties#L48-L50`](../nbproject/project.properties#L48-L50): `javac.test.classpath=\ ${javac.classpath}:\ ${build.classes.dir}`
  * In [`nbproject/project.properties#L74-L76`](../nbproject/project.properties#L74-L76): `run.classpath=\ ${javac.classpath}:\ ${build.classes.dir}`
  * In [`nbproject/project.properties#L82-L84`](../nbproject/project.properties#L82-L84): `run.test.classpath=\ ${javac.test.classpath}:\ ${build.test.classes.dir}`
  * In [`build.xml#L12`](../build.xml#L12): `<import file="nbproject/build-impl.xml"/>`
  * **Flagged Absolute Path**: In [`nbproject/private/private.properties#L8`](../nbproject/private/private.properties#L8), there is a machine-specific absolute path from macOS:
    `user.properties.file=/Users/fritzkevin/Library/Application Support/NetBeans/16/build.properties`

* **Location of `sqlite-jdbc-3.23.1.jar`**:
  * **Both**: The root directory contains [`sqlite-jdbc-3.23.1.jar`](../sqlite-jdbc-3.23.1.jar) (referenced by `project.properties:35`), and [`dist/lib/sqlite-jdbc-3.23.1.jar`](../dist/lib/sqlite-jdbc-3.23.1.jar) also exists (copied by NetBeans Ant packaging, referenced in the built JAR manifest `Class-Path: lib/sqlite-jdbc-3.23.1.jar`).

* **Main Class & Execution Commands**:
  * Main class: [`Controller.Main`](../src/Controller/Main.java#L15) (defined in [`nbproject/project.properties#L66`](../nbproject/project.properties#L66)).
  * **NetBeans**: Open project and press **F6** (or right-click project → **Run**).
  * **Ant**:
    ```bash
    ant clean
    ant compile
    ant jar
    ant run
    ```
  * **Without Ant (CLI)**:
    ```bash
    javac -cp sqlite-jdbc-3.23.1.jar -d build/classes $(find src -name "*.java")
    java -cp "sqlite-jdbc-3.23.1.jar:build/classes" Controller.Main
    ```

* **Build Execution Test (`ant clean jar`)**:
  * **Success**: Tested in an isolated directory; `ant clean jar` succeeds (`BUILD SUCCESSFUL`).
  * **System Note**: `ant` is not pre-installed in the Linux host's system PATH (`ant: command not found`).
  * **Warnings emitted**:
    * `warning: [options] bootstrap class path is not set in conjunction with -source 8`
    * `warning: [options] source/target value 8 is obsolete and will be removed in a future release`
    * `Note: src/View/MgmtUser.java uses unchecked or unsafe operations.` (due to raw type `JComboBox` in [`MgmtUser.java#L184`](../src/View/MgmtUser.java#L184)).

---

### 2. Git State

* **Tracked Files (`git ls-files`)**:
  * Running `git ls-files` returns **0 files**. The repository was freshly initialized (`git init`), has **no commits yet**, and nothing is staged.
  * None of `build/`, `dist/`, `nbproject/private/`, the SQLite binaries, or `database.db` are currently tracked in Git.

* **Git Configuration Files**:
  * [`.gitignore`](../.gitignore) exists (46 lines).
  * `.gitattributes` does **not** exist.

* **Ignore Status of Critical Files**:
  * `build/`, `dist/`, and `nbproject/private/` are ignored by [`.gitignore#L33-L37`](../.gitignore#L33-L37).
  * `sqlite-jdbc-3.23.1.jar` is explicitly unignored by [`.gitignore#L44`](../.gitignore#L44) (`!/sqlite-jdbc-3.23.1.jar`).
  * [`database.db`](../database.db) is **not ignored**. If `git add .` is executed, it will be committed.

* **Line Endings in `.java` Files**:
  * **Mixed**:
    * **CRLF**: [`src/Controller/SQLite.java`](../src/Controller/SQLite.java), [`src/Controller/Main.java`](../src/Controller/Main.java), [`src/Model/User.java`](../src/Model/User.java).
    * **LF**: All remaining 15 `.java` files in `src/Model/` and `src/View/`.

* **Large or Binary Files Risking Tracking**:
  * [`sqlite-jdbc-3.23.1.jar`](../sqlite-jdbc-3.23.1.jar) (6.7 MB JAR binary).
  * [`database.db`](../database.db) (32 KB SQLite database file with runtime state).

---

### 3. Database

* **JDBC URL**:
  * In [`src/Controller/SQLite.java#L17`](../src/Controller/SQLite.java#L17):
    `String driverURL = "jdbc:sqlite:" + "database.db";`
  * This is a **relative path**. It resolves relative to the current working directory (`user.dir`), so running the app from `dist/` creates or looks for `dist/database.db` instead of the root database.

* **Schema Created in Code**:
  * Table `history` ([`SQLite.java#L31-L37`](../src/Controller/SQLite.java#L31-L37)):
    * `id INTEGER PRIMARY KEY AUTOINCREMENT`
    * `username TEXT NOT NULL`
    * `name TEXT NOT NULL`
    * `stock INTEGER DEFAULT 0`
    * `timestamp TEXT NOT NULL`
  * Table `logs` ([`SQLite.java#L49-L55`](../src/Controller/SQLite.java#L49-L55)):
    * `id INTEGER PRIMARY KEY AUTOINCREMENT`
    * `event TEXT NOT NULL`
    * `username TEXT NOT NULL`
    * `desc TEXT NOT NULL`
    * `timestamp TEXT NOT NULL`
  * Table `product` ([`SQLite.java#L67-L72`](../src/Controller/SQLite.java#L67-L72)):
    * `id INTEGER PRIMARY KEY AUTOINCREMENT`
    * `name TEXT NOT NULL UNIQUE`
    * `stock INTEGER DEFAULT 0`
    * `price REAL DEFAULT 0.00`
  * Table `users` ([`SQLite.java#L84-L90`](../src/Controller/SQLite.java#L84-L90)):
    * `id INTEGER PRIMARY KEY AUTOINCREMENT`
    * `username TEXT NOT NULL UNIQUE`
    * `password TEXT NOT NULL`
    * `role INTEGER DEFAULT 2`
    * `locked INTEGER DEFAULT 0`

* **First Run Creation & Seeding**:
  * **Only exists as `database.db`**: In [`src/Controller/Main.java#L27-L63`](../src/Controller/Main.java#L27-L63), the code to create tables and seed data is **commented out**.
  * If [`database.db`](../database.db) is missing or deleted, the application connects to SQLite (creating an empty 0-byte file), but fails with runtime SQL errors (`no such table: users`) because table creation is never executed.

* **Seed / Test Accounts**:
  * Found in [`Main.java#L58-L62`](../src/Controller/Main.java#L58-L62) and stored in [`database.db`](../database.db):
    * `admin` (Role: 5 / Admin)
    * `manager` (Role: 4 / Manager)
    * `staff` (Role: 3 / Staff)
    * `client1` (Role: 2 / Client)
    * `client2` (Role: 2 / Client)
  * **Password Format**: Passwords are stored in **100% PLAINTEXT** (no hashing, no salt, no encryption).

* **Current Contents of `database.db`**:
  * `users`: 5 rows (`admin`, `manager`, `staff`, `client1`, `client2`).
  * `product`: 3 rows (`Antivirus` - stock 5, price 500.0; `Firewall` - stock 3, price 1000.0; `Scanner` - stock 10, price 100.0).
  * `history`: 3 rows (purchases for `admin`, `manager`, `staff` dated 2019-04-03).
  * `logs`: 3 rows (`NOTICE` events for `admin` and `manager` user creation dated 2019-03-27).

---

### 4. App Structure

* **Entry Point & Screen Flow**:
  * Entry point: [`Controller.Main.main()`](../src/Controller/Main.java#L19), which creates [`View.Frame`](../src/View/Frame.java#L104).
  * Initial screen displayed: [`Login`](../src/View/Login.java) ([`Frame.java#L236`](../src/View/Frame.java#L236)).
  * Clicking "REGISTER" switches to [`Register`](../src/View/Register.java) ([`Login.java#L90`](../src/View/Login.java#L90)).
  * Clicking "LOGIN" in [`Login.java#L85-L87`](../src/View/Login.java#L85-L87) calls `frame.mainNav()` without inspecting inputs.
  * `frame.mainNav()` displays [`HomePnl`](../src/View/Frame.java#L247-L249), which hosts a left sidebar with four permanent navigation buttons: `Admin Home`, `Manager Home`, `Staff Home`, `Client Home`.

* **Role Permissions & Capabilities**:
  * Four roles exist: Client (2), Staff (3), Manager (4), Admin (5) (with Role 1 defined in UI as Disabled in [`MgmtUser.java#L183`](../src/View/MgmtUser.java#L183)).
  * **What each screen actually lets them do**:
    * [`AdminHome`](../src/View/AdminHome.java), [`ManagerHome`](../src/View/ManagerHome.java), [`StaffHome`](../src/View/StaffHome.java), and [`ClientHome`](../src/View/ClientHome.java) all instantiate and display the same four sub-views: `MgmtUser`, `MgmtProduct`, `MgmtHistory`, `MgmtLogs`.
    * In all four views, the lines intended to disable role buttons are commented out ([`AdminHome.java#L48-L52`](../src/View/AdminHome.java#L48-L52), [`ManagerHome.java#L48-L52`](../src/View/ManagerHome.java#L48-L52), [`StaffHome.java#L48-L52`](../src/View/StaffHome.java#L48-L52), [`ClientHome.java#L48-L52`](../src/View/ClientHome.java#L48-L52)).
    * Consequently, **any user can access all functions of every role**.

* **Role Verification & Enforcement**:
  * Role is **never checked**:
    * In [`Login.java#L85-L87`](../src/View/Login.java#L85-L87), `loginBtnActionPerformed` directly calls `frame.mainNav()` without authentication.
    * No user session or role is stored in memory.
    * In [`Frame.java#L182-L200`](../src/View/Frame.java#L182-L200), any button (`adminBtn`, `managerBtn`, `staffBtn`, `clientBtn`) can be clicked at any time by anyone.

* **History and Logs Recording**:
  * **Where they are written**: In [`src/Controller/SQLite.java#L149-L168`](../src/Controller/SQLite.java#L149-L168), methods `addHistory` and `addLogs` write to SQLite tables `history` and `logs`.
  * **What they record**:
    * `history`: username, product name, quantity/stock, timestamp.
    * `logs`: event type, username, description, timestamp.
  * **Active Execution**: Neither `addHistory` nor `addLogs` is called **anywhere** in active code. Purchases, logins, and administrative changes produce no logs or history entries.

---

### 5. Security Issues

1. **SQL Built by String Concatenation (SQL Injection)**:
   * String concatenation is used in all parameter-driven queries instead of `PreparedStatement`:
     * [`SQLite.java#L150`](../src/Controller/SQLite.java#L150): `INSERT INTO history ... VALUES('" + username + "','" + name + "'...`
     * [`SQLite.java#L161`](../src/Controller/SQLite.java#L161): `INSERT INTO logs ... VALUES('" + event + "','" + username + "'...`
     * [`SQLite.java#L172`](../src/Controller/SQLite.java#L172): `INSERT INTO product ... VALUES('" + name + "'...`
     * [`SQLite.java#L183`](../src/Controller/SQLite.java#L183): `INSERT INTO users ... VALUES('" + username + "','" + password + "')`
     * [`SQLite.java#L283`](../src/Controller/SQLite.java#L283): `INSERT INTO users ... VALUES('" + username + "','" + password + "','" + role + "')`
     * [`SQLite.java#L295`](../src/Controller/SQLite.java#L295): `DELETE FROM users WHERE username='" + username + "';`
     * [`SQLite.java#L307`](../src/Controller/SQLite.java#L307): `SELECT ... FROM product WHERE name='" + name + "';`
   * *Note*: A commented-out `PreparedStatement` example exists in [`SQLite.java#L189-L194`](../src/Controller/SQLite.java#L189-L194), but is unused.

2. **Plaintext Password Storage & Display**:
   * Passwords are stored in plaintext in the database ([`SQLite.java#L87`](../src/Controller/SQLite.java#L87), [`Main.java#L58-L62`](../src/Controller/Main.java#L58-L62)). No hashing (e.g. bcrypt, Argon2, PBKDF2) or salt exists.
   * **Plaintext Password Exposure in UI**: In [`MgmtUser.java#L52`](../src/View/MgmtUser.java#L52) and [`MgmtUser.java#L91`](../src/View/MgmtUser.java#L91), every user's plaintext password is loaded into column 1 of the management table and displayed on screen.
   * **Plaintext Input Fields**: In [`Login.java#L18`](../src/View/Login.java#L18) and [`Register.java#L17, L20`](../src/View/Register.java#L17-L20), passwords are typed into standard `JTextField` instead of `JPasswordField`, displaying cleartext characters on screen.

3. **Missing Input Validation & Ignored Fields**:
   * Login form ([`Login.java#L85-L87`](../src/View/Login.java#L85-L87)): No validation; fields are ignored.
   * Registration form ([`Register.java#L100`](../src/View/Register.java#L100), [`Frame.java#L259-L261`](../src/View/Frame.java#L259-L261)):
     * `confpass` is forwarded to `frame.registerAction` but **never verified** against `password`.
     * No checks for null/empty usernames, password length/complexity, or existing usernames.
   * Product forms ([`MgmtProduct.java#L188, L209-L211, L232-L234`](../src/View/MgmtProduct.java#L188-L234)): No validation for valid integers (stock) or numbers (price).

4. **Hardcoded Credentials & Metadata**:
   * Hardcoded test accounts in source comments: [`Main.java#L58-L62`](../src/Controller/Main.java#L58-L62).
   * Developer machine path exposed in [`nbproject/private/private.properties#L8`](../nbproject/private/private.properties#L8).
   * Developer name recorded in [`nbproject/project.properties#L8`](../nbproject/project.properties#L8).

5. **Error Handling & Information Leakage**:
   * In [`SQLite.java#L238`](../src/Controller/SQLite.java#L238), `getLogs()` executes `ex.printStackTrace()`, leaking full stack traces and internal class details to standard error.
   * Other database methods execute `System.out.print(ex)`, printing raw SQLite exception messages to the console.
   * In [`SQLite.java#L278`](../src/Controller/SQLite.java#L278), `getUsers()` has an empty catch block (`catch (Exception ex) {}`), silently swallowing database failures.

6. **Missing Authorization, Lockout, and Session Management**:
   * **Bypassed Authentication**: Any user can enter the application without credentials.
   * **Bypassed Authorization**: Any user can switch to Admin, Manager, Staff, or Client panels.
   * **No Account Lockout**: The schema has a `locked` column ([`SQLite.java#L89`](../src/Controller/SQLite.java#L89)), but no failed attempt counter or lockout mechanism exists.
   * **No Session Handling**: There is no session token or authenticated user reference. Logout ([`Frame.java#L202-L204`](../src/View/Frame.java#L202-L204)) merely switches panels back to `loginPnl`.

7. **Sensitive Data in Standard Output / Logs**:
   * In [`MgmtUser.java#L237-L238`](../src/View/MgmtUser.java#L237-L238), the "CHANGE PASSWORD" handler prints newly entered passwords directly to standard output:
     ```java
     System.out.println(password.getText());
     System.out.println(confpass.getText());
     ```

8. **Missing Course Project Specifications**:
   * Password hashing with salt (e.g. bcrypt/Argon2/PBKDF2).
   * Parameterized queries (`PreparedStatement`) on all SQL operations.
   * Enforced Role-Based Access Control (RBAC) on both UI and backend.
   * Audit logging for security events (login success/failure, role edits, lock/unlock, user creation).
   * Input validation with regex whitelisting and length limits.
   * Account lockout after $N$ failed attempts.

---

### 6. Code Health

* **Dead or Unused Methods**:
  * In [`SQLite.java`](../src/Controller/SQLite.java):
    * `createNewDatabase()` ([L19-L28](../src/Controller/SQLite.java#L19-L28))
    * `createHistoryTable()`, `createLogsTable()`, `createProductTable()`, `createUserTable()` ([L30-L99](../src/Controller/SQLite.java#L30-L99))
    * `dropHistoryTable()`, `dropLogsTable()`, `dropProductTable()`, `dropUserTable()` ([L101-L147](../src/Controller/SQLite.java#L101-L147))
    * `addHistory()` ([L149-L158](../src/Controller/SQLite.java#L149-L158))
    * `addLogs()` ([L160-L169](../src/Controller/SQLite.java#L160-L169))
    * `addProduct()` ([L171-L180](../src/Controller/SQLite.java#L171-L180))
    * `addUser(String, String, int)` ([L282-L292](../src/Controller/SQLite.java#L282-L292))
    * `removeUser()` ([L294-L304](../src/Controller/SQLite.java#L294-L304))
    * Field `DEBUG_MODE` ([L16](../src/Controller/SQLite.java#L16)) is toggled in [`MgmtLogs.java#L142-L145`](../src/View/MgmtLogs.java#L142-L145) but never used anywhere.
  * In [`Model/History.java#L27-L32`](../src/Model/History.java#L27-L32): 3-argument constructor is never called.
  * In [`Model/Logs.java#L27-L39`](../src/Model/Logs.java#L27-L39): 2-argument and 3-argument constructors are never called.

* **Stubbed Action Handlers (Logic not implemented)**:
  * In [`MgmtUser.java`](../src/View/MgmtUser.java):
    * `editRoleBtnActionPerformed` ([L181-L195](../src/View/MgmtUser.java#L181-L195)) only prints to console; does not update DB.
    * `deleteBtnActionPerformed` ([L198-L205](../src/View/MgmtUser.java#L198-L205)) only prints to console; does not delete user.
    * `lockBtnActionPerformed` ([L208-L220](../src/View/MgmtUser.java#L208-L220)) only prints to console; does not lock/unlock.
    * `chgpassBtnActionPerformed` ([L223-L241](../src/View/MgmtUser.java#L223-L241)) only prints to console; does not change password.
  * In [`MgmtProduct.java`](../src/View/MgmtProduct.java):
    * `purchaseBtnActionPerformed` ([L176-L190](../src/View/MgmtProduct.java#L176-L190)), `addBtnActionPerformed` ([L193-L212](../src/View/MgmtProduct.java#L193-L212)), `editBtnActionPerformed` ([L215-L236](../src/View/MgmtProduct.java#L215-L236)), and `deleteBtnActionPerformed` ([L239-L247](../src/View/MgmtProduct.java#L239-L247)) only print to stdout.
  * In [`MgmtLogs.java#L137-L139`](../src/View/MgmtLogs.java#L137-L139): `clearBtnActionPerformed` is an empty method body.

* **Code Duplication**:
  * **Duplicated Views**: [`AdminHome.java`](../src/View/AdminHome.java), [`ManagerHome.java`](../src/View/ManagerHome.java), [`StaffHome.java`](../src/View/StaffHome.java), and [`ClientHome.java`](../src/View/ClientHome.java) (~203 lines each, ~800 lines total) are 99% identical copies, differing only in class name, welcome string, and background color.
  * **Duplicated Helper Method**: `designer(JTextField component, String text)` is copy-pasted identically across three files:
    * [`MgmtProduct.java#L54-L60`](../src/View/MgmtProduct.java#L54-L60)
    * [`MgmtUser.java#L58-L64`](../src/View/MgmtUser.java#L58-L64)
    * [`MgmtHistory.java#L63-L69`](../src/View/MgmtHistory.java#L63-L69)
  * **Duplicated Table Clearing**: A loop removing rows one-by-one is copy-pasted across all 4 management panels instead of `tableModel.setRowCount(0)`:
    * [`MgmtHistory.java#L44-L46`](../src/View/MgmtHistory.java#L44-L46)
    * [`MgmtLogs.java#L35-L37`](../src/View/MgmtLogs.java#L35-L37)
    * [`MgmtProduct.java#L40-L42`](../src/View/MgmtProduct.java#L40-L42)
    * [`MgmtUser.java#L43-L45`](../src/View/MgmtUser.java#L43-L45)

* **Hand-Edits Inside NetBeans Generated Code**:
  * No manual hand-edits were made inside the generated `// <editor-fold ... desc="Generated Code">` blocks; the code generator boundaries and `.form` XML definitions remain synchronized.
  * Custom modifications were added directly into constructors immediately following `initComponents()` (e.g. [`MgmtUser.java#L31-L39`](../src/View/MgmtUser.java#L31-L39)).

* **Test Suite**:
  * [`test/`](../test) is completely empty (0 files). No unit or integration tests exist.

---

### 7. Documentation Gaps

* **Outdated / Incomplete README**:
  * [`README.md#L1-L14`](../README.md#L1-L14) only contains 14 lines advising installation of SQLite CLI tools via `dnf` or Canvas.
  * It does not explain how to build, run, test, or configure the Java application.
  * Telling users to grab SQLite binaries from Canvas is misleading for the Java application, as the bundled [`sqlite-jdbc-3.23.1.jar`](../sqlite-jdbc-3.23.1.jar) already contains native drivers for Windows and Linux.
* **Missing Contributor Information**:
  1. Target JDK version (Java 8 compatibility).
  2. Build system requirements (Apache Ant 1.9+ or NetBeans IDE 8.x/16.x).
  3. Working directory sensitivity for SQLite database resolution.
  4. Role ID mapping (1: Disabled, 2: Client, 3: Staff, 4: Manager, 5: Admin).
  5. Default test accounts and passwords.
  6. Instructions on how to re-seed or initialize a clean database.

---

### Summary Table

| Finding | Severity | File:Line |
| :--- | :---: | :--- |
| Authentication entirely bypassed on Login screen | **High** | [`src/View/Login.java#L85-L87`](../src/View/Login.java#L85-L87) |
| Missing authorization / RBAC checks on role panels | **High** | [`src/View/Frame.java#L182-L200`](../src/View/Frame.java#L182-L200) |
| Plaintext passwords stored in database (no hash or salt) | **High** | [`src/Controller/SQLite.java#L87`](../src/Controller/SQLite.java#L87), [`src/Controller/SQLite.java#L183`](../src/Controller/SQLite.java#L183) |
| Plaintext passwords displayed on User Management table | **High** | [`src/View/MgmtUser.java#L52`](../src/View/MgmtUser.java#L52), [`src/View/MgmtUser.java#L91`](../src/View/MgmtUser.java#L91) |
| SQL Injection in `addUser(username, password)` | **High** | [`src/Controller/SQLite.java#L183`](../src/Controller/SQLite.java#L183) |
| SQL Injection in `addUser(username, password, role)` | **High** | [`src/Controller/SQLite.java#L283`](../src/Controller/SQLite.java#L283) |
| SQL Injection in `removeUser(username)` | **High** | [`src/Controller/SQLite.java#L295`](../src/Controller/SQLite.java#L295) |
| SQL Injection in `getProduct(name)` | **High** | [`src/Controller/SQLite.java#L307`](../src/Controller/SQLite.java#L307) |
| SQL Injection in `addProduct(name, stock, price)` | **High** | [`src/Controller/SQLite.java#L172`](../src/Controller/SQLite.java#L172) |
| SQL Injection in `addHistory(...)` and `addLogs(...)` | **High** | [`src/Controller/SQLite.java#L150`](../src/Controller/SQLite.java#L150), [`src/Controller/SQLite.java#L161`](../src/Controller/SQLite.java#L161) |
| Registration ignores confirmation password check | **High** | [`src/View/Frame.java#L259-L261`](../src/View/Frame.java#L259-L261), [`src/View/Register.java#L100`](../src/View/Register.java#L100) |
| Plaintext password fields in UI (`JTextField` instead of `JPasswordField`) | **Medium** | [`src/View/Login.java#L18`](../src/View/Login.java#L18), [`src/View/Register.java#L17, L20`](../src/View/Register.java#L17-L20) |
| Passwords printed to console on password change | **Medium** | [`src/View/MgmtUser.java#L237-L238`](../src/View/MgmtUser.java#L237-L238) |
| Hardcoded seed credentials in source | **Medium** | [`src/Controller/Main.java#L58-L62`](../src/Controller/Main.java#L58-L62) |
| Missing account lockout and brute force protection | **Medium** | [`src/Controller/SQLite.java#L89`](../src/Controller/SQLite.java#L89), [`src/View/Login.java#L85`](../src/View/Login.java#L85) |
| Missing session management and token handling | **Medium** | [`src/View/Frame.java#L202-L204`](../src/View/Frame.java#L202-L204), [`src/View/Login.java#L85`](../src/View/Login.java#L85) |
| Audit logging not invoked during application runtime | **Medium** | [`src/Controller/SQLite.java#L160`](../src/Controller/SQLite.java#L160) |
| Potential `NullPointerException` during history search | **Medium** | [`src/View/MgmtHistory.java#L185-L190`](../src/View/MgmtHistory.java#L185-L190) |
| Machine-specific absolute path in NetBeans private properties | **Low** | [`nbproject/private/private.properties#L8`](../nbproject/private/private.properties#L8) |
| Binary database file unignored in `.gitignore` | **Low** | [`.gitignore`](../.gitignore) |
| Mixed line endings across `.java` files (CRLF vs LF) | **Low** | [`src/Controller/SQLite.java`](../src/Controller/SQLite.java), [`src/Controller/Main.java`](../src/Controller/Main.java), [`src/Model/User.java`](../src/Model/User.java) |
| Stack trace printed on error in `getLogs()` | **Low** | [`src/Controller/SQLite.java#L238`](../src/Controller/SQLite.java#L238) |
| Empty catch block silently swallowing exceptions in `getUsers()` | **Low** | [`src/Controller/SQLite.java#L278`](../src/Controller/SQLite.java#L278) |
| Extensive code duplication across *Home views (~800 duplicate lines) | **Low** | [`src/View/AdminHome.java`](../src/View/AdminHome.java), [`ManagerHome.java`](../src/View/ManagerHome.java), [`StaffHome.java`](../src/View/StaffHome.java), [`ClientHome.java`](../src/View/ClientHome.java) |
| Duplicate `designer()` UI method in management classes | **Low** | [`src/View/MgmtProduct.java#L54-L60`](../src/View/MgmtProduct.java#L54-L60), [`MgmtUser.java#L58-L64`](../src/View/MgmtUser.java#L58-L64), [`MgmtHistory.java#L63-L69`](../src/View/MgmtHistory.java#L63-L69) |
| Stubbed management buttons that only print to stdout | **Low** | [`src/View/MgmtUser.java#L181-L241`](../src/View/MgmtUser.java#L181-L241), [`src/View/MgmtProduct.java#L176-L247`](../src/View/MgmtProduct.java#L176-L247) |
| Outdated and incomplete documentation | **Low** | [`README.md#L1-L14`](../README.md#L1-L14) |
| Empty test directory (`test/`) with zero automated tests | **Low** | [`test/`](../test) |
