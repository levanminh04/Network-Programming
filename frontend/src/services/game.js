// Fake API cho game lobby và matchmaking

const ONLINE_PLAYERS_KEY = 'online_players';
const MATCHES_KEY = 'active_matches';

// Khởi tạo dữ liệu mẫu
const initFakeData = () => {
  const existingPlayers = localStorage.getItem(ONLINE_PLAYERS_KEY);
  if (!existingPlayers) {
    const samplePlayers = [
      { id: 1, username: 'admin', status: 'online', rating: 1200, gamesPlayed: 45 },
      { id: 2, username: 'player1', status: 'online', rating: 1100, gamesPlayed: 32 },
      { id: 3, username: 'player2', status: 'online', rating: 1350, gamesPlayed: 67 },
      { id: 4, username: 'pro_gamer', status: 'online', rating: 1500, gamesPlayed: 120 },
      { id: 5, username: 'newbie', status: 'online', rating: 800, gamesPlayed: 5 },
      { id: 6, username: 'card_master', status: 'online', rating: 1400, gamesPlayed: 89 },
      { id: 7, username: 'lucky_player', status: 'online', rating: 950, gamesPlayed: 23 },
      { id: 8, username: 'veteran', status: 'online', rating: 1600, gamesPlayed: 200 }
    ];
    localStorage.setItem(ONLINE_PLAYERS_KEY, JSON.stringify(samplePlayers));
  }
};

// Lấy danh sách người chơi online
const getOnlinePlayers = () => {
  initFakeData();
  return JSON.parse(localStorage.getItem(ONLINE_PLAYERS_KEY) || '[]');
};

// Lưu danh sách người chơi
const saveOnlinePlayers = (players) => {
  localStorage.setItem(ONLINE_PLAYERS_KEY, JSON.stringify(players));
};

// Lấy matches đang hoạt động
const getActiveMatches = () => {
  return JSON.parse(localStorage.getItem(MATCHES_KEY) || '[]');
};

// Lưu matches
const saveActiveMatches = (matches) => {
  localStorage.setItem(MATCHES_KEY, JSON.stringify(matches));
};

export const gameAPI = {
  // Lấy danh sách người chơi online
  getOnlinePlayers: async () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const players = getOnlinePlayers();
        resolve({ success: true, players });
      }, 500);
    });
  },

  // Tìm trận ngẫu nhiên
  findRandomMatch: async (currentUserId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const players = getOnlinePlayers();
        const availablePlayers = players.filter(p => p.id !== currentUserId);
        
        if (availablePlayers.length === 0) {
          resolve({ success: false, message: 'Không có người chơi nào online' });
          return;
        }

        // Chọn ngẫu nhiên
        const randomPlayer = availablePlayers[Math.floor(Math.random() * availablePlayers.length)];
        
        // Tạo match
        const matchId = Date.now();
        const match = {
          id: matchId,
          players: [currentUserId, randomPlayer.id],
          status: 'waiting',
          createdAt: new Date().toISOString()
        };

        const matches = getActiveMatches();
        matches.push(match);
        saveActiveMatches(matches);

        resolve({ 
          success: true, 
          match, 
          opponent: randomPlayer,
          message: `Đã tìm thấy đối thủ: ${randomPlayer.username}` 
        });
      }, 2000 + Math.random() * 3000); // 2-5 giây
    });
  },

  // Tạo match trực tiếp với người chơi
  createDirectMatch: async (currentUserId, opponentId) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const players = getOnlinePlayers();
        const opponent = players.find(p => p.id === opponentId);
        
        if (!opponent) {
          reject({ success: false, message: 'Người chơi không tồn tại' });
          return;
        }

        const matchId = Date.now();
        const match = {
          id: matchId,
          players: [currentUserId, opponentId],
          status: 'waiting',
          createdAt: new Date().toISOString()
        };

        const matches = getActiveMatches();
        matches.push(match);
        saveActiveMatches(matches);

        resolve({ 
          success: true, 
          match, 
          opponent,
          message: `Đã tạo trận với ${opponent.username}` 
        });
      }, 1000);
    });
  },

  // Lấy thông tin match
  getMatch: async (matchId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const matches = getActiveMatches();
        const match = matches.find(m => m.id === matchId);
        resolve({ success: true, match });
      }, 500);
    });
  }
};
