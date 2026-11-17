# Pruebas con Postman

Las colecciones usan datos ficticios, generan emails nuevos por corrida y capturan IDs y JWT de las respuestas. No requieren conocer los IDs de la base. Los valores del environment coinciden con la sección **Datos de prueba** del README principal. La API de Usuarios debe estar levantada y PostgreSQL disponible.

## Archivos

| Archivo | Alcance |
|---|---|
| [Usuarios](DeliveryGo-Usuarios.postman_collection.json) | 45 solicitudes automáticas para CU-01 a CU-10 y cuatro pasos de restablecimiento con token recibido |
| [Notificaciones](DeliveryGo-Integracion-Notificaciones.postman_collection.json) | Bienvenida y recuperación completas entre los dos servicios por HTTP |
| [Catálogo](DeliveryGo-Integracion-Catalogo.postman_collection.json) | JWT real de Usuarios aceptado por Catálogo; permisos ADMIN/CLIENTE y 401 sin credencial |
| [Environment local](Local.postman_environment.json) | URLs y credenciales de ejemplo, exclusivas de la demo local |

## Postman de escritorio

1. Importar las colecciones y el environment; seleccionarlo.
2. Ajustar `baseUrl`: `http://localhost:8089` con Usuarios solo, o `http://localhost:8080` con Notificaciones en 8089.
3. Ejecutar en orden la carpeta **01 Recorrido automatico** de Usuarios con Collection Runner. No ejecutar toda la colección de Usuarios sin configurar el token del paso siguiente.
4. Para la carpeta **02 Restablecimiento con token recibido**, copiar el token recibido en `recoveryToken`. En modo mock local se obtiene del log sólo si se arrancó con `MOCK_LOG_RECOVERY_TOKEN=true`. Conservar las variables de la corrida anterior (opción de persistir variables del Runner); alternativamente ejecutar los pasos con **Send**, en orden, en la misma colección. No volver a ejecutar la carpeta 01 entre la solicitud y el restablecimiento, porque genera otro usuario.
5. Para una prueba totalmente automática del restablecimiento, usar la colección de integración con Notificaciones. Se autentica como el destinatario y obtiene el token del mensaje del envío simulado. Esto no representa una recuperación por correo real: el canal EMAIL del módulo externo es simulado.

## Postman CLI

Verificado con **Postman CLI 1.73.0**. La selección de carpeta usa `-i` (no `--folder`). No hace falta iniciar sesión en Postman para ejecutar los archivos locales. El aviso sobre credenciales de Postman corresponde a la publicación en su nube, no a la autenticación de DeliveryGo.

Desde la raíz del repositorio:

```powershell
postman collection run doc/postman/DeliveryGo-Usuarios.postman_collection.json -e doc/postman/Local.postman_environment.json -i "01 Recorrido automatico" --reporters cli
```

Con ambos módulos configurados según [la guía de integración](../integracion/README.md):

```powershell
postman collection run doc/postman/DeliveryGo-Integracion-Notificaciones.postman_collection.json -e doc/postman/Local.postman_environment.json --env-var baseUrl=http://localhost:8080 --delay-request 250 --reporters cli
postman collection run doc/postman/DeliveryGo-Integracion-Catalogo.postman_collection.json -e doc/postman/Local.postman_environment.json --env-var baseUrl=http://localhost:8080 --reporters cli
```

Notificaciones se procesa en segundo plano. La colección consulta el historial hasta 20 veces, con la pausa configurada; si no llega el mensaje, falla la prueba. No sustituye la respuesta por un mock ni omite el error. La colección de Catálogo crea una categoría de prueba y la elimina al terminar.

Para conservar un reporte sin credenciales:

```powershell
New-Item -ItemType Directory -Force target/postman | Out-Null
postman collection run doc/postman/DeliveryGo-Usuarios.postman_collection.json -e doc/postman/Local.postman_environment.json -i "01 Recorrido automatico" --reporters cli,json --reporter-json-export target/postman/usuarios.json
python doc/postman/resumir_resultado.py target/postman/usuarios.json target/postman/usuarios-resumen.json
```

Los reportes completos contienen JWT y cuerpos con contraseñas de prueba: se guardan en `target/`, excluido de Git. La evidencia compartida sólo incluye nombres de solicitudes, estados HTTP y cantidades de comprobaciones. Las corridas dejan usuarios ficticios y direcciones con baja lógica; no borran usuarios ni reinician la base.
