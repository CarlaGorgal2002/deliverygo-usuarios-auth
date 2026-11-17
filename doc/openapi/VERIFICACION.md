# Verificación

Cómo se comprueba que el contrato (`deliverygo-usuarios.yaml`) y el backend implementado coinciden, y qué evidencia existe de cada caso de uso.

## Contrato OpenAPI

- El YAML (OpenAPI 3.0.3) describe las diecisiete operaciones de CU-01 a CU-10 con sus DTO, ejemplos, validaciones, seguridad y errores. Se puede abrir en Swagger Editor o importar en Postman.
- Cuatro operaciones son públicas (registro, login y las dos de recuperación de contraseña), nueve se protegen con el Bearer de una persona y tres (API-03, API-05 y API-16) se leen sin credencial desde otros módulos o con el Bearer del titular.
- El 16/11/2026 se comparó `/v3/api-docs` del servicio en ejecución con el YAML: las 16 operaciones de ese momento, sus códigos de respuesta y sus esquemas de seguridad coincidían. El 17/11/2026 se quitaron el esquema `moduleApiKey` y luego el token de módulo (API-17): los módulos leen sin credencial; ese cambio está cubierto por las pruebas de integración y hay que repetir la comparación en Swagger UI (`http://localhost:8089/swagger-ui.html`), que es la fuente de verdad del contrato real (clase 9).
- El token de ejemplo del YAML tiene una firma ficticia: el comportamiento real lo cubren las pruebas siguientes.

## Pruebas automáticas del backend

Se ejecutan con `mvn test` (JDK 25). Son pruebas de integración con Spring Boot y MockMvc sobre la cadena real de filtros, seguridad, validación, servicios y base H2 en memoria (perfil `test`, `src/test/resources/application-test.properties`), más pruebas unitarias del JWT. La aplicación usa PostgreSQL; las pruebas no necesitan Docker. Resultado de la última corrida: **92 pruebas, 0 fallas, 0 errores** (17/11/2026).

| Clase | Pruebas | Cobertura |
|---|---|---|
| `AuthApiTest` | 12 | CU-01 y CU-02: alta, email normalizado y duplicado, validación por campo, campos extra ignorados, JSON ilegible, contraseña de más de 72 bytes, login (mismo 401 para email inexistente y contraseña incorrecta, 403 sólo con la contraseña correcta) y rutas públicas aun con token viejo |
| `PerfilApiTest` | 11 | CU-03 y CU-04 y las reglas RN-04, RN-05, RN-09: titular, 401 por token ausente, alterado, basura, vencido o de otra clave, 403 cruzado incluidos ADMIN y COMERCIO, IDs inválidos, titular inactivo, usuario inexistente, PUT completo e idempotente, campos protegidos, email de otro usuario, rutas y métodos fuera del contrato |
| `DireccionesApiTest` | 7 | CU-05: recorrido completo (listar vacío, altas, predeterminada, PUT, baja lógica, repetir baja), dirección ajena como inexistente, PUT que ignora predeterminada y activa, límites de campos, valor no convertible, 401 y usuario inactivo |
| `AdministracionApiTest` | 7 | CU-06 y CU-07 (RN-11, RN-12): desactivar y reactivar (el login y el token vigente de una cuenta inactiva son rechazados), idempotencia, 401/403 para quien no es administrador (CLIENTE, COMERCIO, token de módulo), el rol del token no alcanza (un administrador degradado o inactivo en la base recibe 403), un administrador no modifica su propia cuenta, asignación de roles con el token viejo conservando el anterior y validación de cuerpos e ids |
| `RecuperacionApiTest` | 8 | CU-08 (RN-13): 202 idéntico para cuenta existente, inexistente e inactiva y aviso sólo si existe, normalización y validación del email, falla de Notificaciones sin efecto en la respuesta, ruta pública con token viejo y sólo la huella en la base, restablecimiento de un solo uso, un pedido nuevo invalida el anterior, token inexistente, vencido o de cuenta inactiva con el mismo 400 y política de contraseña sin consumir el token (un campo extra se ignora) |
| `SesionApiTest` | 7 | CU-09 (RN-14): validar devuelve rol y estado vigentes aunque el token diga otro, renovar emite un token que sirve y trae el rol actual, cuenta inactiva (403) o inexistente (401), sin token, vencido o alterado (401), y una API Key de módulo no renueva ni valida (403) |
| `ModulosApiTest` | 10 | CU-10 y RN-15: lecturas de módulos sin credencial (estado y rol sin datos personales, perfil y direcciones, también de cuentas inactivas), rol vigente, una `X-Api-Key` o un Bearer vacío o viejo se ignoran, ids inexistentes e inválidos, con el JWT de otra persona (también ADMIN) rige el titular (403), sin credencial nada se escribe (401) y `POST /auth/module-token` ya no existe |
| `CorrelacionApiTest` | 5 | Reutilización, generación e invalidez de `X-Correlation-Id`; preflight CORS con PATCH; origen no autorizado |
| `BienvenidaApiTest` | 4 | INT-01: se solicita después del commit del alta (otra transacción ya ve al usuario, como la consulta de Notificaciones), no se solicita si el alta falla, una falla del cliente no cambia el 201 |
| `JwtServiceTest` | 6 | Emisión y lectura de `sub`=id y `role`; rechazo de otra clave, vencido, sin firma y `sub` no numérico; clave mínima de 32 bytes |
| `IntegridadApiTest` | 1 | RN-02 / DM-11: si el control del service no ve un email repetido, la restricción única de la base lo rechaza y se informa 409 `EMAIL_ALREADY_REGISTERED` |
| `AdministradorInicialTest` | 1 | DM-07: el administrador configurado por `ADMIN_EMAIL` y `ADMIN_PASSWORD` inicia sesión con rol ADMIN (con su propia base en memoria) |
| `StatusApiTest` | 2 | `GET /api/v1/status`: 200 con `{"status":"ok"}` sin credencial y también con un token inválido |

