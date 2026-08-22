import type { VentaDiaria } from '../types/reports'

interface SalesChartProps {
  data: VentaDiaria[]
  formatCurrency: (value: number) => string
}

const dayFormatter = new Intl.DateTimeFormat('es-PE', {
  weekday: 'short',
  day: '2-digit',
  timeZone: 'UTC',
})

function formatDay(value: string) {
  return dayFormatter.format(new Date(`${value}T00:00:00Z`)).replace('.', '')
}

export function SalesChart({ data, formatCurrency }: SalesChartProps) {
  const maxValue = Math.max(...data.map((item) => item.totalVentas), 1)

  if (data.length === 0) {
    return (
      <div className="empty-state empty-state--compact">
        <span className="empty-state__icon"><i className="bi bi-bar-chart" /></span>
        <strong>Aún no hay ventas en el periodo</strong>
        <span>Los movimientos aparecerán aquí cuando se registren.</span>
      </div>
    )
  }

  return (
    <div className="sales-chart" role="img" aria-label="Ventas diarias del periodo">
      <div className="sales-chart__plot">
        {data.map((item) => {
          const height = Math.max(7, (item.totalVentas / maxValue) * 100)
          return (
            <div className="sales-chart__column" key={item.fecha}>
              <span className="sales-chart__value">{formatCurrency(item.totalVentas)}</span>
              <div className="sales-chart__track">
                <span
                  className="sales-chart__bar"
                  style={{ height: `${height}%` }}
                  title={`${formatDay(item.fecha)}: ${formatCurrency(item.totalVentas)}`}
                />
              </div>
              <span className="sales-chart__day">{formatDay(item.fecha)}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
