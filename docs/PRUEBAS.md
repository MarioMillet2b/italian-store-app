# Guía de prueba con Mario y Floro
Usar staging, nunca una venta falsa en producción.
1. Administrador verificado: abrir Magazzino e app y crear clave por dispositivo.
2. Instalar APK de pruebas y conectar URL HTTPS de staging + clave.
3. Crear ubicaciones, renombrar métodos y desactivar uno; comprobar persistencia.
4. Mover unidades no asignadas a dos ubicaciones; comprobar total intacto.
5. Buscar producto y escanear Code128. Dos tallas/colores deben ser líneas distintas.
6. Registrar venta con método activo. Revisar precio, stock y diario en web.
7. Reservar última unidad desde carrito web; el POS debe rechazar venderla.
8. Cortar conexión antes de confirmar: no permitir registrar. Cortarla durante
   confirmación: recuperar la misma solicitud sin duplicar venta.
9. Cerrar/reabrir app con solicitud incierta; debe conservarse. No permitir
   cambiar de tienda, sí renovar clave del mismo usuario/origen.
10. Registrar devolución y repetir petición: una sola reposición. Rechazar más
    unidades que las vendidas. Efectuar reembolso manual si corresponde.
11. Revocar/caducar clave; debe bloquear operaciones.
12. Probar PNG compartido y PDF/Print Framework en T50m Pro y Brother. Confirmar
    modelo, ancho, conectividad y recepción desde Katasymbol, no asumirlo.
13. Antes de uso real: definir firma estable APK y conservación privada de clave.
    Las instalaciones debug de CI pueden tener distinta firma; no desinstalar
    una app con una solicitud sin resolver para instalar otra build.

Las pruebas automatizadas no sustituyen las comprobaciones con móviles,
cámara, Google Play Services y hardware de impresión.

