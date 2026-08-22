import { useEffect } from 'react'
import type { EstadoCatalogo } from '../types/catalog'

interface CatalogStatusDialogProps {
  entityLabel: string
  name: string
  estado: EstadoCatalogo
  isSubmitting: boolean
  inactiveImpact: string
  onCancel: () => void
  onConfirm: () => void
}

export function CatalogStatusDialog({
  entityLabel,
  name,
  estado,
  isSubmitting,
  inactiveImpact,
  onCancel,
  onConfirm,
}: CatalogStatusDialogProps) {
  const willActivate = estado === 'INACTIVO'

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !isSubmitting) onCancel()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isSubmitting, onCancel])

  return (
    <div
      className="modal-backdrop modal-backdrop--confirm"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onCancel()}
    >
      <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-catalog-status-title">
        <span className={`confirm-dialog__icon ${willActivate ? 'confirm-dialog__icon--success' : ''}`}>
          <i className={`bi ${willActivate ? 'bi-check-circle' : 'bi-pause-circle'}`} />
        </span>
        <h2 id="confirm-catalog-status-title">¿{willActivate ? 'Activar' : 'Inactivar'} {entityLabel}?</h2>
        <p>
          {willActivate
            ? <><strong>{name}</strong> volverá a estar disponible en los formularios de productos.</>
            : <><strong>{name}</strong> {inactiveImpact}</>}
        </p>
        <div className="confirm-dialog__actions">
          <button className="secondary-button" type="button" onClick={onCancel} disabled={isSubmitting}>Cancelar</button>
          <button className={`primary-button primary-button--inline ${willActivate ? '' : 'primary-button--danger'}`} type="button" onClick={onConfirm} disabled={isSubmitting}>
            {isSubmitting ? <span className="spinner-border spinner-border-sm" /> : <i className={`bi ${willActivate ? 'bi-check2' : 'bi-pause'}`} />}
            {willActivate ? 'Sí, activar' : 'Sí, inactivar'}
          </button>
        </div>
      </section>
    </div>
  )
}
