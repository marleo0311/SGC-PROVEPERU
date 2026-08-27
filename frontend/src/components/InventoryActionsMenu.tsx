import { useCallback, useEffect, useRef, useState, type CSSProperties } from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import type { StockInventario } from '../types/inventory'

interface InventoryActionsMenuProps {
  stock: StockInventario
  canViewKardex: boolean
  canAdjust: boolean
  canTransfer: boolean
  canEditMinimum: boolean
  onAdjust: (stock: StockInventario) => void
  onTransfer: (stock: StockInventario) => void
  onEditMinimum: (stock: StockInventario) => void
}

const menuWidth = 196
const menuGap = 6
const viewportGap = 8

export function InventoryActionsMenu({
  stock,
  canViewKardex,
  canAdjust,
  canTransfer,
  canEditMinimum,
  onAdjust,
  onTransfer,
  onEditMinimum,
}: InventoryActionsMenuProps) {
  const [open, setOpen] = useState(false)
  const [position, setPosition] = useState<CSSProperties>({})
  const triggerRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const menuId = `inventory-actions-${stock.idSede}-${stock.idProducto}`
  const actionCount = Number(canViewKardex) + Number(canAdjust) + Number(canTransfer) + Number(canEditMinimum)

  const updatePosition = useCallback(() => {
    const trigger = triggerRef.current
    if (!trigger) return

    const rect = trigger.getBoundingClientRect()
    const estimatedHeight = actionCount * 39 + 12
    const opensBelow = window.innerHeight - rect.bottom >= estimatedHeight + menuGap
    const top = opensBelow
      ? rect.bottom + menuGap
      : Math.max(viewportGap, rect.top - estimatedHeight - menuGap)
    const left = Math.max(
      viewportGap,
      Math.min(rect.right - menuWidth, window.innerWidth - menuWidth - viewportGap),
    )

    setPosition({ top, left })
  }, [actionCount])

  const closeMenu = useCallback(() => setOpen(false), [])

  useEffect(() => {
    if (!open) return

    const frame = window.requestAnimationFrame(() => {
      updatePosition()
      menuRef.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus()
    })

    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (triggerRef.current?.contains(target) || menuRef.current?.contains(target)) return
      closeMenu()
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Escape') return
      closeMenu()
      triggerRef.current?.focus()
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    window.addEventListener('resize', closeMenu)
    window.addEventListener('scroll', closeMenu, true)

    return () => {
      window.cancelAnimationFrame(frame)
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
      window.removeEventListener('resize', closeMenu)
      window.removeEventListener('scroll', closeMenu, true)
    }
  }, [closeMenu, open, updatePosition])

  function toggleMenu() {
    if (!open) updatePosition()
    setOpen((current) => !current)
  }

  function runAction(action: (stock: StockInventario) => void) {
    closeMenu()
    action(stock)
  }

  return (
    <div className="inventory-actions-menu">
      <button
        ref={triggerRef}
        className="inventory-actions-trigger"
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        aria-label={`Mostrar acciones de ${stock.nombreProducto}`}
        onClick={toggleMenu}
      >
        <i className="bi bi-three-dots-vertical" />
        <span>Acciones</span>
        <i className={`bi bi-chevron-${open ? 'up' : 'down'}`} />
      </button>

      {open && createPortal(
        <div
          ref={menuRef}
          id={menuId}
          className="inventory-actions-dropdown"
          role="menu"
          aria-label={`Acciones para ${stock.nombreProducto}`}
          style={position}
        >
          {canViewKardex && (
            <Link
              className="inventory-kardex-button"
              to={`/app/kardex?producto=${stock.idProducto}&sede=${stock.idSede}`}
              role="menuitem"
              onClick={closeMenu}
            >
              <i className="bi bi-clock-history" /> Kardex
            </Link>
          )}
          {canAdjust && (
            <button className="inventory-adjust-button" type="button" role="menuitem" onClick={() => runAction(onAdjust)}>
              <i className="bi bi-sliders" /> Ajustar
            </button>
          )}
          {canTransfer && (
            <button className="inventory-transfer-button" type="button" role="menuitem" onClick={() => runAction(onTransfer)}>
              <i className="bi bi-arrow-left-right" /> Transferir
            </button>
          )}
          {canEditMinimum && (
            <button className="inventory-minimum-button" type="button" role="menuitem" onClick={() => runAction(onEditMinimum)}>
              <i className="bi bi-bell" /> Mínimo
            </button>
          )}
        </div>,
        document.body,
      )}
    </div>
  )
}
