import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Brand } from '../components/Brand'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'

interface LocationState {
  from?: { pathname?: string }
}

export function LoginPage() {
  const [usuarioLogin, setUsuarioLogin] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const { isAuthenticated, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  if (isAuthenticated) {
    return <Navigate to="/app" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      await signIn(usuarioLogin, password)
      const state = location.state as LocationState | null
      navigate(state?.from?.pathname || '/app', { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-showcase" aria-label="Presentación de PROVEPERÚ">
        <div className="login-showcase__glow login-showcase__glow--one" />
        <div className="login-showcase__glow login-showcase__glow--two" />
        <Brand inverted />

        <div className="login-showcase__content">
          <span className="eyebrow eyebrow--light">Control centralizado</span>
          <h1>Tu operación comercial, clara y bajo control.</h1>
          <p>
            Ventas, inventario y finanzas conectados en una sola plataforma para tomar mejores decisiones.
          </p>
          <div className="login-showcase__features">
            <div><i className="bi bi-graph-up-arrow" /><span><strong>Indicadores en tiempo real</strong><small>Una visión rápida del negocio</small></span></div>
            <div><i className="bi bi-shield-check" /><span><strong>Acceso protegido</strong><small>Permisos según cada rol</small></span></div>
            <div><i className="bi bi-boxes" /><span><strong>Inventario conectado</strong><small>Alertas para actuar a tiempo</small></span></div>
          </div>
        </div>

        <div className="login-showcase__footer">
          <span><i className="bi bi-lock-fill" /> Conexión segura</span>
          <span>SGC v1.0</span>
        </div>
      </section>

      <section className="login-panel">
        <div className="login-card">
          <div className="login-card__mobile-brand"><Brand /></div>
          <div className="login-card__heading">
            <span className="login-card__welcome"><i className="bi bi-stars" /> Bienvenido</span>
            <h2>Inicia sesión</h2>
            <p>Ingresa tus credenciales para acceder al sistema.</p>
          </div>

          {error && (
            <div className="alert-message alert-message--danger" role="alert">
              <i className="bi bi-exclamation-circle-fill" aria-hidden="true" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="login-form">
            <label htmlFor="usuarioLogin">Usuario</label>
            <div className="field-control">
              <i className="bi bi-person" aria-hidden="true" />
              <input
                id="usuarioLogin"
                name="usuarioLogin"
                type="text"
                value={usuarioLogin}
                onChange={(event) => setUsuarioLogin(event.target.value)}
                placeholder="Escribe tu usuario"
                autoComplete="username"
                maxLength={180}
                required
                autoFocus
              />
            </div>

            <label htmlFor="password">Contraseña</label>
            <div className="field-control">
              <i className="bi bi-lock" aria-hidden="true" />
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Escribe tu contraseña"
                autoComplete="current-password"
                maxLength={200}
                required
              />
              <button
                type="button"
                className="field-control__action"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`} />
              </button>
            </div>

            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? (
                <><span className="spinner-border spinner-border-sm" aria-hidden="true" /> Validando…</>
              ) : (
                <>Ingresar al sistema <i className="bi bi-arrow-right" aria-hidden="true" /></>
              )}
            </button>
          </form>

          <div className="login-card__support">
            <i className="bi bi-info-circle" aria-hidden="true" />
            <span>Si no puedes ingresar, contacta al administrador del sistema.</span>
          </div>
        </div>
        <p className="login-panel__copyright">© {new Date().getFullYear()} PROVEPERÚ · Sistema de Gestión Comercial</p>
      </section>
    </main>
  )
}
