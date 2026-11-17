# Entrega Swagger del Grupo 02

Contrato de **Usuarios y Autenticación** de DeliveryGo, preparado el **30 de octubre de 2026**, versión **1.1.0**, en OpenAPI 3.0.3 y YAML. Incluye las diecisiete operaciones de los diez casos de uso (los cinco CORE, CU-01 a CU-05, y los cinco de evolución, CU-06 a CU-10), sus DTO, ejemplos, validaciones, seguridad y respuestas de error.

**Archivo principal para entregar: [deliverygo-usuarios.yaml](deliverygo-usuarios.yaml).** Es autónomo: todas sus referencias están dentro del mismo archivo. Corresponde al Grupo 02 académico, denominado Equipo 1 del ecosistema en la guía funcional.

Este contrato parte del diseño de la primera entrega y lo completa con CU-06 a CU-10 ([DISENO_CU_EVOLUCION.md](DISENO_CU_EVOLUCION.md)). El backend del repositorio implementa las diecisiete operaciones y la Swagger generada por Springdoc coincide con él (operaciones, códigos de respuesta y seguridad). El comportamiento del servidor lo comprueban las pruebas automáticas descritas en [VERIFICACION.md](VERIFICACION.md). Lo que sigue pendiente de acuerdo externo está en [ACUERDOS_Y_BRECHAS.md](ACUERDOS_Y_BRECHAS.md).

## Archivos de la entrega

| Archivo | Contenido |
|---|---|
| `deliverygo-usuarios.yaml` | Contrato completo; 17 operaciones |
| `DISENO_CU_EVOLUCION.md` | Diseño de CU-06 a CU-10: reglas RN-11 a RN-15, entidad ENT-04, decisiones DM-13 a DM-17, fichas, secuencias y matriz de trazabilidad |
| `TRAZABILIDAD_Y_FUENTES.md` | Correspondencia con API, CU, RN, SRV, DTO y los 23 adjuntos |
| `ACUERDOS_Y_BRECHAS.md` | Decisiones de contrato, integración JWT, estado de la implementación y pendientes |
| `VERIFICACION.md` | Pruebas automáticas del backend, comparación con Swagger y recorrido para la demo |

## Cómo abrir los YAML

1. Importar `deliverygo-usuarios.yaml` en Swagger Editor o en una herramienta que soporte OpenAPI 3.0.3. También puede importarse en Postman como definición OpenAPI.
2. Revisar las secciones **Autenticación**, **Usuarios** y **Direcciones**. El contrato puede leerse sin ejecutar Java ni Maven.
3. Para ejecutar solicitudes, seleccionar el servidor correcto. El puerto local **8089** coincide con `application.properties`; la URL de integración es configurable y todavía no identifica un despliegue real. Con `mvn spring-boot:run`, la Swagger UI del propio servicio está en `http://localhost:8089/swagger-ui.html`.
4. Registrar un usuario e iniciar sesión usando `email` y `password`. Copiar el `token` de la respuesta real y pegarlo en **Authorize**, sin escribir el prefijo `Bearer`.
5. Usar el `usuarioId` real en las rutas del perfil y direcciones. El token incluido en los ejemplos tiene una firma ficticia y no permite iniciar sesión.

Para probar los casos de evolución en local, definir antes de arrancar `ADMIN_EMAIL` y `ADMIN_PASSWORD` (primer administrador), `MODULE_KEY_PEDIDOS` y las demás `MODULE_KEY_*` (claves con las que cada módulo pide su token, de al menos 16 caracteres) y, para ver el token de recuperación sin Notificaciones real, `MOCK_LOG_RECOVERY_TOKEN=true`. En **Authorize** hay dos esquemas: `bearerAuth` (JWT), que para una persona se obtiene con el login y para un módulo con `POST /api/v1/auth/module-token`, y `moduleApiKey` (`X-Api-Key`), la clave del módulo como alternativa para sus consultas de lectura.

