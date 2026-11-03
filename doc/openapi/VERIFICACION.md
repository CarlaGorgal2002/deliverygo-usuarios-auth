# Verificación de los contratos

Los cuatro archivos YAML (16 operaciones, CU-01 a CU-10) se validaron el **30/10/2026** con Python 3.14, PyYAML, `openapi-spec-validator` y `openapi-schema-validator`. Los paquetes y comandos están fijados en `requirements.txt` y en el README. La validación es local y no envía los adjuntos a un servicio externo.

## Verificación ejecutada

- Conformidad de las cuatro especificaciones con OpenAPI 3.0.3.
- Ausencia de claves YAML duplicadas y referencias externas o rotas.
- Resolución de todos los `$ref` y validación de ejemplos de cuerpos, propiedades, parámetros y cabeceras contra sus esquemas.
- Cuatro operaciones públicas (registro, login y las dos de recuperación de contraseña), siete protegidas por Bearer, dos que aceptan Bearer o API Key de módulo (API-03 y API-05) y una exclusiva de módulos (API-16).
- Las dieciséis operaciones exactas (CU-01 a CU-10); identificadores de operación únicos y parámetros de ruta completos.
- Ningún cuerpo en GET, DELETE ni selección de predeterminada; ninguna respuesta con contenido en 204.
- Errores de ejemplo con `status` igual al HTTP y `path` de la operación correspondiente.
- Correlación en respuestas; desafío `WWW-Authenticate` en 401; datos sensibles fuera de los DTO de salida.
- **70 escenarios de esquemas**, positivos y negativos: campos faltantes, blancos, email inválido, contraseña corta, propiedades prohibidas, opcionales nulos, IDs inválidos, límites de Long, y los DTO de estado, rol, recuperación y restablecimiento de contraseña.
- Vistas generadas idénticas a la proyección del contrato principal.

El validador cuenta las comprobaciones de ejemplos después de expandir referencias; un componente reutilizado puede verificarse más de una vez. Esa cifra no representa endpoints adicionales ni pruebas HTTP ejecutadas.

**Resultado: especificaciones y ejemplos válidos; vistas sincronizadas.** El archivo `resultado-validacion.txt` contiene la salida reproducible del comando.

## Qué no demuestra la validación del YAML

`validar_contratos.py` comprueba el documento OpenAPI y sus ejemplos; no ejecuta el servidor. El token de ejemplo tiene una firma ficticia. El comportamiento real lo cubren las pruebas de la sección siguiente.

## Pruebas automáticas del backend

Se ejecutan con `mvn test` (JDK 25). Son pruebas de integración con Spring Boot y MockMvc sobre la cadena real de filtros, seguridad, validación, servicios y base H2, más pruebas unitarias del JWT. Resultado de la última corrida: **82 pruebas, 0 fallas, 0 errores**.

