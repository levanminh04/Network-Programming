// Fake API cho authentication
const STORAGE_KEY = 'card_game_users';
const CURRENT_USER_KEY = 'current_user';

// Khởi tạo dữ liệu mẫu nếu chưa có
const initFakeData = () => {
  const existingUsers = localStorage.getItem(STORAGE_KEY);
  if (!existingUsers) {
    const sampleUsers = [
      { id: 1, username: 'admin', password: '123', email: 'admin@test.com' },
      { id: 2, username: 'player1', password: '123', email: 'player1@test.com' },
      { id: 3, username: 'player2', password: '123', email: 'player2@test.com' }
    ];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sampleUsers));
  }
};

// Lấy danh sách users
const getUsers = () => {
  initFakeData();
  return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
};

// Lưu users
const saveUsers = (users) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(users));
};

// Fake API functions
export const authAPI = {
  // Đăng nhập
  login: async (username, password) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const users = getUsers();
        const user = users.find(u => u.username === username && u.password === password);
        
        if (user) {
          // Lưu user hiện tại
          localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
          resolve({ success: true, user });
        } else {
          reject({ success: false, message: 'Tên đăng nhập hoặc mật khẩu không đúng' });
        }
      }, 1000); // Giả lập delay API
    });
  },

  // Đăng ký
  register: async (username, password, email) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const users = getUsers();
        
        // Kiểm tra username đã tồn tại
        if (users.find(u => u.username === username)) {
          reject({ success: false, message: 'Tên đăng nhập đã tồn tại' });
          return;
        }

        // Tạo user mới
        const newUser = {
          id: Date.now(),
          username,
          password,
          email
        };

        users.push(newUser);
        saveUsers(users);
        
        resolve({ success: true, user: newUser });
      }, 1000);
    });
  },

  // Lấy user hiện tại
  getCurrentUser: () => {
    const user = localStorage.getItem(CURRENT_USER_KEY);
    return user ? JSON.parse(user) : null;
  },

  // Đăng xuất
  logout: () => {
    localStorage.removeItem(CURRENT_USER_KEY);
  }
};
