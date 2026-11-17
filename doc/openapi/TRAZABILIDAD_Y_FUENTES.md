# Trazabilidad y fuentes del contrato

El contrato parte de la consigna del TPO, la guía funcional por módulos, la plantilla de la primera entrega, el diseño del Grupo 02 (PDF de primera entrega) y la teoría de las clases 1 a 13. Esta carpeta relaciona cada operación con su caso de uso, regla, servicio, DTO e integración.

## Criterio para resolver diferencias

Se usan conjuntamente la consigna, las aclaraciones de clases/Teams, la guía funcional y el diseño específico del Grupo 02. Para los nombres y comportamientos ya desarrollados del módulo se conserva el PDF de la primera entrega. Los laboratorios aportan ejemplos de implementación; no tienen prioridad sobre el contrato particular de Usuarios. Las ambigüedades y diferencias con el código actual se hacen explícitas en `ACUERDOS_Y_BRECHAS.md`.

## Matriz de operaciones

Las rutas de esta tabla tienen el prefijo `/api/v1`. SEC-06 a SEC-10, RN-11 a RN-15 y los IDs de API-10 a API-17, DTO-10 a DTO-17, SRV-10 a SRV-17 y REP-09 a REP-12 se definen en [DISENO_CU_EVOLUCION.md](DISENO_CU_EVOLUCION.md), no en el PDF de la primera entrega. DTO-08 se utiliza para los errores de todas las operaciones.

| API | Método y ruta | CU / SEC | SRV | DTO entrada → salida | RN aplicadas |
|---|---|---|---|---|---|
| API-01 | `POST /auth/register` | CU-01 / SEC-01 | SRV-01 | DTO-01 → DTO-02 | RN-01, RN-02, RN-03, RN-06 |
| API-02 | `POST /auth/login` | CU-02 / SEC-02 | SRV-02 | DTO-03 → DTO-04 | RN-01, RN-03, RN-04, RN-08, RN-09 |
| API-03 | `GET /users/{id}` | CU-03 / SEC-03 | SRV-03 | Sin cuerpo → DTO-02 | RN-03, RN-04, RN-05, RN-09, RN-15 |
| API-04 | `PUT /users/{id}` | CU-04 / SEC-04 | SRV-04 | DTO-05 → DTO-02 | RN-01, RN-02, RN-03, RN-04, RN-05, RN-09, RN-10 |
| API-05 | `GET /users/{id}/addresses` | CU-05 / SEC-05 | SRV-09 | Sin cuerpo → lista DTO-07 | RN-04, RN-05, RN-09, RN-15 |
| API-06 | `POST /users/{id}/addresses` | CU-05 / SEC-05 | SRV-05 | DTO-06 → DTO-07 | RN-01, RN-04, RN-05, RN-07, RN-09 |
| API-07 | `PUT /users/{id}/addresses/{addressId}` | CU-05 / SEC-05 | SRV-06 | DTO-09 → DTO-07 | RN-01, RN-04, RN-05, RN-09 |
| API-08 | `DELETE /users/{id}/addresses/{addressId}` | CU-05 / SEC-05 | SRV-07 | Sin cuerpo → sin cuerpo | RN-04, RN-05, RN-07, RN-09 |
| API-09 | `PATCH /users/{id}/addresses/{addressId}/default` | CU-05 / SEC-05 | SRV-08 | Sin cuerpo → DTO-07 | RN-04, RN-05, RN-07, RN-09 |
| API-10 | `PUT /users/{id}/status` | CU-06 / SEC-06 | SRV-10 | DTO-10 → DTO-02 | RN-04, RN-05, RN-09, RN-11, RN-12 |
| API-11 | `PUT /users/{id}/role` | CU-07 / SEC-07 | SRV-11 | DTO-11 → DTO-02 | RN-05, RN-09, RN-11, RN-12 |
| API-12 | `POST /auth/password/forgot` | CU-08 / SEC-08 | SRV-12 | DTO-12 → sin cuerpo | RN-01, RN-13 |
| API-13 | `POST /auth/password/reset` | CU-08 / SEC-08 | SRV-13 | DTO-13 → sin cuerpo | RN-01, RN-03, RN-13 |
| API-14 | `POST /auth/refresh` | CU-09 / SEC-09 | SRV-14 | Sin cuerpo → DTO-04 | RN-04, RN-09, RN-14 |
| API-15 | `GET /auth/session` | CU-09 / SEC-09 | SRV-15 | Sin cuerpo → DTO-14 | RN-04, RN-09 |
| API-16 | `GET /users/{id}/access` | CU-10 / SEC-10 | SRV-16 | Sin cuerpo → DTO-15 | RN-04, RN-15 |
| API-17 | `POST /auth/module-token` | CU-10 / SEC-10 | SRV-17 | DTO-16 → DTO-17 | RN-15 |

La matriz detalla reglas por operación: por eso RN-07 aparece en baja y selección, pero no se aplica como escritura en el listado ni en el PUT de dirección. La ficha general CU-05 conserva su conjunto de reglas RN-01, RN-04, RN-05, RN-07 y RN-09.

