# Verificación de los contratos

Los cuatro archivos YAML se validaron el **30/10/2026** con Python 3.14, PyYAML, `openapi-spec-validator` y `openapi-schema-validator`. Los paquetes y comandos están fijados en `requirements.txt` y en el README. La validación es local y no envía los adjuntos a un servicio externo.

## Verificación ejecutada

- Conformidad de las cuatro especificaciones con OpenAPI 3.0.3.
- Ausencia de claves YAML duplicadas y referencias externas o rotas.
- Resolución de todos los `$ref` y validación de ejemplos de cuerpos, propiedades, parámetros y cabeceras contra sus esquemas.
- Registro/login públicos y siete operaciones protegidas por Bearer.
- Nueve operaciones CORE exactas; identificadores de operación únicos y parámetros de ruta completos.
- Ningún cuerpo en GET, DELETE ni selección de predeterminada; ninguna respuesta con contenido en 204.
- Errores de ejemplo con `status` igual al HTTP y `path` de la operación correspondiente.
- Correlación en respuestas; desafío `WWW-Authenticate` en 401; datos sensibles fuera de los DTO de salida.
- **51 escenarios de esquemas**, positivos y negativos: campos faltantes, blancos, email inválido, contraseña corta, propiedades prohibidas, opcionales nulos, IDs inválidos y límites de Long.
- Vistas generadas idénticas a la proyección del contrato principal.

El validador cuenta las comprobaciones de ejemplos después de expandir referencias; un componente reutilizado puede verificarse más de una vez. Esa cifra no representa endpoints adicionales ni pruebas HTTP ejecutadas.

**Resultado: especificaciones y ejemplos válidos; vistas sincronizadas.** El archivo `resultado-validacion.txt` contiene la salida reproducible del comando.

## Qué no demuestra esta validación

No se ejecutó el backend ni se alteraron sus datos. No se probó la firma criptográfica, autorización real, serialización de Jackson, transacciones, concurrencia ni integración HTTP entre grupos. El token de ejemplo tiene una firma ficticia. Las diferencias observadas por lectura de código están en `ACUERDOS_Y_BRECHAS.md`.

Una descripción OpenAPI puede expresar una regla como «sólo el titular» o «una sola dirección predeterminada», pero esa regla necesita código y pruebas funcionales. Los siguientes escenarios son criterios de aceptación pendientes de ejecutar sobre una implementación conforme.

## Recorrido funcional para la demo

Preparar dos usuarios ficticios A y B; obtener sus IDs de las respuestas de registro. Eliminar dependencias de IDs fijos en los scripts de prueba. Para probar un usuario INACTIVO mientras CU-06 no existe, preparar el estado con una fixture controlada del entorno de pruebas, no con un endpoint inventado.

| Paso | Operación | Resultado esperado |
|---|---|---|
| 1 | Registrar A con email nuevo | 201, `Location`, rol CLIENTE, estado ACTIVO, sin password/hash |
| 2 | Repetir el mismo email, incluyendo otra capitalización | 409 `EMAIL_ALREADY_REGISTERED`; un solo registro persistido |
| 3 | Login A con email y contraseña correctos | 200, token, tipo Bearer, expiraEn, usuarioId, nombre y rol |
| 4 | Consultar `/users/{idA}` con token A | 200 con el perfil de A |
| 5 | PUT completo del perfil, sin telefono | 200 y telefono null; nombre/apellido/email conservan lo enviado |
| 6 | Listar direcciones antes del alta | 200 y `[]` |
| 7 | Crear D1 sin predeterminada | 201, id asignado, predeterminada false |
| 8 | Crear D2 con predeterminada true | 201, D2 true y D1 false |
| 9 | Seleccionar D1 mediante PATCH sin cuerpo | 200, D1 true y D2 false |
| 10 | Repetir la selección de D1 | 200, una sola predeterminada, sin nuevos registros |
| 11 | PUT completo de D1 omitiendo piso y departamento | 200; ambos null; sigue predeterminada |
| 12 | DELETE de D1 | 204 con longitud de cuerpo cero; queda inactiva y sin predeterminada |
| 13 | Listar nuevamente | Sólo D2, con predeterminada false; no se elige otra automáticamente |
| 14 | Repetir DELETE de D1 | 404 `ADDRESS_NOT_FOUND`; estado persistido sin cambios |

