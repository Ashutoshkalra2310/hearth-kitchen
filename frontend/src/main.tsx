import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, NavLink, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import CreateOrderPage from './pages/CreateOrderPage'
import IngredientPage from './pages/IngredientPage'
import OrderPage from './pages/OrderPage'
import OrdersPage from './pages/OrdersPage'
import StockPage from './pages/StockPage'
import './styles.css'

function Layout() {
  return (
    <>
      <header>
        <span className="brand">Hearth Kitchens</span>
        <nav>
          <NavLink to="/orders" end>Orders</NavLink>
          <NavLink to="/orders/new">New order</NavLink>
          <NavLink to="/stock">Stock</NavLink>
        </nav>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/orders" replace />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/new" element={<CreateOrderPage />} />
          <Route path="orders/:id" element={<OrderPage />} />
          <Route path="stock" element={<StockPage />} />
          <Route path="stock/:id" element={<IngredientPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
