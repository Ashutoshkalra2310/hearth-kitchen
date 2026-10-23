import type { OrderStatus, Shortage } from './api'
import { formatQuantity, statusLabel } from './format'

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{statusLabel(status)}</span>
}

export function ErrorMessage({ message }: { message?: string }) {
  return message ? <p className="error">{message}</p> : null
}

export function Loading() {
  return <p className="muted">Loading…</p>
}

export function ShortageTable({ shortages }: { shortages: Shortage[] }) {
  const hasSubstitutes = shortages.some((s) => s.substituteName)
  return (
    <div className="panel panel-error">
      <strong>Release rejected: not enough stock. Nothing was reserved.</strong>
      <table>
        <thead>
          <tr>
            <th>Ingredient</th>
            <th className="num">Required</th>
            <th className="num">Available</th>
            <th className="num">Shortfall</th>
            {hasSubstitutes && <th>Substitute</th>}
          </tr>
        </thead>
        <tbody>
          {shortages.map((s) => (
            <tr key={s.ingredientId}>
              <td>{s.ingredientName}</td>
              <td className="num">{formatQuantity(s.required, s.unit)}</td>
              <td className="num">{formatQuantity(s.available, s.unit)}</td>
              <td className="num short">{formatQuantity(s.shortfall, s.unit)}</td>
              {hasSubstitutes && (
                <td>
                  {s.substituteName
                    ? `${s.substituteName}: only ${formatQuantity(s.substituteAvailable ?? 0, s.unit)} available`
                    : '—'}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