## Rechazos y consistencia

| Escenario | Resultado esperado | Trazabilidad |
|---|---|---|
| Registro sin un campo obligatorio o con nombre en blanco | 400 con details por campo | RN-01 |
| Registro que intenta asignar `rol: ADMIN` | 400; no crea cuenta privilegiada | RN-06, DC-01 |
| Login con email inexistente y con contraseña incorrecta | Mismo 401 y mensaje genérico, sin distinguir causa | RN-08 |
| Login de cuenta INACTIVA con contraseña incorrecta | 401 genérico; no informa estado | RN-08 |
| Login de cuenta INACTIVA con contraseña correcta | 403 `USER_INACTIVE` | RN-04 |
| Operación protegida sin token, con token vencido o firma alterada | 401 `UNAUTHENTICATED`; DTO-08 | RN-09 |
| Token A sobre ruta de usuario B | 403 antes de revelar información de B | RN-05 |
| Token ADMIN humano sobre perfil de otra persona | 403; ADMIN no equivale a servicio consumidor | RN-05 |
| Dirección B solicitada bajo ruta de A con token A | 404 `ADDRESS_NOT_FOUND`, igual que una dirección inexistente | RN-05 |
| PUT de perfil que sólo envía nombre | 400; PUT no es parcial | RN-01, DC-03 |
| PUT de perfil que incluye password, rol o estado | 400; ningún campo protegido cambia | RN-10, DC-01 |
| PUT de perfil con email de B | 409; no persiste ningún cambio parcial | RN-02 |
| PUT de dirección con predeterminada o activa | 400; se usa API-09 para seleccionar | RN-07, DC-01 |
| JSON ilegible o tipos incorrectos | 400 normalizado, sin stack trace | RN-01 |
| ID cero, negativo, decimal, texto o fuera de Long | 400 tras los controles de autenticación pertinentes | DC-07 |
| X-Correlation-Id válido | Mismo valor en cabecera de respuesta y errores | DC-06 |
| X-Correlation-Id ausente | Se genera uno y se informa en respuesta | DC-06 |
| X-Correlation-Id inválido | 400 con ID nuevo y formato de error común | DC-06 |
| Falla de la bienvenida tras registrar | Registro se mantiene; respuesta 201 | INT-01 |
| Dos registros simultáneos con el mismo email normalizado | Uno 201 y otro 409; una sola fila | RN-02 |
| Selecciones simultáneas de predeterminada | Al finalizar hay a lo sumo una activa predeterminada | RN-07, DM-10 |
| Falla dentro del cambio de predeterminada | Rollback de ambos cambios; no queda un estado parcial | RN-07, DM-10 |

## Integración pendiente de acuerdo

Después de acordar y documentar la credencial de módulo:

| Escenario | Resultado esperado |
|---|---|
| Pedidos consulta perfil y direcciones con permiso explícito | 200 |
| Catálogo consulta perfil autorizado | 200 |
| Catálogo intenta listar direcciones | 403 |
| Módulo consumidor intenta modificar el perfil de un usuario | 403 |
| Módulo autorizado consulta perfil INACTIVO | 200 con estado INACTIVO; decide con sus propias reglas |
| Titular INACTIVO consulta su propio perfil | 403 `USER_INACTIVE` |
| Servicio propaga JWT A para acceder a B | 403; propagar el token no amplía permisos |

Para cada escenario ejecutado registrar fecha, commit, preparación, request, respuesta HTTP y comprobación de persistencia cuando corresponda. No marcar estas tablas como aprobadas sin esas evidencias.
