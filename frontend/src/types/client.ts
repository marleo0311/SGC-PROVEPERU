import type { EstadoCatalogo, Pagina } from './catalog'

export type TipoPersona = 'NATURAL' | 'JURIDICA'
export type TipoDocumentoCliente = 'DNI' | 'RUC'

export interface Cliente {
  id: number
  tipoPersona: TipoPersona
  tipoDocumento: TipoDocumentoCliente
  numeroDocumento: string
  nombres: string | null
  apellidos: string | null
  razonSocial: string | null
  nombreComercial: string | null
  nombreMostrar: string
  direccion: string | null
  telefono: string | null
  whatsapp: string | null
  correo: string | null
  permiteCredito: boolean
  estado: EstadoCatalogo
  fechaRegistro: string
  fechaActualizacion: string
}

export interface ClienteFiltros {
  buscar: string
  estado: EstadoCatalogo | ''
  tipoPersona: TipoPersona | ''
  permiteCredito: boolean | ''
  page: number
  size: number
}

export interface ClienteGuardarRequest {
  tipoPersona: TipoPersona
  tipoDocumento: TipoDocumentoCliente
  numeroDocumento: string
  nombres: string | null
  apellidos: string | null
  razonSocial: string | null
  nombreComercial: string | null
  direccion: string | null
  telefono: string | null
  whatsapp: string | null
  correo: string | null
  permiteCredito: boolean
}

export interface ConsultaDocumentoCliente {
  encontrado: boolean
  origen: 'LOCAL' | 'EXTERNO' | 'NO_ENCONTRADO' | 'NO_CONFIGURADO'
  consultaExternaHabilitada: boolean
  idCliente: number | null
  estadoCliente: EstadoCatalogo | null
  tipoPersona: TipoPersona
  tipoDocumento: TipoDocumentoCliente
  numeroDocumento: string
  nombres: string | null
  apellidos: string | null
  razonSocial: string | null
  nombreComercial: string | null
  nombreMostrar: string | null
  direccion: string | null
  estadoContribuyente: string | null
  condicionDomicilio: string | null
  mensaje: string
}

export interface ClienteOperacion {
  tipoOperacion: string
  idOperacion: number
  referencia: string
  estado: string
  importe: number
  fechaHora: string
}

export interface ClientePrecioEspecial {
  id: number
  idCliente: number
  idProducto: number
  codigoProducto: string
  nombreProducto: string
  precio: number
  vigenteDesde: string
  vigenteHasta: string | null
  estado: EstadoCatalogo
  fechaRegistro: string
}

export interface ClienteHistorial {
  cliente: Cliente
  resumen: {
    totalOperaciones: number
    importeTotal: number
    saldoPendiente: number
    ultimaOperacion: string | null
  }
  operaciones: ClienteOperacion[]
  preciosEspeciales: ClientePrecioEspecial[]
}

export interface ClientePrecioEspecialRequest {
  idProducto: number
  precio: number
  vigenteDesde: string
  vigenteHasta: string | null
}

export type PaginaClientes = Pagina<Cliente>
