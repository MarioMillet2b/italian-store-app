import { useMemo, useState } from 'react';
import Header from './components/Header';
import MetricCard from './components/MetricCard';
import PosPanel from './components/PosPanel';
import ShelfManager from './components/ShelfManager';
import InventoryTable from './components/InventoryTable';
import MovementList from './components/MovementList';
import { initialMovements, initialProducts, initialShelves } from './data';

const defaultProductForm = {
  sku: '',
  name: '',
  category: 'General',
  stock: 0,
  price: 0,
  minStock: 0,
};

const defaultShelfForm = {
  selectedShelfId: initialShelves[0]?.id ?? 0,
  shelfName: '',
  shelfZone: '',
  productId: null,
  productName: '',
  productSku: '',
  productStock: 0,
  productPrice: 0,
  productMinStock: 0,
  assignProductId: '',
};

const formatCurrency = (value) =>
  new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  }).format(value || 0);

function App() {
  const [products, setProducts] = useState(initialProducts);
  const [shelves, setShelves] = useState(initialShelves);
  const [movements, setMovements] = useState(initialMovements);
  const [activeTab, setActiveTab] = useState('pos');
  const [productForm, setProductForm] = useState(defaultProductForm);
  const [shelfForm, setShelfForm] = useState(defaultShelfForm);
  const [posForm, setPosForm] = useState({
    productId: String(initialProducts[0]?.id ?? ''),
    type: 'salida',
    quantity: 1,
    note: '',
  });

  const metrics = useMemo(() => {
    const inventoryValue = products.reduce((sum, product) => sum + product.stock * product.price, 0);
    const lowStockItems = products.filter((product) => product.stock <= product.minStock).length;
    const totalUnits = products.reduce((sum, product) => sum + product.stock, 0);
    return { inventoryValue, lowStockItems, totalUnits };
  }, [products]);

  const addMovement = ({ productId, type, quantity, note }) => {
    const newMovement = {
      id: Date.now(),
      type,
      productId,
      quantity: type === 'salida' ? -Math.abs(quantity) : Math.abs(quantity),
      note: note || 'Movimiento manual',
      timestamp: new Date().toISOString(),
    };
    setMovements((prev) => [newMovement, ...prev]);
  };

  const handleProductChange = (event) => {
    const { name, value } = event.target;
    setProductForm((prev) => ({
      ...prev,
      [name]: name === 'stock' || name === 'price' || name === 'minStock' ? Number(value) : value,
    }));
  };

  const handleCreateProduct = (event) => {
    event.preventDefault();

    if (!productForm.name || !productForm.sku) return;

    const nextProduct = {
      id: Date.now(),
      sku: productForm.sku,
      name: productForm.name,
      category: productForm.category,
      stock: Number(productForm.stock),
      price: Number(productForm.price),
      minStock: Number(productForm.minStock),
      shelfId: shelfForm.selectedShelfId || initialShelves[0]?.id,
    };

    setProducts((prev) => [nextProduct, ...prev]);
    setProductForm(defaultProductForm);
    setPosForm((prev) => ({ ...prev, productId: String(nextProduct.id) }));
  };

  const handleCreateShelf = () => {
    const nextShelf = {
      id: Date.now(),
      name: shelfForm.shelfName || `Estante ${String.fromCharCode(65 + shelves.length)}${shelves.length + 1}`,
      zone: shelfForm.shelfZone || 'Nueva zona',
    };
    setShelves((prev) => [...prev, nextShelf]);
    setShelfForm((prev) => ({ ...prev, selectedShelfId: nextShelf.id, shelfName: '', shelfZone: '' }));
  };

  const handleDeleteShelf = (shelfId) => {
    setShelves((prev) => prev.filter((shelf) => shelf.id !== shelfId));
    setProducts((prev) => prev.map((product) => (product.shelfId === shelfId ? { ...product, shelfId: null } : product)));
    setShelfForm((prev) => ({ ...prev, selectedShelfId: initialShelves[0]?.id ?? 0 }));
  };

  const handleDeleteProduct = (productId) => {
    setProducts((prev) => prev.filter((product) => product.id !== productId));
    setPosForm((prev) => ({ ...prev, productId: String(prev.productId === String(productId) ? initialProducts[0]?.id ?? '' : prev.productId) }));
  };

  const handleAssignProduct = () => {
    const productId = Number(shelfForm.assignProductId);
    if (!productId || !shelfForm.selectedShelfId) return;
    setProducts((prev) => prev.map((product) => (product.id === productId ? { ...product, shelfId: shelfForm.selectedShelfId } : product)));
    setShelfForm((prev) => ({ ...prev, assignProductId: '' }));
  };

  const handlePosChange = (event) => {
    const { name, value } = event.target;
    setPosForm((prev) => ({ ...prev, [name]: name === 'quantity' ? Number(value) : value }));
  };

  const handlePosSubmit = (event) => {
    event.preventDefault();
    const productId = Number(posForm.productId);
    const selectedProduct = products.find((product) => product.id === productId);
    if (!selectedProduct || !posForm.quantity) return;

    if (posForm.type === 'salida' && selectedProduct.stock < posForm.quantity) {
      return;
    }

    const delta = posForm.type === 'salida' ? -Math.abs(posForm.quantity) : Math.abs(posForm.quantity);
    setProducts((prev) => prev.map((product) => (product.id === productId ? { ...product, stock: Math.max(0, product.stock + delta) } : product)));
    addMovement({ productId, type: posForm.type, quantity: posForm.quantity, note: posForm.note });
    setPosForm((prev) => ({ ...prev, quantity: 1, note: '' }));
  };

  const inventoryTab = (
    <section className="card">
      <div className="tabs" aria-label="Selector de vistas">
        <button className={`tab ${activeTab === 'pos' ? 'active' : ''}`} type="button" onClick={() => setActiveTab('pos')}>POS</button>
        <button className={`tab ${activeTab === 'inventory' ? 'active' : ''}`} type="button" onClick={() => setActiveTab('inventory')}>Inventario</button>
        <button className={`tab ${activeTab === 'warehouse' ? 'active' : ''}`} type="button" onClick={() => setActiveTab('warehouse')}>Almacén</button>
      </div>

      {activeTab === 'pos' ? (
        <PosPanel products={products} form={posForm} onChange={handlePosChange} onSubmit={handlePosSubmit} />
      ) : activeTab === 'inventory' ? (
        <InventoryTable products={products} />
      ) : (
        <ShelfManager
          shelves={shelves}
          products={products}
          selectedShelfId={shelfForm.selectedShelfId}
          onCreateShelf={handleCreateShelf}
          onCreateProduct={(event) => {
            event.preventDefault();
            if (!shelfForm.productName || !shelfForm.productSku) return;
            const nextProduct = {
              id: Date.now(),
              sku: shelfForm.productSku,
              name: shelfForm.productName,
              category: 'General',
              stock: Number(shelfForm.productStock),
              price: Number(shelfForm.productPrice),
              minStock: Number(shelfForm.productMinStock),
              shelfId: shelfForm.selectedShelfId,
            };
            setProducts((prev) => [nextProduct, ...prev]);
            setShelfForm((prev) => ({ ...prev, productName: '', productSku: '', productStock: 0, productPrice: 0, productMinStock: 0 }));
            setPosForm((prev) => ({ ...prev, productId: String(nextProduct.id) }));
          }}
          onDeleteShelf={handleDeleteShelf}
          onDeleteProduct={handleDeleteProduct}
          onAssignProduct={handleAssignProduct}
          form={shelfForm}
          setForm={setShelfForm}
        />
      )}
    </section>
  );

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
        {inventoryTab}

        <aside className="card sidebar-panel">
          <div className="panel-header compact">
            <h3>Resumen</h3>
            <span className="status-pill subtle">Live</span>
          </div>
          <div className="summary-stack">
            <div className="summary-item">
              <span>Estantes</span>
              <strong>{shelves.length}</strong>
            </div>
            <div className="summary-item">
              <span>Productos críticos</span>
              <strong>{metrics.lowStockItems}</strong>
            </div>
            <div className="summary-item">
              <span>Último movimiento</span>
              <strong>{movements[0]?.type ?? 'Sin registro'}</strong>
            </div>
          </div>

          <div className="divider" />

          <div className="timeline-panel">
            <h3>Historial</h3>
            <MovementList products={products} movements={movements.slice(0, 5)} />
          </div>
        </aside>
      </div>
    </div>
  );
}

export default App;
