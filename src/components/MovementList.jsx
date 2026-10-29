function MovementList({ products, movements }) {
  if (!movements.length) {
    return <div className="empty-state">Todavía no hay movimientos.</div>;
  }

  return (
    <div className="movement-list">
      {movements.map((movement) => {
        const product = products.find((item) => item.id === movement.productId);

        return (
          <div key={movement.id} className={`movement-item ${movement.type}`}>
            <div className="movement-header">
              <strong>{movement.type.toUpperCase()}</strong>
              <span className="movement-quantity">
                {movement.type === 'salida' ? '-' : '+'}
                {Math.abs(movement.quantity)}
              </span>
            </div>
            <div>{product?.name ?? 'Producto no encontrado'}</div>
            <div className="small-text">
              {new Date(movement.timestamp).toLocaleString('es-ES')}
            </div>
            <div className="small-text">{movement.note}</div>
          </div>
        );
      })}
    </div>
  );
}

export default MovementList;
