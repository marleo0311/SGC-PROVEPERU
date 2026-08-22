import { useCallback, useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { NavLink } from 'react-router-dom'
import { ToastMessage } from '../components/ToastMessage'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorDetails, getApiErrorMessage } from '../services/api'
import {
  changeUserStatus,
  createRole,
  createUser,
  getRole,
  listPermissions,
  listRoles,
  listUsers,
  resetUserPassword,
  updateRolePermissions,
  updateUser,
} from '../services/security-admin.service'
import type {
  EstadoUsuario,
  PaginaUsuarios,
  Permiso,
  RolResumen,
  UsuarioAdmin,
} from '../types/security-admin'

const dateTimeFormatter = new Intl.DateTimeFormat('es-PE', {
  day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

const moduleLabels: Record<string, { label: string; icon: string; tone: string }> = {
  SEGURIDAD: { label: 'Seguridad', icon: 'bi-shield-lock', tone: 'violet' },
  CATALOGO: { label: 'Catálogo', icon: 'bi-box-seam', tone: 'blue' },
  CLIENTES: { label: 'Clientes', icon: 'bi-people', tone: 'teal' },
  VENTAS: { label: 'Ventas', icon: 'bi-receipt', tone: 'amber' },
  INVENTARIO: { label: 'Inventario', icon: 'bi-boxes', tone: 'blue' },
  COMPRAS: { label: 'Compras', icon: 'bi-cart3', tone: 'teal' },
  PROVEEDORES: { label: 'Proveedores', icon: 'bi-building', tone: 'violet' },
  TRANSPORTE: { label: 'Transporte', icon: 'bi-truck', tone: 'amber' },
  CAJA: { label: 'Caja', icon: 'bi-cash-stack', tone: 'teal' },
  REPORTES: { label: 'Reportes', icon: 'bi-bar-chart-line', tone: 'blue' },
  COTIZACIONES: { label: 'Cotizaciones', icon: 'bi-file-earmark-text', tone: 'violet' },
  PEDIDOS: { label: 'Pedidos', icon: 'bi-bag-check', tone: 'amber' },
  CUENTAS_COBRAR: { label: 'Cuentas por cobrar', icon: 'bi-wallet2', tone: 'teal' },
  CUENTAS_PAGAR: { label: 'Cuentas por pagar', icon: 'bi-credit-card', tone: 'blue' },
  COMPROBANTES: { label: 'Comprobantes', icon: 'bi-file-earmark-check', tone: 'violet' },
  DEVOLUCIONES: { label: 'Devoluciones', icon: 'bi-arrow-counterclockwise', tone: 'amber' },
  IMPRESION: { label: 'Impresión', icon: 'bi-printer', tone: 'blue' },
}

function moduleMeta(module: string) {
  return moduleLabels[module] ?? {
    label: module.toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase()),
    icon: 'bi-grid',
    tone: 'blue',
  }
}

function groupPermissions(items: Permiso[]): Array<[string, Permiso[]]> {
  const grouped = items.reduce<Record<string, Permiso[]>>((result, permission) => {
    (result[permission.modulo] ??= []).push(permission)
    return result
  }, {})
  return Object.entries(grouped).sort(([a], [b]) => a.localeCompare(b))
}

function pageNumbers(currentPage: number, totalPages: number) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index)
  const start = Math.min(Math.max(currentPage - 2, 0), totalPages - 5)
  return Array.from({ length: 5 }, (_, index) => start + index)
}

interface ToastState { tone: 'success' | 'danger'; message: string }

function SecurityTabs() {
  return (
    <nav className="security-tabs" aria-label="Administración de seguridad">
      <NavLink to="/app/usuarios"><i className="bi bi-person-gear" /><span><strong>Usuarios</strong><small>Cuentas y accesos</small></span></NavLink>
      <NavLink to="/app/roles"><i className="bi bi-shield-check" /><span><strong>Roles</strong><small>Perfiles de trabajo</small></span></NavLink>
      <NavLink to="/app/permisos"><i className="bi bi-key" /><span><strong>Permisos</strong><small>Acciones disponibles</small></span></NavLink>
    </nav>
  )
}

