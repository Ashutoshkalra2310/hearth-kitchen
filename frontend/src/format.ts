import type { BaseUnit, MovementType, OrderStatus } from './api'

// Stock is stored in g / ml / pcs but shown in kg / L / pcs, like the brief's seed table.
const DISPLAY_UNITS: Record<BaseUnit, { label: string; factor: number }> = {
  G: { label: 'kg', factor: 1000 },
  ML: { label: 'L', factor: 1000 },
  PCS: { label: 'pcs', factor: 1 },
}

function displayNumber(quantity: number, unit: BaseUnit): string {
  return (quantity / DISPLAY_UNITS[unit].factor).toLocaleString(undefined, { maximumFractionDigits: 3 })
}

export function formatQuantity(quantity: number, unit: BaseUnit): string {
  return `${displayNumber(quantity, unit)} ${DISPLAY_UNITS[unit].label}`
}

/** "+2.4 kg", "−2.4 kg", or "—" for no change. */
export function formatChange(change: number, unit: BaseUnit): string {
  if (change === 0) return '—'
  const sign = change > 0 ? '+' : '−'
  return `${sign}${formatQuantity(Math.abs(change), unit)}`
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString()
}

export function orderRef(id: number): string {
  return `PO-${id}`
}

export function statusLabel(status: OrderStatus): string {
  return status.charAt(0) + status.slice(1).toLowerCase()
}

export const MOVEMENT_LABELS: Record<MovementType, string> = {
  OPENING: 'Opening stock',
  RESERVE: 'Reserved',
  UNRESERVE: 'Reservation freed',
  CONSUME: 'Consumed',
}

/** What a movement's quantity does to on hand and reserved (same rules as the backend). */
export const MOVEMENT_EFFECTS: Record<MovementType, { onHand: number; reserved: number }> = {
  OPENING: { onHand: 1, reserved: 0 },
  RESERVE: { onHand: 0, reserved: 1 },
  UNRESERVE: { onHand: 0, reserved: -1 },
  CONSUME: { onHand: -1, reserved: -1 },
}
