# Acta de decisiones técnicas

Módulo Usuarios y Autenticación · Grupo 02. Este documento reúne y ordena las decisiones del módulo, que antes estaban repartidas en tres documentos. No las reemplaza: cada fila indica dónde está la justificación completa (alternativas consideradas, motivo e impacto).

| Serie | Qué cubre | Documento con el detalle |
|---|---|---|
| DM-01 a DM-12 | Modelado de la primera entrega | Primera entrega (`G02_Usuarios_PrimeraEntrega_v1.5.pdf`, sección 7) |
| DM-13 a DM-19 | Modelado de la evolución (CU-06 a CU-10) y del cierre | [DISENO_CU_EVOLUCION.md](openapi/DISENO_CU_EVOLUCION.md) |
| DC-01 a DC-10 | Detalles del contrato OpenAPI | [ACUERDOS_Y_BRECHAS.md](openapi/ACUERDOS_Y_BRECHAS.md) |

## Decisiones de modelado

| ID | Decisión | Dónde se ve |
|---|---|---|
| DM-01 | Rol como entidad propia, un rol por usuario | `domain/Rol`, REL-02 |
| DM-02 | Dirección como entidad propia, no embebida | `domain/Direccion`, REL-01 |
| DM-03 | Sesión sin persistencia: JWT firmado en lugar de una entidad Sesión | `security/JwtService` |
| DM-04 | Baja lógica de direcciones | Campo `activa`, `DireccionService` |
| DM-05 | Estado del usuario como atributo enumerado (ACTIVO, INACTIVO) | `domain/EstadoUsuario` |
| DM-06 | Los otros módulos referencian al usuario por id y usan la API; nunca la base | `doc/integracion/README.md` |
| DM-07 | El registro público sólo crea CLIENTE; el primer administrador sale de `ADMIN_EMAIL` y `ADMIN_PASSWORD` | `UsuarioService`, `AdministradorInicialTest` |
| DM-08 | Sesión e identidad se validan en un filtro de seguridad | `JwtAuthenticationFilter`, `SecurityConfig` |
| DM-09 | Validación en dos niveles: formato en DTO y Controller, reglas en el Service | `dto/`, `service/` |
| DM-10 | Escrituras transaccionales en el Service (`@Transactional`) | `service/` |
| DM-11 | Base relacional propia del módulo: PostgreSQL (H2 sólo en las pruebas) | `docker-compose.yml`, `application.properties` |
| DM-12 | Relaciones unidireccionales y de carga diferida | `domain/` |
| DM-13 | La administración (CU-06, CU-07) la hace un ADMIN verificado contra la base, con rutas bajo `/users/{id}` | `AdministracionService` |
| DM-14 | El token de recuperación es aleatorio de 256 bits y se guarda sólo su hash | `RecuperacionService`, ENT-04 |
| DM-15 | Renovación deslizante: un JWT vigente se canjea por uno nuevo, sin refresh token persistido | `AuthService.renovarSesion` |
| DM-16 | Los módulos piden a Usuarios un JWT con rol MODULO (`POST /auth/module-token`) y lo usan como Bearer, sólo lectura; la clave en `X-Api-Key` sigue aceptándose como alternativa | `AuthService.emitirTokenModulo`, `ApiKeyAuthenticationFilter`, `SecurityConfig` |
| DM-17 | El JWT no se revoca; el rol y el estado vigentes se consultan con CU-09 y CU-10 | API-15, API-16 |
| DM-18 | El alta de usuario no abre una transacción propia: `save()` ya es transaccional y la bienvenida se pide después | `UsuarioService.registrarUsuario` |
| DM-19 | Sin bloqueos de filas ni ejecución asíncrona; un email repetido en una carrera lo rechaza la restricción única (409) | `GlobalExceptionHandler`, `IntegridadApiTest` |

## Decisiones del contrato

