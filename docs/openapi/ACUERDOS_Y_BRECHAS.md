# Decisiones del contrato y trabajo pendiente

Este documento acompaña los YAML del Grupo 02. Distingue el diseño ya presentado, las decisiones que hacen preciso el intercambio y los acuerdos externos pendientes. El backend Java ya implementa las nueve operaciones del contrato; este documento registra qué se resolvió, qué decisiones se tomaron al hacerlo y qué sigue pendiente de acuerdo con otros equipos. No se declara completada la Etapa 2: faltan los CU de evolución y los acuerdos de integración.

## Base elegida

La referencia funcional es `G02_Usuarios_PrimeraEntrega_v1.5.pdf`, especialmente las secciones 4, 5, 12 y 13. Aunque el nombre del archivo termina en v1.5, su portada indica versión 1.0. Para evitar ambigüedad, se identifica siempre por nombre completo y hash en el inventario de fuentes. El PDF anterior del repositorio, `G02_Usuarios_PrimeraEntrega_1.0.pdf`, no sustituye al adjunto indicado por el usuario.

Se mantienen los cinco endpoints obligatorios de la consigna y los cuatro que el equipo añadió para completar direcciones. Los ejemplos de Comercio enseñan patrones de implementación; sus rutas `/auth/login`, `/v1/comercios`, `username` y `expiresInSeconds` no reemplazan los contratos particulares de Usuarios.

La transcripción C13 vuelve a mencionar los diez CU para el trabajo final. Este avance formaliza los cinco CORE ya desarrollados y registra los cinco de evolución al final de este documento. No se presenta el CORE como el alcance total de la entrega final.

## Decisiones que completan el contrato

Son precisiones de diseño de esta entrega, no nuevas exigencias atribuidas al profesor.

| ID | Decisión | Motivo e impacto |
|---|---|---|
| DC-01 | Rechazar propiedades desconocidas en los DTO de entrada con `400 VALIDATION_ERROR` | `additionalProperties: false` hace verificable que registro no acepta rol/id y que perfil no modifica estado/contraseña. El backend debe configurarlo explícitamente. |
| DC-02 | Recortar espacios exteriores y normalizar el email a minúsculas antes de comparar y persistir | Evita duplicados por capitalización. Coincide con la intención del servicio actual. La entrada debe satisfacer el formato email del esquema; el recorte defensivo no amplía los valores admitidos por ese formato. |
| DC-03 | `PUT` reemplaza todos los datos editables; opcionales omitidos o `null` quedan sin valor | Formaliza el PUT completo de la sección 12.2. `telefono` en blanco también se elimina. En respuestas, telefono/piso/departamento sin valor aparecen como `null`. No se interpreta omisión como «conservar» en PUT. |
| DC-04 | Serializar fechas con zona horaria y ejemplos UTC, compatibles con `format: date-time` | La primera entrega pide ISO-8601 pero modela `LocalDateTime`. Hay que definir la conversión de almacenamiento a la zona acordada; no agregar `Z` a una hora local sin convertirla. |
| DC-05 | En alta, contraseña mínima de 8 caracteres; en login, obligatoria y no vacía | Sigue las validaciones específicas de DTO-01 y DTO-03. No se reaplica al login una política de registro. La contraseña nunca se recorta ni normaliza. El email de login usa formato email. |
| DC-06 | Correlación en todas las respuestas, incluida seguridad; aceptar 1–128 caracteres `A-Za-z0-9._-` | Se devuelve el ID válido recibido o se genera uno. Una cabecera inválida produce 400 con ID nuevo. Las respuestas incluyen `Cache-Control: no-store`; las altas incluyen `Location` y los 401 `WWW-Authenticate`. Estos detalles completan el contrato HTTP. |
| DC-07 | IDs Long positivos; 400 para formato/rango inválido; 404 si el usuario autenticado/autorizado solicitado no existe | Completa casos técnicos omitidos de algunas filas del PDF, especialmente API-06. Primero se autentica y autoriza; no se usa 404 para permitir consultar la existencia de perfiles ajenos. |

`numero` y `piso` siguen siendo texto, como en el diseño: no se convierten a entero porque pueden contener `S/N`, `PB` o valores alfanuméricos. El atributo `referencia` existe en el código actual pero no en los DTO de la primera entrega; queda fuera del contrato CORE. Añadirlo después requiere documentar la extensión.

Las longitudes máximas siguen el modelo de datos (DER) y son las mismas en DTO, columnas y OpenAPI: nombre y apellido 80, email 120, teléfono 30, calle 100, número/piso/departamento/código postal 10, ciudad y provincia 80. La contraseña tiene mínimo 8 caracteres y, como BCrypt sólo considera los primeros 72 bytes, el registro rechaza con 400 las que superan 72 bytes en UTF-8 en lugar de truncarlas en silencio; el login acepta cualquier longitud (simplemente no coincidirá).

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

