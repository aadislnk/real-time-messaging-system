import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '../components/Button'
import { register } from '../auth/authApi'
import { useAuth } from '../auth/AuthProvider'
import { AuthFrame, FormError } from './LoginPage'

export default function RegisterPage() {
  const { signIn } = useAuth(); const navigate = useNavigate()
  const [values, setValues] = useState({ displayName: '', email: '', password: '' }); const [error, setError] = useState(''); const [busy, setBusy] = useState(false)
  async function submit(event) {
    event.preventDefault(); setError('')
    if (!values.displayName.trim() || !values.email || values.password.length < 10) return setError('Use a name, valid email, and password of at least 10 characters.')
    setBusy(true)
    try { signIn(await register(values)); navigate('/chat', { replace: true }) } catch (e) { setError(e) } finally { setBusy(false) }
  }
  return <AuthFrame title="Create your account" subtitle="A simpler way to stay connected.">
    <form onSubmit={submit} className="auth-form">
      {error && <FormError error={error} />}
      <label>Display name<input autoFocus maxLength="80" value={values.displayName} onChange={(e) => setValues({ ...values, displayName: e.target.value })} autoComplete="name" /></label>
      <label>Email<input type="email" maxLength="254" value={values.email} onChange={(e) => setValues({ ...values, email: e.target.value })} autoComplete="email" /></label>
      <label>Password <span className="label-hint">10–72 characters</span><input type="password" minLength="10" maxLength="72" value={values.password} onChange={(e) => setValues({ ...values, password: e.target.value })} autoComplete="new-password" /></label>
      <Button disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</Button>
    </form>
    <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
  </AuthFrame>
}