| `NotificacionesClientHttpTest` | 11 | Cuerpo HTTP, autenticación de servicio, correlación, plantillas, caché, errores y configuración de URL |

La concurrencia (varias solicitudes simultáneas sobre el mismo usuario) no se prueba ni se resuelve con bloqueos: excede lo visto en la cátedra. La regla RN-07 se cumple dentro de cada transacción y el único conflicto de datos que el cliente puede provocar, el email repetido, lo cubre la restricción única de la base.

Dos comprobaciones más con el servidor levantado:

- **Cotejo con el Swagger generado.** Se comparó `/v3/api-docs` con el YAML: las 16 operaciones de ese momento, los códigos de respuesta de cada una y los esquemas de seguridad coincidían, sin diferencias (anterior a API-17).
- **Recorrido real por HTTP.** Con el administrador inicial y las claves de módulo configuradas por variable de entorno: registro con 201 y `Location`, 409 con un `X-Correlation-Id` propio, login, 403 sobre otro usuario, 400 por ID `abc`, alta de dirección con 201 y `Location`; validar y renovar la sesión (200); el administrador asigna COMERCIO (200) y un CLIENTE que intenta lo mismo recibe 403; el token viejo informa el rol nuevo en `/auth/session`; API-16 con la clave de Pedidos (200), sin clave, con clave incorrecta (401), con un JWT de usuario (403) y API-05 con la clave de Catálogo (403) y de Pedidos (200); este recorrido se hizo el 16/11 con la cabecera `X-Api-Key` anterior; hoy esas lecturas no llevan credencial; recuperación de contraseña (202 para cuenta existente e inexistente, restablecimiento 204, segundo uso 400 `INVALID_RESET_TOKEN`, login con la clave nueva 200 y con la vieja 401); el administrador desactiva la cuenta (login `USER_INACTIVE`, token vigente `USER_INACTIVE`, el módulo la sigue viendo con estado INACTIVO), la reactiva (200) y se le prohíbe modificarse a sí mismo (403). Todo dio los resultados esperados.

La suite Maven usa H2. La verificación HTTP del 17/11 se ejecutó sobre PostgreSQL 16 con el backend en Docker: la colección de Usuarios (con el token de módulo, API-17, que después se eliminó) corrió con newman, 52 solicitudes y 159 comprobaciones, sin fallas. Ya sin credencial entre módulos, la colección se repitió el 17/11 con la Postman CLI sobre H2: 43 solicitudes y 145 comprobaciones, sin fallas. La del 16/11, anterior a API-17, había hecho 45 solicitudes y 155 comprobaciones en esa colección, 12 solicitudes y 40 comprobaciones de Notificaciones con el parche local, y 7 solicitudes y 8 comprobaciones con Catálogo sin modificarlo. Ver [evidencia](../postman/evidencia/) y [alcance exacto de las integraciones](../integracion/README.md). No demuestra firma asimétrica, distribución/rotación de secretos ni el ciclo de compra de todo el ecosistema.

