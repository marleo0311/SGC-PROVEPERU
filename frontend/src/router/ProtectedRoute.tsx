import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div className="app-loader" role="status">
        <span className="app-loader__mark"><i className="bi bi-box-seam-fill" /></span>
        <span className="spinner-border spinner-border-sm" aria-hidden="true" />
        <span>Validando tu sesión…</span>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
