# Revisión de experiencia Android 0.2

## Contexto y evidencia inicial

Cliente: propietario que registra ventas y gestiona prendas desde Android, con
conexión al servidor. Objetivo: encontrar prendas, elegir variantes y revisar la
venta con pocos pasos. Se mantiene la marca de Floro y Android nativo.

Se capturó la aplicación anterior en un emulador Pixel 6a API29 con catálogo
sintético. La primera serie se rechazó porque un aviso de System UI tapaba la
interfaz. La segunda serie se revisó sin ese aviso: catálogo, carrito e inventario.
No se usaron ventas ni credenciales de una tienda real.

Hallazgos observados:

- Cada talla repetía una tarjeta de gran altura, sin imagen; una pantalla mostraba
  aproximadamente dos variantes. Prioridad alta: agrupar por producto.
- La búsqueda exigía pulsar Buscar y no había categorías. Prioridad alta:
  categorías visibles y búsqueda inmediata.
- El carrito se insertaba antes del catálogo y desplazaba las prendas fuera de
  pantalla. Prioridad alta: acceso fijo al total y revisión en panel propio.
- Inventario repetía todos los botones por variante. Prioridad media: presentar
  primero las prendas y concentrar las acciones en la ficha de variante.
- La interfaz solo tenía italiano. Prioridad alta: recursos completos ES/IT,
  selección persistente y conservación de la venta al cambiar idioma.

## Verificación requerida

Ejecutar `scripts/android-ui-check.sh` en emulador e inspeccionar sus capturas.
Las pruebas verifican categorías, variantes, cantidades, carrito, búsqueda,
idioma y ausencia de escrituras. CI conserva imágenes como artifacts.
La compilación, lint y tests JVM no sustituyen la revisión visual. Una pantalla
con aviso del sistema o carga no sirve como evidencia del resultado.

Pendiente de pruebas físicas: cámara, impresoras y valoración del propietario en
su Samsung. No se afirma compatibilidad de impresoras ni auditoría completa de
accesibilidad a partir de estas pruebas.
