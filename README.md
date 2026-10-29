# Floro Milano · app Android

Mini POS, inventario por ubicaciones y etiquetas para el móvil de Floro.
Kotlin + Jetpack Compose. Requiere conexión; no cobra ni emite documentos fiscales.

## Compilar
Instalar Android Studio, JDK 17 o superior y Android SDK 36. Abrir este proyecto
o crear local.properties con sdk.dir apuntando al SDK instalado.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK: app/build/outputs/apk/debug/app-debug.apk. GitHub Actions publica el APK
como artifact de cada ejecución correcta. Se distribuye para pruebas, sin
subirlo al repositorio. La versión release necesita firma privada estable;
no guardar keystores ni claves en Git. Ver docs/PRUEBAS.md antes de reinstalar.

## Conectar
En la tienda: administrador → Magazzino e app → Collega app Android.
Crear una clave del dispositivo y copiarla solo a la app. En Impostazioni,
configurar el origen HTTPS de staging o producción y la clave. La app comprueba
la conexión antes de guardar. No hay dominio ni credencial de producción embebidos.

Métodos de pago y ubicaciones son editables desde Magazzino. Inventario permite
entradas, ajustes, traslados, editar variantes y crear productos en borrador.
La edición avanzada de fotos y atributos existentes sigue disponible en el panel
web. Las etiquetas Code128 se pueden exportar como PNG o imprimir con Android.
Compatibilidad directa con T50m Pro/Katasymbol y Brother pendiente de prueba.

## Documentación
- [Arquitectura y decisiones](docs/ARQUITECTURA.md)
- [Pruebas físicas y limitaciones](docs/PRUEBAS.md)
- [Instrucciones para agentes](AGENTS.md)
- Backend: https://github.com/MarioMillet2b/italian-store
- Contrato: docs/OPERACIONES_TIENDA.md en el backend.
- Futuro MCP y proveedores: docs/MCP_TIENDA_FUTURO.md y docs/ENVIOS_Y_PROVEEDORES.md.

Nunca enviar la clave por chat ni adjuntarla a capturas. TLS se verifica siempre;
los redirects no reciben Authorization. Hay protección persistente frente a
reintentos de ventas y movimientos. Ver arquitectura antes de modificarla.
