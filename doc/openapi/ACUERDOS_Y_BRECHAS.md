# Decisiones del contrato y trabajo pendiente

Este documento acompaña los YAML del Grupo 02. Distingue el diseño ya presentado, las decisiones que hacen preciso el intercambio y los acuerdos externos pendientes. El backend Java implementa las diecisiete operaciones del contrato (los cinco CU CORE y los cinco de evolución); este documento registra qué se resolvió, qué decisiones se tomaron al hacerlo y qué sigue pendiente de acuerdo con otros equipos. El diseño de CU-06 a CU-10 está en [DISENO_CU_EVOLUCION.md](DISENO_CU_EVOLUCION.md).

## Base elegida

La referencia funcional es `G02_Usuarios_PrimeraEntrega_v1.5.pdf`, especialmente las secciones 4, 5, 12 y 13. Aunque el nombre del archivo termina en v1.5, su portada indica versión 1.0. Para evitar ambigüedad, se identifica siempre por nombre completo y hash en el inventario de fuentes. El PDF anterior del repositorio, `G02_Usuarios_PrimeraEntrega_1.0.pdf`, no sustituye al adjunto indicado por el usuario.

Se mantienen los cinco endpoints obligatorios de la consigna y los cuatro que el equipo añadió para completar direcciones. Los ejemplos de Comercio enseñan patrones de implementación; sus rutas `/auth/login`, `/v1/comercios`, `username` y `expiresInSeconds` no reemplazan los contratos particulares de Usuarios.

La transcripción C13 vuelve a mencionar los diez CU para el trabajo final. El contrato formaliza los diez: los cinco CORE (API-01 a API-09) y los cinco de evolución (API-10 a API-17). La guía funcional pide incorporar los CU evolutivos «priorizados»; el equipo los incorporó todos.

## Decisiones que completan el contrato

Son precisiones de diseño de esta entrega, no nuevas exigencias atribuidas al profesor.

| ID | Decisión | Motivo e impacto |
|---|---|---|
| DC-01 | Ignorar las propiedades que no forman parte del DTO de entrada (comportamiento por defecto de Spring) | Los DTO no tienen rol, estado, id ni contraseña donde no corresponde: si un cliente los envía, se descartan y no cambian nada. Las pruebas lo comprueban en registro, perfil, direcciones, administración y recuperación. |
| DC-02 | Recortar espacios exteriores y normalizar el email a minúsculas antes de comparar y persistir | Evita duplicados por capitalización. Coincide con la intención del servicio actual. La entrada debe satisfacer el formato email del esquema; el recorte defensivo no amplía los valores admitidos por ese formato. |
| DC-03 | `PUT` reemplaza todos los datos editables; opcionales omitidos o `null` quedan sin valor | Formaliza el PUT completo de la sección 12.2. `telefono` en blanco también se elimina. En respuestas, telefono/piso/departamento sin valor aparecen como `null`. No se interpreta omisión como «conservar» en PUT. |
| DC-04 | Serializar fechas con zona horaria y ejemplos UTC, compatibles con `format: date-time` | La primera entrega pide ISO-8601 pero modela `LocalDateTime`. Hay que definir la conversión de almacenamiento a la zona acordada; no agregar `Z` a una hora local sin convertirla. |
| DC-05 | En alta, contraseña mínima de 8 caracteres; en login, obligatoria y no vacía | Sigue las validaciones específicas de DTO-01 y DTO-03. No se reaplica al login una política de registro. La contraseña nunca se recorta ni normaliza. El email de login usa formato email. |
| DC-06 | Correlación en todas las respuestas, incluida seguridad; aceptar 1–128 caracteres `A-Za-z0-9._-` | Se devuelve el ID válido recibido o se genera uno. Una cabecera inválida produce 400 con ID nuevo. Las respuestas incluyen `Cache-Control: no-store`; las altas incluyen `Location` y los 401 `WWW-Authenticate`. Estos detalles completan el contrato HTTP. |
| DC-07 | IDs Long positivos; 400 para formato/rango inválido; 404 si el usuario autenticado/autorizado solicitado no existe | Completa casos técnicos omitidos de algunas filas del PDF, especialmente API-06. Primero se autentica y autoriza; no se usa 404 para permitir consultar la existencia de perfiles ajenos. |
| DC-08 | Los módulos consumidores piden a Usuarios un JWT con rol `MODULO` (`POST /auth/module-token`, API-17) presentando su nombre y una clave propia, y lo usan como Bearer, o mandan la clave en `X-Api-Key`; en ambos casos sólo pueden leer | El TPO fija "Autenticación: Bearer JWT" y la guía de seguridad de la cátedra (`Seguridad.jpg`) muestra a Usuarios emitiendo el JWT; la clase 10 describe pedir autorización con "un rol y una key". `X-Api-Key` se conserva porque Notificaciones y Analítica ya la mandan, y Pagos espera un JWT emitido por Usuarios. Habilita API-03 (todos los módulos), API-05 (sólo Pedidos) y API-16 (todos). |
| DC-09 | La solicitud de recuperación de contraseña responde siempre 202; el token es de un solo uso, vence a los 30 minutos y sólo se guarda su hash | Una respuesta distinta según exista la cuenta revelaría qué emails están registrados (RN-13), igual que RN-08 en el login. Un token con estado y vencimiento es lo que permite el uso único. |
| DC-10 | El JWT no se revoca; el rol y el estado vigentes se consultan con API-15 y API-16 | Mantiene DM-03 (sesión sin estado). Un token emitido conserva su rol hasta vencer, pero las operaciones sobre perfil y direcciones rechazan a las cuentas inactivas y la administración verifica al administrador en la base. |

