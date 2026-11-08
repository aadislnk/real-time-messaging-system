const KEY = 'sandesh.auth'

export function readSession() {
  try {
    const session = JSON.parse(localStorage.getItem(KEY) || 'null')
    if (session?.expiresAt && new Date(session.expiresAt).getTime() <= Date.now()) {
      clearSession()
      return null
    }
    return session
  } catch {
    clearSession()
    return null
  }
}
export function saveSession(session) { localStorage.setItem(KEY, JSON.stringify(session)) }
export function clearSession() { localStorage.removeItem(KEY) }
