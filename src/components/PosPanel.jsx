function PosPanel({ products, form, onChange, onSubmit }) {
  const selectedProduct = products.find((product) => product.id === Number(form.productId));

  return (
    <div className="pos-panel">
      <div className="panel-header">
        <h2>Punto de venta</h2>
        <span className="status-pill subtle">Caja</span>
      </div>

      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Producto
          <select name="productId" value={form.productId} onChange={onChange}>
            {products.map((product) => (
              <option key={product.id} value={String(product.id)}>
                {product.name}
              </option>
            ))}
          </select>
        </label>

        <div className="two-col">
          <label>
            Operación
            <select name="type" value={form.type} onChange={onChange}>
              <option value="salida">Venta / salida</option>
              <option value="entrada">Compra / entrada</option>
              <option value="ajuste">Ajuste</option>
            </select>
          </label>

          <label>
            Cantidad
            <input type="number" name="quantity" min="1" value={form.quantity} onChange={onChange} />
          </label>
        </div>

        <label>
          Nota
          <input name="note" value={form.note} onChange={onChange} placeholder="Pedido, venta, revisión..." />
        </label>

        <div className="pos-summary">
          <span>Stock actual</span>
          <strong>{selectedProduct ? selectedProduct.stock : 0} uds.</strong>
        </div>

        <div className="button-row">
          <button className="primary" type="submit">Registrar operación</button>
        </div>
      </form>
    </div>
  );
}

export default PosPanel;
