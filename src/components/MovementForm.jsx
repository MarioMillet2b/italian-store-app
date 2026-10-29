function MovementForm({ products, form, onChange, onSubmit }) {
  return (
    <div>
      <h3>Movimiento de stock</h3>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Producto
          <select name="productId" value={form.productId} onChange={onChange} disabled={!products.length}>
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
            <select name="type" value={form.type} onChange={onChange}>
              <option value="entrada">Entrada</option>
              <option value="salida">Salida</option>
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
        <div className="button-row">
          <button className="primary" type="submit" disabled={!products.length}>
            Registrar movimiento
          </button>
        </div>
      </form>
    </div>
  );
}

export default MovementForm;