| Clase | Pruebas | Cobertura |
|---|---|---|
| `AuthApiTest` | 13 | CU-01 y CU-02: alta, email normalizado y duplicado, validación por campo, campos no permitidos, tipos incorrectos, contraseña de más de 72 bytes, login (mismo 401 para email inexistente y contraseña incorrecta, 403 sólo con la contraseña correcta), rutas públicas aun con token viejo, dos registros simultáneos |
| `PerfilApiTest` | 11 | CU-03 y CU-04 y las reglas RN-04, RN-05, RN-09: titular, 401 por token ausente, alterado, basura, vencido o de otra clave, 403 cruzado incluidos ADMIN y COMERCIO, IDs inválidos, titular inactivo, usuario inexistente, PUT completo e idempotente, campos protegidos, email de otro usuario, rutas y métodos fuera del contrato |
| `DireccionesApiTest` | 9 | CU-05: recorrido completo (listar vacío, altas, predeterminada, PUT, baja lógica, repetir baja), dirección ajena como inexistente, PUT sin predeterminada ni activa, límites de campos, tipos incorrectos, 401, usuario inactivo, y dos pruebas de concurrencia sobre la predeterminada |
| `AdministracionApiTest` | 8 | CU-06 y CU-07 (RN-11, RN-12): desactivar y reactivar (el login y el token vigente de una cuenta inactiva son rechazados), idempotencia, 401/403 para quien no es administrador (CLIENTE, COMERCIO, API Key), el rol del token no alcanza (un administrador degradado o inactivo en la base recibe 403), un administrador no modifica su propia cuenta, asignación de roles con el token viejo conservando el anterior, validación de cuerpos e ids, y concurrencia: dos administradores que se degradan a la vez (5 rondas) nunca quedan sin ninguno |
| `RecuperacionApiTest` | 9 | CU-08 (RN-13): 202 idéntico para cuenta existente, inexistente e inactiva y aviso sólo si existe, normalización y validación del email, falla de Notificaciones sin efecto en la respuesta, ruta pública con token viejo y sólo la huella en la base, restablecimiento de un solo uso, un pedido nuevo invalida el anterior, token inexistente, vencido o de cuenta inactiva con el mismo 400, política de contraseña y campos no permitidos sin consumir el token, y concurrencia: seis restablecimientos simultáneos con el mismo token, sólo uno se aplica |
| `SesionApiTest` | 7 | CU-09 (RN-14): validar devuelve rol y estado vigentes aunque el token diga otro, renovar emite un token que sirve y trae el rol actual, cuenta inactiva (403) o inexistente (401), sin token, vencido o alterado (401), y una API Key de módulo no renueva ni valida (403) |
| `ModulosApiTest` | 10 | CU-10 y RN-15: estado y rol sin datos personales, cuenta inactiva con 200, rol vigente, 401 sin clave, con clave incorrecta, vacía o demasiado corta, 403 para un JWT de usuario o ADMIN, el JWT manda sobre la clave, ids inexistentes e inválidos, API-03 con cualquier módulo (también inactivas), API-05 sólo para Pedidos, los módulos nunca escriben (PUT, POST, DELETE, PATCH y administración dan 403) y las rutas públicas no se rompen por una clave inválida |
| `CorrelacionApiTest` | 5 | Reutilización, generación e invalidez de `X-Correlation-Id`; preflight CORS con PATCH; origen no autorizado |
| `BienvenidaApiTest` | 3 | INT-01: se solicita después del alta, no se solicita si el alta falla, una falla del cliente no cambia el 201 |
| `JwtServiceTest` | 6 | Emisión y lectura de `sub`=id y `role`; rechazo de otra clave, vencido, sin firma y `sub` no numérico; clave mínima de 32 bytes |
| `AdministradorInicialTest` | 1 | DM-07: el administrador configurado por `ADMIN_EMAIL` y `ADMIN_PASSWORD` inicia sesión con rol ADMIN (con su propia base en memoria) |

Además se hizo una prueba de mutación de la concurrencia. Al quitar temporalmente el bloqueo de la fila del usuario (`findByIdForUpdate`) fallaron las dos pruebas de concurrencia de direcciones en tres corridas consecutivas y, en el servicio de administración, la prueba de los dos administradores que se degradan a la vez; al quitar el consumo atómico del token (`consumir`) falló la prueba de restablecimientos simultáneos. Con las garantías restituidas las 82 pruebas pasan. Dos comprobaciones más con el servidor levantado:

- **Cotejo con el Swagger generado.** Se comparó `/v3/api-docs` con el YAML: las 16 operaciones, los códigos de respuesta de cada una y los esquemas de seguridad (`bearerAuth` y `moduleApiKey`) coinciden, sin diferencias.
- **Recorrido real por HTTP.** Con el administrador inicial y las claves de módulo configuradas por variable de entorno: registro con 201 y `Location`, 409 con un `X-Correlation-Id` propio, login, 403 sobre otro usuario, 400 por ID `abc`, alta de dirección con 201 y `Location`; validar y renovar la sesión (200); el administrador asigna COMERCIO (200) y un CLIENTE que intenta lo mismo recibe 403; el token viejo informa el rol nuevo en `/auth/session`; API-16 con la clave de Pedidos (200), sin clave, con clave incorrecta (401), con un JWT de usuario (403) y API-05 con la clave de Catálogo (403) y de Pedidos (200); recuperación de contraseña (202 para cuenta existente e inexistente, restablecimiento 204, segundo uso 400 `INVALID_RESET_TOKEN`, login con la clave nueva 200 y con la vieja 401); el administrador desactiva la cuenta (login `USER_INACTIVE`, token vigente `USER_INACTIVE`, el módulo la sigue viendo con estado INACTIVO), la reactiva (200) y se le prohíbe modificarse a sí mismo (403). Todo dio los resultados esperados.

No cubren: firma asimétrica (no implementada), credencial de módulos consumidores (sin acordar), integración HTTP real con otros grupos, ni la base persistente de ambientes compartidos (las pruebas usan H2 en memoria).

## Recorrido funcional para la demo

