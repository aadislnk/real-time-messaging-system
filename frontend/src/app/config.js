const configuredApiUrl = import.meta.env.VITE_API_BASE_URL
const configuredWsUrl = import.meta.env.VITE_WS_URL
const browserOrigin = typeof window !== 'undefined' ? window.location.origin : ''

export const API_BASE_URL = (configuredApiUrl || browserOrigin).replace(/\/$/, '')
const defaultWsUrl = typeof window !== 'undefined'
  ? `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`
  : 'ws://localhost:8080/ws'
const wsUrl = configuredWsUrl || defaultWsUrl

export const WS_URL = typeof window !== 'undefined' && window.location.protocol === 'https:'
  ? wsUrl.replace(/^ws:/, 'wss:')
  : wsUrl
