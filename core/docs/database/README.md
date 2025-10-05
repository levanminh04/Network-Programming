# Database Documentation

## 📚 **Overview**

Comprehensive database architecture documentation for **Game Rút Bài May Mắn** (Card Game Lucky Draw). MVP-focused design với khả năng mở rộng trong tương lai.

---

## 📁 **Documentation Structure**

```
database/
├── README.md                      # This file - Quick reference guide
├── database-design.md             # Complete database design documentation
└── V1__cardgame_mvp.sql          # MVP schema + seed data (ready to execute)
```

---

## 🎯 **Quick Start**

### **1. Prerequisites**
- MySQL 8.0+ installed
- Database user with CREATE/INSERT/SELECT permissions
- UTF8MB4 character set support

### **2. Initialize Database**

```bash
# Execute MVP schema script
mysql -u root -p < V1__cardgame_mvp.sql

# Expected output:
# ✅ Database 'cardgame_db' created
# ✅ 6 tables created (users, user_profiles, cards, games, game_rounds, active_sessions)
# ✅ 36 cards seeded
# ✅ Verification query executed (4 suits × 9 cards)
```

### **3. Verify Installation**

```sql
USE cardgame_db;

-- Check tables
SHOW TABLES;
-- Expected: 6 tables

-- Verify card deck
SELECT suit, COUNT(*) as count FROM cards GROUP BY suit;
-- Expected: HEARTS(9), DIAMONDS(9), CLUBS(9), SPADES(9)

-- Check schema version
SELECT 'V1 - MVP' as version;
```

---

## 📊 **MVP Database Schema**

### **Active Tables (Used in MVP)**

| Table | Purpose | Key Fields | Status |
|-------|---------|------------|--------|
| **users** | Authentication & accounts | username, password, last_login | ✅ ACTIVE |
| **user_profiles** | Game statistics | games_won, games_played | ✅ ACTIVE |
| **cards** | 36-card deck reference | suit, rank, card_value | ✅ ACTIVE |
| **games** | Game sessions & results | status, winner_id, scores | ✅ ACTIVE |
| **game_rounds** | Round-by-round tracking | card_ids, is_auto_picked | ✅ ACTIVE |
| **active_sessions** | Online status tracking | last_heartbeat, status | ✅ ACTIVE |

### **DEFERRED Features (Preserved for Future)**
- 🔶 Password hashing (BCrypt)
- 🔶 ELO rating system
- 🔶 Advanced leaderboards (daily/weekly/monthly)
- 🔶 Audit logging
- 🔶 Detailed analytics

---

## 🔑 **Key Features**

### **1. 36-Card Deck**
- **4 Suits**: ♥ HEARTS, ♦ DIAMONDS, ♣ CLUBS, ♠ SPADES
- **9 Ranks**: A (1), 2, 3, 4, 5, 6, 7, 8, 9
- **NO 10/J/Q/K** in MVP