Una persona con rol ADMIN o COMERCIO no se convierte en un módulo consumidor. Para el CORE, todos los roles humanos sólo operan sobre su propio perfil. El registro público crea CLIENTE y no permite autoasignarse permisos. El alta o cambio de otros roles pertenece a CU-07.

| Actor | Consulta de perfil API-03 | Listado de direcciones API-05 | Escrituras sobre perfil/direcciones |
|---|---|---|---|
| Titular autenticado y ACTIVO | Su propio perfil | Sus direcciones | Sólo sus recursos |
| Pedidos con credencial propia autorizada | Diseñada | Diseñado | No |
| Logística, Catálogo, Notificaciones, Pagos, Opiniones y Analítica con credencial propia autorizada | Diseñada | No | No |
| Otro usuario o consumidor sin permiso | 403 | 403 | 403 |

**La credencial propia de los módulos sigue pendiente**, tal como señala la primera entrega. El esquema `bearerAuth` de los YAML formaliza el JWT de usuario; no habilita una supuesta identidad de servicio. Las extensiones `x-integracion-modulos` de API-03 y API-05 registran las capacidades previstas sin inventar un API key, un flujo OAuth ni roles técnicos aceptados. Hasta acordar ese mecanismo, esas consultas con identidad propia de servicio deben permanecer deshabilitadas o probarse con un mock explícito. Una llamada que propague el JWT del usuario conserva los permisos de ese usuario, no los de un módulo.

Cuando se implemente esa integración, los módulos autorizados podrán leer el perfil de un usuario INACTIVO con 200 y decidir con sus reglas; el titular inactivo recibe 403. Pedidos podrá listar direcciones activas y consultar API-03 para conocer el estado del usuario. Es la distinción especificada en CU-03 y SRV-09.

## Notificaciones y disponibilidad

INT-01 solicita la bienvenida después del alta. La ruta de referencia es `POST /api/v1/notifications`, propiedad del equipo Notificaciones. El DTO definitivo, autenticación, tiempos de espera y propagación de correlación todavía requieren su contrato; por eso esa API no aparece como un endpoint propio de Usuarios.

Un fallo de bienvenida no revierte el alta ni transforma el 201 en un error. El cliente se invoca después de confirmar el alta (`NotificacionesClient`, con el mock declarado `NotificacionesClientMock`, que sólo registra el id del usuario en el log, sin email ni nombre). `BienvenidaApiTest` verifica que se solicita una vez por alta confirmada, que no se solicita si el registro falla y que una excepción del cliente no cambia el 201. No hay integración HTTP real: queda sujeta al contrato de Notificaciones.

## Estado de la implementación frente al contrato

Las diferencias relevadas el 30/10/2026 sobre el backend anterior quedaron resueltas. La columna de evidencia indica qué prueba automática lo comprueba (detalle en [VERIFICACION.md](VERIFICACION.md)).

| Área | Resolución en el backend | Evidencia |
|---|---|---|
| Login | Recibe `email` y `password`; devuelve `token`, `tipo`, `expiraEn`, `usuarioId`, `nombre` y `rol` | `AuthApiTest` |
| JWT | `sub` = id, `role`, `iat`, `exp`; claim `UID` eliminado; HS256 | `JwtServiceTest`, `AuthApiTest` |
| Registro | DTO validado, email normalizado y único (también en concurrencia), rol CLIENTE y estado ACTIVO fijos, contraseña con hash BCrypt | `AuthApiTest` |
| PUT de perfil | Reemplazo completo de campos editables; rechaza password, rol y estado; 409 si el email es de otro usuario | `PerfilApiTest` |
| Direcciones | API-05 a API-09 implementadas; baja lógica; una sola predeterminada, garantizada con bloqueo pesimista de la fila del usuario | `DireccionesApiTest` (incluye dos pruebas de concurrencia) |
| DTO de dirección | Sin `referencia` en las entradas ni salidas; el atributo se conserva sólo en la entidad | `DireccionesApiTest` |
| Formato de errores | `ErrorResponse` con `timestamp`, `status`, `code`, `message`, `path`, `correlationId` y `details`; un único `GlobalExceptionHandler` | todas las clases de prueba |
| Códigos de error | Catálogo del PDF en inglés; inactivo devuelve 403 `USER_INACTIVE` | `AuthApiTest`, `PerfilApiTest` |
| Seguridad | Sólo el titular opera sobre su recurso (`@PreAuthorize`); ADMIN y COMERCIO humanos reciben 403 | `PerfilApiTest` |
| IDs de ruta | `Long` positivo; 400 para texto, cero, negativo, decimal o fuera de rango, después de autenticar | `PerfilApiTest` |
| Ausencia o falla del token | 401 `UNAUTHENTICATED` con `WWW-Authenticate: Bearer`, en el formato común (entry point y access denied handler) | `PerfilApiTest`, `DireccionesApiTest` |
| Titular inactivo | 403 `USER_INACTIVE` en lectura y escritura; la consulta de módulo autorizado sigue pendiente | `PerfilApiTest`, `DireccionesApiTest` |
| Fechas | UTC, truncadas a segundos, serializadas con zona | `AuthApiTest` |
| Validación | Starter de Validation; propiedades desconocidas y tipos incorrectos rechazados con 400 (sin conversión silenciosa) | `AuthApiTest`, `DireccionesApiTest` |
| Correlación | `X-Correlation-Id` validado, generado si falta, devuelto en cabecera, cuerpo y log (MDC); `Cache-Control: no-store`; `Location` en los 201 | `CorrelacionApiTest` |
| CORS | Orígenes configurables (`CORS_ALLOWED_ORIGINS`); permite PATCH y `X-Correlation-Id`; expone `X-Correlation-Id` y `Location` | `CorrelacionApiTest` |
| Swagger generado | Interfaces `I*Controller` con anotaciones de springdoc; operaciones públicas sin candado; coincide con el YAML | revisión de `/v3/api-docs` |
| Java y Maven | `java.version` 25 única; groupId `ar.edu.uade.api.deliverygo`; paquete `ar.edu.uade.api.deliverygo.usuarios` | `mvn test` |
| Secreto JWT | Fuera del repositorio: variable `JWT_SECRET`; se exige un mínimo de 32 bytes | `JwtServiceTest` |

