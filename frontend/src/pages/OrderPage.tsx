import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError, api, type OrderDetail, type Shortage } from '../api'
import { ErrorMessage, Loading, ShortageTable, StatusBadge } from '../components'
import { MOVEMENT_LABELS, formatDateTime, formatQuantity, orderRef } from '../format'
import { useLoad } from '../useLoad'

export default function OrderPage() {
  const id = Number(useParams().id)
  const { data: order, setData: setOrder, error } = useLoad(() => api.getOrder(id), [id])

  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string>()
  const [shortages, setShortages] = useState<Shortage[]>([])

  async function run(action: (id: number) => Promise<OrderDetail>, confirmText?: string) {
    if (confirmText && !window.confirm(confirmText)) return
    setBusy(true)
    setActionError(undefined)
    setShortages([])
    try {
      setOrder(await action(id))
    } catch (err) {
      setActionError((err as Error).message)
      setShortages(err instanceof ApiError ? err.shortages : [])
      // Refresh in case the order or stock changed since the page loaded.
      api.getOrder(id).then(setOrder, () => {})
    } finally {
      setBusy(false)
    }
  }

  if (error) return <ErrorMessage message={error} />
  if (!order) return <Loading />

  const ref = orderRef(order.id)
  const isDraft = order.status === 'DRAFT'

  return (
    <>
      <p>
        <Link to="/orders">← Orders</Link>
      </p>
      <div className="title-row">
        <h1>
          {ref} · {order.recipeName} × {order.batches}
        </h1>
        <StatusBadge status={order.status} />
      </div>
      <p className="muted">
        Yields {order.yieldQuantity * order.batches} {order.yieldUnit} · Created {formatDateTime(order.createdAt)}
        {order.releasedAt && ` · Released ${formatDateTime(order.releasedAt)}`}
        {order.completedAt && ` · Completed ${formatDateTime(order.completedAt)}`}
        {order.cancelledAt && ` · Cancelled ${formatDateTime(order.cancelledAt)}`}
      </p>

      <div className="actions">
        {order.status === 'DRAFT' && (
          <button disabled={busy} onClick={() => run(api.releaseOrder)}>
            Release
          </button>
        )}
        {order.status === 'RELEASED' && (
          <button
            disabled={busy}
            onClick={() => run(api.completeOrder, `Complete ${ref}? This consumes its reserved stock.`)}
          >
            Complete
          </button>
        )}
        {(order.status === 'DRAFT' || order.status === 'RELEASED') && (
          <button
            className="secondary"
            disabled={busy}
            onClick={() =>
              run(
                api.cancelOrder,
                `Cancel ${ref}?${order.status === 'RELEASED' ? ' Its reserved stock will be freed.' : ''}`,
              )
            }
          >
            Cancel order
          </button>
        )}
        {(order.status === 'COMPLETED' || order.status === 'CANCELLED') && (
          <span className="muted">No further actions for a {order.status.toLowerCase()} order.</span>
        )}
      </div>

      {shortages.length > 0 ? <ShortageTable shortages={shortages} /> : <ErrorMessage message={actionError} />}

      <h2>Ingredient requirements</h2>
      <table>
        <thead>
          <tr>
            <th>Ingredient</th>
            <th className="num">Required</th>
            {isDraft && <th className="num">Available now</th>}
            {isDraft && <th></th>}
          </tr>
        </thead>
        <tbody>
          {order.requirements.map((r) => {
            const short = r.available < r.requiredQuantity
            return (
              <tr key={r.ingredientId}>
                <td>
                  <Link to={`/stock/${r.ingredientId}`}>{r.ingredientName}</Link>
                </td>
                <td className="num">{formatQuantity(r.requiredQuantity, r.unit)}</td>
                {isDraft && <td className="num">{formatQuantity(r.available, r.unit)}</td>}
                {isDraft && <td className={short ? 'short' : 'ok'}>{short ? 'Short' : 'OK'}</td>}
              </tr>
            )
          })}
        </tbody>
      </table>

      <h2>Stock movements</h2>
      {order.movements.length === 0 ? (
        <p className="muted">No stock movements yet. Releasing the order reserves its ingredients.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Ingredient</th>
              <th>Movement</th>
              <th className="num">Quantity</th>
            </tr>
          </thead>
          <tbody>
            {order.movements.map((m) => (
              <tr key={m.id}>
                <td>{formatDateTime(m.createdAt)}</td>
                <td>
                  <Link to={`/stock/${m.ingredientId}`}>{m.ingredientName}</Link>
                </td>
                <td>{MOVEMENT_LABELS[m.type]}</td>
                <td className="num">{formatQuantity(m.quantity, m.unit)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
