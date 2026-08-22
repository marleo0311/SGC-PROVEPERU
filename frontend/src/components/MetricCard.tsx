interface MetricCardProps {
  label: string
  value: string
  detail: string
  icon: string
  tone: 'blue' | 'teal' | 'amber' | 'violet'
}

export function MetricCard({ label, value, detail, icon, tone }: MetricCardProps) {
  return (
    <article className={`metric-card metric-card--${tone}`}>
      <div className="metric-card__topline">
        <span className="metric-card__label">{label}</span>
        <span className="metric-card__icon" aria-hidden="true">
          <i className={`bi ${icon}`} />
        </span>
      </div>
      <strong className="metric-card__value">{value}</strong>
      <span className="metric-card__detail">{detail}</span>
    </article>
  )
}