`numero` y `piso` siguen siendo texto, como en el diseño: no se convierten a entero porque pueden contener `S/N`, `PB` o valores alfanuméricos.

Las longitudes máximas siguen el modelo de datos (DER) y son las mismas en DTO, columnas y OpenAPI: nombre y apellido 80, email 120, teléfono 30, calle 100, número/piso/departamento/código postal 10, ciudad y provincia 80. La contraseña tiene mínimo 8 caracteres y, como BCrypt sólo considera los primeros 72 bytes, el registro rechaza con 400 las que superan 72 bytes en UTF-8 en lugar de truncarlas en silencio; el login acepta cualquier longitud (simplemente no coincidirá).

`INVALID_RESET_TOKEN` (400) se suma al catálogo de errores para el token de recuperación inexistente, vencido, usado o de una cuenta inactiva; no se distingue la causa.

`details` se define como una lista de `{campo, mensaje}` únicamente en `VALIDATION_ERROR`. No repite el valor recibido; para un cuerpo ilegible se usa `campo: body`. El estado inactivo devuelve **403**, como establece expresamente la sección 12.2.3 del PDF (el código anterior devolvía 422).

Además de los códigos del catálogo, el servidor usa cuatro códigos técnicos para rutas o formatos que ninguna operación del contrato contempla, con el mismo formato DTO-08: `NOT_FOUND` (404, ruta inexistente), `METHOD_NOT_ALLOWED` (405), `NOT_ACCEPTABLE` (406) y `UNSUPPORTED_MEDIA_TYPE` (415). Están fuera del `enum` de códigos del YAML porque ninguna operación documentada los devuelve; si el Portal necesita tratarlos, conviene sumarlos al catálogo compartido.

## JWT y autorización entre módulos

Usuarios autentica y emite; el Portal transporta el token; cada backend consumidor valida y autoriza. La imagen `Seguridad.jpg` muestra alternativas con acceso directo y gateway. Para este contrato se adopta el flujo directo coherente con `descarga.svg`; no se introduce una dependencia obligatoria de gateway.

La teoría, página etiquetada 27, explicita firma asimétrica. Los ZIP de laboratorio y `JwtService` usan HMAC con clave compartida. Se toma la firma asimétrica como objetivo; **RS256 es una propuesta técnica**, todavía no un algoritmo pactado con los demás grupos, y **no está implementada**: hoy se firma con HS256. La clave privada quedaría sólo en Usuarios; los verificadores recibirían la pública por un mecanismo acordado. No se inventa un endpoint JWKS ni se copian claves del laboratorio al YAML.

| Dato | Contrato | Situación actual |
|---|---|---|
| `sub` | ID estable del usuario convertido a texto | Implementado: `sub` es el id (antes era el email) |
| `role` | Un rol: `CLIENTE`, `COMERCIO`, `REPARTIDOR`, `ADMIN`; sin `ROLE_` | Implementado |
| `iat` y `exp` | NumericDate en segundos | Implementado (JJWT) |
| `expiraEn` | Vencimiento absoluto ISO-8601, coherente con `exp` | Implementado en la respuesta de login |
| `UID` | No es necesario si `sub` contiene el ID | Eliminado |
| Algoritmo y claves | Firma asimétrica, algoritmo y distribución a acordar | **Pendiente**: HS256 con secreto en `JWT_SECRET` (mínimo 32 bytes; el servicio no arranca con uno más corto) |

