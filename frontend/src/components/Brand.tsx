interface BrandProps {
  compact?: boolean
  inverted?: boolean
}

export function Brand({ compact = false, inverted = false }: BrandProps) {
  return (
    <div className={`brand ${inverted ? 'brand--inverted' : ''}`} aria-label="PROVEPERÚ">
      <span className="brand__mark" aria-hidden="true">
        <i className="bi bi-box-seam-fill" />
      </span>
      {!compact && (
        <span className="brand__copy">
          <strong>PROVEPERÚ</strong>
          <small>Gestión comercial</small>
        </span>
      )}
    </div>
  )
}
