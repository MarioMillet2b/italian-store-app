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


Para cambios de interfaz, leer docs/UX_E_IDIOMAS.md. Todos los textos de interfaz
van en strings.xml y values-es/strings.xml. Las categorías y traducciones de datos
vienen del servidor; nunca traducir IDs o alterar precios/cantidades al cambiar idioma.
Usar el catálogo agrupado por producto, sin reconstruir grupos a partir de una
página parcial de variantes. Las pruebas UI solo ejecutan un gateway sintético en
emuladores; gatewayOverride no debe activarse mediante ajustes ni intents del APK.
