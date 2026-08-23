import type { MetodoPago } from '../types/accounts'
import type {
  CanalPedido,
  Cotizacion,
  CotizacionGuardarRequest,
  EstadoCotizacion,
  EstadoPedido,
  FiltrosComerciales,
  PaginaCotizaciones,
  PaginaPedidos,
  PaginaVentas,
  Pedido,
  PedidoGuardarRequest,
  PrecioProducto,
  Venta,
  VentaCrearRequest,
} from '../types/commercial'
import { api } from './api'

export async function listQuotes(filters: FiltrosComerciales): Promise<PaginaCotizaciones> { const { data } = await api.get<PaginaCotizaciones>('/v1/cotizaciones', { params: commercialParams(filters) }); return data }
export async function getQuote(id: number): Promise<Cotizacion> { const { data } = await api.get<Cotizacion>(`/v1/cotizaciones/${id}`); return data }
export async function createQuote(request: CotizacionGuardarRequest): Promise<Cotizacion> { const { data } = await api.post<Cotizacion>('/v1/cotizaciones', request); return data }
export async function updateQuote(id: number, request: CotizacionGuardarRequest): Promise<Cotizacion> { const { data } = await api.put<Cotizacion>(`/v1/cotizaciones/${id}`, request); return data }
export async function changeQuoteStatus(id: number, estado: EstadoCotizacion): Promise<Cotizacion> { const { data } = await api.patch<Cotizacion>(`/v1/cotizaciones/${id}/estado`, { estado }); return data }
export async function convertQuoteToOrder(id: number, request: { idSede: number | null; canal: CanalPedido; observacion: string | null }): Promise<Pedido> { const { data } = await api.post<Pedido>(`/v1/cotizaciones/${id}/convertir-pedido`, request); return data }

export async function listOrders(filters: FiltrosComerciales & { canal?: CanalPedido | '' }): Promise<PaginaPedidos> { const { data } = await api.get<PaginaPedidos>('/v1/pedidos', { params: { ...commercialParams(filters), canal: filters.canal || undefined } }); return data }
export async function getOrder(id: number): Promise<Pedido> { const { data } = await api.get<Pedido>(`/v1/pedidos/${id}`); return data }
export async function createOrder(request: PedidoGuardarRequest): Promise<Pedido> { const { data } = await api.post<Pedido>('/v1/pedidos', request); return data }
export async function confirmOrder(id: number): Promise<Pedido> { const { data } = await api.post<Pedido>(`/v1/pedidos/${id}/confirmar`); return data }
export async function cancelOrder(id: number): Promise<Pedido> { const { data } = await api.post<Pedido>(`/v1/pedidos/${id}/cancelar`); return data }
export async function changeOrderStatus(id: number, estado: EstadoPedido): Promise<Pedido> { const { data } = await api.patch<Pedido>(`/v1/pedidos/${id}/estado`, { estado }); return data }

export async function listSales(filters: FiltrosComerciales & { condicionPago?: string }): Promise<PaginaVentas> { const { data } = await api.get<PaginaVentas>('/v1/ventas', { params: { ...commercialParams(filters), condicionPago: filters.condicionPago || undefined } }); return data }
export async function getSale(id: number): Promise<Venta> { const { data } = await api.get<Venta>(`/v1/ventas/${id}`); return data }
export async function createSale(request: VentaCrearRequest): Promise<Venta> { const { data } = await api.post<Venta>('/v1/ventas', request); return data }
export async function annulSale(id: number, motivo: string): Promise<Venta> { const { data } = await api.post<Venta>(`/v1/ventas/${id}/anular`, { motivo }); return data }
export async function listSaleMethods(): Promise<MetodoPago[]> { const { data } = await api.get<MetodoPago[]>('/v1/ventas/metodos-pago'); return data }
export async function listProductPrices(id: number): Promise<PrecioProducto[]> { const { data } = await api.get<PrecioProducto[]>(`/v1/productos/${id}/precios`); return data }

function commercialParams(filters: FiltrosComerciales) {
  return { idCliente: filters.idCliente || undefined, estado: filters.estado || undefined, desde: filters.desde || undefined, hasta: filters.hasta || undefined, page: filters.page, size: filters.size }
}
