# HikariCP Database Configuration

## 📁 Files Created

```
core/
├── pom.xml (updated - added HikariCP dependency)
├── src/main/java/
│   ├── com.n9.core.CoreServer.java (updated - added DB initialization)
│   └── com/n9/core/database/
│       └── DatabaseManager.java (NEW - HikariCP pool manager)
├── src/main/resources/
│   └── database.properties (NEW - DB config)
└── src/test/java/com/n9/core/database/
    └── TestDatabaseConnection.java (NEW - test DB connection)
```

---

## 🚀 SETUP INSTRUCTIONS

### Step 1: Cài đặt MySQL

```bash
# Kiểm tra MySQL đang chạy
mysql --version

# Login vào MySQL
mysql -u root -p
```

### Step 2: Tạo Database

```sql
-- Trong MySQL shell
CREATE DATABASE cardgame_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verify
SHOW DATABASES;

-- Run script tạo tables
SOURCE d:/Project/Network-Programming/core/db/DB_SCRIPT.sql;

-- Verify tables
USE cardgame_db;
SHOW TABLES;
-- Expected: users, user_profiles, cards, games, game_rounds, active_sessions
```

### Step 3: Cấu hình Password

**File: `DatabaseManager.java` (line 52)**

```java
config.setPassword(""); // ← THAY ĐỔI NẾU BẠN CÓ PASSWORD
```

Hoặc dùng environment variable:

```java
config.setPassword(System.getenv("DB_PASSWORD"));
```

### Step 4: Build Project

```bash
cd d:\Project\Network-Programming

# Compile
mvn clean compile

# Hoặc full build
mvn clean install
```

### Step 5: Test Database Connection

```bash
# Run test class
cd core
mvn test -Dtest=TestDatabaseConnection

# Hoặc run trực tiếp
mvn exec:java -Dexec.mainClass="com.n9.core.database.TestDatabaseConnection"
```

**Expected Output:**

```
=== Testing Database Connection ===

1. Initializing DatabaseManager...
✅ HikariCP Connection Pool initialized successfully!
   Pool Name: CardGame-MySQL-Pool
   JDBC URL: jdbc:mysql://localhost:3306/cardgame_db...
   Max Pool Size: 10
   ✅ DatabaseManager initialized

2. Running health check...
   ✅ Database is healthy

3. Testing query: SELECT COUNT(*) FROM users
   ✅ Query successful!
   Total users in database: 0

4. Testing query: SELECT COUNT(*) FROM cards
   ✅ Query successful!
   Total cards in database: 36

5. Pool Statistics:
=== HikariCP Pool Statistics ===
Active Connections: 0
Idle Connections: 2
Total Connections: 2
Threads Awaiting Connection: 0

=== ALL TESTS PASSED ✅ ===
```

### Step 6: Start com.n9.core.CoreServer

```bash
# Run com.n9.core.CoreServer
mvn exec:java -Dexec.mainClass="com.n9.core.CoreServer"
```

**Expected Output:**

```
Starting Core Server...
Initializing database connection pool...
✅ HikariCP Connection Pool initialized successfully!
   Pool Name: CardGame-MySQL-Pool
   JDBC URL: jdbc:mysql://localhost:3306/cardgame_db...
   Max Pool Size: 10
✅ Database connected successfully
=== HikariCP Pool Statistics ===
Active Connections: 0
Idle Connections: 2
Total Connections: 2
Threads Awaiting Connection: 0
✅ Core server started on port: 9090
   Server is ready to accept connections!
```

---

## ⚙️ CONFIGURATION

### Database Properties

**File: `src/main/resources/database.properties`**

```properties
# Database URL
db.url=jdbc:mysql://localhost:3306/cardgame_db

# Credentials
db.username=root
db.password=

# Pool Size (MVP: 10 connections)
hikari.maxPoolSize=10
hikari.minIdle=2

# Timeouts
hikari.connectionTimeout=30000    # 30 seconds
hikari.idleTimeout=600000         # 10 minutes
hikari.maxLifetime=1800000        # 30 minutes
```

### Adjust Pool Size

**Công thức:** `maxPoolSize = cores * 2 + diskSpindles`

Example:
- 4 CPU cores → 4 * 2 + 1 = 9 ≈ **10 connections**
- 8 CPU cores → 8 * 2 + 1 = 17 ≈ **17 connections**

---

## 🐛 TROUBLESHOOTING

### Error: "Communications link failure"

```
Caused by: com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**Solutions:**
1. Check MySQL is running: `mysql -u root -p`
2. Check port 3306: `netstat -an | findstr 3306`
3. Verify database exists: `SHOW DATABASES;`

### Error: "Access denied for user 'root'@'localhost'"

```
java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: YES)
```

**Solutions:**
1. Đổi password trong `DatabaseManager.java`
2. Reset MySQL password:
   ```bash
   ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
   FLUSH PRIVILEGES;
   ```

### Error: "Unknown database 'cardgame_db'"

```
java.sql.SQLSyntaxErrorException: Unknown database 'cardgame_db'
```

**Solutions:**
```sql
CREATE DATABASE cardgame_db;
SOURCE d:/Project/Network-Programming/core/db/DB_SCRIPT.sql;
```

### Error: Connection leak detected

```
WARN HikariPool - Connection leak detection triggered
```

**Solutions:**
- Bạn quên `close()` connection!
- Dùng try-with-resources:
  ```java
  try (Connection conn = dbManager.getConnection()) {
      // Your code
  } // Auto close
  ```

---

## 📊 MONITORING

### Check Pool Statistics

```java
DatabaseManager.getInstance().printPoolStats();
```

Output:
```
=== HikariCP Pool Statistics ===
Active Connections: 3      ← Đang được dùng
Idle Connections: 7        ← Rảnh rỗi trong pool
Total Connections: 10      ← Tổng
Threads Awaiting Connection: 0  ← Đang chờ (nếu > 0 = cần tăng pool size)
```

---

## ✅ NEXT STEPS

1. ✅ HikariCP setup DONE
2. ⏳ Tạo AuthService (sử dụng DatabaseManager)
3. ⏳ Tạo SessionManager
4. ⏳ Update ClientConnectionHandler để handle USER.REGISTER/LOGIN

---

## 📚 REFERENCES

- HikariCP Docs: https://github.com/brettwooldridge/HikariCP
- MySQL Connector/J: https://dev.mysql.com/doc/connector-j/en/
- JDBC Tutorial: https://docs.oracle.com/javase/tutorial/jdbc/