### **2. Game Flow Support**
- ✅ User Registration & Login (plain text password for MVP)
- ✅ Quick Match Matchmaking
- ✅ 3-Round Gameplay (10-second timeout per round)
- ✅ Timeout Auto-Pick (random card if player doesn't choose)
- ✅ Game Completion (winner determination, statistics update)
- ✅ Player Quit Handling (remaining player wins)

### **3. Leaderboard**
- **Simple MVP**: Sort by `user_profiles.games_won` (descending)
- **No separate leaderboards table** in MVP
- **Win Rate**: `games_won / games_played`

### **4. Online Status**
- **Detection**: `last_heartbeat` within 30 seconds = online
- **Heartbeat**: Client sends every 15 seconds
- **Cleanup**: Sessions with `last_heartbeat > 60s ago` are stale

---

## 📖 **Common Queries**

### **User Authentication**
```sql
-- Login validation (MVP - plain text)
SELECT id, username, email, status
FROM users
WHERE username = ? AND password = ? AND status = 'ACTIVE';
```

### **Leaderboard (Top 100)**
```sql
SELECT 
    u.username,
    up.games_won,
    up.games_played,
    ROUND(up.games_won / NULLIF(up.games_played, 0) * 100, 2) as win_rate
FROM user_profiles up
JOIN users u ON up.user_id = u.id
ORDER BY up.games_won DESC
LIMIT 100;
```

### **Online Users**
```sql
SELECT u.username, s.status, s.last_heartbeat
FROM users u
JOIN active_sessions s ON u.id = s.user_id
WHERE s.last_heartbeat > DATE_SUB(NOW(), INTERVAL 30 SECOND)
  AND s.status != 'DISCONNECTED';
```

### **Quick Match Candidates**
```sql
SELECT u.id, u.username, s.session_id
FROM users u
JOIN active_sessions s ON u.id = s.user_id
WHERE s.status = 'IN_LOBBY'
  AND s.last_heartbeat > DATE_SUB(NOW(), INTERVAL 30 SECOND)
  AND u.id != ?  -- Exclude current user
LIMIT 1;
```

---

## ⚠️ **Important Notes**

### **Security (MVP Limitations)**
- ⚠️ **Plain Text Passwords**: For MVP/academic purposes only
- ⚠️ **No Email Verification**: DEFERRED to production
- ⚠️ **No Account Lockout**: DEFERRED to production
- 🔒 **Production Migration**: Enable BCrypt hashing before deployment

### **Business Rules**
- **3 Rounds per Game**: Fixed (not configurable in MVP)
- **10-Second Timeout**: Per round (enforced in application layer)
- **Auto-Pick on Timeout**: Random remaining card selected
- **Player Quit = Loss**: Remaining player gets automatic win
- **Tie Rounds**: Both players score 0 for that round

### **Performance Considerations**
- **Connection Pool**: Recommended 10-20 connections for MVP
- **Query Cache**: Enable for read-heavy operations
- **Index Usage**: All critical queries use indexed columns
- **Transaction Isolation**: READ_COMMITTED for game operations

---

## 🚀 **Next Steps**

### **For Developers**
1. Review **database-design.md** for complete schema details
2. Implement JPA entities matching table structure
3. Create repositories with transaction management
4. Add validation for business rules
5. Test with 20+ concurrent game sessions

### **For Future Expansion**
1. **Phase 2**: Enable password hashing (V2 migration)
2. **Phase 3**: Add ELO rating system (V3 migration)
3. **Phase 4**: Enable leaderboards table (V4 migration)
4. **Phase 5**: Add audit logging (V5 migration)

See **database-design.md** section "Future Expansion Strategy" for detailed migration plan.

---

## 📚 **Related Documentation**

- **database-design.md** - Complete schema design, ERD, data dictionary
- **V1__cardgame_mvp.sql** - Executable SQL script (schema + seed data)
- **../architecture/threading-model.md** - Concurrency handling
- **../testing/testing-strategy.md** - Database testing approach

---

## 🆘 **Troubleshooting**

### **Issue: Card count mismatch**
```sql
-- Expected: 36 cards total
SELECT COUNT(*) FROM cards;  -- Should return 36

-- If incorrect, re-seed:
TRUNCATE TABLE cards;
-- Then re-run seed section from V1__cardgame_mvp.sql
```

### **Issue: Foreign key constraint errors**
```sql
-- Check relationships
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'cardgame_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL;
```

### **Issue: Slow queries**
```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;  -- 100ms threshold

-- Check missing indexes
SHOW INDEX FROM games;
SHOW INDEX FROM game_rounds;
```

---

## ✅ **Validation Checklist**

- [ ] Database `cardgame_db` created successfully
- [ ] All 6 tables present (SHOW TABLES)
- [ ] 36 cards seeded (4 suits × 9 ranks)
- [ ] All foreign keys configured correctly
- [ ] All indexes created (PRIMARY, UNIQUE, INDEX)
- [ ] Character set UTF8MB4 verified
- [ ] Collation utf8mb4_unicode_ci verified
- [ ] Connection pool configured (application side)
- [ ] Transaction isolation level set (READ_COMMITTED)

---

**Database MVP setup complete! Ready for application integration. 🎯**

For detailed schema documentation, see **database-design.md**.  
For SQL script, execute **V1__cardgame_mvp.sql**.
