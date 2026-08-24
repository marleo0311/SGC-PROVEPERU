import { Navigate, Route, Routes } from 'react-router-dom'
import { PayablesPage, ReceivablesPage } from './pages/AccountsPages'
import { AppShell } from './layout/AppShell'
import { CatalogsPage } from './pages/CatalogsPage'
import { CashPage } from './pages/CashPage'
import { ClientsPage } from './pages/ClientsPage'
import { OrdersPage, QuotesPage, ReceiptsPage, SalesPage } from './pages/CommercialPages'
import { DashboardPage } from './pages/DashboardPage'
import { InventoryPage } from './pages/InventoryPage'
import { KardexPage } from './pages/KardexPage'
import { LoginPage } from './pages/LoginPage'
import { CarriersPage, ExpensesPage } from './pages/LogisticsPages'
import { ModulePage } from './pages/ModulePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProductsPage } from './pages/ProductsPage'
import { PurchasesPage } from './pages/PurchasesPage'
import { ReturnsPage } from './pages/ReturnsPage'
import { ReportsPage } from './pages/ReportsPage'
import { PermissionsPage, RolesPage, UsersPage } from './pages/SecurityPages'
import { SuppliersPage } from './pages/SuppliersPage'
import { SunatDailySummariesPage } from './pages/SunatDailySummariesPage'
import { SunatDiagnosticsPage } from './pages/SunatDiagnosticsPage'
import { ProtectedRoute } from './router/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="catalogos" element={<CatalogsPage />} />
          <Route path="clientes" element={<ClientsPage />} />
          <Route path="productos" element={<ProductsPage />} />
          <Route path="inventario" element={<InventoryPage />} />
          <Route path="kardex" element={<KardexPage />} />
          <Route path="usuarios" element={<UsersPage />} />
          <Route path="roles" element={<RolesPage />} />
          <Route path="permisos" element={<PermissionsPage />} />
          <Route path="proveedores" element={<SuppliersPage />} />
          <Route path="compras" element={<PurchasesPage />} />
          <Route path="transportistas" element={<CarriersPage />} />
          <Route path="gastos" element={<ExpensesPage />} />
          <Route path="cuentas-pagar" element={<PayablesPage />} />
          <Route path="cuentas-cobrar" element={<ReceivablesPage />} />
          <Route path="cotizaciones" element={<QuotesPage />} />
          <Route path="pedidos" element={<OrdersPage />} />
          <Route path="ventas" element={<SalesPage />} />
          <Route path="devoluciones" element={<ReturnsPage />} />
          <Route path="comprobantes" element={<ReceiptsPage />} />
          <Route path="resumenes-sunat" element={<SunatDailySummariesPage />} />
          <Route path="diagnostico-sunat" element={<SunatDiagnosticsPage />} />
          <Route path="caja" element={<CashPage />} />
          <Route path="reportes" element={<ReportsPage />} />
          <Route path=":module" element={<ModulePage />} />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
