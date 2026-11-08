import { request } from '../api/httpClient'
export const syncMessages = (token, id, afterSequence = 0, size = 100, signal) =>
  request(`/api/conversations/${id}/messages?afterSequence=${afterSequence}&size=${size}`, { token, signal })
export const sendMessage = (token, id, body) =>
  request(`/api/conversations/${id}/messages`, { token, method: 'POST', body })
export const acknowledge = (token, conversationId, messageId, type) =>
  request(`/api/conversations/${conversationId}/messages/${messageId}/receipts`, {
    token, method: 'POST', body: { type }
  })
