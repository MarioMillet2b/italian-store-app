# Catálogo visual e idiomas · versión 0.2.0

## Objetivo
Encontrar y vender prendas sin buscar una variante cada vez. Se conserva Android
nativo, conexión obligatoria, un inventario del servidor y el control de reintentos.

## Navegación
- Vender e Inventario muestran un producto por tarjeta, con foto y todas sus variantes.
- Categorías desplazables, búsqueda con espera de 300 ms, orden por nombre/precio,
  filtro de disponibilidad y filtro de existencias bajas en Inventario.
- Vista de tarjetas o lista compacta; la preferencia se conserva en el dispositivo.
- Tocar una tarjeta abre un panel de variantes con precio, disponibilidad y cantidad.
  El botón + de un producto con una única variante añade una unidad directamente.
- La barra de venta permanece al pie. El carrito permite sumar, restar y quitar líneas,
  seleccionar pago y registrar tras revisar el total. No procesa el pago.
- Escanear una variante en Vender añade esa variante; en Inventario abre su ficha.
- Almacén separa ubicaciones, métodos e histórico. Los ajustes de producto usan estados
  con etiquetas humanas, sin pedir códigos como draft/published al usuario.

## Español e italiano
strings.xml es el catálogo italiano; values-es/strings.xml contiene español.
Ambos tienen las mismas claves y argumentos, comprobados por tests JVM.
La app sigue inicialmente el idioma del teléfono (los demás idiomas usan italiano)
y permite elegir teléfono / Español / Italiano. Android 13+ usa LocaleManager;
versiones anteriores conservan la selección y recrean el contexto de la actividad.
Se conservan carrito, cantidades, origen y solicitudes pendientes al cambiar idioma.
Accept-Language acompaña cada petición. El backend devuelve categorías, nombres,
atributos y errores de negocio localizados. Los nombres configurados por el dueño,
notas e instantáneas históricas son datos: no se traducen ni reescriben automáticamente.
Los nombres canónicos de atributos nuevos tampoco deben cambiarse como si fueran
etiquetas: traducir un identificador puede crear otro atributo en el servidor.

## Archivos y mantenimiento
- CatalogScreen: búsqueda, categorías y tarjetas/lista.
- SaleSheets: variante y revisión de venta.
- WarehouseScreen: gestión y registros.
- SettingsScreen / AppLanguage: conexión e idiomas.
- CatalogParser / CatalogModels: contrato de productos agrupados.
- UiComponents: imágenes públicas con caché RAM de 6 MB, cantidades y formato monetario.
- OperationDialogs / ProductDialog / ProductDataDialog: acciones puntuales.

No hay librerías nuevas de imágenes o red en tiempo de ejecución. Las miniaturas
usan HTTP del sistema, sin Authorization, con TLS y sin redirecciones. Se limita
el tamaño descargado y se reduce la imagen antes de almacenarla en caché RAM.

## Backend necesario
Esta versión necesita GET /api/operations/v1/catalog y las categorías de /context.
Desplegar el cambio del backend en staing antes de probar el APK. /variants sigue
existiendo para la versión 0.1.0; no hay migraciones de datos en esta actualización.
La paginación es por producto: nunca dividir sus tallas entre páginas de variantes.

## Validación
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
Los tests JVM cubren formatos y traducciones, agrupación, precios e identidad de
variantes y límites de ubicación. UiExperienceTest recorre categorías, selección,
cantidades, carrito, búsqueda y cambio de idioma sin enviar una venta.

scripts/android-ui-check.sh solo admite emuladores. En CI usa KVM y un perfil
nuevo; localmente puede usar FLORO_UI_SERIAL=emulator-5556. Instala APK de app y de
instrumentación y borra únicamente ese perfil de prueba. No usarlo en un móvil real.
StoreApplication.gatewayOverride solo lo asigna la instrumentación; nunca añadir
un ajuste, intent ni un arranque de producción que active datos ficticios.
Las imágenes de prueba son derivados de fotos de Floro aprobadas, solo en androidTest.

Inspeccionar las capturas: rechazar pantallas de carga, avisos del sistema o diálogos
que tapen la app. Los tests de emulador no acreditan cámara, impresoras, rendimiento
en el móvil de Floro ni accesibilidad completa. Seguir PRUEBAS.md en los móviles reales.

## Categorie e organizzazione

Il server restituisce i conteggi per categoria e discendenti. I filtri nascondono
le categorie vuote; tutte rimangono disponibili per classificare un nuovo prodotto
o modificarne la categoria. Le varianti ereditano la categoria del prodotto.