export function UsersPage() {
  const [pageData, setPageData] = useState<PaginaUsuarios | null>(null)
  const [searchValue, setSearchValue] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [roles, setRoles] = useState<RolResumen[]>([])
  const [formTarget, setFormTarget] = useState<UsuarioAdmin | 'new' | null>(null)
  const [statusTarget, setStatusTarget] = useState<UsuarioAdmin | null>(null)
  const [passwordTarget, setPasswordTarget] = useState<UsuarioAdmin | null>(null)
  const [statusSubmitting, setStatusSubmitting] = useState(false)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { session, hasAnyAuthority } = useAuth()

  const canCreate = hasAnyAuthority('SEG_USUARIOS_CREAR')
  const canEdit = hasAnyAuthority('SEG_USUARIOS_EDITAR')
  const canChangeStatus = hasAnyAuthority('SEG_USUARIOS_ESTADO')
  const canResetPassword = hasAnyAuthority('SEG_USUARIOS_PASSWORD')

  useEffect(() => {
    let active = true
    listUsers(query, page)
      .then((response) => { if (active) { setPageData(response); setError('') } })
      .catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [query, page, refreshKey])

  useEffect(() => {
    let active = true
    listRoles().then((response) => { if (active) setRoles(response.filter((role) => role.estado === 'ACTIVO')) }).catch(() => undefined)
    return () => { active = false }
  }, [])

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setIsLoading(true); setPage(0); setQuery(searchValue.trim())
  }

  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }
  const closeForm = useCallback(() => setFormTarget(null), [])
  const closePassword = useCallback(() => setPasswordTarget(null), [])
  const closeToast = useCallback(() => setToast(null), [])

  function handleSaved(user: UsuarioAdmin) {
    const action = formTarget === 'new' ? 'creado' : 'actualizado'
    setFormTarget(null); setToast({ tone: 'success', message: `${user.nombreCompleto} fue ${action} correctamente.` }); refresh()
  }

  async function confirmStatus() {
    if (!statusTarget) return
    const next: EstadoUsuario = statusTarget.estado === 'ACTIVO' ? 'SUSPENDIDO' : 'ACTIVO'
    setStatusSubmitting(true)
    try {
      const updated = await changeUserStatus(statusTarget.id, next)
      setPageData((current) => current ? { ...current, contenido: current.contenido.map((user) => user.id === updated.id ? updated : user) } : current)
      setToast({ tone: 'success', message: `${updated.nombreCompleto} ahora está ${updated.estado === 'ACTIVO' ? 'activo' : 'suspendido'}.` })
      setStatusTarget(null)
    } catch (requestError) { setToast({ tone: 'danger', message: getApiErrorMessage(requestError) }) }
    finally { setStatusSubmitting(false) }
  }

  const activeCount = pageData?.contenido.filter((user) => user.estado === 'ACTIVO').length ?? 0
  const roleCount = new Set(pageData?.contenido.map((user) => user.rol.id)).size

  return (
    <>
      <section className="security-page">
        <header className="page-header security-page__header"><div><span className="eyebrow">Control de acceso</span><h1>Usuarios</h1><p>Administra las cuentas que pueden ingresar al sistema.</p></div>{canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setFormTarget('new')}><i className="bi bi-person-plus" /> Nuevo usuario</button>}</header>
        <SecurityTabs />
        <section className="security-summary-grid"><article><span className="security-summary-icon security-summary-icon--blue"><i className="bi bi-people" /></span><span><small>Usuarios encontrados</small><strong>{pageData?.totalElementos ?? 0}</strong></span></article><article><span className="security-summary-icon security-summary-icon--teal"><i className="bi bi-person-check" /></span><span><small>Activos en esta página</small><strong>{activeCount}</strong></span></article><article><span className="security-summary-icon security-summary-icon--violet"><i className="bi bi-shield-check" /></span><span><small>Roles representados</small><strong>{roleCount}</strong></span></article></section>
        <section className="catalog-toolbar"><form className="catalog-search" onSubmit={search}><i className="bi bi-search" /><input type="search" value={searchValue} onChange={(event) => setSearchValue(event.target.value)} placeholder="Buscar por nombre o usuario" aria-label="Buscar usuarios" /><button type="submit">Buscar</button></form>{query && <button className="clear-filter-button" type="button" onClick={() => { setSearchValue(''); setQuery(''); setPage(0); setIsLoading(true) }}><i className="bi bi-x-circle" /> Limpiar</button>}<button className="secondary-button secondary-button--inline" type="button" onClick={refresh} disabled={isLoading}><i className={`bi bi-arrow-clockwise ${isLoading ? 'inventory-spin' : ''}`} /> Actualizar</button></section>
        <section className="catalog-panel">{isLoading && !pageData ? <SecurityTableSkeleton /> : error ? <SecurityError message={error} onRetry={refresh} /> : pageData?.contenido.length === 0 ? <SecurityEmpty icon="bi-person-x" title="No hay usuarios para mostrar" description={query ? 'Prueba con otro término de búsqueda.' : 'Crea la primera cuenta de trabajo para tu equipo.'} /> : <><div className="catalog-table-wrap"><table className="catalog-table users-table"><thead><tr><th>Usuario</th><th>Cuenta</th><th>Rol asignado</th><th>Último acceso</th><th>Estado</th><th className="catalog-table__actions-heading security-actions-heading">Acciones</th></tr></thead><tbody>{pageData?.contenido.map((user) => { const isCurrent = user.id === session?.usuario.idUsuario; return <tr key={user.id}><td><div className="security-user-cell"><span><i className="bi bi-person" /></span><span><strong>{user.nombreCompleto}</strong><small>{isCurrent ? 'Tu cuenta actual' : `Registrado ${dateTimeFormatter.format(new Date(user.fechaRegistro))}`}</small></span></div></td><td><strong className="security-login">@{user.usuarioLogin}</strong></td><td><span className="security-role-badge"><i className="bi bi-shield-check" /> {user.rol.nombre}</span></td><td>{user.ultimoAcceso ? <span className="security-last-access"><strong>{dateTimeFormatter.format(new Date(user.ultimoAcceso))}</strong><small>Acceso registrado</small></span> : <span className="table-muted">Aún no ingresó</span>}</td><td><span className={`security-user-status security-user-status--${user.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {user.estado === 'ACTIVO' ? 'Activo' : 'Suspendido'}</span></td><td><div className="product-actions security-actions">{canEdit && <button type="button" onClick={() => setFormTarget(user)} title="Editar usuario"><i className="bi bi-pencil" /></button>}{canResetPassword && <button className="security-actions__password" type="button" onClick={() => setPasswordTarget(user)} title="Restablecer contraseña"><i className="bi bi-key" /></button>}{canChangeStatus && <button className={user.estado === 'ACTIVO' ? 'product-actions__danger' : 'product-actions__success'} type="button" onClick={() => setStatusTarget(user)} disabled={isCurrent} title={isCurrent ? 'No puedes suspender tu cuenta actual' : user.estado === 'ACTIVO' ? 'Suspender usuario' : 'Activar usuario'}><i className={`bi ${user.estado === 'ACTIVO' ? 'bi-pause' : 'bi-play'}`} /></button>}</div></td></tr> })}</tbody></table></div>{pageData && pageData.totalPaginas > 0 && <footer className="catalog-pagination"><span>Mostrando {pageData.contenido.length} de {pageData.totalElementos} usuarios</span><nav>{<button type="button" disabled={pageData.pagina === 0} onClick={() => { setPage(pageData.pagina - 1); setIsLoading(true) }}><i className="bi bi-chevron-left" /></button>}{pageNumbers(pageData.pagina, pageData.totalPaginas).map((item) => <button className={item === pageData.pagina ? 'active' : ''} type="button" key={item} onClick={() => { setPage(item); setIsLoading(true) }}>{item + 1}</button>)}<button type="button" disabled={pageData.ultima} onClick={() => { setPage(pageData.pagina + 1); setIsLoading(true) }}><i className="bi bi-chevron-right" /></button></nav></footer>}</>}</section>
      </section>
      {formTarget && <UserFormModal key={formTarget === 'new' ? 'new' : formTarget.id} user={formTarget === 'new' ? undefined : formTarget} roles={roles} onClose={closeForm} onSaved={handleSaved} />}
      {statusTarget && <SecurityConfirmDialog icon={statusTarget.estado === 'ACTIVO' ? 'bi-person-dash' : 'bi-person-check'} tone={statusTarget.estado === 'ACTIVO' ? 'danger' : 'success'} title={statusTarget.estado === 'ACTIVO' ? '¿Suspender usuario?' : '¿Activar usuario?'} message={<><strong>{statusTarget.nombreCompleto}</strong> {statusTarget.estado === 'ACTIVO' ? 'perderá el acceso al sistema.' : 'podrá ingresar nuevamente al sistema.'}</>} confirmLabel={statusTarget.estado === 'ACTIVO' ? 'Sí, suspender' : 'Sí, activar'} isSubmitting={statusSubmitting} onCancel={() => setStatusTarget(null)} onConfirm={confirmStatus} />}
      {passwordTarget && <PasswordModal user={passwordTarget} onClose={closePassword} onSaved={() => { setPasswordTarget(null); setToast({ tone: 'success', message: `La contraseña de ${passwordTarget.nombreCompleto} fue restablecida.` }) }} />}
      {toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}
    </>
  )
}

export function RolesPage() {
  const [roles, setRoles] = useState<RolResumen[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [editorTarget, setEditorTarget] = useState<RolResumen | 'new' | null>(null)
  const [toast, setToast] = useState<ToastState | null>(null)
  const { hasAnyAuthority } = useAuth()
  const canCreate = hasAnyAuthority('SEG_ROLES_CREAR')
  const canManage = hasAnyAuthority('SEG_ROLES_PERMISOS')

  useEffect(() => {
    let active = true
    listRoles().then((response) => { if (active) { setRoles(response); setError('') } }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) }).finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [refreshKey])

  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }
  const closeEditor = useCallback(() => setEditorTarget(null), [])
  const closeToast = useCallback(() => setToast(null), [])
  function saved(role: RolResumen) { const created = editorTarget === 'new'; setEditorTarget(null); setToast({ tone: 'success', message: `${role.nombre} fue ${created ? 'creado' : 'actualizado'} correctamente.` }); refresh() }

  return <><section className="security-page"><header className="page-header security-page__header"><div><span className="eyebrow">Control de acceso</span><h1>Roles</h1><p>Define responsabilidades y permisos para cada perfil de trabajo.</p></div>{canCreate && <button className="primary-button primary-button--inline" type="button" onClick={() => setEditorTarget('new')}><i className="bi bi-plus-lg" /> Nuevo rol</button>}</header><SecurityTabs />
    <section className="roles-intro"><span><i className="bi bi-diagram-3" /></span><span><strong>Permisos organizados por función</strong><small>Cada usuario obtiene las acciones autorizadas a través de su rol asignado.</small></span><span><b>{roles.length}</b><small>roles configurados</small></span></section>
    {isLoading ? <div className="roles-grid">{[1, 2, 3, 4].map((item) => <div className="skeleton role-card-skeleton" key={item} />)}</div> : error ? <section className="catalog-panel"><SecurityError message={error} onRetry={refresh} /></section> : roles.length === 0 ? <section className="catalog-panel"><SecurityEmpty icon="bi-shield-x" title="No hay roles configurados" description="Crea un rol para asignar permisos a los usuarios." /></section> : <section className="roles-grid">{roles.map((role, index) => <article className="role-card" key={role.id}><header><span className={`role-card__icon role-card__icon--${['blue', 'violet', 'teal', 'amber'][index % 4]}`}><i className="bi bi-shield-check" /></span><span className={`catalog-status catalog-status--${role.estado.toLowerCase()}`}><i className="bi bi-circle-fill" /> {role.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}</span></header><h2>{role.nombre}</h2><p>{role.descripcion || 'Perfil de acceso configurado para el sistema.'}</p><footer><span><i className="bi bi-key" /> Permisos asignados</span>{canManage && <button type="button" onClick={() => setEditorTarget(role)}>Administrar <i className="bi bi-arrow-right" /></button>}</footer></article>)}</section>}
  </section>{editorTarget && <RoleEditorModal key={editorTarget === 'new' ? 'new' : editorTarget.id} role={editorTarget === 'new' ? undefined : editorTarget} onClose={closeEditor} onSaved={saved} />}{toast && <ToastMessage tone={toast.tone} message={toast.message} onClose={closeToast} />}</>
}

export function PermissionsPage() {
  const [permissions, setPermissions] = useState<Permiso[]>([])
  const [selectedModule, setSelectedModule] = useState('')
  const [searchValue, setSearchValue] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let active = true
    listPermissions().then((response) => { if (active) { setPermissions(response); setError('') } }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) }).finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [refreshKey])

  const modules = useMemo(() => Array.from(new Set(permissions.map((permission) => permission.modulo))).sort(), [permissions])
  const visible = permissions.filter((permission) => (!selectedModule || permission.modulo === selectedModule) && (!searchValue.trim() || `${permission.codigo} ${permission.nombre} ${permission.descripcion ?? ''}`.toLowerCase().includes(searchValue.trim().toLowerCase())))
  const grouped = useMemo(() => groupPermissions(visible), [visible])
  function refresh() { setIsLoading(true); setRefreshKey((current) => current + 1) }

  return <section className="security-page"><header className="page-header"><div><span className="eyebrow">Control de acceso</span><h1>Permisos</h1><p>Consulta las acciones disponibles y el módulo al que pertenecen.</p></div><div className="permissions-total"><span><i className="bi bi-key" /></span><span><strong>{permissions.length}</strong><small>permisos disponibles</small></span></div></header><SecurityTabs />
    <section className="permissions-notice"><i className="bi bi-info-circle" /><span><strong>Catálogo administrado por el sistema</strong><small>Los permisos no se crean ni eliminan desde esta pantalla; se asignan desde la sección Roles.</small></span><NavLink to="/app/roles">Administrar roles <i className="bi bi-arrow-right" /></NavLink></section>
    <section className="catalog-toolbar"><div className="catalog-search"><i className="bi bi-search" /><input type="search" value={searchValue} onChange={(event) => setSearchValue(event.target.value)} placeholder="Buscar por código, nombre o descripción" aria-label="Buscar permisos" /></div><div className="catalog-filter"><i className="bi bi-grid" /><select value={selectedModule} onChange={(event) => setSelectedModule(event.target.value)}><option value="">Todos los módulos</option>{modules.map((module) => <option key={module} value={module}>{moduleMeta(module).label}</option>)}</select></div>{(selectedModule || searchValue) && <button className="clear-filter-button" type="button" onClick={() => { setSelectedModule(''); setSearchValue('') }}><i className="bi bi-x-circle" /> Limpiar</button>}</section>
    {isLoading ? <div className="permissions-groups"><div className="skeleton permissions-group-skeleton" /><div className="skeleton permissions-group-skeleton" /></div> : error ? <section className="catalog-panel"><SecurityError message={error} onRetry={refresh} /></section> : visible.length === 0 ? <section className="catalog-panel"><SecurityEmpty icon="bi-search" title="No encontramos permisos" description="Cambia el módulo o el término de búsqueda." /></section> : <div className="permissions-groups">{grouped.map(([module, modulePermissions]) => { const meta = moduleMeta(module); return <section className="permissions-group" key={module}><header><span className={`permissions-group__icon permissions-group__icon--${meta.tone}`}><i className={`bi ${meta.icon}`} /></span><span><h2>{meta.label}</h2><p>{modulePermissions?.length ?? 0} acciones disponibles</p></span><span className="permissions-group__code">{module}</span></header><div className="permissions-list">{modulePermissions?.map((permission) => <article key={permission.id}><span className="permission-action-icon"><i className="bi bi-check2-circle" /></span><span><strong>{permission.nombre}</strong><small>{permission.descripcion || 'Autoriza esta acción dentro del módulo.'}</small></span><code>{permission.codigo}</code></article>)}</div></section> })}</div>}
  </section>
}

function UserFormModal({ user, roles, onClose, onSaved }: { user?: UsuarioAdmin; roles: RolResumen[]; onClose: () => void; onSaved: (user: UsuarioAdmin) => void }) {
  const [values, setValues] = useState({ nombreCompleto: user?.nombreCompleto ?? '', usuarioLogin: user?.usuarioLogin ?? '', password: '', idRol: user ? String(user.rol.id) : '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  useModalBehavior(isSubmitting, onClose)

  function change(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) { const { name, value } = event.target; setValues((current) => ({ ...current, [name]: value })); setErrors((current) => ({ ...current, [name]: '' })) }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const nextErrors: Record<string, string> = {}
    if (!values.nombreCompleto.trim()) nextErrors.nombreCompleto = 'Ingresa el nombre completo.'
    if (!values.usuarioLogin.trim()) nextErrors.usuarioLogin = 'Ingresa el usuario de acceso.'
    if (!user && values.password.length < 12) nextErrors.password = 'La contraseña debe tener al menos 12 caracteres.'
    if (!values.idRol) nextErrors.idRol = 'Selecciona un rol.'
    if (Object.keys(nextErrors).length) { setErrors(nextErrors); return }
    setIsSubmitting(true); setSubmitError('')
    try { const saved = user ? await updateUser(user.id, { nombreCompleto: values.nombreCompleto.trim(), usuarioLogin: values.usuarioLogin.trim(), idRol: Number(values.idRol) }) : await createUser({ nombreCompleto: values.nombreCompleto.trim(), usuarioLogin: values.usuarioLogin.trim(), password: values.password, idRol: Number(values.idRol) }); onSaved(saved) }
    catch (requestError) { const details = getApiErrorDetails(requestError); setSubmitError(details.message); setErrors(details.fieldErrors) }
    finally { setIsSubmitting(false) }
  }
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}><section className="form-modal security-form-modal" role="dialog" aria-modal="true"><header className="form-modal__header"><div><span className="form-modal__icon client-form-modal__icon"><i className={`bi ${user ? 'bi-person-gear' : 'bi-person-plus'}`} /></span><span><small>Administración de accesos</small><h2>{user ? 'Editar usuario' : 'Crear usuario'}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header><form onSubmit={submit} noValidate><div className="form-modal__body">{submitError && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{submitError}</span></div>}<div className="security-form-hero"><span><i className="bi bi-person-badge" /></span><span><strong>Información de la cuenta</strong><small>El usuario utilizará estas credenciales para ingresar al sistema.</small></span></div><div className="product-form-grid"><SecurityField label="Nombre completo" name="nombreCompleto" error={errors.nombreCompleto}><input id="nombreCompleto" name="nombreCompleto" value={values.nombreCompleto} onChange={change} maxLength={180} autoFocus placeholder="Nombre y apellidos" /></SecurityField><SecurityField label="Usuario de acceso" name="usuarioLogin" error={errors.usuarioLogin}><div className="security-login-input"><span>@</span><input id="usuarioLogin" name="usuarioLogin" value={values.usuarioLogin} onChange={change} maxLength={180} placeholder="usuario" /></div></SecurityField>{!user && <SecurityField label="Contraseña temporal" name="password" error={errors.password} hint="Mínimo 12 caracteres" wide><input id="password" name="password" type="password" value={values.password} onChange={change} maxLength={200} placeholder="Contraseña inicial segura" autoComplete="new-password" /></SecurityField>}<SecurityField label="Rol asignado" name="idRol" error={errors.idRol} wide><select id="idRol" name="idRol" value={values.idRol} onChange={change}><option value="">Seleccionar rol</option>{roles.map((role) => <option key={role.id} value={role.id}>{role.nombre}</option>)}</select></SecurityField></div></div><footer className="form-modal__footer"><span><i className="bi bi-shield-check" /> Los permisos se obtienen del rol seleccionado.</span><div><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting || roles.length === 0}>{isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {user ? 'Guardar cambios' : 'Crear usuario'}</>}</button></div></footer></form></section></div>
}

function PasswordModal({ user, onClose, onSaved }: { user: UsuarioAdmin; onClose: () => void; onSaved: () => void }) {
  const [password, setPassword] = useState(''); const [confirm, setConfirm] = useState(''); const [error, setError] = useState(''); const [isSubmitting, setIsSubmitting] = useState(false)
  useModalBehavior(isSubmitting, onClose)
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (password.length < 12) { setError('La contraseña debe tener al menos 12 caracteres.'); return } if (password !== confirm) { setError('Las contraseñas no coinciden.'); return } setIsSubmitting(true); setError(''); try { await resetUserPassword(user.id, password); onSaved() } catch (requestError) { setError(getApiErrorMessage(requestError)) } finally { setIsSubmitting(false) } }
  return <div className="modal-backdrop modal-backdrop--confirm" role="presentation"><section className="password-modal" role="dialog" aria-modal="true"><span className="password-modal__icon"><i className="bi bi-key" /></span><h2>Restablecer contraseña</h2><p>Define una nueva contraseña para <strong>{user.nombreCompleto}</strong>.</p><form onSubmit={submit}>{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}<label><span>Nueva contraseña</span><input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="new-password" autoFocus /></label><label><span>Confirmar contraseña</span><input type="password" value={confirm} onChange={(event) => setConfirm(event.target.value)} autoComplete="new-password" /></label><small><i className="bi bi-info-circle" /> Utiliza al menos 12 caracteres.</small><div><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>{isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} Restablecer</button></div></form></section></div>
}

function RoleEditorModal({ role, onClose, onSaved }: { role?: RolResumen; onClose: () => void; onSaved: (role: RolResumen) => void }) {
  const [name, setName] = useState(role?.nombre ?? ''); const [description, setDescription] = useState(role?.descripcion ?? ''); const [permissions, setPermissions] = useState<Permiso[]>([]); const [selected, setSelected] = useState<Set<number>>(new Set()); const [isLoading, setIsLoading] = useState(true); const [isSubmitting, setIsSubmitting] = useState(false); const [error, setError] = useState(''); const [search, setSearch] = useState('')
  useModalBehavior(isSubmitting, onClose)
  useEffect(() => { let active = true; Promise.all([listPermissions(), role ? getRole(role.id) : Promise.resolve(null)]).then(([all, detail]) => { if (!active) return; setPermissions(all); setSelected(new Set(detail?.permisos.map((permission) => permission.id) ?? [])) }).catch((requestError: unknown) => { if (active) setError(getApiErrorMessage(requestError)) }).finally(() => { if (active) setIsLoading(false) }); return () => { active = false } }, [role])
  const visible = permissions.filter((permission) => !search.trim() || `${permission.nombre} ${permission.codigo}`.toLowerCase().includes(search.trim().toLowerCase()))
  const grouped = groupPermissions(visible)
  function toggle(id: number) { setSelected((current) => { const next = new Set(current); if (next.has(id)) next.delete(id); else next.add(id); return next }) }
  function toggleModule(items: Permiso[]) { const allSelected = items.every((item) => selected.has(item.id)); setSelected((current) => { const next = new Set(current); items.forEach((item) => allSelected ? next.delete(item.id) : next.add(item.id)); return next }) }
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (!role && !name.trim()) { setError('Ingresa el nombre del rol.'); return } setIsSubmitting(true); setError(''); try { const saved = role ? await updateRolePermissions(role.id, Array.from(selected)) : await createRole({ nombre: name.trim(), descripcion: description.trim() || null, idsPermisos: Array.from(selected) }); onSaved(saved) } catch (requestError) { setError(getApiErrorMessage(requestError)) } finally { setIsSubmitting(false) } }
  return <div className="modal-backdrop role-editor-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onClose()}><section className="role-editor-modal" role="dialog" aria-modal="true"><header><div><span><i className="bi bi-shield-check" /></span><span><small>Configuración de acceso</small><h2>{role ? `Permisos de ${role.nombre}` : 'Crear nuevo rol'}</h2></span></div><button className="icon-button" type="button" onClick={onClose}><i className="bi bi-x-lg" /></button></header>{isLoading ? <div className="role-editor-loading"><span className="spinner-border" /><strong>Preparando permisos…</strong></div> : <form onSubmit={submit}><div className="role-editor-body">{error && <div className="alert-message alert-message--danger"><i className="bi bi-exclamation-circle-fill" /><span>{error}</span></div>}<section className="role-editor-details"><label><span>Nombre del rol *</span><input value={name} onChange={(event) => setName(event.target.value)} disabled={Boolean(role)} maxLength={80} placeholder="Ej. Supervisor de almacén" /></label><label><span>Descripción</span><input value={description} onChange={(event) => setDescription(event.target.value)} disabled={Boolean(role)} maxLength={250} placeholder="Responsabilidad principal del rol" /></label></section><div className="role-permissions-toolbar"><div><i className="bi bi-search" /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar permiso" /></div><span><strong>{selected.size}</strong> seleccionados</span><button type="button" onClick={() => setSelected(new Set(permissions.map((permission) => permission.id)))}>Seleccionar todos</button><button type="button" onClick={() => setSelected(new Set())}>Limpiar</button></div><div className="role-permission-groups">{grouped.map(([module, items]) => { const meta = moduleMeta(module); const permissionsInModule = items ?? []; const selectedCount = permissionsInModule.filter((item) => selected.has(item.id)).length; return <section key={module}><header><span className={`permissions-group__icon permissions-group__icon--${meta.tone}`}><i className={`bi ${meta.icon}`} /></span><span><strong>{meta.label}</strong><small>{selectedCount} de {permissionsInModule.length}</small></span><button type="button" onClick={() => toggleModule(permissionsInModule)}>{selectedCount === permissionsInModule.length ? 'Quitar todos' : 'Seleccionar módulo'}</button></header><div>{permissionsInModule.map((permission) => <label className={selected.has(permission.id) ? 'active' : ''} key={permission.id}><input type="checkbox" checked={selected.has(permission.id)} onChange={() => toggle(permission.id)} /><span className="role-permission-check"><i className="bi bi-check2" /></span><span><strong>{permission.nombre}</strong><small>{permission.descripcion || permission.codigo}</small></span><code>{permission.codigo}</code></label>)}</div></section> })}</div></div><footer><span><i className="bi bi-shield-lock" /> Los cambios afectarán a los usuarios con este rol.</span><div><button className="secondary-button" type="button" onClick={onClose} disabled={isSubmitting}>Cancelar</button><button className="primary-button primary-button--inline" type="submit" disabled={isSubmitting}>{isSubmitting ? <><span className="spinner-border spinner-border-sm" /> Guardando…</> : <><i className="bi bi-check2" /> {role ? 'Actualizar permisos' : 'Crear rol'}</>}</button></div></footer></form>}</section></div>
}

function useModalBehavior(isSubmitting: boolean, onClose: () => void) {
  useEffect(() => { const previous = document.body.style.overflow; document.body.style.overflow = 'hidden'; const keydown = (event: KeyboardEvent) => { if (event.key === 'Escape' && !isSubmitting) onClose() }; window.addEventListener('keydown', keydown); return () => { document.body.style.overflow = previous; window.removeEventListener('keydown', keydown) } }, [isSubmitting, onClose])
}

function SecurityField({ label, name, error, hint, wide, children }: { label: string; name: string; error?: string; hint?: string; wide?: boolean; children: React.ReactNode }) { return <label className={`product-form-field ${wide ? 'product-form-field--wide' : ''} ${error ? 'product-form-field--error' : ''}`} htmlFor={name}><span className="product-form-field__label">{label} <b>*</b>{hint && <small>{hint}</small>}</span>{children}{error && <span className="product-form-field__error"><i className="bi bi-exclamation-circle" /> {error}</span>}</label> }

function SecurityConfirmDialog({ icon, tone, title, message, confirmLabel, isSubmitting, onCancel, onConfirm }: { icon: string; tone: string; title: string; message: React.ReactNode; confirmLabel: string; isSubmitting: boolean; onCancel: () => void; onConfirm: () => void }) { return <div className="modal-backdrop modal-backdrop--confirm"><section className="confirm-dialog"><span className={`confirm-dialog__icon ${tone === 'success' ? 'confirm-dialog__icon--success' : ''}`}><i className={`bi ${icon}`} /></span><h2>{title}</h2><p>{message}</p><div className="confirm-dialog__actions"><button className="secondary-button" type="button" onClick={onCancel} disabled={isSubmitting}>Cancelar</button><button className={`primary-button primary-button--inline ${tone === 'danger' ? 'primary-button--danger' : ''}`} type="button" onClick={onConfirm} disabled={isSubmitting}>{isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-check2" />} {confirmLabel}</button></div></section></div> }
function SecurityTableSkeleton() { return <div className="catalog-table-skeleton"><div className="skeleton catalog-table-skeleton__header" />{[1, 2, 3, 4, 5].map((item) => <div className="skeleton catalog-table-skeleton__row" key={item} />)}</div> }
function SecurityError({ message, onRetry }: { message: string; onRetry: () => void }) { return <div className="catalog-message"><span className="catalog-message__icon catalog-message__icon--danger"><i className="bi bi-cloud-slash" /></span><h2>No pudimos cargar la información</h2><p>{message}</p><button className="secondary-button secondary-button--inline" type="button" onClick={onRetry}><i className="bi bi-arrow-clockwise" /> Reintentar</button></div> }
function SecurityEmpty({ icon, title, description }: { icon: string; title: string; description: string }) { return <div className="catalog-message"><span className="catalog-message__icon"><i className={`bi ${icon}`} /></span><h2>{title}</h2><p>{description}</p></div> }
