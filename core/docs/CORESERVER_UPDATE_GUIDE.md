# Hướng Dẫn Cập Nhật CoreServer.java
## Tích Hợp AuthService & SessionManager

> **Mục tiêu:** Khởi tạo đầy đủ dependencies cho ClientConnectionHandler  
> **Thời gian:** 10 phút

---

## 1. CURRENT STATE (Hiện Tại)

**File:** `d:\Project\Network-Programming\core\src\main\java\com\n9\core\CoreServer.java`

Hiện tại CoreServer chỉ khởi tạo:
- ✅ DatabaseManager
- ✅ GameService
- ❌ **THIẾU:** AuthService
- ❌ **THIẾU:** SessionManager

---

## 2. CẦN THÊM

### 2.1. Import Statements

```java
import com.n9.core.service.AuthService;
import com.n9.core.service.SessionManager;
```

### 2.2. Khởi Tạo Services (Trong main method)

**Tìm đoạn code:**

```java
// Initialize DatabaseManager
DatabaseManager dbManager = DatabaseManager.getInstance();
if (!dbManager.isHealthy()) {
    System.err.println("❌ Database connection failed!");
    System.exit(1);
}
System.out.println("✅ Database connected successfully");

// Initialize GameService
GameService gameService = new GameService();
```

**Thay bằng:**

```java
// ========== INITIALIZE DATABASE ==========
DatabaseManager dbManager = DatabaseManager.getInstance();
if (!dbManager.isHealthy()) {
    System.err.println("❌ Database connection failed!");
    System.exit(1);
}
System.out.println("✅ Database connected successfully");

// ========== INITIALIZE SERVICES ==========
// 1. AuthService (B1)
AuthService authService = new AuthService(dbManager);
System.out.println("✅ AuthService initialized");

// 2. SessionManager (B2)
SessionManager sessionManager = new SessionManager(dbManager);
System.out.println("✅ SessionManager initialized");

// 3. GameService (existing)
GameService gameService = new GameService();
System.out.println("✅ GameService initialized");
```

### 2.3. Truyền Dependencies vào CoreServerListener

**Tìm đoạn code:**

```java
CoreServerListener listener = new CoreServerListener(
    serverSocket,
    executorService,
    gameService
);
```

**Thay bằng:**

```java
CoreServerListener listener = new CoreServerListener(
    serverSocket,
    executorService,
    gameService,
    authService,        // THÊM
    sessionManager      // THÊM
);
```

### 2.4. Shutdown Hook (Cleanup Sessions)

**Tìm đoạn code trong shutdown hook:**

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("\n🛑 Shutting down Core Server...");
    try {
        serverSocket.close();
        executorService.shutdown();
        dbManager.shutdown();
        System.out.println("✅ Server shutdown complete");
    } catch (Exception e) {
        System.err.println("❌ Error during shutdown: " + e.getMessage());
    }
}));
```

**Không cần thay đổi** (SessionManager cleanup tự động khi app tắt)

---

## 3. CODE HOÀN CHỈNH

**File:** `CoreServer.java` (Updated)

```java
package com.n9.core;

import com.n9.core.database.DatabaseManager;
import com.n9.core.network.CoreServerListener;
import com.n9.core.service.AuthService;
import com.n9.core.service.GameService;
import com.n9.core.service.SessionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CoreServer - Main entry point for Core Backend
 * 
 * Responsibilities:
 * - Initialize ServerSocket (TCP port 8080)
 * - Setup ExecutorService (CachedThreadPool)
 * - Initialize services (Auth, Session, Game)
 * - Start CoreServerListener
 * 
 * @version 1.1.0 (với AuthService & SessionManager)
 */
public class CoreServer {
    
    private static final int PORT = 8080;
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Core Server...");
        
        try {
            // ========== INITIALIZE DATABASE ==========
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isHealthy()) {
                System.err.println("❌ Database connection failed!");
                System.exit(1);
            }
            System.out.println("✅ Database connected successfully");
            
            // ========== INITIALIZE SERVICES ==========
            // 1. AuthService (B1)
            AuthService authService = new AuthService(dbManager);
            System.out.println("✅ AuthService initialized");
            
            // 2. SessionManager (B2)
            SessionManager sessionManager = new SessionManager(dbManager);
            System.out.println("✅ SessionManager initialized");
            
            // 3. GameService (existing)
            GameService gameService = new GameService();
            System.out.println("✅ GameService initialized");
            
            // ========== INITIALIZE SERVER SOCKET ==========
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("✅ Server listening on port " + PORT);
            
            // ========== INITIALIZE THREAD POOL ==========
            ExecutorService executorService = Executors.newCachedThreadPool();
            System.out.println("✅ Thread pool created");
            
            // ========== REGISTER SHUTDOWN HOOK ==========
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down Core Server...");
                try {
                    serverSocket.close();
                    executorService.shutdown();
                    dbManager.shutdown();
                    System.out.println("✅ Server shutdown complete");
                } catch (Exception e) {
                    System.err.println("❌ Error during shutdown: " + e.getMessage());
                }
            }));
            
            // ========== START SERVER LISTENER ==========
            CoreServerListener listener = new CoreServerListener(
                serverSocket,
                executorService,
                gameService,
                authService,        // THÊM
                sessionManager      // THÊM
            );
            
            Thread listenerThread = new Thread(listener, "CoreServerListener");
            listenerThread.start();
            
            System.out.println("✅ Core Server started successfully");
            System.out.println("📡 Waiting for connections...\n");
            
            // Keep main thread alive
            listenerThread.join();
            
        } catch (IOException e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("⚠️ Server interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## 4. VERIFY

### 4.1. Compile

```bash
cd d:\Project\Network-Programming
mvn clean compile -pl core
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Compiling 8 source files with javac [debug release 17] to target\classes
```

### 4.2. Run

```bash
mvn exec:java -Dexec.mainClass="com.n9.core.CoreServer" -pl core
```

**Expected output:**
```
🚀 Starting Core Server...
✅ Database connected successfully
✅ AuthService initialized
✅ SessionManager initialized
✅ GameService initialized
✅ Server listening on port 8080
✅ Thread pool created
✅ Core Server started successfully
📡 Waiting for connections...
```

### 4.3. Troubleshooting

| Error | Cause | Solution |
|-------|-------|----------|
| `ClassNotFoundException: AuthService` | Chưa compile | `mvn clean compile` |
| `NullPointerException in AuthService` | DatabaseManager chưa init | Check database connection |
| `Port 8080 already in use` | Server đang chạy | Kill process hoặc đổi port |

---

## 5. NEXT STEPS

✅ **ĐÃ XONG:**
- CoreServer khởi tạo đầy đủ services
- AuthService & SessionManager ready
- ClientConnectionHandler có đủ dependencies

⏭️ **TIẾP THEO:**
1. Implement AuthService.java (theo A3_B1_IMPLEMENTATION_GUIDE.md)
2. Implement SessionManager.java
3. Update ClientConnectionHandler routing
4. Test với TestCoreClient

---

**Chúc may mắn! 🚀**
