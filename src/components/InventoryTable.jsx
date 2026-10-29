const formatCurrency = (value) =>
  new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  }).format(value || 0);

const getStatus = (stock, minStock) => (stock <= minStock ? 'low' : 'ok');

function InventoryTable({ products }) {
  if (!products.length) {
    return <div className="empty-state">No hay productos registrados todavía.</div>;
  }

  return (
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
  );
}

export default InventoryTable;