## Backend en Docker Compose (clase 13)

El `Dockerfile` (multi-stage), el `.dockerignore` y `compose.backend.yaml` levantan el backend junto con PostgreSQL en la red interna de Compose; el backend llega a la base por el nombre del servicio (`postgres`). Verificado el 17/11/2026:

```powershell
docker compose -f docker-compose.yml -f compose.backend.yaml up -d --build
docker compose -f docker-compose.yml -f compose.backend.yaml exec postgres psql -U deliverygo -d deliverygo_usuarios -c "SELECT id, email, estado FROM usuarios ORDER BY id DESC LIMIT 2;"
```

| Comprobación | Resultado |
|---|---|
| Imagen construida con `docker build` | Correcta (compila con Maven en la primera etapa y ejecuta sólo con el JRE) |
| Arranque: la base reporta `Healthy` antes de iniciar el backend (`depends_on` con `service_healthy`) | Correcto; `/v3/api-docs` responde 200 |
| `POST /api/v1/auth/register` con un usuario ficticio | 201, rol CLIENTE y estado ACTIVO |
| Mismo registro repetido | 409 `EMAIL_ALREADY_REGISTERED` |
| Consulta SQL directa al contenedor de PostgreSQL | El usuario creado figura en la tabla `usuarios` |
| `docker compose ... down` | Apaga los contenedores y conserva el volumen con los datos |

## Recorrido funcional para la demo

Los 20 pasos están cubiertos por `AuthApiTest`, `PerfilApiTest`, `DireccionesApiTest`, `AdministracionApiTest`, `RecuperacionApiTest`, `SesionApiTest` y `ModulosApiTest` (para CU-05, principalmente `recorridoCompletoDelCasoDeUso`) y pueden repetirse a mano desde Swagger UI.

Preparar dos usuarios ficticios A y B; obtener sus IDs de las respuestas de registro. Eliminar dependencias de IDs fijos en los scripts de prueba. El estado INACTIVO se prepara con CU-06 desde una sesión ADMIN, como hace la colección de Postman.

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
| 18 | Un módulo consulta `GET /users/{idA}/access` sin credencial | 200 con estado INACTIVO y rol, sin datos personales |
| 19 | Reactivar a A (`ACTIVO`) | 200; A vuelve a iniciar sesión |
| 20 | A pide `POST /auth/password/forgot` y luego `POST /auth/password/reset` con el token recibido | 202, luego 204; login con la clave nueva 200, con la vieja 401; repetir el restablecimiento: 400 `INVALID_RESET_TOKEN` |

## Rechazos y consistencia

Todos los escenarios de esta tabla tienen una prueba automática, con una excepción: el rollback ante una falla a mitad del cambio de predeterminada se garantiza porque ambos cambios ocurren en una única transacción (`@Transactional`), pero no hay una prueba que fuerce una falla intermedia.

