import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { clearSession, readSession, saveSession } from './authStorage'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => readSession())
  const logout = useCallback(() => { clearSession(); setSession(null) }, [])
  const signIn = useCallback((next) => { saveSession(next); setSession(next); return next }, [])

  useEffect(() => {
    const handleUnauthorized = () => logout()
    window.addEventListener('sandesh:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('sandesh:unauthorized', handleUnauthorized)
  }, [logout])

  useEffect(() => {
    if (!session?.expiresAt) return undefined
    const remaining = new Date(session.expiresAt).getTime() - Date.now()
    if (remaining <= 0) { logout(); return undefined }
    const timer = window.setTimeout(logout, remaining)
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(() => ({ session, token: session?.accessToken, user: session ? {
    id: session.userId, displayName: session.displayName, email: session.email
  } : null, signIn, logout }), [session, signIn, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
