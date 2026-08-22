import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="not-found">
      <span className="not-found__code">404</span>
      <h1>Esta página no existe</h1>
      <p>La dirección puede haber cambiado o no estar disponible.</p>
      <Link className="primary-button primary-button--inline" to="/app">
        <i className="bi bi-house-door" /> Volver al inicio
      </Link>
    </main>
  )
}
