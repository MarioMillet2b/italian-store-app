import { useMemo, useState } from 'react';
import { initialMovements, initialProducts } from './data';

const defaultProductForm = {
  sku: '',
  name: '',
  category: 'General',
  stock: 0,
  price: 0,
  minStock: 0,
};

const formatCurrency = (value) =>
  new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  }).format(value || 0);

const getStatus = (stock, minStock) => (stock <= minStock ? 'low' : 'ok');

function App() {
  const [products, setProducts] = useState(initialProducts);
  const [movements, setMovements] = useState(initialMovements);
  const [activeTab, setActiveTab] = useState('inventory');
  const [productForm, setProductForm] = useState(defaultProductForm);
  const [movementForm, setMovementForm] = useState({
    productId: String(initialProducts[0]?.id ?? ''),
    type: 'entrada',
    quantity: 1,
    note: '',
  });

  const metrics = useMemo(() => {
    const inventoryValue = products.reduce((sum, product) => sum + product.stock * product.price, 0);
    const lowStockItems = products.filter((product) => product.stock <= product.minStock).length;
    const totalUnits = products.reduce((sum, product) => sum + product.stock, 0);
    return { inventoryValue, lowStockItems, totalUnits };
  }, [products]);

  const handleProductChange = (event) => {
    const { name, value } = event.target;
    setProductForm((prev) => ({
      ...prev,
      [name]: name === 'stock' || name === 'price' || name === 'minStock' ? Number(value) : value,
    }));
  };

  const handleCreateProduct = (event) => {
    event.preventDefault();

    if (!productForm.name || !productForm.sku) {
      return;
    }

    const nextProduct = {
      id: Date.now(),
      sku: productForm.sku,
      name: productForm.name,
      category: productForm.category,
      stock: Number(productForm.stock),
      price: Number(productForm.price),
      minStock: Number(productForm.minStock),
    };

    setProducts((prev) => [nextProduct, ...prev]);
    setProductForm(defaultProductForm);
    setMovementForm((prev) => ({ ...prev, productId: String(nextProduct.id) }));
  };

  const handleMovementChange = (event) => {
    const { name, value } = event.target;
    setMovementForm((prev) => ({
      ...prev,
      [name]: name === 'quantity' ? Number(value) : value,
    }));
  };

  const handleAddMovement = (event) => {
    event.preventDefault();

    const productId = Number(movementForm.productId);
    const selectedProduct = products.find((product) => product.id === productId);
    if (!selectedProduct) {
      return;
    }

    const quantity = Number(movementForm.quantity);
    if (!quantity) {
      return;
    }

    const delta = movementForm.type === 'salida' ? -Math.abs(quantity) : Math.abs(quantity);
    const nextStock = selectedProduct.stock + delta;

    setProducts((prev) =>
      prev.map((product) =>
        product.id === productId
          ? { ...product, stock: Math.max(0, nextStock) }
          : product
      )
    );

    const newMovement = {
      id: Date.now(),
      type: movementForm.type,
      productId,
      quantity: movementForm.type === 'salida' ? -Math.abs(quantity) : Math.abs(quantity),
      note: movementForm.note || 'Movimiento manual',
      timestamp: new Date().toISOString(),
    };

    setMovements((prev) => [newMovement, ...prev]);
    setMovementForm((prev) => ({ ...prev, quantity: 1, note: '' }));
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <h1>Italian Store App</h1>
          <div className="small-text">Control de almacén y punto de venta</div>
        </div>
        <div className="status-pill">MVP activo</div>
      </header>

      <section className="grid">
        <article className="card metric">
          <span className="label">Valor del inventario</span>
          <span className="value">{formatCurrency(metrics.inventoryValue)}</span>
        </article>
        <article className="card metric">
          <span className="label">Productos</span>
          <span className="value">{products.length}</span>
        </article>
        <article className="card metric">
          <span className="label">Unidades totales</span>
          <span className="value">{metrics.totalUnits}</span>
        </article>
        <article className="card metric">
          <span className="label">Stock bajo</span>
          <span className="value">{metrics.lowStockItems}</span>
        </article>
      </section>

      <div className="layout">
        <section className="card">
          <div className="tabs">
            <button
              className={`tab ${activeTab === 'inventory' ? 'active' : ''}`}
              onClick={() => setActiveTab('inventory')}
              type="button"
            >
              Inventario
            </button>
            <button
              className={`tab ${activeTab === 'movements' ? 'active' : ''}`}
              onClick={() => setActiveTab('movements')}
              type="button"
            >
              Movimientos
            </button>
          </div>

          {activeTab === 'inventory' ? (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>SKU</th>
                    <th>Stock</th>
                    <th>Precio</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr key={product.id}>
                      <td>
                        <strong>{product.name}</strong>
                        <div className="small-text">{product.category}</div>
                      </td>
                      <td>{product.sku}</td>
                      <td>{product.stock}</td>
                      <td>{formatCurrency(product.price)}</td>
                      <td>
                        <span className={`badge ${getStatus(product.stock, product.minStock)}`}>
                          {product.stock <= product.minStock ? 'Bajo' : 'OK'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="movement-list">
              {movements.map((movement) => {
                const product = products.find((item) => item.id === movement.productId);
                return (
                  <div key={movement.id} className={`movement-item ${movement.type}`}>
                    <strong>{movement.type.toUpperCase()}</strong>
                    <div>{product?.name ?? 'Producto no encontrado'}</div>
                    <div className="small-text">
                      {Math.abs(movement.quantity)} unidades · {new Date(movement.timestamp).toLocaleString('es-ES')}
                    </div>
                    <div className="small-text">{movement.note}</div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        <aside className="card">
          <h3>Alta de producto</h3>
          <form className="form-grid" onSubmit={handleCreateProduct}>
            <label>
              SKU
              <input name="sku" value={productForm.sku} onChange={handleProductChange} placeholder="P-010" />
            </label>
            <label>
              Nombre
              <input name="name" value={productForm.name} onChange={handleProductChange} placeholder="Queso Parmesano" />
            </label>
            <div className="two-col">
              <label>
                Categoría
                <input name="category" value={productForm.category} onChange={handleProductChange} />
              </label>
              <label>
                Stock mínimo
                <input type="number" name="minStock" value={productForm.minStock} onChange={handleProductChange} />
              </label>
            </div>
            <div className="two-col">
              <label>
                Cantidad
                <input type="number" name="stock" value={productForm.stock} onChange={handleProductChange} />
              </label>
              <label>
                Precio
                <input type="number" step="0.01" name="price" value={productForm.price} onChange={handleProductChange} />
              </label>
            </div>
            <div className="button-row">
              <button className="primary" type="submit">Guardar producto</button>
            </div>
          </form>

          <h3 style={{ marginTop: '22px' }}>Movimiento de stock</h3>
          <form className="form-grid" onSubmit={handleAddMovement}>
            <label>
              Producto
              <select name="productId" value={movementForm.productId} onChange={handleMovementChange}>
                {products.map((product) => (
                  <option key={product.id} value={String(product.id)}>
                    {product.name}
                  </option>
                ))}
              </select>
            </label>
            <div className="two-col">
              <label>
                Tipo
                <select name="type" value={movementForm.type} onChange={handleMovementChange}>
                  <option value="entrada">Entrada</option>
                  <option value="salida">Salida</option>
                  <option value="ajuste">Ajuste</option>
                </select>
              </label>
              <label>
                Cantidad
                <input type="number" name="quantity" min="1" value={movementForm.quantity} onChange={handleMovementChange} />
              </label>
            </div>
            <label>
              Nota
              <input name="note" value={movementForm.note} onChange={handleMovementChange} placeholder="Pedido, venta, revisión..." />
            </label>
            <div className="button-row">
              <button className="primary" type="submit">Registrar movimiento</button>
            </div>
          </form>
        </aside>
      </div>
    </div>
  );
}

export default App;
