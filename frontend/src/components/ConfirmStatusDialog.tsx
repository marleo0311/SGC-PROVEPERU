import type { Producto } from '../types/catalog'

interface ConfirmStatusDialogProps {
  product: Producto
  isSubmitting: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function ConfirmStatusDialog({ product, isSubmitting, onCancel, onConfirm }: ConfirmStatusDialogProps) {
  const willActivate = product.estado === 'INACTIVO'

  return (
    <div className="modal-backdrop modal-backdrop--confirm" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onCancel()}>
      <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-status-title">
        <span className={`confirm-dialog__icon ${willActivate ? 'confirm-dialog__icon--success' : ''}`}>
          <i className={`bi ${willActivate ? 'bi-check-circle' : 'bi-pause-circle'}`} />
        </span>
        <h2 id="confirm-status-title">{willActivate ? '¿Activar producto?' : '¿Inactivar producto?'}</h2>
        <p>
          {willActivate
            ? <><strong>{product.nombre}</strong> volverá a estar disponible para las operaciones comerciales.</>
            : <><strong>{product.nombre}</strong> dejará de estar disponible para nuevas operaciones.</>}
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
