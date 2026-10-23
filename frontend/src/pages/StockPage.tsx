import { Link } from 'react-router-dom'
import { api } from '../api'
import { ErrorMessage, Loading } from '../components'
import { formatQuantity } from '../format'
import { useLoad } from '../useLoad'

export default function StockPage() {
  const { data: stock, error } = useLoad(api.listStock, [])
  const nameById = new Map(stock?.map((i) => [i.id, i.name]))

  return (
    <>
      <h1>Stock</h1>
      <p className="muted">Available = on hand − reserved. Open an ingredient to see every movement behind its numbers.</p>
      <ErrorMessage message={error} />
      {!stock && !error && <Loading />}
      {stock && (
        <table>
          <thead>
            <tr>
              <th>Ingredient</th>
              <th className="num">On hand</th>
              <th className="num">Reserved</th>
              <th className="num">Available</th>
              <th>Substitute</th>
            </tr>
          </thead>
          <tbody>
            {stock.map((i) => (
              <tr key={i.id}>
                <td>
                  <Link to={`/stock/${i.id}`}>{i.name}</Link>
                </td>
                <td className="num">{formatQuantity(i.onHand, i.unit)}</td>
                <td className="num">{formatQuantity(i.reserved, i.unit)}</td>
                <td className={`num ${i.available === 0 ? 'short' : ''}`}>{formatQuantity(i.available, i.unit)}</td>
                <td>{i.substituteId ? nameById.get(i.substituteId) : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
