import axios from 'axios'
import type { MetodoPago } from '../types/accounts'
import type { Caja, MovimientoCaja, MovimientoCajaRequest, PaginaMovimientosCaja, ResumenCaja, SesionCaja, TipoMovimientoCaja } from '../types/cash'
import { api } from './api'

export async function listCashRegisters(): Promise<Caja[]> { const { data } = await api.get<Caja[]>('/v1/cajas'); return data }
export async function listCashMethods(): Promise<MetodoPago[]> { const { data } = await api.get<MetodoPago[]>('/v1/cajas/metodos-pago'); return data }
export async function getActiveCashSession(idCaja: number): Promise<SesionCaja | null> { try { const { data } = await api.get<SesionCaja>(`/v1/cajas/${idCaja}/sesion-activa`); return data } catch (error) { if (axios.isAxiosError(error) && error.response?.status === 404) return null; throw error } }
export async function openCashSession(idCaja: number, saldoInicial: number): Promise<SesionCaja> { const { data } = await api.post<SesionCaja>(`/v1/cajas/${idCaja}/aperturas`, { saldoInicial }); return data }
export async function getCashSummary(idSesion: number): Promise<ResumenCaja> { const { data } = await api.get<ResumenCaja>(`/v1/sesiones-caja/${idSesion}/resumen`); return data }
export async function listCashMovements(idSesion: number, filters: { tipo: TipoMovimientoCaja | ''; desde: string; hasta: string; page: number; size: number }): Promise<PaginaMovimientosCaja> { const { data } = await api.get<PaginaMovimientosCaja>(`/v1/sesiones-caja/${idSesion}/movimientos`, { params: { tipo: filters.tipo || undefined, desde: filters.desde ? `${filters.desde}T00:00:00Z` : undefined, hasta: filters.hasta ? `${filters.hasta}T23:59:59Z` : undefined, page: filters.page, size: filters.size } }); return data }
export async function createCashMovement(idSesion: number, request: MovimientoCajaRequest): Promise<MovimientoCaja> { const { data } = await api.post<MovimientoCaja>(`/v1/sesiones-caja/${idSesion}/movimientos`, request); return data }
export async function closeCashSession(idSesion: number, saldoReal: number, observacion: string | null): Promise<SesionCaja> { const { data } = await api.post<SesionCaja>(`/v1/sesiones-caja/${idSesion}/cierre`, { saldoReal, observacion }); return data }