Las reglas de concurrencia se probaron con ejecuciones simultáneas reales (dos registros con el mismo email; selecciones y altas simultáneas de predeterminada). Para comprobar que esas pruebas detectan el problema, se retiró temporalmente el bloqueo de la fila del usuario y ambas fallaron en tres corridas seguidas; con el bloqueo restituido pasan.

## Pendientes y riesgos conocidos

| Tema | Situación | Quién debe resolverlo |
|---|---|---|
| Firma asimétrica (RS256) y distribución de la clave pública | No implementada; HS256 con secreto compartido | Todos los grupos |
| Credencial propia de los módulos consumidores | Sin definir; las consultas «de módulo» (Pedidos, Catálogo, etc.) no están habilitadas | Todos los grupos |
| Cambio de `sub` (email → id) | Implementado; los consumidores y el Portal deben leer `sub` como id | Portal y módulos consumidores |
| Contrato real de Notificaciones (INT-01) | Mock declarado que sólo escribe en el log | Notificaciones |
| CU-06 a CU-10 | No implementados; ver el backlog | Este equipo |
| Tokens vigentes de un usuario que pasa a INACTIVO | Siguen siendo válidos para el JWT, pero el servicio responde 403 `USER_INACTIVE`; la revocación se define con CU-06/CU-09 | Este equipo |
| Secreto de desarrollo | El valor por defecto es sólo local. Un secreto anterior estuvo en el historial de Git: rotar el secreto de cualquier ambiente que lo haya usado | Este equipo |
| Consola H2 y base en memoria | Habilitadas sólo para desarrollo; deshabilitar y usar base persistente en ambientes compartidos | Este equipo |
| Códigos técnicos fuera del enum del YAML | `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE` | Acordar el catálogo común |
| Diagramas | Los `.puml` e imágenes de `docs/Diagramas` no se regeneraron; el diagrama de clases sigue mostrando `LocalDateTime` y coincide con la entidad | Este equipo |

## Backlog de evolución

| CU | Función | Definición pendiente antes de publicar un contrato de evolución |
|---|---|---|
| CU-06 | Desactivar o reactivar usuario | Quién puede hacerlo, transiciones y efecto sobre tokens vigentes |
| CU-07 | Administrar roles | Permisos de administración, roles asignables y actualización de claims |
| CU-08 | Recuperar contraseña | Token de recuperación, expiración, consumo único y contrato de Notificaciones |
| CU-09 | Renovar o validar sesión/token | Mecanismo de renovación/validación, almacenamiento y revocación si corresponde |
| CU-10 | Consultar estado y rol para otro módulo | Credencial de servicio, DTO mínimo y permisos por consumidor |

No se añadieron endpoints sin diseño para aparentar que estos cinco casos están terminados. La consulta completa de perfil por API-03 y su futura autorización entre módulos ya forman parte del CORE; CU-10 es la consulta liviana adicional de estado/rol.

## Orden de avance

1. Compartir el YAML con el Portal para fijar `email`, DTO de sesión, PUT y errores; comunicar el cambio de `sub` y las diferencias frente al ejemplo de Comercio.
2. Acordar firma, claves, emisor/audiencia e identidad de consumidores; implementar sólo los permisos pactados.
3. Reemplazar el mock de bienvenida por el contrato real de Notificaciones.
4. Implementar los casos de evolución (CU-06 a CU-10) con su contrato antes del código.
5. Ante cualquier cambio, actualizar el YAML, regenerar las vistas, correr `validar_contratos.py` y `mvn test`, y contrastar la Swagger generada con el contrato.
