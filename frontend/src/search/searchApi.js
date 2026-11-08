import { request } from '../api/httpClient'
export const searchMessages = (token, query, size = 50) =>
  request(`/api/messages/search?q=${encodeURIComponent(query)}&size=${size}`, { token })
