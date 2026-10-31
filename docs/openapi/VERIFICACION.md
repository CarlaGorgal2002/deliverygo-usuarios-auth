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

## Qué no demuestra la validación del YAML

`validar_contratos.py` comprueba el documento OpenAPI y sus ejemplos; no ejecuta el servidor. El token de ejemplo tiene una firma ficticia. El comportamiento real lo cubren las pruebas de la sección siguiente.

## Pruebas automáticas del backend

Se ejecutan con `mvn test` (JDK 25). Son pruebas de integración con Spring Boot y MockMvc sobre la cadena real de filtros, seguridad, validación, servicios y base H2, más pruebas unitarias del JWT. Resultado de la última corrida: **47 pruebas, 0 fallas, 0 errores**.

| Clase | Pruebas | Cobertura |
|---|---|---|
| `AuthApiTest` | 13 | CU-01 y CU-02: alta, email normalizado y duplicado, validación por campo, campos no permitidos, tipos incorrectos, contraseña de más de 72 bytes, login (mismo 401 para email inexistente y contraseña incorrecta, 403 sólo con la contraseña correcta), rutas públicas aun con token viejo, dos registros simultáneos |
| `PerfilApiTest` | 11 | CU-03 y CU-04 y las reglas RN-04, RN-05, RN-09: titular, 401 por token ausente, alterado, basura, vencido o de otra clave, 403 cruzado incluidos ADMIN y COMERCIO, IDs inválidos, titular inactivo, usuario inexistente, PUT completo e idempotente, campos protegidos, email de otro usuario, rutas y métodos fuera del contrato |
| `DireccionesApiTest` | 9 | CU-05: recorrido completo (listar vacío, altas, predeterminada, PUT, baja lógica, repetir baja), dirección ajena como inexistente, PUT sin predeterminada ni activa, límites de campos, tipos incorrectos, 401, usuario inactivo, y dos pruebas de concurrencia sobre la predeterminada |
| `CorrelacionApiTest` | 5 | Reutilización, generación e invalidez de `X-Correlation-Id`; preflight CORS con PATCH; origen no autorizado |
| `BienvenidaApiTest` | 3 | INT-01: se solicita después del alta, no se solicita si el alta falla, una falla del cliente no cambia el 201 |
| `JwtServiceTest` | 6 | Emisión y lectura de `sub`=id y `role`; rechazo de otra clave, vencido, sin firma y `sub` no numérico; clave mínima de 32 bytes |

Además se hizo una prueba de mutación de la concurrencia: al quitar temporalmente el bloqueo de la fila del usuario (`findByIdForUpdate`), las dos pruebas de concurrencia de direcciones fallaron en tres corridas consecutivas; con el bloqueo restituido pasan. Y un recorrido real por HTTP con el servidor levantado (registro con 201 y `Location`, 409 con un `X-Correlation-Id` propio, login, 403 sobre otro usuario, 400 por ID `abc`, alta de dirección con 201 y `Location`, y registro del mock INT-01 en el log) dio los resultados esperados.

No cubren: firma asimétrica (no implementada), credencial de módulos consumidores (sin acordar), integración HTTP real con otros grupos, ni la base persistente de ambientes compartidos (las pruebas usan H2 en memoria).

## Recorrido funcional para la demo

Los 14 pasos están cubiertos por `AuthApiTest`, `PerfilApiTest` y `DireccionesApiTest` (principalmente `recorridoCompletoDelCasoDeUso`) y pueden repetirse a mano desde Swagger UI.

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

Todos los escenarios de esta tabla tienen una prueba automática, con una excepción: el rollback ante una falla a mitad del cambio de predeterminada se garantiza porque ambos cambios ocurren en una única transacción con la fila del usuario bloqueada, pero no hay una prueba que fuerce una falla intermedia.

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

Estos escenarios no se ejecutaron ni están implementados: dependen de la credencial de módulo, todavía sin acordar.

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
