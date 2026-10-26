export const initialProducts = [
  { id: 1, sku: 'P-001', name: 'Espresso', category: 'Café', stock: 25, price: 3.6, minStock: 8 },
  { id: 2, sku: 'P-002', name: 'Pan Integral', category: 'Panadería', stock: 18, price: 2.4, minStock: 5 },
  { id: 3, sku: 'P-003', name: 'Leche Entera', category: 'Lácteos', stock: 12, price: 1.9, minStock: 5 },
  { id: 4, sku: 'P-004', name: 'Aceite de Oliva', category: 'Abarrotes', stock: 9, price: 8.5, minStock: 4 },
  { id: 5, sku: 'P-005', name: 'Agua 1L', category: 'Bebidas', stock: 30, price: 1.2, minStock: 10 },
];

export const initialMovements = [
  { id: 1, type: 'entrada', productId: 1, quantity: 12, note: 'Pedido de proveedor', timestamp: '2026-10-01T09:00:00' },
  { id: 2, type: 'salida', productId: 3, quantity: 4, note: 'Venta local', timestamp: '2026-10-01T12:30:00' },
  { id: 3, type: 'ajuste', productId: 2, quantity: -2, note: 'Ajuste por rotura', timestamp: '2026-10-02T16:10:00' },
];