## Correspondencia con persistencia e integración

| Servicio | Repositorios del diseño | Entidades y relaciones | Integración |
|---|---|---|---|
| SRV-01 | REP-01, REP-04, REP-03 | ENT-01 Usuario, ENT-02 Rol, REL-02 | INT-01 bienvenida (cliente HTTP real de Notificaciones; mock declarado por defecto); INT-02 Portal |
| SRV-02 | REP-01 | ENT-01, ENT-02, REL-02 | INT-02; token para el ecosistema |
| SRV-03 | REP-02 | ENT-01, ENT-02, REL-02 | INT-02, INT-03, INT-04, INT-05 según autorización |
| SRV-04 | REP-02, REP-01, REP-03 | ENT-01; rol sólo de lectura | INT-02 |
| SRV-05 | REP-02, REP-08, REP-07 | ENT-01, ENT-03 Direccion, REL-01 | INT-02 |
| SRV-06 y SRV-07 | REP-02, REP-06, REP-07 | ENT-01, ENT-03, REL-01 | INT-02 |
| SRV-08 | REP-02, REP-06, REP-08, REP-07 | ENT-01, ENT-03, REL-01 | INT-02 |
| SRV-09 | REP-02, REP-05 | ENT-01, ENT-03, REL-01 | INT-02; Pedidos en INT-03 con JWT de módulo |
| SRV-10 y SRV-11 | REP-03, REP-04, REP-09 | ENT-01, ENT-02, REL-02 | INT-02 |
| SRV-12 y SRV-13 | REP-01, REP-10, REP-11, REP-12 | ENT-01, ENT-04 TokenRecuperacion, REL-03 | INT-02; INT-06 Notificaciones (cliente HTTP real; mock declarado por defecto) |
| SRV-14 y SRV-15 | REP-02 | ENT-01, ENT-02, REL-02 | INT-02, INT-03, INT-04, INT-05 |
| SRV-16 | REP-02 | ENT-01, ENT-02, REL-02 | INT-03, INT-04, INT-05 con JWT de módulo |
| SRV-17 | — | — | INT-03, INT-04, INT-05 (emisión del token de módulo) |

No se exponen entidades JPA ni repositorios como contratos. El servicio es responsable de las reglas y transacciones; el controlador valida la entrada y adapta HTTP; la capa de seguridad verifica identidad y permisos. Cada módulo mantiene su base de datos propia.

## Reglas y evidencia contractual

| RN | Aplicación en YAML |
|---|---|
| RN-01 | Campos required, formatos, minLength, patrón no vacío, tipos y respuesta 400 con details |
| RN-02 | Email normalizado y 409 en registro/modificación |
| RN-03 | password writeOnly; sin password/passwordHash en salidas; hash irreversible indicado |
| RN-04 | 403 USER_INACTIVE; verificación posterior a contraseña en login; los módulos con JWT de módulo reciben la cuenta inactiva con 200 |
| RN-05 | sub frente a id, autorización por titular, 403 y ocultación de direcciones ajenas con 404 |
| RN-06 | Registro sin rol/estado de entrada; respuesta CLIENTE/ACTIVO |
| RN-07 | predeterminada false por omisión, selección en una transacción, PUT sin ese atributo y baja que la desmarca |
| RN-08 | Un único error genérico para email inexistente/contraseña incorrecta |
| RN-09 | Bearer en operaciones protegidas, claims y vencimiento; 401 ante falta o invalidez |
| RN-10 | PUT de perfil limitado a nombre, apellido, email y telefono |
| RN-11 | API-10 y API-11 sólo para ADMIN, verificado contra la base; 403 `FORBIDDEN` y `USER_INACTIVE` |
| RN-12 | 403 cuando el administrador se modifica a sí mismo |
| RN-13 | API-12 con 202 uniforme; API-13 con `INVALID_RESET_TOKEN`; token de un solo uso, vencimiento y hash |
| RN-14 | API-14 y API-15 con 401 para token vencido o cuenta inexistente y 403 para cuenta inactiva |
| RN-15 | Token de módulo (rol MODULO) pedido en API-17; seguridad alternativa en API-03 y API-05; API-16 exclusiva de módulos |

La definición de esquemas permite validar la estructura. Las reglas de estado, pertenencia y persistencia necesitan las pruebas de ejecución propuestas en `VERIFICACION.md`.

## Referencias técnicas complementarias

La sintaxis y las reglas de OpenAPI se contrastaron con la [especificación oficial 3.0.3](https://spec.openapis.org/oas/v3.0.3). La declaración de seguridad sigue [Swagger Bearer Authentication](https://swagger.io/doc/specification/v3_0/authentication/bearer-authentication/). Los tipos de claims y precauciones de validación se contrastaron con [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html) y [RFC 8725](https://www.rfc-editor.org/rfc/rfc8725.html). Estas fuentes aclaran estándares técnicos; no agregan funcionalidades al alcance académico.
