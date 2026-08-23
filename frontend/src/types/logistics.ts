import type { EstadoCatalogo, Pagina } from './catalog'

export type TipoDocumentoTransportista = 'DNI' | 'RUC'
export type TipoGasto = 'TRANSPORTE' | 'CARGA' | 'DESCARGA' | 'MOVILIDAD' | 'OTRO'

export interface Transportista {
  id: number
  tipoDocumento: TipoDocumentoTransportista | null
  numeroDocumento: string | null
  nombreRazonSocial: string
  empresaTransporte: string | null
  telefono: string | null
  direccion: string | null
  estado: EstadoCatalogo
  fechaRegistro: string
  fechaActualizacion: string
}

export interface TransportistaGuardarRequest {
  tipoDocumento: TipoDocumentoTransportista | null
  numeroDocumento: string | null
  nombreRazonSocial: string
  empresaTransporte: string | null
  telefono: string | null
  direccion: string | null
}

export interface TransportistaFiltros {
  buscar: string
  estado: EstadoCatalogo | ''
  page: number
  size: number
}

export interface Gasto {
  id: number
  idCompra: number | null
  idTransportista: number | null
  transportista: string | null
  idUsuario: number
  usuarioLogin: string
  tipoGasto: TipoGasto
  descripcion: string | null
  importe: number
  fecha: string
  numeroComprobante: string | null
  fechaRegistro: string
}

export interface GastoCrearRequest {
  idTransportista: number | null
  tipoGasto: TipoGasto
  descripcion: string | null
  importe: number
  fecha: string
  numeroComprobante: string | null
}

export interface GastoFiltros {
  idTransportista: number | ''
  tipoGasto: TipoGasto | ''
  desde: string
  hasta: string
  page: number
  size: number
}

export type PaginaTransportistas = Pagina<Transportista>
export type PaginaGastos = Pagina<Gasto>
