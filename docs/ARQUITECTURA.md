# Arquitectura y mantenimiento

Kotlin 2.2.20, Jetpack Compose, AGP 8.13.0, Gradle 8.13, SDK 36, min Android 8.
Java 17 o superior para compilar. Runtime de Android, sin servidor propio.

## Archivos
- StoreViewModel: estado en memoria, heartbeat de 10 segundos, catálogo paginado,
  operaciones y resolución de solicitudes inciertas.
- StoreApi: HttpURLConnection del sistema, JSON nativo, coroutines IO, sin
  redirects ni logging de credenciales. Timeouts 10s conexión / 15s lectura.
- SecureVault: AES-GCM con clave no exportable en Android Keystore; preferencias
  privadas cifradas; backups Android deshabilitados.
- StoreUrl: solo origen HTTPS sin rutas, query ni credenciales.
- MainActivity: Compose, italiano, venta/inventario/magazzino/impostazioni.
- Labels: Code128 con ZXing, PNG compartido mediante proveedor privado y permisos
  temporales; Android Print Framework para impresión/PDF.
- MoneyInput: validación decimal exacta en céntimos.

No se guarda catálogo local ni se autoriza una venta con stock del teléfono.
El servidor valida precio y existencias y respeta reservas web. App exige
conexión, con banner permanente al detectar pérdida o rechazo de acceso.
El heartbeat detecta la desconexión en aproximadamente 10s más timeout; una
acción puede fallar antes y bloquear de inmediato.

## Solicitud de resultado incierto
Guardar origen, ruta, payload y UUID cifrados antes de enviar. Una única solicitud
en vuelo; ante timeout no generar otro UUID. Persistirla también al cerrar la app.
El servidor devuelve el mismo resultado al reintentarlo. Bloquear nuevas
operaciones hasta resolver. Es recuperación de una operación online, no venta
offline ni sincronización automática. Confirmar respuesta antes de vaciar venta.

Una respuesta 422 de validación permite corregir la solicitud. Un fallo de
autenticación o límite de peticiones conserva el pendiente: una venta anterior
podría haberse confirmado aunque su respuesta no llegara. Primero consultar
GET /operations/{request_id}; solo si no existe se reenvía el mismo POST. Un fallo 5xx
puede ocultar un commit exitoso: conservar solicitud. Un 409 requiere investigar
la identidad/payload; no descartarlo silenciosamente. Nunca editar el payload
pendiente. Para rotar una clave, conservar el mismo origen y el mismo usuario
administrador; cambiar de URL requiere resolver pendientes.
Al cambiar de tienda se limpian catálogo/venta; las credenciales candidatas se
validan antes de reemplazar las anteriores.

## Dependencias
Compose/Activity/Lifecycle/coroutines: Android y Kotlin mantenidos oficialmente.
Google Code Scanner: cámara sin permiso propio y UI de Google Play Services.
En Pixel y Samsung con Play Services; primer uso puede necesitar descargar el
módulo. Siempre existe búsqueda manual como respaldo.
ZXing core: solo generar códigos Code128. JSON y HTTP son nativos de Android.
JUnit y org.json son dependencias exclusivas de pruebas.
No SDK de impresora hasta confirmar protocolo y licencia de cada modelo.

## Contrato
Ver docs/OPERACIONES_TIENDA.md del backend. Base /api/operations/v1.
API v1 y moneda de /context. Precios, métodos y ubicaciones proceden del servidor.
Drafts creados en móvil parten con stock cero; registrar entrada explícita.
No borrar historial: archivar productos, desactivar métodos/ubicaciones.

## Comprobación física pendiente
Pixel 6a de Mario y Galaxy S24 o superior de Floro. Revisar teclado, tamaños de
texto, escaneo, suspensión/reanudación, expiración de clave, desconexión durante
venta, rotación de credencial y etiquetas con ambas impresoras.
Katasymbol no se da por compatible solo porque acepte compartir una imagen.
No hay SDK Bluetooth/USB de T50m Pro/Brother integrado en esta versión.

