import type {
  CatalogoOpciones,
  Categoria,
  EstadoCatalogo,
  Marca,
  Pagina,
  Producto,
  ProductoFiltros,
  ProductoGuardarRequest,
  UnidadMedida,
} from '../types/catalog'
import { api } from './api'

export async function listProducts(filters: ProductoFiltros): Promise<Pagina<Producto>> {
  const { data } = await api.get<Pagina<Producto>>('/v1/productos', {
    params: {
      buscar: filters.buscar || undefined,
      estado: filters.estado || undefined,
      idCategoria: filters.idCategoria || undefined,
      page: filters.page,
      size: filters.size,
    },
  })
  return data
}

export async function listCategories(estado?: EstadoCatalogo): Promise<Categoria[]> {
  const { data } = await api.get<Categoria[]>('/v1/categorias', { params: { estado } })
  return data
}

export async function getCatalogOptions(): Promise<CatalogoOpciones> {
  const [categorias, marcas, unidades] = await Promise.all([
    api.get<Categoria[]>('/v1/categorias', { params: { estado: 'ACTIVO' } }),
    api.get<Marca[]>('/v1/marcas', { params: { estado: 'ACTIVO' } }),
    api.get<UnidadMedida[]>('/v1/unidades-medida', { params: { estado: 'ACTIVO' } }),
  ])

  return {
    categorias: categorias.data,
    marcas: marcas.data,
    unidades: unidades.data,
  }
}

export async function createProduct(request: ProductoGuardarRequest): Promise<Producto> {
  const { data } = await api.post<Producto>('/v1/productos', request)
  return data
}

export async function updateProduct(id: number, request: ProductoGuardarRequest): Promise<Producto> {
  const updateRequest = {
    codigoInterno: request.codigoInterno,
    codigoBarras: request.codigoBarras,
    nombre: request.nombre,
    descripcion: request.descripcion,
    idCategoria: request.idCategoria,
    idMarca: request.idMarca,
    idUnidadBase: request.idUnidadBase,
    stockMinimo: request.stockMinimo,
  }
  const { data } = await api.put<Producto>(`/v1/productos/${id}`, updateRequest)
  return data
}

export async function changeProductStatus(id: number, estado: EstadoCatalogo): Promise<Producto> {
  const { data } = await api.patch<Producto>(`/v1/productos/${id}/estado`, { estado })
  return data
}
