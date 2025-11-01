import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
// THAY ĐỔI: Sử dụng AppSingleFile - File React duy nhất với đầy đủ chức năng
import App from './AppSingleFile.jsx'

// Nếu muốn quay lại routing version cũ, uncomment dòng dưới và comment dòng trên:
// import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
