import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './layout/AppShell'
import { CatalogsPage } from './pages/CatalogsPage'
import { DashboardPage } from './pages/DashboardPage'
import { InventoryPage } from './pages/InventoryPage'
import { LoginPage } from './pages/LoginPage'
import { ModulePage } from './pages/ModulePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProductsPage } from './pages/ProductsPage'
import { ProtectedRoute } from './router/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="catalogos" element={<CatalogsPage />} />
          <Route path="productos" element={<ProductsPage />} />
          <Route path="inventario" element={<InventoryPage />} />
          <Route path=":module" element={<ModulePage />} />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
