import type { EstadoCatalogo, Pagina } from './catalog'

export interface Proveedor {
  id: number
  ruc: string
  razonSocial: string
  nombreComercial: string | null
  direccion: string | null
  telefono: string | null
  correo: string | null
  personaContacto: string | null
  estado: EstadoCatalogo
  fechaRegistro: string
  fechaActualizacion: string
}

export interface ProveedorGuardarRequest {
  ruc: string
  razonSocial: string
  nombreComercial: string | null
  direccion: string | null
  telefono: string | null
  correo: string | null
  personaContacto: string | null
}

export interface ProveedorCompra {
  idCompra: number
  tipoComprobante: string | null
  numeroComprobante: string | null
  fecha: string
  estado: string
  importeTotal: number
  saldoPendiente: number
}

export interface ProveedorHistorial {
  proveedor: Proveedor
  resumen: {
    totalCompras: number
    importeTotal: number
    saldoPendiente: number
    ultimaCompra: string | null
  }
  compras: ProveedorCompra[]
}

export interface ProveedorFiltros {
  buscar: string
  estado: EstadoCatalogo | ''
  page: number
  size: number
}

export type PaginaProveedores = Pagina<Proveedor>
