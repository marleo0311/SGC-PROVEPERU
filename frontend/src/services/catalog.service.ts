import type {
  CatalogoOpciones,
  Categoria,
  CategoriaGuardarRequest,
  EstadoCatalogo,
  Marca,
  MarcaGuardarRequest,
  Pagina,
  PresentacionProducto,
  PresentacionProductoGuardarRequest,
  PrecioProducto,
  Producto,
  ProductoFiltros,
  ProductoGuardarRequest,
  UnidadMedida,
  UnidadMedidaGuardarRequest,
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

export async function getProduct(id: number): Promise<Producto> {
  const { data } = await api.get<Producto>(`/v1/productos/${id}`)
  return data
}

export async function listProductPrices(id: number): Promise<PrecioProducto[]> {
  const { data } = await api.get<PrecioProducto[]>(`/v1/productos/${id}/precios`)
  return data
}

export async function listProductPresentations(id: number): Promise<PresentacionProducto[]> {
  const { data } = await api.get<PresentacionProducto[]>(`/v1/productos/${id}/presentaciones`)
  return data
}

export async function createProductPresentation(
  idProducto: number,
  request: PresentacionProductoGuardarRequest,
): Promise<PresentacionProducto> {
  const { data } = await api.post<PresentacionProducto>(
    `/v1/productos/${idProducto}/presentaciones`, request,
  )
  return data
}

export async function updateProductPresentation(
  idProducto: number,
  id: number,
  request: PresentacionProductoGuardarRequest,
): Promise<PresentacionProducto> {
  const { data } = await api.put<PresentacionProducto>(
    `/v1/productos/${idProducto}/presentaciones/${id}`, request,
  )
  return data
}

export async function changeProductPresentationStatus(
  idProducto: number,
  id: number,
  estado: EstadoCatalogo,
): Promise<PresentacionProducto> {
  const { data } = await api.patch<PresentacionProducto>(
    `/v1/productos/${idProducto}/presentaciones/${id}/estado`, { estado },
  )
  return data
}

export async function listCategories(estado?: EstadoCatalogo, buscar = ''): Promise<Categoria[]> {
  const { data } = await api.get<Categoria[]>('/v1/categorias', {
    params: { estado, buscar: buscar || undefined },
  })
  return data
}

export async function createCategory(request: CategoriaGuardarRequest): Promise<Categoria> {
  const { data } = await api.post<Categoria>('/v1/categorias', request)
  return data
}

export async function updateCategory(id: number, request: CategoriaGuardarRequest): Promise<Categoria> {
  const { data } = await api.put<Categoria>(`/v1/categorias/${id}`, request)
  return data
}

export async function changeCategoryStatus(id: number, estado: EstadoCatalogo): Promise<Categoria> {
  const { data } = await api.patch<Categoria>(`/v1/categorias/${id}/estado`, { estado })
  return data
}

export async function listBrands(estado?: EstadoCatalogo, buscar = ''): Promise<Marca[]> {
  const { data } = await api.get<Marca[]>('/v1/marcas', {
    params: { estado, buscar: buscar || undefined },
  })
  return data
}

export async function createBrand(request: MarcaGuardarRequest): Promise<Marca> {
  const { data } = await api.post<Marca>('/v1/marcas', request)
  return data
}

export async function updateBrand(
  brand: Marca,
  request: MarcaGuardarRequest,
  estado: EstadoCatalogo = brand.estado,
): Promise<Marca> {
  const { data } = await api.put<Marca>(`/v1/marcas/${brand.id}`, { ...request, estado })
  return data
}

export async function changeBrandStatus(brand: Marca, estado: EstadoCatalogo): Promise<Marca> {
  return updateBrand(brand, { nombre: brand.nombre }, estado)
}

export async function listUnits(estado?: EstadoCatalogo, buscar = ''): Promise<UnidadMedida[]> {
  const { data } = await api.get<UnidadMedida[]>('/v1/unidades-medida', {
    params: { estado, buscar: buscar || undefined },
  })
  return data
}

export async function createUnit(request: UnidadMedidaGuardarRequest): Promise<UnidadMedida> {
  const { data } = await api.post<UnidadMedida>('/v1/unidades-medida', request)
  return data
}

export async function updateUnit(
  unit: UnidadMedida,
  request: UnidadMedidaGuardarRequest,
  estado: EstadoCatalogo = unit.estado,
): Promise<UnidadMedida> {
  const { data } = await api.put<UnidadMedida>(`/v1/unidades-medida/${unit.id}`, {
    ...request,
    estado,
  })
  return data
}

export async function changeUnitStatus(
  unit: UnidadMedida,
  estado: EstadoCatalogo,
): Promise<UnidadMedida> {
  return updateUnit(unit, {
    codigo: unit.codigo,
    nombre: unit.nombre,
    codigoSunat: unit.codigoSunat,
    permiteDecimales: unit.permiteDecimales,
  }, estado)
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
  const { data } = await api.put<Producto>(`/v1/productos/${id}`, request)
  return data
}

export async function changeProductStatus(id: number, estado: EstadoCatalogo): Promise<Producto> {
  const { data } = await api.patch<Producto>(`/v1/productos/${id}/estado`, { estado })
  return data
}
