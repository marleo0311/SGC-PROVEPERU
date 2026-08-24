export interface NavigationItem {
  label: string
  path: string
  icon: string
  authorities?: string[]
}

export interface NavigationGroup {
  label: string
  items: NavigationItem[]
}

export const navigationGroups: NavigationGroup[] = [
  {
    label: 'Inicio',
    items: [{ label: 'Dashboard', path: '/app', icon: 'bi-grid-1x2-fill' }],
  },
  {
    label: 'Comercial',
    items: [
      { label: 'Ventas', path: '/app/ventas', icon: 'bi-receipt', authorities: ['VEN_VENTAS_VER'] },
      { label: 'Devoluciones', path: '/app/devoluciones', icon: 'bi-arrow-counterclockwise', authorities: ['DEV_DEVOLUCIONES_VER'] },
      { label: 'Cotizaciones', path: '/app/cotizaciones', icon: 'bi-file-earmark-text', authorities: ['COT_COTIZACIONES_VER'] },
      { label: 'Pedidos', path: '/app/pedidos', icon: 'bi-bag-check', authorities: ['PED_PEDIDOS_VER'] },
      { label: 'Clientes', path: '/app/clientes', icon: 'bi-people', authorities: ['CLI_CLIENTES_VER'] },
    ],
  },
  {
    label: 'Inventario',
    items: [
      { label: 'Catálogos', path: '/app/catalogos', icon: 'bi-tags', authorities: ['CAT_CATEGORIAS_VER', 'CAT_MARCAS_VER', 'CAT_UNIDADES_VER'] },
      { label: 'Productos', path: '/app/productos', icon: 'bi-box-seam', authorities: ['CAT_PRODUCTOS_VER'] },
      { label: 'Existencias', path: '/app/inventario', icon: 'bi-boxes', authorities: ['INV_STOCK_VER'] },
      { label: 'Kardex', path: '/app/kardex', icon: 'bi-arrow-left-right', authorities: ['INV_KARDEX_VER'] },
    ],
  },
  {
    label: 'Abastecimiento',
    items: [
      { label: 'Compras', path: '/app/compras', icon: 'bi-cart3', authorities: ['CMP_COMPRAS_VER'] },
      { label: 'Proveedores', path: '/app/proveedores', icon: 'bi-building', authorities: ['PRV_PROVEEDORES_VER'] },
      { label: 'Transportistas', path: '/app/transportistas', icon: 'bi-truck', authorities: ['TRN_TRANSPORTISTAS_VER'] },
      { label: 'Cuentas por pagar', path: '/app/cuentas-pagar', icon: 'bi-credit-card', authorities: ['CXP_CUENTAS_VER'] },
    ],
  },
  {
    label: 'Finanzas',
    items: [
      { label: 'Caja', path: '/app/caja', icon: 'bi-cash-stack', authorities: ['CAJ_CAJAS_VER'] },
      { label: 'Cuentas por cobrar', path: '/app/cuentas-cobrar', icon: 'bi-wallet2', authorities: ['CXC_CUENTAS_VER'] },
      { label: 'Gastos', path: '/app/gastos', icon: 'bi-graph-down-arrow', authorities: ['TRN_GASTOS_VER'] },
    ],
  },
  {
    label: 'Control',
    items: [
      { label: 'Comprobantes', path: '/app/comprobantes', icon: 'bi-file-earmark-check', authorities: ['VEN_COMPROBANTES_VER'] },
      { label: 'Resúmenes SUNAT', path: '/app/resumenes-sunat', icon: 'bi-cloud-check', authorities: ['VEN_COMPROBANTES_VER'] },
      { label: 'Diagnóstico SUNAT', path: '/app/diagnostico-sunat', icon: 'bi-shield-check', authorities: ['VEN_SUNAT_ENVIAR'] },
      { label: 'Reportes', path: '/app/reportes', icon: 'bi-bar-chart-line', authorities: ['REP_REPORTES_VER'] },
    ],
  },
  {
    label: 'Administración',
    items: [
      { label: 'Usuarios', path: '/app/usuarios', icon: 'bi-person-gear', authorities: ['SEG_USUARIOS_VER'] },
      { label: 'Roles', path: '/app/roles', icon: 'bi-shield-check', authorities: ['SEG_ROLES_VER'] },
      { label: 'Permisos', path: '/app/permisos', icon: 'bi-key', authorities: ['SEG_PERMISOS_VER'] },
    ],
  },
]

export const navigationItems = navigationGroups.flatMap((group) => group.items)
