import { API_BASE_URL } from '../app/config'
import { ApiError } from './apiError'

export async function request(path, { token, method = 'GET', body, signal } = {}) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      signal,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {})
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError({ message: 'Unable to reach Sandesh. Check your connection.' })
  }

  const text = await response.text()
  let data
  try { data = text ? JSON.parse(text) : undefined } catch { data = undefined }
  if (!response.ok) {
    if (response.status === 401 && token && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('sandesh:unauthorized'))
    }
    throw new ApiError({
      status: response.status,
      code: data?.code || 'HTTP_ERROR',
      message: data?.message || `Request failed (${response.status}).`,
      violations: data?.violations || [],
      path: data?.path || path
    })
  }
  return data
}
