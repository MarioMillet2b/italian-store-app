import { useMemo, useState } from 'react';
import Header from './components/Header';
import InventoryTable from './components/InventoryTable';
import MetricCard from './components/MetricCard';
import MovementForm from './components/MovementForm';
import MovementList from './components/MovementList';
import ProductForm from './components/ProductForm';
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
      <Header />

      <section className="grid">
        <MetricCard label="Valor del inventario" value={formatCurrency(metrics.inventoryValue)} />
        <MetricCard label="Productos" value={String(products.length)} />
        <MetricCard label="Unidades totales" value={String(metrics.totalUnits)} />
        <MetricCard label="Stock bajo" value={String(metrics.lowStockItems)} />
      </section>

      <div className="layout">
        <section className="card">
          <div className="tabs" aria-label="Selector de vistas">
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

          {activeTab === 'inventory' ? <InventoryTable products={products} /> : <MovementList products={products} movements={movements} />}
        </section>

        <aside className="card sidebar-panel">
          <ProductForm form={productForm} onChange={handleProductChange} onSubmit={handleCreateProduct} />
          <div className="divider" />
          <MovementForm
            products={products}
            form={movementForm}
            onChange={handleMovementChange}
            onSubmit={handleAddMovement}
          />
        </aside>
      </div>
    </div>
  );
}

export default App;
