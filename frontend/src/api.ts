// Types mirror the backend read models. Quantities are in the ingredient's base unit (g, ml, pcs).

export type BaseUnit = 'G' | 'ML' | 'PCS'
export type OrderStatus = 'DRAFT' | 'RELEASED' | 'COMPLETED' | 'CANCELLED'
export type MovementType = 'OPENING' | 'RESERVE' | 'UNRESERVE' | 'CONSUME'

export const ORDER_STATUSES: OrderStatus[] = ['DRAFT', 'RELEASED', 'COMPLETED', 'CANCELLED']

/** Same limit the backend enforces. */
export const MAX_BATCHES = 1000

export interface IngredientStock {
  id: number
  name: string
  unit: BaseUnit
  onHand: number
  reserved: number
  available: number
}

export interface Movement {
  id: number
  type: MovementType
  quantity: number
  createdAt: string
  ingredientId: number
  ingredientName: string
  unit: BaseUnit
  orderId: number | null
}

export interface IngredientDetail {
  stock: IngredientStock
  movements: Movement[]
}

export interface RecipeLine {
  ingredientId: number
  ingredientName: string
  unit: BaseUnit
  quantityPerBatch: number
}

export interface Recipe {
  id: number
  name: string
  yieldQuantity: number
  yieldUnit: string
  lines: RecipeLine[]
}

export interface OrderSummary {
  id: number
  recipeName: string
  batches: number
  status: OrderStatus
  createdAt: string
}

export interface Requirement {
  ingredientId: number
  ingredientName: string
  unit: BaseUnit
  requiredQuantity: number
  available: number
}

export interface OrderDetail {
  id: number
  recipeId: number
  recipeName: string
  yieldQuantity: number
  yieldUnit: string
  batches: number
  status: OrderStatus
  createdAt: string
  releasedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  requirements: Requirement[]
  movements: Movement[]
}

export interface Shortage {
  ingredientId: number
  ingredientName: string
  unit: BaseUnit
  required: number
  available: number
  shortfall: number
}

/** Error from the API. `message` is the problem detail; `shortages` is set when a release is rejected. */
export class ApiError extends Error {
  status: number
  shortages: Shortage[]

  constructor(status: number, message: string, shortages: Shortage[] = []) {
    super(message)
    this.status = status
    this.shortages = shortages
  }
}

async function request<T>(method: 'GET' | 'POST', path: string, body?: unknown): Promise<T> {
  const response = await fetch(`/api${path}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}))
    throw new ApiError(response.status, problem.detail ?? `Request failed (${response.status})`, problem.shortages ?? [])
  }
  return response.json()
}

export const api = {
  listStock: () => request<IngredientStock[]>('GET', '/ingredients'),
  getIngredient: (id: number) => request<IngredientDetail>('GET', `/ingredients/${id}`),
  listRecipes: () => request<Recipe[]>('GET', '/recipes'),
  listOrders: (status?: OrderStatus) =>
    request<OrderSummary[]>('GET', status ? `/orders?status=${status}` : '/orders'),
  getOrder: (id: number) => request<OrderDetail>('GET', `/orders/${id}`),
  createOrder: (recipeId: number, batches: number) =>
    request<OrderDetail>('POST', '/orders', { recipeId, batches }),
  releaseOrder: (id: number) => request<OrderDetail>('POST', `/orders/${id}/release`),
  completeOrder: (id: number) => request<OrderDetail>('POST', `/orders/${id}/complete`),
  cancelOrder: (id: number) => request<OrderDetail>('POST', `/orders/${id}/cancel`),
}
