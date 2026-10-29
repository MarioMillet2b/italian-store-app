# Trabajo en este repositorio
App Android nativa para Floro Milano. Código de negocio servidor en
MarioMillet2b/italian-store, rama de integración staing. No modificar producción
ni main de la tienda sin petición explícita de Mario.

Leer README.md y docs/ARQUITECTURA.md antes de cambiar flujos de inventario o
seguridad. Mantener Kotlin/Compose y dependencias mínimas. No agregar frameworks
HTTP, bases locales u offline queues sin una necesidad documentada.
No hay ventas offline. Mantener HTTPS, TLS verificado, tokens cifrados y
reintentos con UUID persistido. Nunca registrar secretos ni seguir redirects
con Authorization. No mezclar solicitudes ni catálogos entre tiendas.

Antes de entregar: ./gradlew testDebugUnitTest lintDebug assembleDebug.
Actualizar documentación si cambia la API, compilación o flujo del cliente.
No afirmar compatibilidad de impresora sin pruebas con el modelo exacto.
Las ventas son registros internos: no adición de cobros ni recibos fiscales.
No guardar keystores, local.properties, tokens o APK en el historial de Git.

