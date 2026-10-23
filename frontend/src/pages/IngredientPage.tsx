import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import { ErrorMessage, Loading } from '../components'
import { MOVEMENT_EFFECTS, MOVEMENT_LABELS, formatChange, formatDateTime, formatQuantity, orderRef } from '../format'
import { useLoad } from '../useLoad'

export default function IngredientPage() {
  const id = Number(useParams().id)
  const { data, error } = useLoad(() => api.getIngredient(id), [id])

  if (error) return <ErrorMessage message={error} />
  if (!data) return <Loading />

  const { stock, movements } = data

  // Replay the movements in order to show the balance after each one.
  let onHand = 0
  let reserved = 0
  const rows = movements.map((m) => {
    const effect = MOVEMENT_EFFECTS[m.type]
    const onHandChange = effect.onHand * m.quantity
    const reservedChange = effect.reserved * m.quantity
    onHand += onHandChange
    reserved += reservedChange
    return { ...m, onHandChange, reservedChange, onHandAfter: onHand, reservedAfter: reserved }
  })

  return (
    <>
      <p>
        <Link to="/stock">← Stock</Link>
      </p>
      <h1>{stock.name}</h1>
      <div className="summary">
        <div>
          <span className="muted">On hand</span>
          <strong>{formatQuantity(stock.onHand, stock.unit)}</strong>
        </div>
        <div>
          <span className="muted">Reserved</span>
          <strong>{formatQuantity(stock.reserved, stock.unit)}</strong>
        </div>
        <div>
          <span className="muted">Available</span>
          <strong>{formatQuantity(stock.available, stock.unit)}</strong>
        </div>
      </div>

      <h2>Movements</h2>
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Movement</th>
            <th>Order</th>
            <th className="num">On hand change</th>
            <th className="num">Reserved change</th>
            <th className="num">On hand after</th>
            <th className="num">Reserved after</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((m) => (
            <tr key={m.id}>
              <td>{formatDateTime(m.createdAt)}</td>
              <td>{MOVEMENT_LABELS[m.type]}</td>
              <td>{m.orderId ? <Link to={`/orders/${m.orderId}`}>{orderRef(m.orderId)}</Link> : '—'}</td>
              <td className="num">{formatChange(m.onHandChange, m.unit)}</td>
              <td className="num">{formatChange(m.reservedChange, m.unit)}</td>
              <td className="num">{formatQuantity(m.onHandAfter, m.unit)}</td>
              <td className="num">{formatQuantity(m.reservedAfter, m.unit)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}