Swagger UI permite explorar la API, mientras que OpenAPI es la definición formal de sus operaciones. La autenticación HTTP Bearer se declara con `type: http`, `scheme: bearer`; las cinco operaciones públicas (registro, login, token de módulo y las dos de recuperación de contraseña) la desactivan mediante `security: []`; API-03 y API-05 admiten el Bearer de una persona o la credencial de un módulo (Bearer o `X-Api-Key`), y API-16 sólo la de un módulo. Referencias técnicas: [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3) y [Swagger Bearer Authentication](https://swagger.io/doc/specification/v3_0/authentication/bearer-authentication/).

## Operaciones incluidas

Todas las rutas comienzan con `/api/v1`. Son públicos el registro, el login y la recuperación de contraseña. API-14, API-15 y las demás exigen JWT; las escrituras sobre un perfil o sus direcciones exigen además ser el titular; la administración (API-10 y API-11) exige ser ADMIN. Los módulos consumidores, con su JWT de módulo o su `X-Api-Key`, sólo leen (API-03, API-05 para Pedidos, y API-16).

| API | Método y ruta | Éxito | Caso de uso |
|---|---|---|---|
| API-01 | `POST /auth/register` | 201 + usuario | CU-01 |
| API-02 | `POST /auth/login` | 200 + sesión | CU-02 |
| API-03 | `GET /users/{id}` | 200 + usuario | CU-03 |
| API-04 | `PUT /users/{id}` | 200 + usuario actualizado | CU-04 |
| API-05 | `GET /users/{id}/addresses` | 200 + lista, incluida `[]` | CU-05 |
| API-06 | `POST /users/{id}/addresses` | 201 + dirección | CU-05 |
| API-07 | `PUT /users/{id}/addresses/{addressId}` | 200 + dirección actualizada | CU-05 |
| API-08 | `DELETE /users/{id}/addresses/{addressId}` | 204 sin cuerpo | CU-05 |
| API-09 | `PATCH /users/{id}/addresses/{addressId}/default` | 200 + dirección seleccionada | CU-05 |
| API-10 | `PUT /users/{id}/status` | 200 + usuario | CU-06 |
| API-11 | `PUT /users/{id}/role` | 200 + usuario | CU-07 |
| API-12 | `POST /auth/password/forgot` | 202 sin cuerpo | CU-08 |
| API-13 | `POST /auth/password/reset` | 204 sin cuerpo | CU-08 |
| API-14 | `POST /auth/refresh` | 200 + sesión | CU-09 |
| API-15 | `GET /auth/session` | 200 + rol y estado vigentes | CU-09 |
| API-16 | `GET /users/{id}/access` | 200 + estado y rol | CU-10 |
| API-17 | `POST /auth/module-token` | 200 + JWT con rol MODULO | CU-10 |

Los dos `PUT` reciben el conjunto completo de campos modificables. La baja de direcciones es lógica. Elegir una predeterminada desmarca la anterior en la misma transacción; eliminarla deja al usuario sin predeterminada. Cada operación contiene su trazabilidad en `x-trazabilidad`. Las operaciones de administración, recuperación de contraseña y sesión se explican en [DISENO_CU_EVOLUCION.md](DISENO_CU_EVOLUCION.md).

## Integración con los equipos

Coordinar con los demás grupos lo que no depende de este módulo: algoritmo y distribución de claves del JWT (hoy HS256 con secreto compartido; la firma asimétrica es una propuesta), la entrega y rotación de las claves con las que cada módulo pide su token y la adopción de las correcciones de Notificaciones y los demás consumidores ([guía y evidencia](../integracion/README.md)). Los claims actuales son `sub` = id del usuario, `role`, `iat` y `exp`; los consumidores no deben leer `sub` como email, y el rol de un token emitido puede quedar desactualizado hasta que venza: el vigente se consulta con API-15 o API-16.

No debe reemplazarse este archivo por una exportación automática de Springdoc: se perderían ejemplos, trazabilidad y restricciones ya diseñadas. Ante un cambio de comportamiento, actualizar el YAML y volver a correr `mvn test`.

Para cotejar el YAML con la implementación, abrir la Swagger UI del servicio (`/swagger-ui.html`) o `/v3/api-docs` y comparar las operaciones, las respuestas y la seguridad.
