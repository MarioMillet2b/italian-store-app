# Italian Store App

Aplicación móvil (Android) inicializada con Capacitor para gestionar un almacén, controlar existencias y operar como mini POS.

## Objetivo

Crear un MVP para un negocio pequeño o mediano con estas capacidades:

- Inventario de productos con stock, precio y alertas por stock mínimo
- Entradas, salidas y ajustes de inventario
- Resumen del valor total del almacén
- Registro de movimientos por producto
- Base para integración futura con la web del negocio (sincronización de ventas físicas y descuento de stock en la tienda online)
- Preparación para expandir con etiquetas impresas, gestión de estructura del almacén y más módulos de negocio

## Stack

- React + Vite
- Capacitor para Android
- CSS nativo sin dependencias extras

## Scripts

- `npm install`
- `npm run dev` — ejecuta la app web
- `npm run build` — compila la versión de producción
- `npx cap sync android` — sincroniza la app web con Android
- `npx cap open android` — abre el proyecto Android en Android Studio

## MVP implementado

- Dashboard con métricas de almacén
- Alta de productos con SKU, categoría, stock y precio
- Registro manual de entradas/salidas/ajustes
- Estado de stock bajo con indicadores visuales
- Historial de movimientos
- Estructura lista para continuar con conexión entre tienda física y web

## Siguiente evolución sugerida

1. Persistencia local con SQLite o Capacitor Storage
2. Autenticación de usuarios y roles (encargado de almacén, administrador)
3. Mini POS con carrito y cobro
4. Sincronización con la tienda web para descontar stock por venta física
5. Generación de etiquetas para impresión
6. Estructura de almacenes por secciones/zona
