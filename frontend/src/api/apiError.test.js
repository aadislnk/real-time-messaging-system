import { describe, expect, it } from 'vitest'
import { ApiError, errorMessage } from './apiError'

describe('ApiError', () => {
  it('keeps backend field violations available to forms', () => {
    const error = new ApiError({ status: 400, message: 'Validation failed', violations: [{ field: 'email', message: 'Invalid email' }] })
    expect(error.fieldMessage('email')).toBe('Invalid email')
  })
  it('normalizes an expired session message', () => {
    expect(errorMessage(new ApiError({ status: 401 }))).toContain('expired')
  })
  it('keeps invalid login credentials separate from expired sessions', () => {
    expect(errorMessage(new ApiError({
      status: 401,
      message: 'Invalid email or password.',
      path: '/api/auth/login'
    }))).toBe('Invalid email or password.')
  })
  it('does not show a permission error for a login response', () => {
    expect(errorMessage(new ApiError({ status: 403, path: '/api/auth/login' }))).toBe('Invalid email or password.')
  })
})