Los 20 pasos están cubiertos por `AuthApiTest`, `PerfilApiTest`, `DireccionesApiTest`, `AdministracionApiTest`, `RecuperacionApiTest`, `SesionApiTest` y `ModulosApiTest` (para CU-05, principalmente `recorridoCompletoDelCasoDeUso`) y pueden repetirse a mano desde Swagger UI.

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
| 15 | Validar la sesión de A con `GET /auth/session` y renovarla con `POST /auth/refresh` | 200; rol y estado vigentes; el token nuevo sirve |
| 16 | Un ADMIN asigna COMERCIO a A con `PUT /users/{idA}/role` | 200, rol COMERCIO; `GET /auth/session` con el token viejo informa COMERCIO |
| 17 | Un ADMIN desactiva a A con `PUT /users/{idA}/status` | 200, estado INACTIVO; login de A: 403 `USER_INACTIVE`; el token vigente de A sobre su perfil: 403 `USER_INACTIVE` |
| 18 | Un módulo consulta `GET /users/{idA}/access` con su API Key | 200 con estado INACTIVO y rol, sin datos personales |
| 19 | Reactivar a A (`ACTIVO`) | 200; A vuelve a iniciar sesión |
| 20 | A pide `POST /auth/password/forgot` y luego `POST /auth/password/reset` con el token recibido | 202, luego 204; login con la clave nueva 200, con la vieja 401; repetir el restablecimiento: 400 `INVALID_RESET_TOKEN` |

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
| Un CLIENTE, COMERCIO o una API Key intenta cambiar estado o rol | 403 `FORBIDDEN`; sin token, 401 | RN-11 |
| Un ADMIN degradado o desactivado después de iniciar sesión intenta administrar | 403 (el rol del token no alcanza; se verifica la base) | RN-11, DM-13 |
| Un ADMIN modifica su propia cuenta | 403; la cuenta no cambia | RN-12 |
| Dos ADMIN se degradan entre sí al mismo tiempo | Uno 200 y el otro 403; queda exactamente un ADMIN | RN-12 |
| Estado o rol fuera del catálogo, campo extra, cuerpo vacío | 400 `VALIDATION_ERROR` | RN-01, DC-01 |
| Recuperación para un email inexistente, de cuenta inactiva o existente | El mismo 202 y el mismo cuerpo; sólo se avisa a Notificaciones si la cuenta está activa | RN-13, DC-09 |
| Restablecer con token inexistente, vencido, usado o de cuenta inactiva | 400 `INVALID_RESET_TOKEN`, igual en todos los casos | RN-13 |
| Seis restablecimientos simultáneos con el mismo token | Uno 204 y cinco 400 | RN-13 |
| Pedir un token nuevo | El anterior deja de servir | RN-13 |
| Renovar con un token vencido, alterado o sin token | 401 `UNAUTHENTICATED` | RN-14 |
| Renovar o validar con la cuenta inactiva / eliminada | 403 `USER_INACTIVE` / 401 | RN-04, RN-14 |
| API Key de módulo en API-14 o API-15 | 403 `FORBIDDEN` | RN-14 |
| API-16 sin clave, con clave incorrecta, vacía o de menos de 16 caracteres | 401 `UNAUTHENTICATED` | RN-15 |
| API-16 con un JWT de usuario (titular o ADMIN) | 403 `FORBIDDEN` | RN-15 |
| JWT y API Key en la misma solicitud | Manda el JWT | RN-15 |

## Integración de los módulos consumidores

La credencial de módulo se resolvió con una API Key por módulo (DC-08, RN-15). Todos los escenarios siguientes tienen prueba automática en `ModulosApiTest`:

| Escenario | Resultado esperado |
|---|---|
| Pedidos consulta el perfil y las direcciones con su clave | 200 |
| Catálogo consulta el perfil con su clave | 200 |
| Catálogo intenta listar direcciones | 403 |
| Un módulo intenta modificar un perfil, crear, borrar o seleccionar una dirección, o cambiar estado o rol | 403 |
| Un módulo consulta el perfil de un usuario INACTIVO | 200 con estado INACTIVO; decide con sus propias reglas |
| El titular INACTIVO consulta su propio perfil | 403 `USER_INACTIVE` |
| Un módulo consulta estado y rol de un usuario (API-16) | 200 sin datos personales, también si está INACTIVO |
| Servicio propaga el JWT de A para acceder a B | 403; propagar el token no amplía permisos |

Lo que sigue sin probarse porque depende de otros equipos: que cada módulo reciba, guarde y use su clave, la rotación de claves, el acuerdo de firma asimétrica y el contrato real de Notificaciones (INT-01 e INT-06).

Para cada escenario ejecutado a mano registrar fecha, commit, preparación, request, respuesta HTTP y comprobación de persistencia cuando corresponda.