| ID | Decisión |
|---|---|
| DC-01 | Se ignoran las propiedades que no forman parte del DTO de entrada |
| DC-02 | El email se recorta y se normaliza a minúsculas antes de comparar y guardar |
| DC-03 | `PUT` reemplaza todos los datos editables; los opcionales omitidos quedan sin valor |
| DC-04 | Fechas ISO-8601 con zona horaria |
| DC-05 | Contraseña de al menos 8 caracteres en el alta; en el login, obligatoria y no vacía |
| DC-06 | `X-Correlation-Id` en todas las respuestas |
| DC-07 | IDs positivos: 400 si el formato es inválido y 404 si el usuario no existe |
| DC-08 | Los módulos piden un JWT con rol MODULO presentando su nombre y su clave (API-17), o mandan su clave en `X-Api-Key`; en ambos casos sólo pueden leer, y si llegan las dos cosas manda el JWT |
| DC-09 | La recuperación de contraseña responde siempre 202; el token es de un solo uso y vence a los 30 minutos |
| DC-10 | El JWT no se revoca; se consulta el rol y el estado vigentes |

## Cambios del cierre de la entrega (17/11/2026)

| Cambio | Motivo | Impacto |
|---|---|---|
| Se eliminan los bloqueos de filas, `@Async` y los scripts de validación (DM-19) | Exceden lo visto en la cátedra | Ninguno sobre otros módulos; Notificaciones se llama de forma sincrónica con timeout |
| Los módulos pueden pedir un JWT de módulo (API-17) y usarlo como Bearer, además de seguir mandando `X-Api-Key` (DM-16, DC-08) | El TPO fija la convención "Autenticación: Bearer JWT" y la guía de seguridad de la cátedra (`Seguridad.jpg`) muestra que Usuarios emite el JWT; pero Notificaciones y Analítica ya mandan `X-Api-Key`, y Pagos espera un JWT de servicio emitido por Usuarios | Nadie queda sin servicio: los equipos que hoy mandan `X-Api-Key` (Notificaciones, Opiniones y Analítica) pueden seguir así, y Pagos y los demás pueden pasarse al Bearer sin que Usuarios cambie |
| Backend y PostgreSQL en Docker (`Dockerfile`, `compose.backend.yaml`) | Clase 13 | Ninguno |

### Registro de cambios para el docente

La política de cambios del TPO pide registrar el cambio, analizar su impacto en otros módulos y contar con aprobación docente antes de modificar un contrato obligatorio. Este apartado es ese registro.

| Aspecto | Detalle |
|---|---|
| Qué cambió | Se agregó API-17 (`POST /auth/module-token`), que emite un JWT de módulo (rol `MODULO`, sólo lectura), y se mantiene `X-Api-Key` como forma alternativa de identificar a un módulo en API-03, API-05 (sólo Pedidos) y API-16 |
| Por qué | El TPO fija "Autenticación: Bearer JWT", pero Notificaciones y Analítica ya mandan `X-Api-Key` y Pagos espera un JWT emitido por Usuarios. Aceptar ambas evita romper a los equipos que ya integraron |
| Contratos obligatorios | Sin cambios: registro, login, `GET`/`PUT /users/{id}` y `POST /users/{id}/addresses` quedan como estaban. API-17 es una operación nueva y las otras dos formas de identificación sólo amplían quién puede leer |
| Impacto en otros módulos | Ninguno obligatorio: quien ya usa `X-Api-Key` sigue igual; quien prefiera Bearer pide su token. Detalle por equipo en [integración](integracion/README.md) (aviso a los equipos del 17/11/2026) |
| Seguridad | Las claves son una por módulo, de al menos 16 caracteres, por variable de entorno y sin valores por defecto; se comparan en tiempo constante; el módulo nunca escribe ni renueva sesiones; si llegan JWT y clave, manda el JWT |
| Verificación | 97 pruebas automáticas y recorrido HTTP sobre PostgreSQL ([verificación](openapi/VERIFICACION.md)) |
| Estado de la aprobación | Se somete a la consideración del docente; al momento de esta entrega no consta una respuesta suya, y la decisión queda registrada como decisión del grupo hasta que la haya |

## Pendientes conocidos

La firma asimétrica del JWT (hoy HS256 con secreto compartido), la revocación de tokens, un broker para Notificaciones y las migraciones con Flyway están registrados como evolución futura en [BACKLOG.md](BACKLOG.md). El estado de cada integración con otros equipos está en [integracion/README.md](integracion/README.md).
