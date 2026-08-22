import type { Cliente } from '../types/client'

interface ClientStatusDialogProps {
  client: Cliente
  isSubmitting: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function ClientStatusDialog({ client, isSubmitting, onCancel, onConfirm }: ClientStatusDialogProps) {
  const willActivate = client.estado === 'INACTIVO'

  return (
    <div className="modal-backdrop modal-backdrop--confirm" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && !isSubmitting && onCancel()}>
      <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-client-status-title">
        <span className={`confirm-dialog__icon ${willActivate ? 'confirm-dialog__icon--success' : ''}`}>
          <i className={`bi ${willActivate ? 'bi-person-check' : 'bi-person-dash'}`} />
        </span>
        <h2 id="confirm-client-status-title">{willActivate ? '¿Activar cliente?' : '¿Inactivar cliente?'}</h2>
        <p>
          {willActivate
            ? <><strong>{client.nombreMostrar}</strong> podrá participar nuevamente en operaciones comerciales.</>
            : <><strong>{client.nombreMostrar}</strong> dejará de estar disponible para nuevas operaciones.</>}
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
