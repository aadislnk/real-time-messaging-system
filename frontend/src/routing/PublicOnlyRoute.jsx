import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthProvider'
export default function PublicOnlyRoute() {
  return useAuth().session ? <Navigate to="/chat" replace /> : <Outlet />
}
