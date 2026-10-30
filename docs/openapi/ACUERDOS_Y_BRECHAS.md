# Decisiones del contrato y trabajo pendiente

Este documento acompaña los YAML del Grupo 02. Distingue el diseño ya presentado, las decisiones que hacen preciso el intercambio y los acuerdos externos pendientes. El alcance de este avance es entregar contratos; no se modificó la implementación Java ni se declaró completada la Etapa 2.

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

No se inventan longitudes máximas de nombre, calle o teléfono que los adjuntos no establecen. Antes de cerrar la implementación, conviene acordar los límites de almacenamiento y la política de longitud en bytes que admita el codificador de contraseñas, y reflejarlos de forma coherente en DTO, persistencia y OpenAPI.

`details` se define como una lista de `{campo, mensaje}` únicamente en `VALIDATION_ERROR`. No repite el valor recibido; para un cuerpo ilegible se usa `campo: body`. El estado inactivo devuelve **403**, como establece expresamente la sección 12.2.3 del PDF, no el 422 que usa el código actual.

## JWT y autorización entre módulos

Usuarios autentica y emite; el Portal transporta el token; cada backend consumidor valida y autoriza. La imagen `Seguridad.jpg` muestra alternativas con acceso directo y gateway. Para este contrato se adopta el flujo directo coherente con `descarga.svg`; no se introduce una dependencia obligatoria de gateway.

La teoría, página etiquetada 27, explicita firma asimétrica. Los ZIP de laboratorio y `JwtService` actual usan HMAC con clave compartida. Se toma la firma asimétrica como objetivo; **RS256 es una propuesta técnica**, todavía no un algoritmo pactado con los demás grupos. La clave privada queda sólo en Usuarios; los verificadores reciben la pública por un mecanismo acordado. No se inventa un endpoint JWKS ni se copian claves del laboratorio al YAML.

| Dato | Contrato objetivo | Situación actual |
|---|---|---|
| `sub` | ID estable del usuario convertido a texto | Email |
| `role` | Un rol: `CLIENTE`, `COMERCIO`, `REPARTIDOR`, `ADMIN`; sin `ROLE_` | Claim `role` ya presente |
| `iat` y `exp` | NumericDate en segundos | Presentes mediante JJWT |
| `expiraEn` | Vencimiento absoluto ISO-8601, coherente con `exp` | Respuesta con `expiresInSeconds` |
| `UID` | No es necesario si `sub` contiene el ID | Claim personalizado usado por el filtro |
| Algoritmo y claves | Firma asimétrica, algoritmo y distribución a acordar | HMAC con secreto configurable |

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

Un fallo de bienvenida no revierte el alta ni transforma el 201 en un error. El diseño contempla un mock declarado; la clase `NotificacionesClientes` del repositorio está vacía y el mock todavía debe materializarse. No hay evidencia de una integración HTTP real en este avance.

## Diferencias comprobadas en el repositorio

Relevamiento estático del 30/10/2026. «Parcial» significa que hay código, no que se haya validado su comportamiento por HTTP.

| Área | Evidencia actual | Trabajo necesario para cumplir el YAML |
|---|---|---|
| Login de entrada | `AuthController` recibe `LoginDto.username`; `LoginRequest` está vacío | Recibir `email` y `password` como DTO-03 |
| Login de salida | Devuelve `TokenDto(token, tipo, expiresInSeconds)`; `LoginResponse` está vacío | Devolver `expiraEn`, `usuarioId`, `nombre`, `rol`, además de token y tipo |
| JWT | `sub=email`, `UID=id`, firma HMAC | Coordinar el contrato de claims y firma; actualizar emisor y consumidores juntos |
| Registro | Implementa unicidad, hash, rol CLIENTE y estado ACTIVO | Validar DTO, restricciones de campos desconocidos, cabeceras y bienvenida |
| PUT de perfil | Actualiza sólo campos no nulos | Exigir nombre/apellido/email y aplicar reemplazo de opcionales |
| Direcciones | `DireccionController` y `DireccionService` vacíos | Implementar API-05 a API-09, mapeo, pertenencia y transacciones |
| DTO de dirección | Incluyen `referencia` | Ajustar al diseño o registrar una extensión explícita antes de cambiar el contrato |
| Formato de errores | `ErrorResponse` serializa getters `codigo`, `mensaje`, `fecha`, `status` | Unificar `timestamp`, `status`, `code`, `message`, `path`, `correlationId` y details de validación |
| Códigos de error | Usa nombres en español; inactivo devuelve 422 | Usar el catálogo del PDF y 403 para `USER_INACTIVE` |
| Seguridad | El filtro considera módulo a todo rol distinto de CLIENTE para GET | Sustituir por identificación explícita de consumidor y permisos por operación; no ampliar acceso por rol humano |
| Rutas de seguridad | La comprobación de titularidad usa regex limitada a 1–18 dígitos | Validar todos los IDs Long y rutas protegidas sin caminos que eludan el control |
| Ausencia de token | Queda delegada al comportamiento por defecto de Spring Security | Configurar respuesta uniforme 401; tanto entry point como access denied handler deben emitir DTO-08 |
| Consulta de inactivos | `UsuarioService.consultarPerfil` rechaza siempre el estado INACTIVO | Distinguir consulta del titular de consulta de módulo autorizada cuando se implemente esa identidad |
| Fechas | DTO usa `LocalDateTime` | Serializar con zona definida conforme a DC-04 |
| Validación | No hay `@Valid` en estos controladores; el POM no declara el starter de Validation | Agregar validación en la frontera y manejo uniforme de JSON/ruta inválidos |
| Correlación | No se gestiona uniformemente; error del filtro no incluye correlationId | Crear/propagar el ID y devolverlo en cabecera y ErrorResponse |
| CORS | Configura origen 5174; no incluye PATCH ni X-Correlation-Id | Acordar origen del Portal; permitir PATCH, cabecera de correlación y preflight; exponer cabeceras de respuesta necesarias |
| Swagger generado | `OpenApiConfig` agrega seguridad global sin documentar excepciones en operaciones públicas | Anotar correctamente registro/login, DTO, estados y ejemplos; comprobar paridad con el contrato |
| Java | `release=25` junto a `source/target=24` | Unificar configuración para Java 25 según el instructivo; no fue necesario compilar para validar YAML |

Las reglas de concurrencia también requieren implementación real: la unicidad del email debe apoyarse en la base de datos y elegir una predeterminada debe impedir dos seleccionadas simultáneamente. Un `@Transactional` aislado no constituye evidencia de que se haya probado esa concurrencia.

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

1. Compartir el YAML con el Portal para fijar `email`, DTO de sesión, PUT y errores; comunicar las diferencias frente al ejemplo de Comercio.
2. Implementar las nueve operaciones y sus validaciones; unificar errores de aplicación y seguridad.
3. Acordar firma, claves, emisor/audiencia e identidad de consumidores; implementar sólo los permisos pactados.
4. Integrar bienvenida con un contrato real o un mock declarado y verificable.
5. Ejecutar los escenarios funcionales, de seguridad y concurrencia; contrastar Swagger generado y contrato entregado.
