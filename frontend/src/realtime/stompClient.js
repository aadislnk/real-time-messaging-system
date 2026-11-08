import { Client } from '@stomp/stompjs'
import { WS_URL } from '../app/config'

export function createRealtimeClient({ token, onEvent, onStatus, onReconnect }) {
  let connectedOnce = false
  const client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 4000,
    maxReconnectDelay: 20000,
    connectHeaders: { Authorization: `Bearer ${token}` },
    beforeConnect: async () => { client.connectHeaders = { Authorization: `Bearer ${token}` } },
    onConnect: () => {
      const reconnect = connectedOnce
      connectedOnce = true
      onStatus?.('connected')
      client.subscribe('/user/queue/events', (frame) => {
        try { onEvent?.(JSON.parse(frame.body)) } catch { /* malformed events are ignored */ }
      })
      if (reconnect) onReconnect?.()
    },
    onDisconnect: () => onStatus?.('offline'),
    onStompError: () => onStatus?.('error'),
    onWebSocketClose: () => onStatus?.('reconnecting'),
    onWebSocketError: () => onStatus?.('error')
  })
  return {
    activate: () => { onStatus?.('connecting'); client.activate() },
    disconnect: () => client.deactivate(),
    sendTyping: (conversationId, typing) => {
      if (client.connected) client.publish({
        destination: `/app/conversations/${conversationId}/typing`,
        body: JSON.stringify({ typing })
      })
    }
  }
}