**Aviso para los equipos consumidores y el Portal:** el cambio de `sub` (email → id) rompe cualquier lector que use `sub` como email. Hay que avisarles antes de integrar.

El tipo de `sub` y la unidad de `exp` siguen [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html). Para implementar la verificación, fijar los algoritmos permitidos y validar emisor/audiencia según los valores que acuerde el ecosistema; no confiar en el algoritmo declarado por un token recibido. Véase [RFC 8725](https://www.rfc-editor.org/rfc/rfc8725.html). Esas decisiones de despliegue no se presentan como acuerdos ya celebrados.

Una persona con rol ADMIN o COMERCIO no se convierte en un módulo consumidor. Todos los roles humanos operan sobre su propio perfil; la única excepción es la administración de cuentas ajenas (CU-06, CU-07), reservada a ADMIN y verificada contra la base. El registro público crea CLIENTE y no permite autoasignarse permisos: los demás roles los asigna un ADMIN con CU-07, y el primer ADMIN se aprovisiona por configuración (`ADMIN_EMAIL`, `ADMIN_PASSWORD`; sin ellas no se crea ninguno).

| Actor | Perfil API-03 | Direcciones API-05 | Estado y rol API-16 | Escrituras sobre perfil/direcciones | Estado y rol de otras cuentas (API-10, API-11) |
|---|---|---|---|---|---|
| Titular autenticado y ACTIVO | Su propio perfil | Sus direcciones | 403 | Sólo sus recursos | 403 |
| ADMIN ACTIVO | 403 (no es el titular) | 403 | 403 | 403 | Sí, salvo su propia cuenta |
| Pedidos con credencial de módulo | Sí, también cuentas INACTIVAS | Sí, también cuentas INACTIVAS | Sí | No (403) | No (403) |
| Logística, Catálogo, Notificaciones, Pagos, Opiniones y Analítica con credencial de módulo | Sí, también cuentas INACTIVAS | 403 | Sí | No (403) | No (403) |
| Sin credencial, credencial inválida o de otra persona | 401 / 403 | 401 / 403 | 401 / 403 | 401 / 403 | 401 / 403 |

**Credencial de los módulos (DC-08).** La primera entrega dejó pendiente cómo se identifica un módulo. El TPO fija como convención "Autenticación: Bearer JWT", la guía de seguridad de la cátedra (`Seguridad.jpg`) muestra a Usuarios emitiendo el JWT, y los demás equipos ya implementaron cosas distintas (Notificaciones y Analítica mandan `X-Api-Key`; Pagos espera un JWT de servicio). Se resolvió aceptar las dos formas: cada módulo tiene una clave propia, configurada por variable de entorno (`MODULE_KEY_PEDIDOS`, `MODULE_KEY_LOGISTICA`, `MODULE_KEY_CATALOGO`, `MODULE_KEY_NOTIFICACIONES`, `MODULE_KEY_PAGOS`, `MODULE_KEY_OPINIONES_ANALITICA`); la presenta una sola vez en `POST /auth/module-token` y recibe un JWT con `role = MODULO` y `sub` = nombre del módulo, que usa como Bearer hasta que vence (8 horas por defecto, `JWT_MODULE_EXPIRATION_MINUTES`). Módulo desconocido, clave incorrecta o clave no configurada (vacía o de menos de 16 caracteres) dan el mismo 401: no hay claves por defecto. Alternativamente, el módulo manda esa misma clave en `X-Api-Key` en cada llamada, con los mismos permisos de sólo lectura; si llegan el JWT y la clave, manda el JWT. Qué clave usa cada equipo para pedir su token sigue siendo un acuerdo entre equipos y debe entregarse por un canal seguro.

Una llamada que propague el JWT del usuario conserva los permisos de ese usuario, no los de un módulo. Los módulos autorizados leen el perfil de un usuario INACTIVO con 200 y deciden con sus reglas; el titular inactivo recibe 403. Pedidos lista direcciones activas y consulta API-03 o API-16 para conocer el estado del usuario. Es la distinción especificada en CU-03 y SRV-09.

## Notificaciones y disponibilidad

INT-01 solicita la bienvenida después del alta e INT-06 solicita el envío del token de recuperación de contraseña (CU-08). Ambas consumen el contrato publicado por Notificaciones (repo `DeliveryGo-Notificaciones`, puerto 8089) tal como está; Usuarios no adapta su API a ese módulo y esas rutas no aparecen como endpoints propios de Usuarios.

La implementación se elige con `app.notificaciones.modo` (`NOTIFICACIONES_MODO`):

- `mock` (por defecto, declarado): `NotificacionesClientMock` sólo escribe en el log el id del usuario, sin email ni nombre. Con `MOCK_LOG_RECOVERY_TOKEN=true` escribe además el token de recuperación, únicamente para demostrar el flujo en local.
- `http`: `NotificacionesClientHttp` llama a la API real con `RestClient`, con URL base (`NOTIFICACIONES_URL`) y tiempo de espera (`NOTIFICACIONES_TIMEOUT_MS`, 2000 ms por defecto) configurables.

Cómo llama el cliente HTTP:

| Aspecto | Resolución |
|---|---|
| Autenticación | `Authorization: Bearer <JWT>` de servicio emitido en cada llamada: HS256 con el mismo `JWT_SECRET`, `sub` = id del destinatario, `role` = `SISTEMA`, vigencia de 2 minutos. Regla: Usuarios ignora en su propia API los tokens con `role` = `SISTEMA` (`JwtService.ROL_SERVICIO`): `JwtAuthenticationFilter` no autentica a nadie con ellos, aunque el `sub` sea un usuario existente, y la solicitud sigue como no autenticada (401 en rutas protegidas, incluida `POST /api/v1/auth/refresh`). Lo verifica `SesionApiTest` |
| Plantilla | `GET /api/v1/templates`; se busca por nombre una plantilla con `activa = true` y su id queda en caché. Si un envío recibe 4xx (por ejemplo, porque Notificaciones recreó la plantilla con otro id), la entrada se descarta y el próximo envío la vuelve a buscar. Si no existe, el envío falla con un mensaje que nombra la plantilla faltante |
| Envío | `POST /api/v1/notifications` con `{ destinatarioId, plantillaId, parametros }`; bienvenida: `{ nombre }`; recuperación: `{ nombre, token }`. Se espera 201 con `{ id, destinatarioId, plantillaId, canal, estado, cantidadIntentos }` |
| Nombres de plantilla | `NOTIFICACIONES_PLANTILLA_BIENVENIDA` (`USUARIO_BIENVENIDA_EMAIL`) y `NOTIFICACIONES_PLANTILLA_RECUPERACION` (`RECUPERACION_PASSWORD_EMAIL`) |
| Ejecución | Llamada HTTP sincrónica y acotada: timeout de conexión y de lectura (`NOTIFICACIONES_TIMEOUT_MS`, 2 s por defecto). Se pide después de guardar el alta o el token; si falla, `UsuarioService` o `AuthService` capturan la excepción, la registran y la respuesta no cambia |
| Correlación | Se propaga el `X-Correlation-Id` de la solicitud en curso (MDC `correlationId`) |
| Respuestas | Los DTO de respuesta ignoran explícitamente los campos desconocidos (`@JsonIgnoreProperties`), sin depender de la configuración global de Jackson |

Un fallo de Notificaciones (plantilla inexistente, 4xx/5xx, tiempo de espera o servicio caído) no revierte el alta ni la recuperación, ni cambia la respuesta: `NotificacionesClientHttp` lanza `NotificacionesException` y `UsuarioService` o `AuthService` la atrapan y la registran. `BienvenidaApiTest` verifica que la bienvenida se solicita una vez por alta confirmada, después del commit, que no se solicita si el registro falla y que una excepción del cliente no cambia el 201. `NotificacionesClientHttpTest` verifica con `MockRestServiceServer` el cuerpo, el Bearer de servicio, el `X-Correlation-Id`, la plantilla inexistente, el 503, el servicio caído y la caché de plantillas. La clase 4 plantea el envío de notificaciones como una operación que podría ser asíncrona; aquí se mantiene sincrónica con timeout porque un broker o una cola exceden lo visto en la cátedra, y se declara como límite conocido.

**Verificación real del 16/11/2026.** Las plantillas ya existen en el commit revisado de Notificaciones. Se detectaron y corrigieron en una copia local el rechazo de `SISTEMA` y la deserialización del contacto. Con el [parche y la configuración documentados](../integracion/README.md), los flujos INT-01, INT-05 e INT-06 pasaron por HTTP. El parche todavía debe incorporarlo el equipo externo; no se presenta como publicado. El canal EMAIL del módulo sigue siendo simulado.

## Estado de la implementación frente al contrato

Las diferencias relevadas el 30/10/2026 sobre el backend anterior quedaron resueltas. La columna de evidencia indica qué prueba automática lo comprueba (detalle en [VERIFICACION.md](VERIFICACION.md)).

| Área | Resolución en el backend | Evidencia |
|---|---|---|
| Login | Recibe `email` y `password`; devuelve `token`, `tipo`, `expiraEn`, `usuarioId`, `nombre` y `rol` | `AuthApiTest` |
| JWT | `sub` = id, `role`, `iat`, `exp`; claim `UID` eliminado; HS256 | `JwtServiceTest`, `AuthApiTest` |
| Registro | DTO validado, email normalizado y único, rol CLIENTE y estado ACTIVO fijos, contraseña con hash BCrypt | `AuthApiTest` |
| PUT de perfil | Reemplazo completo de campos editables; ignora password, rol y estado; 409 si el email es de otro usuario | `PerfilApiTest` |
| Direcciones | API-05 a API-09 implementadas; baja lógica; una sola predeterminada | `DireccionesApiTest` |
| DTO de dirección | Las entradas y salidas no incluyen `activa`, `usuarioId` ni otros datos internos de la entidad | `DireccionesApiTest` |
| Formato de errores | `ErrorResponse` con `timestamp`, `status`, `code`, `message`, `path`, `correlationId` y `details`; un único `GlobalExceptionHandler` | todas las clases de prueba |
| Códigos de error | Catálogo del PDF en inglés; inactivo devuelve 403 `USER_INACTIVE` | `AuthApiTest`, `PerfilApiTest` |
| Seguridad | Permisos por rol en `SecurityConfig` (administración sólo ADMIN, CU-10 sólo módulos, sesión sólo personas); el titular de la ruta lo verifica el controller: ADMIN y COMERCIO humanos reciben 403 sobre una cuenta ajena | `PerfilApiTest` |
| IDs de ruta | `Long` positivo; 400 para texto, cero, negativo, decimal o fuera de rango, después de autenticar | `PerfilApiTest` |
| Ausencia o falla del token | 401 `UNAUTHENTICATED` con `WWW-Authenticate: Bearer`, en el formato común (entry point y access denied handler) | `PerfilApiTest`, `DireccionesApiTest` |
| Titular inactivo | 403 `USER_INACTIVE` en lectura y escritura; los módulos con JWT de módulo sí reciben la cuenta inactiva con 200 | `PerfilApiTest`, `DireccionesApiTest`, `ModulosApiTest` |
| Fechas | UTC, truncadas a segundos, serializadas con zona | `AuthApiTest` |
| Validación | Starter de Validation; propiedades desconocidas ignoradas (DC-01); un JSON mal formado o un valor que no se puede convertir al tipo del campo, 400 | `AuthApiTest`, `DireccionesApiTest` |
| Correlación | `X-Correlation-Id` validado, generado si falta, devuelto en cabecera, cuerpo y log (MDC); `Cache-Control: no-store`; `Location` en los 201 | `CorrelacionApiTest` |
| CORS | Orígenes configurables (`CORS_ALLOWED_ORIGINS`); permite PATCH y `X-Correlation-Id`; expone `X-Correlation-Id` y `Location` | `CorrelacionApiTest` |
| Swagger generado | Interfaces `I*Controller` con anotaciones de springdoc; operaciones públicas sin candado; las 16 operaciones de la comparación del 16/11 (API-17 se agregó después), sus códigos de respuesta y sus esquemas de seguridad coinciden con el YAML | comparación de `/v3/api-docs` con el YAML (ver VERIFICACION.md) |
| Java y Maven | `java.version` 25 única; groupId `ar.edu.uade.api.deliverygo`; paquete `ar.edu.uade.api.deliverygo.usuarios` | `mvn test` |
| Secreto JWT | Fuera del repositorio: variable `JWT_SECRET`; se exige un mínimo de 32 bytes | `JwtServiceTest` |
| CU-06 Desactivar o reactivar | API-10, sólo ADMIN ACTIVO verificado en la base; idempotente; un administrador no modifica su propia cuenta | `AdministracionApiTest` |
| CU-07 Asignar rol | API-11 con las mismas reglas | `AdministracionApiTest` |
| CU-08 Recuperar contraseña | API-12 (202 uniforme) y API-13 (token de un solo uso, vencimiento, sólo hash) | `RecuperacionApiTest` |
| CU-09 Renovar y validar sesión | API-14 renueva con el rol actual; API-15 informa rol y estado vigentes; ambas rechazan cuentas inactivas, inexistentes y tokens vencidos | `SesionApiTest` |
| CU-10 Estado y rol para módulos | API-17 emite el token de módulo; API-16 con ese token; API-03 y API-05 abiertas a módulos según RN-15; los módulos nunca escriben | `ModulosApiTest` |
| Administrador inicial | Se crea sólo con `ADMIN_EMAIL` y `ADMIN_PASSWORD`; sin ellos no hay ninguno | `AdministradorInicialTest` |

Concurrencia: no se usan bloqueos de filas ni pruebas de carga, porque exceden lo visto en la cátedra. Si dos registros con el mismo email llegaran exactamente a la vez, la restricción `UNIQUE` de la base impide el duplicado y `GlobalExceptionHandler` lo informa como 409 `EMAIL_ALREADY_REGISTERED` (`IntegridadApiTest`).

## Pendientes y riesgos conocidos

| Tema | Situación | Quién debe resolverlo |
|---|---|---|
| Firma asimétrica (RS256) y distribución de la clave pública | No implementada; HS256 con secreto compartido | Todos los grupos |
| Claves de los módulos | Implementadas; cada equipo debe recibir la suya por un canal seguro, usar HTTPS fuera de laboratorio y acordar la rotación. Los equipos que hoy mandan `X-Api-Key` (Notificaciones, Opiniones y Analítica) pueden seguir así o pasarse al Bearer; Pagos debe completar su token de servicio con el JWT de API-17 | Todos los grupos |
| Cambio de `sub` (email → id) | Implementado; los consumidores y el Portal deben leer `sub` como id | Portal y módulos consumidores |
| Notificaciones (INT-01 e INT-06) | Cliente HTTP real implementado (`NOTIFICACIONES_MODO=http`); el mock declarado sigue siendo el modo por defecto. Falta que Notificaciones cree las plantillas `USUARIO_BIENVENIDA_EMAIL` y `RECUPERACION_PASSWORD_EMAIL`, comparta el `JWT_SECRET` y consulte el contacto con `GET /api/v1/users/{id}` y su `X-Api-Key` (o el JWT de módulo) | Notificaciones |
| JWT sin revocación (DC-10) | Un token emitido conserva su rol y sigue siendo válido hasta vencer aunque la cuenta cambie de rol, se desactive o restablezca su contraseña. API-15 y API-16 dan el dato vigente; las operaciones sobre perfil y direcciones rechazan a las cuentas inactivas | Este equipo |
| Renovación deslizante | Un token robado y vigente puede renovarse mientras la cuenta esté ACTIVA; se corta con CU-06. Un token de renovación persistido lo evitaría (DM-03 lo anticipa) | Este equipo |
| Recuperación sin límite de pedidos | No hay limitación por cuenta ni por origen; conviene sumarla (por ejemplo, en un gateway) antes de exponer el servicio | Este equipo |
| Primer administrador | Se aprovisiona con `ADMIN_EMAIL` y `ADMIN_PASSWORD`; usar una contraseña propia, no la de los ejemplos | Este equipo |
| Secreto de desarrollo | El valor por defecto es sólo local. Un secreto anterior estuvo en el historial de Git: rotar el secreto de cualquier ambiente que lo haya usado | Este equipo |
| Base de datos | PostgreSQL persistente con `docker-compose.yml` y credenciales de desarrollo por defecto; en ambientes compartidos definir `DB_*` con credenciales propias. Las tablas las crea Hibernate (`ddl-auto=update`), sin migraciones versionadas | Este equipo |
| Códigos técnicos fuera del enum del YAML | `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE` | Acordar el catálogo común |

## Orden de avance

1. Compartir el YAML con el Portal y los módulos: DTO de sesión, errores, administración, recuperación de contraseña y token de módulo (API-17); comunicar el cambio de `sub`.
2. Acordar firma, claves, emisor/audiencia y la forma de entregar y rotar las claves de módulo.
3. Probar de punta a punta INT-01 e INT-06 con `NOTIFICACIONES_MODO=http` cuando Notificaciones tenga sus plantillas y el `JWT_SECRET` compartido; luego evaluar dejar `http` como modo por defecto.
4. Ante cualquier cambio, actualizar el YAML, correr `mvn test` y contrastar la Swagger generada (`/v3/api-docs`) con el contrato.

El inventario de consumidores y sus incompatibilidades actuales está en [Integración](../integracion/README.md). No deben confundirse contratos disponibles y pruebas internas con integraciones efectivamente ejecutadas por los otros equipos.
