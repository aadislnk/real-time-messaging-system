import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import ProtectedRoute from './routing/ProtectedRoute'
import PublicOnlyRoute from './routing/PublicOnlyRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ChatPage from './pages/ChatPage'

export default function App() {
  return <AuthProvider><Routes>
    <Route element={<PublicOnlyRoute />}><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /></Route>
    <Route element={<ProtectedRoute />}><Route path="/chat" element={<ChatPage />} /></Route>
    <Route path="*" element={<Navigate to="/chat" replace />} />
  </Routes></AuthProvider>
}
