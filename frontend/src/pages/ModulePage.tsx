import { Link, useLocation } from 'react-router-dom'
import { navigationItems } from '../config/navigation'

export function ModulePage() {
  const location = useLocation()
  const module = navigationItems.find((item) => item.path === location.pathname)

  return (
    <section className="module-page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Módulo operativo</span>
          <h1>{module?.label || 'Módulo'}</h1>
          <p>Espacio preparado dentro de la navegación principal.</p>
        </div>
      </header>
      <div className="module-placeholder">
        <span className="module-placeholder__icon"><i className={`bi ${module?.icon || 'bi-grid'}`} /></span>
        <span className="status-badge status-badge--info"><i className="bi bi-tools" /> Próxima interfaz</span>
        <h2>La base visual ya está lista</h2>
        <p>
          Este módulo se conectará con los endpoints que ya existen en el backend durante la siguiente etapa del frontend.
        </p>
        <Link className="secondary-button secondary-button--inline" to="/app">
          <i className="bi bi-arrow-left" /> Volver al dashboard
        </Link>
      </div>
    </section>
  )
}
