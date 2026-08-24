import { useCallback, useEffect, useState } from 'react'
import { getApiErrorMessage } from '../services/api'
import { getSunatProductionDiagnostics } from '../services/commercial.service'
import type {
  DiagnosticoSunat,
  EstadoVerificacionSunat,
  SerieDiagnosticoSunat,
} from '../types/commercial'

const dateTime = new Intl.DateTimeFormat('es-PE', {
  dateStyle: 'medium',
  timeStyle: 'medium',
})

export function SunatDiagnosticsPage() {
  const [diagnostic, setDiagnostic] = useState<DiagnosticoSunat | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setDiagnostic(await getSunatProductionDiagnostics())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    getSunatProductionDiagnostics()
      .then((response) => {
        if (active) setDiagnostic(response)
      })
      .catch((requestError: unknown) => {
        if (active) setError(getApiErrorMessage(requestError))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  return (
    <section className="sunat-diagnostic-page">
      <header className="page-header sunat-diagnostic-header">
        <div>
          <span className="eyebrow">Control fiscal seguro</span>
          <h1>Diagnóstico SUNAT Producción</h1>
          <p>Comprueba la preparación técnica sin firmar, enviar ni consumir correlativos reales.</p>
        </div>
        <div className="page-header__actions">
          <span className="sunat-diagnostic-safe"><i className="bi bi-lock" /> Solo lectura</span>
          <button className="secondary-button" type="button" onClick={() => void load()} disabled={loading}>
            <i className={`bi bi-arrow-clockwise ${loading ? 'inventory-spin' : ''}`} /> Actualizar diagnóstico
          </button>
        </div>
      </header>

      {error && <div className="sunat-diagnostic-message error"><i className="bi bi-exclamation-octagon" /><span>{error}</span></div>}
      {loading && !diagnostic ? <DiagnosticSkeleton /> : diagnostic && <>
        <ReadinessHero diagnostic={diagnostic} />

        <section className="sunat-diagnostic-metrics" aria-label="Resumen del diagnóstico">
          <DiagnosticMetric icon="bi-check-circle" label="Aprobados" value={diagnostic.aprobados} tone="success" />
          <DiagnosticMetric icon="bi-exclamation-triangle" label="Advertencias" value={diagnostic.advertencias} tone="warning" />
          <DiagnosticMetric icon="bi-x-octagon" label="Bloqueos" value={diagnostic.bloqueos} tone="danger" />
          <DiagnosticMetric icon="bi-hdd-network" label="Ambiente" value={diagnostic.ambiente} tone="info" />
        </section>

        <section className="sunat-diagnostic-grid">
          <div className="sunat-diagnostic-checks">
            <header className="sunat-diagnostic-section-title">
              <span><i className="bi bi-clipboard2-check" /></span>
              <div><h2>Verificaciones de preparación</h2><p>Cada control explica el resultado y la acción necesaria.</p></div>
            </header>
            <div className="sunat-diagnostic-check-list">
              {diagnostic.verificaciones.map((check) => <DiagnosticCheck key={check.codigo} check={check} />)}
            </div>
          </div>

          <aside className="sunat-diagnostic-certificate">
            <header><span><i className="bi bi-patch-check" /></span><div><small>Identidad de firma</small><h2>Certificado digital</h2></div></header>
            <dl>
              <CertificateFact label="Estado" value={diagnostic.certificado.valido ? 'Vigente' : 'Revisar'} ok={diagnostic.certificado.valido} />
              <CertificateFact label="Clave privada" value={diagnostic.certificado.contieneClavePrivada ? 'Disponible' : 'No disponible'} ok={diagnostic.certificado.contieneClavePrivada} />
              <CertificateFact label="RUC identificado" value={diagnostic.certificado.rucCoincide ? 'Confirmado' : 'Verificación manual'} ok={diagnostic.certificado.rucCoincide} />
            </dl>
            <div className="sunat-diagnostic-certificate__identity">
              <span><small>Titular</small><strong>{diagnostic.certificado.titular ?? 'No disponible'}</strong></span>
              <span><small>Entidad emisora</small><strong>{diagnostic.certificado.emisor ?? 'No disponible'}</strong></span>
              <span><small>Vigencia</small><strong>{dateRange(diagnostic.certificado.validoDesde, diagnostic.certificado.validoHasta)}</strong></span>
            </div>
            <p><i className="bi bi-shield-lock" /> Las contraseñas, la ruta del P12 y la clave SOL nunca se muestran en esta pantalla.</p>
          </aside>
        </section>

        <SeriesPanel series={diagnostic.series} />
      </>}
    </section>
  )
}

function ReadinessHero({ diagnostic }: { diagnostic: DiagnosticoSunat }) {
  const state = diagnostic.emisionRealHabilitada ? 'enabled' : diagnostic.listoParaPiloto ? 'ready' : 'blocked'
  const copy = diagnostic.emisionRealHabilitada
    ? { icon: 'bi-unlock', eyebrow: 'Atención', title: 'La emisión real está habilitada', text: 'Toda nueva boleta o factura utilizará correlativos tributarios de producción. Continúa únicamente durante una operación real supervisada.' }
    : diagnostic.listoParaPiloto
      ? { icon: 'bi-shield-check', eyebrow: 'Preparación completada', title: 'Listo para un piloto supervisado', text: 'No hay bloqueos técnicos. La emisión real permanece protegida hasta activar explícitamente SUNAT_PRODUCTION_ENABLED.' }
      : { icon: 'bi-shield-x', eyebrow: 'Acción requerida', title: 'Todavía no debe habilitarse producción', text: 'Corrige todos los controles marcados como bloqueo y actualiza nuevamente este diagnóstico.' }
  return <section className={`sunat-readiness-hero ${state}`}>
    <span className="sunat-readiness-hero__icon"><i className={`bi ${copy.icon}`} /></span>
    <div><small>{copy.eyebrow}</small><h2>{copy.title}</h2><p>{copy.text}</p></div>
    <span className="sunat-readiness-hero__stamp"><i className="bi bi-clock-history" /> {dateTime.format(new Date(diagnostic.generadoEn))}</span>
  </section>
}

function DiagnosticMetric({ icon, label, value, tone }: { icon: string; label: string; value: string | number; tone: string }) {
  return <article className={`sunat-diagnostic-metric ${tone}`}><span><i className={`bi ${icon}`} /></span><div><small>{label}</small><strong>{value}</strong></div></article>
}

function DiagnosticCheck({ check }: { check: DiagnosticoSunat['verificaciones'][number] }) {
  const icon: Record<EstadoVerificacionSunat, string> = { APROBADO: 'bi-check-lg', ADVERTENCIA: 'bi-exclamation-lg', BLOQUEO: 'bi-x-lg' }
  return <article className={`sunat-diagnostic-check ${check.estado.toLowerCase()}`}>
    <span className="sunat-diagnostic-check__status"><i className={`bi ${icon[check.estado]}`} /></span>
    <div><header><h3>{check.nombre}</h3><small>{friendly(check.estado)}</small></header><p>{check.detalle}</p>{check.accion && <span className="sunat-diagnostic-check__action"><i className="bi bi-arrow-right" /> {check.accion}</span>}</div>
  </article>
}

function CertificateFact({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  return <div><dt>{label}</dt><dd className={ok ? 'ok' : 'warning'}><i className={`bi ${ok ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'}`} /> {value}</dd></div>
}

function SeriesPanel({ series }: { series: SerieDiagnosticoSunat[] }) {
  return <section className="sunat-series-panel">
    <header className="sunat-diagnostic-section-title">
      <span><i className="bi bi-123" /></span>
      <div><h2>Series y siguientes correlativos</h2><p>Consulta directa de PRODUCCIÓN. Esta tabla no reserva ni incrementa ningún número.</p></div>
      <strong>{series.length} series</strong>
    </header>
    <div className="sunat-series-table-wrap"><table><thead><tr><th>Documento</th><th>Serie</th><th>Último utilizado</th><th>Siguiente número</th><th>Estado</th></tr></thead><tbody>
      {series.length === 0 ? <tr><td colSpan={5} className="sunat-series-empty">No se encontraron series de producción.</td></tr> : series.map((item) => <tr key={`${item.tipoDocumento}-${item.serie}`}><td><strong>{friendly(item.tipoDocumento)}</strong></td><td><code>{item.serie}</code></td><td>{formatCorrelative(item.ultimoCorrelativo)}</td><td><strong>{item.serie}-{item.siguienteNumero}</strong></td><td><span className={item.activa ? 'active' : 'inactive'}><i className="bi bi-circle-fill" /> {item.activa ? 'Activa' : 'Inactiva'}</span></td></tr>)}
    </tbody></table></div>
  </section>
}

function DiagnosticSkeleton() {
  return <div className="sunat-diagnostic-skeleton"><span /><span /><span /><span /></div>
}

function formatCorrelative(value: number) {
  return String(value).padStart(8, '0')
}

function friendly(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/(^|\s)\p{L}/gu, (letter) => letter.toUpperCase())
}

function dateRange(from: string | null, to: string | null) {
  if (!from || !to) return 'No disponible'
  return `${new Intl.DateTimeFormat('es-PE').format(new Date(`${from}T12:00:00`))} — ${new Intl.DateTimeFormat('es-PE').format(new Date(`${to}T12:00:00`))}`
}
