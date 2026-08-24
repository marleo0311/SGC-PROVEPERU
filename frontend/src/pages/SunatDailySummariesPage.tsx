import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { getApiErrorMessage } from '../services/api'
import {
  checkDailySummary,
  checkReceiptVoid,
  downloadDailySummaryFile,
  downloadReceiptVoidFile,
  getSunatConfiguration,
  listDailySummaries,
  listReceiptVoids,
  prepareDailySummaries,
  sendDailySummary,
  sendReceiptVoid,
} from '../services/commercial.service'
import type { ComunicacionBajaSunat, ConfiguracionSunat, EstadoResumenDiarioSunat, ResumenDiarioSunat } from '../types/commercial'

const currency = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const dateTime = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

export function SunatDailySummariesPage() {
  const [fecha, setFecha] = useState(todayInLima())
  const [summaries, setSummaries] = useState<ResumenDiarioSunat[]>([])
  const [voids, setVoids] = useState<ComunicacionBajaSunat[]>([])
  const [config, setConfig] = useState<ConfiguracionSunat | null>(null)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const { hasAnyAuthority } = useAuth()
  const canManage = hasAnyAuthority('VEN_SUNAT_RESUMENES_GESTIONAR')
  const canManageVoids = hasAnyAuthority('VEN_SUNAT_BAJAS_GESTIONAR')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [items, configuration, voidItems] = await Promise.all([
        listDailySummaries(fecha),
        getSunatConfiguration(),
        listReceiptVoids(),
      ])
      setSummaries(items)
      setConfig(configuration)
      setVoids(voidItems)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [fecha])

  useEffect(() => {
    let active = true
    Promise.all([listDailySummaries(fecha), getSunatConfiguration(), listReceiptVoids()])
      .then(([items, configuration, voidItems]) => {
        if (!active) return
        setSummaries(items)
        setConfig(configuration)
        setVoids(voidItems)
      })
      .catch((requestError: unknown) => {
        if (active) setError(getApiErrorMessage(requestError))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [fecha])

  const totals = useMemo(() => ({
    summaries: summaries.length,
    receipts: summaries.reduce((sum, item) => sum + item.boletas.length, 0),
    amount: summaries.reduce((sum, item) => sum + item.total, 0),
    pending: summaries.filter((item) => !isAccepted(item.estado)).length,
  }), [summaries])

  async function prepare() {
    setWorking('prepare')
    setError('')
    setNotice('')
    try {
      const created = await prepareDailySummaries(fecha)
      setNotice(`${created.length} resumen(es) generado(s) con ${created.reduce((sum, item) => sum + item.boletas.length, 0)} boleta(s).`)
      await load()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setWorking(null)
    }
  }

  async function act(summary: ResumenDiarioSunat, action: 'send' | 'check') {
    setWorking(`${action}-${summary.id}`)
    setError('')
    setNotice('')
    try {
      const updated = action === 'send'
        ? await sendDailySummary(summary.id)
        : await checkDailySummary(summary.id)
      setSummaries((current) => current.map((item) => item.id === updated.id ? updated : item))
      setNotice(action === 'send'
        ? `SUNAT recibió el resumen y asignó el ticket ${updated.ticket}.`
        : statusMessage(updated))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
      await load()
    } finally {
      setWorking(null)
    }
  }

  async function download(summary: ResumenDiarioSunat, kind: 'xml' | 'cdr') {
    setWorking(`${kind}-${summary.id}`)
    setError('')
    try {
      const blob = await downloadDailySummaryFile(summary.id, kind)
      saveBlob(blob, kind === 'xml' ? `${summary.nombreArchivo}.xml` : `R-${summary.nombreArchivo}.zip`)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setWorking(null)
    }
  }

  async function actVoid(item: ComunicacionBajaSunat, action: 'send' | 'check') {
    if (!item.id) return
    setWorking(`void-${action}-${item.id}`)
    setError('')
    setNotice('')
    try {
      const updated = action === 'send' ? await sendReceiptVoid(item.id) : await checkReceiptVoid(item.id)
      setVoids((current) => current.map((value) => value.id === updated.id ? updated : value))
      setNotice(action === 'send' ? `SUNAT recibió la baja y asignó el ticket ${updated.ticket}.` : `La baja quedó en estado ${friendly(updated.estado)}.`)
    } catch (requestError) { setError(getApiErrorMessage(requestError)); await load() } finally { setWorking(null) }
  }

  async function downloadVoid(item: ComunicacionBajaSunat, kind: 'xml' | 'cdr') {
    if (!item.id) return
    setWorking(`void-${kind}-${item.id}`)
    setError('')
    try {
      const blob = await downloadReceiptVoidFile(item.id, kind)
      saveBlob(blob, kind === 'xml' ? `${item.nombreArchivo}.xml` : `R-${item.nombreArchivo}.zip`)
    } catch (requestError) { setError(getApiErrorMessage(requestError)) } finally { setWorking(null) }
  }

  const ready = Boolean(config?.habilitado && config.certificadoConfigurado && config.credencialesConfiguradas)
  return (
    <section className="daily-summary-page">
      <header className="page-header daily-summary-header">
        <div>
          <span className="eyebrow">Facturación electrónica</span>
          <h1>Resúmenes diarios SUNAT</h1>
          <p>Agrupa las boletas emitidas, envía el resumen y consulta su ticket hasta obtener el CDR.</p>
        </div>
        <div className="page-header__actions">
          <span className={`daily-summary-environment ${config?.ambiente === 'PRODUCCION' ? 'production' : ''}`}>
            <i className="bi bi-cloud-check" /> SUNAT {config?.ambiente ?? '—'}
          </span>
          <button className="secondary-button" type="button" onClick={() => void load()} disabled={loading}>
            <i className={`bi bi-arrow-clockwise ${loading ? 'inventory-spin' : ''}`} /> Actualizar
          </button>
        </div>
      </header>

      <section className="daily-summary-toolbar">
        <label>
          <span>Fecha de emisión de las boletas</span>
          <div><i className="bi bi-calendar3" /><input type="date" value={fecha} max={todayInLima()} onChange={(event) => { setLoading(true); setError(''); setFecha(event.target.value) }} /></div>
        </label>
        <div>
          <small>El resumen usa el identificador RC-AAAAMMDD-correlativo y admite hasta 500 boletas por archivo.</small>
          {canManage && <button className="primary-button primary-button--inline" type="button" disabled={Boolean(working) || !ready} onClick={() => void prepare()}>
            {working === 'prepare' ? <span className="spinner-border spinner-border-sm" /> : <i className="bi bi-file-earmark-plus" />} Generar resumen
          </button>}
        </div>
      </section>

      {!ready && <div className="daily-summary-alert warning"><i className="bi bi-shield-exclamation" /><span>{config?.advertencia ?? 'La configuración SUNAT no está disponible.'} Verifica certificado, credenciales y habilitación antes de enviar.</span></div>}
      {error && <div className="daily-summary-alert error"><i className="bi bi-exclamation-octagon" /><span>{error}</span></div>}
      {notice && <div className="daily-summary-alert success"><i className="bi bi-check-circle" /><span>{notice}</span></div>}

      <section className="daily-summary-metrics">
        <SummaryMetric icon="bi-files" label="Resúmenes" value={totals.summaries} tone="blue" />
        <SummaryMetric icon="bi-receipt" label="Boletas agrupadas" value={totals.receipts} tone="violet" />
        <SummaryMetric icon="bi-cash-stack" label="Total resumido" value={currency.format(totals.amount)} tone="teal" />
        <SummaryMetric icon="bi-hourglass-split" label="Por completar" value={totals.pending} tone="amber" />
      </section>

      <section className="daily-summary-list">
        {loading && summaries.length === 0 ? <SummarySkeleton /> : summaries.length === 0 ? (
          <div className="daily-summary-empty"><span><i className="bi bi-cloud-arrow-up" /></span><h2>No hay resúmenes para esta fecha</h2><p>Genera uno cuando existan boletas pendientes de envío.</p></div>
        ) : summaries.map((summary) => (
          <SummaryCard
            key={summary.id}
            summary={summary}
            canManage={canManage}
            ready={ready}
            working={working}
            onSend={() => void act(summary, 'send')}
            onCheck={() => void act(summary, 'check')}
            onDownload={(kind) => void download(summary, kind)}
          />
        ))}
      </section>

      <header className="daily-summary-section-title">
        <div><span><i className="bi bi-cloud-slash" /></span><span><h2>Comunicaciones de baja</h2><p>Facturas anuladas mediante archivo RA, ticket y CDR de SUNAT.</p></span></div>
        <strong>{voids.length}</strong>
      </header>
      <section className="daily-summary-list">
        {loading && voids.length === 0 ? <SummarySkeleton /> : voids.length === 0 ? <div className="daily-summary-empty"><span><i className="bi bi-file-earmark-x" /></span><h2>No hay comunicaciones de baja</h2><p>Se generan desde el detalle de una factura aceptada.</p></div> : voids.map((item) => <VoidCard key={item.id} item={item} canManage={canManageVoids} ready={ready} working={working} onSend={() => void actVoid(item, 'send')} onCheck={() => void actVoid(item, 'check')} onDownload={(kind) => void downloadVoid(item, kind)} />)}
      </section>
    </section>
  )
}

function VoidCard({ item, canManage, ready, working, onSend, onCheck, onDownload }: { item: ComunicacionBajaSunat; canManage: boolean; ready: boolean; working: string | null; onSend: () => void; onCheck: () => void; onDownload: (kind: 'xml' | 'cdr') => void }) {
  const canSend = !item.ticket && ['GENERADO', 'RECHAZADO', 'ERROR_COMUNICACION'].includes(item.estado)
  const canCheck = Boolean(item.ticket) && !isAccepted(item.estado) && item.estado !== 'RECHAZADO'
  return <article className={`daily-summary-card ${isAccepted(item.estado) ? 'accepted' : ''}`}><header><div><span><i className="bi bi-file-earmark-x" /></span><span><small>{item.ambiente} · {item.fechaDocumento}</small><h2>{item.nombreArchivo ?? item.comprobante}</h2></span></div><StatusBadge status={item.estado} /></header><div className="daily-summary-card__facts"><span><small>Comprobante</small><strong>{item.comprobante}</strong></span><span><small>Motivo</small><strong>{item.motivo}</strong></span><span><small>Ticket</small><strong>{item.ticket ?? 'Pendiente'}</strong></span><span><small>Consultas</small><strong>{item.consultasEstado}</strong></span></div>{(item.descripcionRespuesta || item.errorUltimo) && <div className={`daily-summary-result ${item.errorUltimo ? 'error' : ''}`}><small>{item.errorUltimo ? 'Último error' : 'Respuesta SUNAT'}</small><strong>{item.errorUltimo ?? item.descripcionRespuesta}</strong></div>}<footer><span>{item.fechaGeneracion ? `Generada ${item.fechaGeneracion}` : 'Pendiente'}</span><div>{canManage && canSend && <button className="primary" type="button" disabled={Boolean(working) || !ready} onClick={onSend}><i className="bi bi-send-check" /> Enviar baja</button>}{canManage && canCheck && <button className="primary" type="button" disabled={Boolean(working) || !ready} onClick={onCheck}><i className="bi bi-arrow-repeat" /> Consultar ticket</button>}{item.xmlDisponible && <button type="button" disabled={Boolean(working)} onClick={() => onDownload('xml')}><i className="bi bi-download" /> XML</button>}{item.cdrDisponible && <button type="button" disabled={Boolean(working)} onClick={() => onDownload('cdr')}><i className="bi bi-file-earmark-check" /> CDR</button>}</div></footer></article>
}

function SummaryCard({ summary, canManage, ready, working, onSend, onCheck, onDownload }: {
  summary: ResumenDiarioSunat
  canManage: boolean
  ready: boolean
  working: string | null
  onSend: () => void
  onCheck: () => void
  onDownload: (kind: 'xml' | 'cdr') => void
}) {
  const canSend = !summary.ticket && ['GENERADO', 'RECHAZADO', 'ERROR_COMUNICACION'].includes(summary.estado)
  const canCheck = Boolean(summary.ticket) && !isAccepted(summary.estado) && summary.estado !== 'RECHAZADO'
  return (
    <article className={`daily-summary-card ${isAccepted(summary.estado) ? 'accepted' : ''}`}>
      <header>
        <div><span><i className="bi bi-file-earmark-zip" /></span><span><small>{summary.ambiente} · {summary.fechaDocumentos}</small><h2>{summary.nombreArchivo}</h2></span></div>
        <StatusBadge status={summary.estado} />
      </header>
      <div className="daily-summary-card__facts">
        <span><small>Boletas</small><strong>{summary.boletas.length}</strong></span>
        <span><small>Total</small><strong>{currency.format(summary.total)}</strong></span>
        <span><small>Ticket</small><strong>{summary.ticket ?? 'Pendiente'}</strong></span>
        <span><small>Consultas</small><strong>{summary.consultasEstado}</strong></span>
      </div>
      {(summary.descripcionRespuesta || summary.errorUltimo || summary.observaciones.length > 0) && <div className={`daily-summary-result ${summary.errorUltimo ? 'error' : ''}`}>
        <small>{summary.errorUltimo ? 'Último error' : 'Respuesta SUNAT'}</small>
        <strong>{summary.errorUltimo ?? summary.descripcionRespuesta}</strong>
        {summary.observaciones.length > 0 && <ul>{summary.observaciones.map((item) => <li key={item}>{item}</li>)}</ul>}
      </div>}
      <div className="daily-summary-receipts">
        {summary.boletas.map((receipt) => <span key={receipt.id}><strong>{receipt.numero}</strong><small>{dateTime.format(new Date(receipt.fechaEmision))} · {currency.format(receipt.total)}</small></span>)}
      </div>
      <footer>
        <span>Generado {dateTime.format(new Date(summary.fechaCreacion))}</span>
        <div>
          {canManage && canSend && <button className="primary" type="button" disabled={Boolean(working) || !ready} onClick={onSend}><i className="bi bi-send-check" /> Enviar y obtener ticket</button>}
          {canManage && canCheck && <button className="primary" type="button" disabled={Boolean(working) || !ready} onClick={onCheck}><i className="bi bi-arrow-repeat" /> Consultar ticket</button>}
          {summary.xmlDisponible && <button type="button" disabled={Boolean(working)} onClick={() => onDownload('xml')}><i className="bi bi-download" /> XML</button>}
          {summary.cdrDisponible && <button type="button" disabled={Boolean(working)} onClick={() => onDownload('cdr')}><i className="bi bi-file-earmark-check" /> CDR</button>}
        </div>
      </footer>
    </article>
  )
}

function StatusBadge({ status }: { status: EstadoResumenDiarioSunat }) {
  const icon = isAccepted(status) ? 'bi-check-circle-fill' : ['RECHAZADO', 'ERROR_COMUNICACION'].includes(status) ? 'bi-exclamation-octagon-fill' : status === 'PROCESANDO' ? 'bi-arrow-repeat' : 'bi-clock-fill'
  return <span className={`daily-summary-status ${status.toLowerCase()}`}><i className={`bi ${icon}`} /> {friendly(status)}</span>
}

function SummaryMetric({ icon, label, value, tone }: { icon: string; label: string; value: string | number; tone: string }) {
  return <article><span className={`tone-${tone}`}><i className={`bi ${icon}`} /></span><span><small>{label}</small><strong>{value}</strong></span></article>
}

function SummarySkeleton() {
  return <div className="daily-summary-card"><div className="skeleton skeleton--header" /><div className="skeleton skeleton--panel" /></div>
}

function isAccepted(status: EstadoResumenDiarioSunat) {
  return status === 'ACEPTADO' || status === 'ACEPTADO_CON_OBSERVACIONES'
}

function statusMessage(summary: ResumenDiarioSunat) {
  return summary.estado === 'PROCESANDO'
    ? 'SUNAT todavía está procesando el ticket. Puedes consultarlo nuevamente.'
    : `SUNAT completó el resumen con estado ${friendly(summary.estado)}.`
}

function friendly(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\S/g, (letter) => letter.toUpperCase())
}

function todayInLima() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
