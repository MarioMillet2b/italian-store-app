function ShelfManager({ shelves, products, onCreateShelf, onCreateProduct, onDeleteShelf, onDeleteProduct, onAssignProduct, form, setForm, selectedShelfId }) {
  const shelfProducts = products.filter((product) => product.shelfId === selectedShelfId);

  return (
    <div className="shelf-manager">
      <div className="section-topbar">
        <h3>Almacén</h3>
      </div>

      <div className="two-col compact-form">
        <label>
          Nombre del estante
          <input
            value={form.shelfName}
            onChange={(event) => setForm((prev) => ({ ...prev, shelfName: event.target.value }))}
            placeholder="Estante A1"
          />
        </label>
        <label>
          Zona
          <input
            value={form.shelfZone}
            onChange={(event) => setForm((prev) => ({ ...prev, shelfZone: event.target.value }))}
            placeholder="Café y desayuno"
          />
        </label>
      </div>
      <div className="button-row">
        <button className="secondary" type="button" onClick={onCreateShelf}>+ Crear estante</button>
      </div>

      <div className="shelf-grid">
        {shelves.map((shelf) => (
          <button
            key={shelf.id}
            type="button"
            className={`shelf-card ${selectedShelfId === shelf.id ? 'selected' : ''}`}
            onClick={() => setForm((prev) => ({ ...prev, selectedShelfId: shelf.id }))}
          >
            <div className="shelf-name">{shelf.name}</div>
            <div className="small-text">{shelf.zone}</div>
            <div className="shelf-meta">
              <span>{products.filter((product) => product.shelfId === shelf.id).length} productos</span>
              <span onClick={(event) => { event.stopPropagation(); onDeleteShelf(shelf.id); }}>Eliminar</span>
            </div>
          </button>
        ))}
      </div>

      <div className="subsection">
        <h4>Productos del estante</h4>
        <div className="stack-list">
          {shelfProducts.length ? shelfProducts.map((product) => (
            <div key={product.id} className="stack-item">
              <div>
                <strong>{product.name}</strong>
                <div className="small-text">{product.sku}</div>
              </div>
              <button type="button" className="link-button" onClick={() => onDeleteProduct(product.id)}>Quitar</button>
            </div>
          )) : <div className="small-text">Este estante todavía no tiene productos.</div>}
        </div>
      </div>

      <div className="subsection">
        <h4>Asignar producto</h4>
        <form className="form-grid compact" onSubmit={onCreateProduct}>
          <label>
            Nombre
            <input name="name" value={form.productName} onChange={(event) => setForm((prev) => ({ ...prev, productName: event.target.value }))} />
          </label>
          <div className="two-col">
            <label>
              SKU
              <input name="sku" value={form.productSku} onChange={(event) => setForm((prev) => ({ ...prev, productSku: event.target.value }))} />
            </label>
            <label>
              Stock
              <input type="number" name="stock" value={form.productStock} onChange={(event) => setForm((prev) => ({ ...prev, productStock: Number(event.target.value) }))} />
            </label>
          </div>
          <div className="two-col">
            <label>
              Precio
              <input type="number" step="0.01" name="price" value={form.productPrice} onChange={(event) => setForm((prev) => ({ ...prev, productPrice: Number(event.target.value) }))} />
            </label>
            <label>
              Min.
              <input type="number" name="minStock" value={form.productMinStock} onChange={(event) => setForm((prev) => ({ ...prev, productMinStock: Number(event.target.value) }))} />
            </label>
          </div>
          <div className="button-row">
            <button className="primary" type="submit">Guardar producto</button>
          </div>
        </form>
      </div>

      <div className="subsection">
        <h4>Relacionar producto existente</h4>
        <div className="assign-box">
          <select
            value={form.assignProductId}
            onChange={(event) => setForm((prev) => ({ ...prev, assignProductId: event.target.value }))}
          >
            <option value="">Selecciona un producto</option>
            {products
              .filter((product) => product.shelfId !== selectedShelfId)
              .map((product) => (
                <option key={product.id} value={String(product.id)}>{product.name}</option>
              ))}
          </select>
          <button type="button" className="secondary" onClick={onAssignProduct}>Asignar al estante</button>
        </div>
      </div>
    </div>
  );
}

export default ShelfManager;
