import type { ProductoStockBajo } from '../types/reports'

interface StockAlertListProps {
  products: ProductoStockBajo[]
}

function formatStock(value: number) {
  return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 }).format(value)
}

export function StockAlertList({ products }: StockAlertListProps) {
  if (products.length === 0) {
    return (
      <div className="empty-state empty-state--compact">
        <span className="empty-state__icon empty-state__icon--success"><i className="bi bi-check2-circle" /></span>
        <strong>Inventario bajo control</strong>
        <span>No hay productos con stock crítico.</span>
      </div>
    )
  }

  return (
    <div className="stock-list">
      {products.map((product) => {
        const isEmpty = product.estadoStock === 'AGOTADO' || product.stockDisponible <= 0
        return (
          <article className="stock-item" key={product.idProducto}>
            <span className={`stock-item__icon ${isEmpty ? 'stock-item__icon--danger' : ''}`} aria-hidden="true">
              <i className={`bi ${isEmpty ? 'bi-exclamation-octagon' : 'bi-exclamation-triangle'}`} />
            </span>
            <div className="stock-item__copy">
              <strong>{product.nombreProducto}</strong>
              <span>{product.codigoInterno} · Mínimo {formatStock(product.stockMinimo)} {product.unidadBase}</span>
            </div>
            <div className="stock-item__amount">
              <strong>{formatStock(product.stockDisponible)}</strong>
              <span>{product.unidadBase}</span>
            </div>
          </article>
        )
      })}
    </div>
  )
}
