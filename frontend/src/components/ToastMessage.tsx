import { useEffect } from 'react'

interface ToastMessageProps {
  tone: 'success' | 'danger'
  message: string
  onClose: () => void
}

export function ToastMessage({ tone, message, onClose }: ToastMessageProps) {
  useEffect(() => {
    const timer = window.setTimeout(onClose, 4500)
    return () => window.clearTimeout(timer)
  }, [onClose])

  return (
    <div className={`toast-message toast-message--${tone}`} role="status">
      <span className="toast-message__icon"><i className={`bi ${tone === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'}`} /></span>
      <span><strong>{tone === 'success' ? 'Operación completada' : 'No se pudo completar'}</strong><small>{message}</small></span>
      <button type="button" onClick={onClose} aria-label="Cerrar mensaje"><i className="bi bi-x-lg" /></button>
    </div>
  )
}
