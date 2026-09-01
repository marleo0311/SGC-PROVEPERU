export type EstadoCatalogo = 'ACTIVO' | 'INACTIVO'

export interface Categoria {
  id: number
  nombre: string
  descripcion: string | null
  estado: EstadoCatalogo
}

export interface CategoriaGuardarRequest {
  nombre: string
  descripcion: string | null
}

export interface Marca {
  id: number
  nombre: string
  estado: EstadoCatalogo
}

export interface MarcaGuardarRequest {
  nombre: string
}

export interface UnidadMedida {
  id: number
  codigo: string
  nombre: string
  codigoSunat: string
  permiteDecimales: boolean
  estado: EstadoCatalogo
}

export interface UnidadMedidaGuardarRequest {
  codigo: string
  nombre: string
  codigoSunat: string
  permiteDecimales: boolean
}

export interface Producto {
  id: number
  codigoInterno: string
  codigoBarras: string | null
  nombre: string
  descripcion: string | null
  stockMinimo: number
  estado: EstadoCatalogo
  categoria: Categoria
  marca: Marca | null
  unidadBase: UnidadMedida
  fechaRegistro: string
}

export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
  ultima: boolean
}

export interface ProductoFiltros {
  buscar: string
  estado: EstadoCatalogo | ''
  idCategoria: number | ''
  page: number
  size: number
}

export interface ProductoGuardarRequest {
  codigoInterno: string
  codigoBarras: string | null
  nombre: string
  descripcion: string | null
  idCategoria: number
  idMarca: number | null
  idUnidadBase: number
  stockMinimo: number
  precioMinorista?: number | null
  precioMayorista?: number | null
}

export interface PrecioProducto {
  id: number
  idProducto: number
  tipoPrecio: string
  monto: number
  vigenteDesde: string
  vigenteHasta: string | null
  estado: string
}

export interface PresentacionProducto {
  id: number
  idProducto: number
  producto: string
  unidadPresentacion: UnidadMedida
  unidadBase: UnidadMedida
  nombre: string
  contenidoVariable: boolean
  contenidoBasePredeterminado: number | null
  precioMinorista: number | null
  precioMayorista: number | null
  estado: EstadoCatalogo
}

export interface PresentacionProductoGuardarRequest {
  nombre: string
  idUnidadMedida: number
  contenidoVariable: boolean
  contenidoBasePredeterminado: number | null
  precioMinorista: number | null
  precioMayorista: number | null
}

export interface CatalogoOpciones {
  categorias: Categoria[]
  marcas: Marca[]
  unidades: UnidadMedida[]
}