| Escenario | Resultado esperado | Trazabilidad |
|---|---|---|
| Registro sin un campo obligatorio o con nombre en blanco | 400 con details por campo | RN-01 |
| Registro que intenta asignar `rol: ADMIN` | 201 con rol CLIENTE: el campo se ignora | RN-06, DC-01 |
| Login con email inexistente y con contraseña incorrecta | Mismo 401 y mensaje genérico, sin distinguir causa | RN-08 |
| Login de cuenta INACTIVA con contraseña incorrecta | 401 genérico; no informa estado | RN-08 |
| Login de cuenta INACTIVA con contraseña correcta | 403 `USER_INACTIVE` | RN-04 |
| Operación protegida sin token, con token vencido o firma alterada | 401 `UNAUTHENTICATED`; DTO-08 | RN-09 |
| Token A sobre ruta de usuario B | 403 antes de revelar información de B | RN-05 |
| Token ADMIN humano sobre perfil de otra persona | 403; ADMIN no equivale a servicio consumidor | RN-05 |
| Dirección B solicitada bajo ruta de A con token A | 404 `ADDRESS_NOT_FOUND`, igual que una dirección inexistente | RN-05 |
| PUT de perfil que sólo envía nombre | 400; PUT no es parcial | RN-01, DC-03 |
| PUT de perfil que incluye password, rol o estado | 200; se ignoran y ningún campo protegido cambia | RN-10, DC-01 |
| PUT de perfil con email de B | 409; no persiste ningún cambio parcial | RN-02 |
| PUT de dirección con predeterminada o activa | 200; se ignoran (la predeterminada se elige con API-09) | RN-07, DC-01 |
| JSON ilegible o valor no convertible al tipo del campo | 400 normalizado, sin stack trace | RN-01 |
| ID cero, negativo, decimal, texto o fuera de Long | 400 tras los controles de autenticación pertinentes | DC-07 |
| X-Correlation-Id válido | Mismo valor en cabecera de respuesta y errores | DC-06 |
| X-Correlation-Id ausente | Se genera uno y se informa en respuesta | DC-06 |
| X-Correlation-Id inválido | 400 con ID nuevo y formato de error común | DC-06 |
| Falla de la bienvenida tras registrar | Registro se mantiene; respuesta 201 | INT-01 |
| Falla dentro del cambio de predeterminada | Rollback de ambos cambios; no queda un estado parcial | RN-07, DM-10 |
| Un CLIENTE o COMERCIO intenta cambiar estado o rol | 403 `FORBIDDEN`; sin token (un módulo), 401 | RN-11 |
| Un ADMIN degradado o desactivado después de iniciar sesión intenta administrar | 403 (el rol del token no alcanza; se verifica la base) | RN-11, DM-13 |
| Un ADMIN modifica su propia cuenta | 403; la cuenta no cambia | RN-12 |
| Estado o rol fuera del catálogo, cuerpo vacío | 400 `VALIDATION_ERROR` | RN-01 |
| Recuperación para un email inexistente, de cuenta inactiva o existente | El mismo 202 y el mismo cuerpo; sólo se avisa a Notificaciones si la cuenta está activa | RN-13, DC-09 |
| Restablecer con token inexistente, vencido, usado o de cuenta inactiva | 400 `INVALID_RESET_TOKEN`, igual en todos los casos | RN-13 |
| Pedir un token nuevo | El anterior deja de servir | RN-13 |
| Renovar con un token vencido, alterado o sin token | 401 `UNAUTHENTICATED` | RN-14 |
| Renovar o validar con la cuenta inactiva / eliminada | 403 `USER_INACTIVE` / 401 | RN-04, RN-14 |
| API-14 o API-15 sin token | 401 `UNAUTHENTICATED` | RN-14 |
| API-16 sin token, con un Bearer inválido o con `X-Api-Key` | 401 `UNAUTHENTICATED` | RN-15 |
| API-16 con un JWT de usuario (titular o ADMIN) | 403 `FORBIDDEN` | RN-15 |

## Integración de los módulos consumidores

Los módulos leen sin credencial: entre módulos no hay JWT ni claves (DC-08, RN-15). Todos los escenarios siguientes tienen prueba automática en `ModulosApiTest`:

| Escenario | Resultado esperado |
|---|---|
| Un módulo consulta el perfil y las direcciones sin credencial | 200 |
| Un módulo manda una `X-Api-Key` o un Bearer vacío o viejo | Se ignora: 200 |
| Sin credencial se intenta modificar un perfil, crear, borrar o seleccionar una dirección, o cambiar estado o rol | 401 |
| Un módulo consulta el perfil de un usuario INACTIVO | 200 con estado INACTIVO; decide con sus propias reglas |
| El titular INACTIVO consulta su propio perfil | 403 `USER_INACTIVE` |
| Un módulo consulta estado y rol de un usuario (API-16) | 200 sin datos personales, también si está INACTIVO |
| Servicio propaga el JWT de A para acceder a B | 403; propagar el token no amplía permisos |

Notificaciones ya se probó en ambos sentidos con el parche incluido. Siguen pendientes los otros consumidores indicados en [Integración](../integracion/README.md), y el acuerdo de firma asimétrica.

Para cada escenario ejecutado a mano registrar fecha, commit, preparación, request, respuesta HTTP y comprobación de persistencia cuando corresponda.

## Repetir las comprobaciones de la entrega actual

```powershell
mvn test
```

La suite no necesita Docker (usa H2). 