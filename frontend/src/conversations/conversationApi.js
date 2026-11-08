import { request } from '../api/httpClient'
export const listConversations = (token) => request('/api/conversations', { token })
export const getConversation = (token, id) => request(`/api/conversations/${id}`, { token })
export const createConversation = (token, body) => request('/api/conversations', { token, method: 'POST', body })
export const addMember = (token, id, body) => request(`/api/conversations/${id}/members`, { token, method: 'POST', body })
export const removeMember = (token, id, userId) => request(`/api/conversations/${id}/members/${userId}`, { token, method: 'DELETE' })
export const renameConversation = (token, id, title) => request(`/api/conversations/${id}`, { token, method: 'PATCH', body: { title } })
export const deleteConversation = (token, id, permanent = false) =>
  request(`/api/conversations/${id}?permanent=${permanent}`, { token, method: 'DELETE' })
