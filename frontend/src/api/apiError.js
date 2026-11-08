export class ApiError extends Error {
  constructor({ status = 0, code = 'NETWORK_ERROR', message = 'Something went wrong.', violations = [], path = '' } = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.violations = violations || []
    this.path = path
  }

  fieldMessage(field) {
    return this.violations.find((violation) => violation.field === field)?.message
  }
}

export function errorMessage(error, fallback = 'Something went wrong.') {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      if (error.path === '/api/auth/login') return error.message || 'Invalid email or password.'
      return 'Your session has expired. Please sign in again.'
    }
    if (error.status === 403) {
      if (error.path === '/api/auth/login') return 'Invalid email or password.'
      return 'You do not have permission to do that.'
    }
    if (error.status === 404) return 'That item is no longer available.'
    if (error.status === 409) return error.message || 'This action conflicts with the current conversation.'
    if (error.status === 429) return 'You are sending messages too quickly. Try again shortly.'
    return error.message || fallback
  }
  return error?.message || fallback
}
