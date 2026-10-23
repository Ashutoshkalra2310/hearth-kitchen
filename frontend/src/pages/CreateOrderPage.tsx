import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { MAX_BATCHES, api } from '../api'
import { ErrorMessage, Loading } from '../components'
import { formatQuantity } from '../format'
import { useLoad } from '../useLoad'

export default function CreateOrderPage() {
  const navigate = useNavigate()
  const recipes = useLoad(api.listRecipes, [])
  const stock = useLoad(api.listStock, [])

  const [recipeId, setRecipeId] = useState<number>()
  const [batches, setBatches] = useState('1')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string>()

  const recipe = recipes.data?.find((r) => r.id === recipeId)
  const batchCount = Number(batches)
  const batchesValid = Number.isInteger(batchCount) && batchCount >= 1 && batchCount <= MAX_BATCHES
  const availableById = new Map(stock.data?.map((i) => [i.id, i.available]))

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!recipe || !batchesValid) return
    setSubmitting(true)
    setError(undefined)
    try {
      const order = await api.createOrder(recipe.id, batchCount)
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setError((err as Error).message)
      setSubmitting(false)
    }
  }

  if (recipes.error) return <ErrorMessage message={recipes.error} />
  if (!recipes.data) return <Loading />

  return (
    <>
      <h1>New production order</h1>
      <form className="form" onSubmit={submit}>
        <label>
          Recipe
          <select value={recipeId ?? ''} onChange={(e) => setRecipeId(Number(e.target.value))} required>
            <option value="" disabled>Choose a recipe</option>
            {recipes.data.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name} ({r.yieldQuantity} {r.yieldUnit} per batch)
              </option>
            ))}
          </select>
        </label>
        <label>
          Batches
          <input
            type="number"
            min={1}
            max={MAX_BATCHES}
            step={1}
            value={batches}
            onChange={(e) => setBatches(e.target.value)}
            required
          />
        </label>
        {!batchesValid && <p className="error">Batches must be a whole number from 1 to {MAX_BATCHES}.</p>}
        <ErrorMessage message={error} />
        <div>
          <button type="submit" disabled={!recipe || !batchesValid || submitting}>
            {submitting ? 'Creating…' : 'Create draft order'}
          </button>
        </div>
      </form>

      {recipe && batchesValid && (
        <>
          <h2>
            Ingredients needed for {batchCount} × {recipe.name} ({recipe.yieldQuantity * batchCount} {recipe.yieldUnit})
          </h2>
          <p className="muted">Creating the order doesn't reserve anything. Stock is checked and reserved when you release it.</p>
          <table>
            <thead>
              <tr>
                <th>Ingredient</th>
                <th className="num">Required</th>
                <th className="num">Available now</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {recipe.lines.map((line) => {
                const required = line.quantityPerBatch * batchCount
                const available = availableById.get(line.ingredientId)
                const short = available !== undefined && available < required
                return (
                  <tr key={line.ingredientId}>
                    <td>{line.ingredientName}</td>
                    <td className="num">{formatQuantity(required, line.unit)}</td>
                    <td className="num">{available === undefined ? '…' : formatQuantity(available, line.unit)}</td>
                    <td className={short ? 'short' : 'ok'}>{available === undefined ? '' : short ? 'Short' : 'OK'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </>
      )}
    </>
  )
}
