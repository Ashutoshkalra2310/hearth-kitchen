import { Link, useSearchParams } from 'react-router-dom'
import { ORDER_STATUSES, api, type OrderStatus } from '../api'
import { ErrorMessage, Loading, StatusBadge } from '../components'
import { formatDateTime, orderRef, statusLabel } from '../format'
import { useLoad } from '../useLoad'

export default function OrdersPage() {
  // The filter lives in the URL (?status=RELEASED) so it survives reloads and back/forward.
  const [params, setParams] = useSearchParams()
  const param = params.get('status')
  const status = ORDER_STATUSES.find((s) => s === param)
  const { data: orders, error } = useLoad(() => api.listOrders(status), [status])

  const filters: (OrderStatus | undefined)[] = [undefined, ...ORDER_STATUSES]

  return (
    <>
      <div className="title-row">
        <h1>Production orders</h1>
        <Link className="button" to="/orders/new">New order</Link>
      </div>

      <div className="filters">
        {filters.map((s) => (
          <button
            key={s ?? 'ALL'}
            className={s === status ? 'active' : ''}
            onClick={() => setParams(s ? { status: s } : {})}
          >
            {s ? statusLabel(s) : 'All'}
          </button>
        ))}
      </div>

      <ErrorMessage message={error} />
      {!orders && !error && <Loading />}
      {orders && orders.length === 0 && <p className="muted">No orders.</p>}
      {orders && orders.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Order</th>
              <th>Recipe</th>
              <th className="num">Batches</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td>
                  <Link to={`/orders/${o.id}`}>{orderRef(o.id)}</Link>
                </td>
                <td>{o.recipeName}</td>
                <td className="num">{o.batches}</td>
                <td>
                  <StatusBadge status={o.status} />
                </td>
                <td>{formatDateTime(o.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
