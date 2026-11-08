import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Button from '../components/Button'
import { errorMessage } from '../api/apiError'
import { login } from '../auth/authApi'
import { useAuth } from '../auth/AuthProvider'

export default function LoginPage() {
  const { signIn } = useAuth(); const navigate = useNavigate(); const location = useLocation()
  const [values, setValues] = useState({ email: '', password: '' }); const [error, setError] = useState(''); const [busy, setBusy] = useState(false)
  async function submit(event) {
    event.preventDefault(); setError('')
    if (!values.email || !values.password) return setError('Enter your email and password.')
    setBusy(true)
    try { signIn(await login(values)); navigate(location.state?.from || '/chat', { replace: true }) } catch (e) { setError(e) } finally { setBusy(false) }
  }
  return <AuthFrame title="Welcome back" subtitle="Sign in to continue your conversations.">
    <form onSubmit={submit} className="auth-form">
      {error && <FormError error={error} fallback="Invalid email or password." />}
      <label>Email<input autoFocus type="email" value={values.email} onChange={(e) => setValues({ ...values, email: e.target.value })} autoComplete="email" /></label>
      <label>Password<input type="password" value={values.password} onChange={(e) => setValues({ ...values, password: e.target.value })} autoComplete="current-password" /></label>
      <Button disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</Button>
    </form>
    <p className="auth-switch">Don't have an account? <Link to="/register">Create account</Link></p>
  </AuthFrame>
}

export function FormError({ error, fallback }) {
  const violations = error?.violations || []
  return <div className="form-error" role="alert">
    <div>{typeof error === 'string' ? error : errorMessage(error, fallback)}</div>
    {violations.length > 0 && <ul className="field-errors">{violations.map((violation, index) => <li key={`${violation.field || 'error'}-${index}`}>{violation.field ? `${violation.field}: ` : ''}{violation.message}</li>)}</ul>}
  </div>
}

export function AuthFrame({ title, subtitle, children }) {
  return <div className="auth-page"><div className="auth-brand"><strong>Sandesh</strong></div><div className="auth-card"><h1>{title}</h1>{subtitle && <p className="auth-subtitle">{subtitle}</p>}{children}</div></div>
}
