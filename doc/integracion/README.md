# Integración de Usuarios con el ecosistema

Revisión y pruebas del **16/11/2026**. Usuarios expone sus 17 operaciones sobre PostgreSQL. Se probaron por HTTP la integración de JWT con Catálogo y los flujos de bienvenida y recuperación con Notificaciones. Para Notificaciones fue necesario aplicar el parche local incluido; ese resultado no implica que su repositorio remoto ya tenga la corrección.

## Contrato que deben consumir los equipos

Base local de esta demo: `http://localhost:8080/api/v1`. El puerto habitual de Usuarios solo sigue siendo 8089.

| Dato | Contrato de Usuarios |
|---|---|
| ID de usuario y dirección | `Long` positivo; JSON numérico, no UUID |
| Perfil | `GET /users/{id}`; campos `id`, `nombre`, `apellido`, `email`, `telefono`, `rol`, `estado`, `fechaAlta` |
| Direcciones | `GET /users/{id}/addresses`; lista separada del perfil, exclusiva de Pedidos entre los módulos |
| Estado y rol | `GET /users/{id}/access`; `usuarioId`, `estado`, `rol` |
| Credencial de módulo | JWT con rol `MODULO`: el módulo lo pide una vez con `POST /auth/module-token` (`{"modulo","clave"}`) y lo envía como `Authorization: Bearer`; lo vuelve a pedir al vencer (8 horas por defecto). Alternativa: la misma clave en `X-Api-Key` en cada llamada; si llegan las dos, manda el JWT. El Bearer sigue la convención del TPO; `X-Api-Key` se conserva porque algunos módulos ya la usan |
| Estado | Texto `ACTIVO` / `INACTIVO`; no se publica un booleano `activo` |
| JWT de persona | HS256, `sub` = id convertido a texto, `role` sin prefijo `ROLE_`, `iat` y `exp` en segundos |
| JWT de servicio saliente | `role=SISTEMA`, `sub` = destinatario, duración 2 minutos; Usuarios no lo acepta como sesión |

Todos los módulos pueden leer perfil y acceso con su JWT de módulo o su `X-Api-Key`; sólo Pedidos puede listar direcciones. Según el código publicado (corte del 17/11), Notificaciones y Analítica mandan `X-Api-Key`, Pagos espera un JWT de servicio emitido por Usuarios y Logística sólo tiene mocks: con las dos formas aceptadas ninguno necesita cambiar para empezar. Ningún módulo escribe. Una cuenta inactiva se devuelve a los módulos con 200 para que decidan según sus reglas. El perfil no incluye contraseñas, hashes ni direcciones embebidas.

El Swagger agregado de laboratorio incluido en el material de clase contiene sólo una parte de Usuarios. Para integrar, usar [el contrato completo](../openapi/deliverygo-usuarios.yaml) o `/v3/api-docs` del servicio actual. No convertir UUID a Long mediante hashes o recortes: las referencias externas deben conservar el identificador real del recurso.

## Usuarios y Notificaciones

