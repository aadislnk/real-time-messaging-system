import { request } from '../api/httpClient'
export const login = (values) => request('/api/auth/login', { method: 'POST', body: values })
export const register = (values) => request('/api/auth/register', { method: 'POST', body: values })
