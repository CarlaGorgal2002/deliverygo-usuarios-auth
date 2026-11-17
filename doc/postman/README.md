# Pruebas Postman — Usuarios y Autenticación

Credenciales ficticias, sólo para desarrollo local. No usar en ambientes compartidos.

1. Desde la raíz, levantar PostgreSQL con `docker compose up -d`.
2. Configurar las variables de entorno del proceso Java (en IDE o PowerShell):

```powershell
$env:ADMIN_EMAIL="admin@deliverygo.test"
$env:ADMIN_PASSWORD="AdminDemo2026!"
$env:MODULE_KEY_PEDIDOS="pedidos-demo-local-2026"
$env:NOTIFICACIONES_MODO="mock"
$env:MOCK_LOG_RECOVERY_TOKEN="true"
```

3. Arrancar el backend en puerto 8089. Si se usa IntelliJ, configurar esas variables en Run Configuration (las variables de una terminal no se propagan al IDE). Si el administrador ya existe, usar sus credenciales locales en el environment. No borrar la base para ejecutar estas pruebas.
4. Importar ambos JSON en Postman y seleccionar DeliveryGo - Local. Ejecutar sólo la carpeta `Recorrido automatico` primero. Genera un email único y guarda IDs y JWT automáticamente.
5. Para CLI 1.73.0, desde la raíz:

```powershell
postman --version
postman collection run doc/postman/DeliveryGo_Usuarios.postman_collection.json -e doc/postman/DeliveryGo_Local.postman_environment.json -i "Recorrido automatico"
```

6. Para completar el restablecimiento exitoso, en Postman Desktop ejecutar el recorrido automático y conservar sus variables; copiar el token de recuperación del log del backend en `recoveryToken`, luego ejecutar la carpeta `Restablecimiento manual`. No compartir el token. El CLI no recibe el token por HTTP: es intencional. Para probar esa carpeta por CLI se necesitan las variables de la misma corrida (email, recoveryToken) en una copia local del environment. No subir valores capturados.



## Resultado de la verificación

Pruebas ejecutadas el 16/11/2026.

- Postman CLI: 1.90.0
- Base de datos: PostgreSQL 16 (Docker)
- Backend: Spring Boot, Java 25
- Solicitudes ejecutadas: 24
- Validaciones exitosas: 61
- Fallos: 0
- Tiempo total de ejecución: 4,1 segundos

El recorrido automático se ejecutó exitosamente contra el backend local, utilizando el mock de Notificaciones.

La verificación manual del restablecimiento exitoso de contraseña queda pendiente.