Repositorio externo revisado: [DeliveryGo-Notificaciones](https://github.com/FrancoPedreira/DeliveryGo-Notificaciones/tree/64cf38d6d2ad7324aa8de4922a24e2cc50f3db72).

Se encontraron dos problemas reproducibles en ese commit:

1. `JwtAuthFilter` admite los cuatro roles de persona pero rechaza `SISTEMA`: la búsqueda de plantillas devuelve 401.
2. Jackson 3 elige el constructor con parámetros de `UsuarioContactoResponseDto` e intenta mapear un `activo` ausente a `boolean`. La consulta devuelve 200 desde Usuarios, pero el cliente externo falla al deserializar y termina respondiendo 503.

[El parche](notificaciones-compatibilidad-usuarios.patch) admite `SISTEMA` sólo para `GET /api/v1/templates` y `POST /api/v1/notifications`; no le habilita historial ni administración. También indica con `@JsonCreator` que el DTO de contacto se construye con el constructor vacío y sus setters, conservando el mapeo `estado` → `activo`. Incluye pruebas de permisos. Las plantillas necesarias ya están en el DataLoader de ese commit.

Aplicar **en una copia del repositorio de Notificaciones**, desde su raíz:

```powershell
git apply --check "C:/ruta/deliverygo-usuarios-auth/doc/integracion/notificaciones-compatibilidad-usuarios.patch"
git apply "C:/ruta/deliverygo-usuarios-auth/doc/integracion/notificaciones-compatibilidad-usuarios.patch"
.\mvnw.cmd test
```

No volver a aplicar si el equipo ya integró cambios equivalentes. El parche se verificó con `git apply --check` sobre el commit indicado y la suite completa: **103 pruebas, 0 fallas, 0 errores**. No se modificó ni publicó el repositorio remoto del otro equipo.

Terminal de Usuarios (PowerShell, raíz de este proyecto):

```powershell
docker compose up -d
$env:SERVER_PORT = '8080'
$env:JWT_SECRET = 'deliverygo-integracion-local-2026-no-produccion'
$env:ADMIN_EMAIL = 'admin@deliverygo.test'
$env:ADMIN_PASSWORD = 'AdminDemo2026!'
$env:MODULE_KEY_PEDIDOS = 'pedidos-demo-local-2026'
$env:MODULE_KEY_CATALOGO = 'catalogo-demo-local-2026'
$env:MODULE_KEY_NOTIFICACIONES = 'notificaciones-demo-local-2026'
$env:NOTIFICACIONES_MODO = 'http'
$env:NOTIFICACIONES_URL = 'http://localhost:8089'
mvn spring-boot:run
```

Terminal de Notificaciones (PowerShell, raíz de su proyecto, con el parche):

```powershell
$env:JWT_SECRET = 'deliverygo-integracion-local-2026-no-produccion'
$env:NOTIFICACIONES_USUARIOS_MODO = 'http'
$env:USUARIOS_BASE_URL = 'http://localhost:8080'
$env:USUARIOS_API_KEY = 'notificaciones-demo-local-2026'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8089 --notificaciones.emisor.tasa-fallo=0"
```

Son credenciales públicas de ejemplo para local. El administrador se crea sólo si no existe; cambiar `ADMIN_PASSWORD` no reemplaza la contraseña de un administrador ya persistido.

Ejecutar la colección [DeliveryGo Integración Notificaciones](../postman/DeliveryGo-Integracion-Notificaciones.postman_collection.json) con `baseUrl=http://localhost:8080` y `notificacionesUrl=http://localhost:8089`. Ver los comandos en [Postman](../postman/README.md).

La colección comprueba bienvenida, consulta de perfil con la credencial de módulo, solicitud de recuperación, obtención del mensaje simulado con la sesión del destinatario, restablecimiento, uso único, login nuevo y rechazo de la contraseña anterior. Resultado: **12 solicitudes, 40 comprobaciones, sin fallas**. La llamada de Notificaciones a Usuarios se corrió el 16/11 con `X-Api-Key` (que Usuarios sigue aceptando; la colección ahora usa el JWT de módulo): hay que repetirla con Notificaciones levantado (la parte de Usuarios del recorrido se repitió el 17/11 con newman sin fallas). Usa el mismo `X-Correlation-Id`; no consulta la base de Usuarios.

El envío EMAIL sigue siendo simulado por el módulo externo, como prevé la guía funcional. El historial de ese commit no restringe la lectura al destinatario; el parche no cambia esa política del otro equipo. Por eso esta demo usa exclusivamente datos ficticios. Antes de usar recuperación con información real, Notificaciones debe restringir el historial y evitar exponer tokens a otras personas.

## JWT con Catálogo

Repositorio probado sin modificar código: [Catálogo](https://github.com/tomypedreira-a11y/catalogo-deliverygo/tree/38b6b079ab59658be3476895396c686341909565).

Desde la carpeta `demo` de ese repositorio:

```powershell
$env:JWT_SECRET = 'deliverygo-integracion-local-2026-no-produccion'
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
```

La [colección de Catálogo](../postman/DeliveryGo-Integracion-Catalogo.postman_collection.json) obtiene los JWT mediante login de Usuarios, crea una categoría con ADMIN, comprueba 403 con CLIENTE y 401 sin token, y elimina la categoría creada. **7 solicitudes, 8 comprobaciones, sin fallas**. No demuestra que Catálogo consulte estado vigente de Usuarios: esta integración es la validación del JWT y sus permisos.

## Revisión de los demás consumidores

Estas filas son hallazgos del código publicado, no integraciones completas aprobadas. No se contabilizan mocks como pruebas entre módulos reales.

| Proyecto y commit | Hallazgo | Trabajo necesario del consumidor |
|---|---|---|
| [Pedidos](https://github.com/santi28/uade-deliverygo-ms-orders/tree/d33c6755fc77256d1a932aed029716bddb62953f) | `UsuariosClientMock` sigue activo incluso en perfil `integracion`; no existe cliente HTTP. `UsuarioDto` y las referencias de dirección usan UUID. Su comentario afirma que Usuarios no publica perfil/direcciones, pero ya existen. | Implementar cliente HTTP con clave de Pedidos, consultar perfil y direcciones por separado y usar Long para referencias externas de Usuarios. Los IDs propios de pedidos pueden seguir siendo UUID. |
| [Pagos](https://github.com/JulianaGomez15/Aplicaciones_Interactivas_-_E3_-_Modulo_de_Pagos/tree/dc617e84f25be8ad93cca9dd95b2307cecf50916) | `UsuariosClientRest` existe, pero recibe UUID y su DTO busca `codigoUsuario`, mientras Usuarios devuelve `id`. El cliente común propaga Bearer y su configuración espera un "JWT de servicio" que "lo emite Usuarios" (`integraciones.token-servicio`, vacío). | Adaptar la referencia de usuario a Long y `id`; completar ese token con el JWT de módulo de Pagos (`POST /auth/module-token`) para la lectura entre servicios. Revisar también sus referencias provenientes de Pedidos. |
| [Logística](https://github.com/Nicosantillan96/tpo-deliverygo-repartidores-logistica/tree/e65ada23cbdba928017004439ea2e74b33be7237) | Referencia Long compatible, pero sólo tiene `UsuariosClientMock`. | Añadir implementación HTTP que lea `/users/{id}` o `/users/{id}/access`, traduzca `estado` y se identifique con la clave de Logística. |
| [Opiniones y Analítica, develop](https://github.com/emigsm/tpo-deliverygo-g4-ops-analitica/tree/1c043ded5ef7d78179f13dd784314d94ca3286e7) | `UsuariosHttpClient` usa Long y `/users/{id}` con `X-Api-Key`, que Usuarios sigue aceptando (puede pasarse al Bearer del token de módulo cuando quiera), pero el mock no se desactiva con `integracion`; pasa lo mismo con Comercio. El DTO parcial de usuario no declara ignorar campos adicionales. | Separar perfiles de mocks/HTTP, configurar URLs y clave de módulo, y verificar deserialización del perfil completo. El README todavía describe ausencia de HTTP y autenticación temporal. |
| Portal Web | No hay repositorio final del Portal entre los enlaces recibidos; el ZIP React es un laboratorio de referencia. | Probar registro/login y llamadas protegidas desde su aplicación, con el origen agregado en `CORS_ALLOWED_ORIGINS`. |

Usuarios ya permite configurar una clave distinta para cada módulo. Entregar esas claves y acordar URLs es una coordinación entre equipos. Cambiar el tipo de ID de este servicio o inventar aliases para los UUID de otros equipos rompería el contrato aprobado.

## Evidencia y límites

- Usuarios: pruebas automáticas con `mvn test` (ver [VERIFICACION.md](../openapi/VERIFICACION.md)); 45 solicitudes Postman sobre PostgreSQL y 155 comprobaciones.
- Notificaciones: 103 pruebas de su proyecto con el parche; recorrido HTTP completo con 40 comprobaciones.
- Catálogo: recorrido con JWT real y 8 comprobaciones.
- OpenAPI: las 16 operaciones iniciales coincidían con Springdoc en ejecución; la API-17 (token de módulo) se agregó el 17/11 y su contrato está probado con `ModulosApiTest`.
- [Resúmenes de Postman](../postman/evidencia/) sin JWT, contraseñas ni cuerpos de solicitudes.

La consigna pide demostrar integraciones reales en el ecosistema. Los cruces efectivamente probados acá son Usuarios → Notificaciones, Notificaciones → Usuarios y validación del JWT de Usuarios en Catálogo; no equivalen a haber completado el ciclo de compra. Pedidos, Pagos, Logística, Analítica y Portal requieren los ajustes y la prueba conjunta indicados arriba. La firma asimétrica también sigue pendiente de acuerdo común: la integración actual usa HS256, igual que el laboratorio y los consumidores revisados.
