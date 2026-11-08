import { request } from '../api/httpClient'
export const searchUsers = (token, query, signal) =>
  request(`/api/users?q=${encodeURIComponent(query)}`, { token, signal })
