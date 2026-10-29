function ProductForm({ form, onChange, onSubmit }) {
  return (
    <div>
      <h3>Alta de producto</h3>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          SKU
          <input name="sku" value={form.sku} onChange={onChange} placeholder="P-010" />
        </label>
        <label>
          Nombre
          <input name="name" value={form.name} onChange={onChange} placeholder="Queso Parmesano" />
        </label>
        <div className="two-col">
          <label>
            Categoría
            <input name="category" value={form.category} onChange={onChange} />
          </label>
          <label>
            Stock mínimo
            <input type="number" name="minStock" value={form.minStock} onChange={onChange} />
          </label>
        </div>
        <div className="two-col">
          <label>
            Cantidad
            <input type="number" name="stock" value={form.stock} onChange={onChange} />
          </label>
          <label>
            Precio
            <input type="number" step="0.01" name="price" value={form.price} onChange={onChange} />
          </label>
        </div>
        <div className="button-row">
          <button className="primary" type="submit">Guardar producto</button>
        </div>
      </form>
    </div>
  );
}

export default ProductForm;
