# Backlog del módulo Usuarios y Autenticación

Grupo 02 · Equipo 1 del ecosistema DeliveryGo. Los diez casos de uso vienen de la guía funcional de la cátedra (cinco CORE y cinco de evolución). Estado al 17/11/2026.

| ID | Caso de uso | Prioridad | Estado | API | Secuencia | Prueba automática |
|---|---|---|---|---|---|---|
| CU-01 | Registrar usuario validando email único | CORE | Hecho | API-01 | [SEC-01](Diagramas/SEC-01.png) | `AuthApiTest`, `BienvenidaApiTest`, `IntegridadApiTest` |
| CU-02 | Iniciar sesión y generar JWT | CORE | Hecho | API-02 | [SEC-02](Diagramas/SEC-02.png) | `AuthApiTest`, `JwtServiceTest` |
| CU-03 | Consultar perfil de usuario | CORE | Hecho | API-03 | [SEC-03](Diagramas/SEC-03.png) | `PerfilApiTest`, `ModulosApiTest` |
| CU-04 | Modificar datos del perfil | CORE | Hecho | API-04 | [SEC-04](Diagramas/SEC-04.png) | `PerfilApiTest` |
| CU-05 | Administrar direcciones | CORE | Hecho | API-05 a API-09 | [SEC-05](Diagramas/SEC-05.png) | `DireccionesApiTest` |
| CU-06 | Desactivar o reactivar usuario | Evolución | Hecho | API-10 | [SEC-06](Diagramas/SEC-06.png) | `AdministracionApiTest` |
| CU-07 | Asignar o modificar rol | Evolución | Hecho | API-11 | [SEC-07](Diagramas/SEC-07.png) | `AdministracionApiTest` |
| CU-08 | Recuperar o restablecer contraseña | Evolución | Hecho | API-12, API-13 | [SEC-08](Diagramas/SEC-08.png) | `RecuperacionApiTest` |
| CU-09 | Renovar o validar sesión/token | Evolución | Hecho | API-14, API-15 | [SEC-09](Diagramas/SEC-09.png) | `SesionApiTest` |
| CU-10 | Consultar estado y rol para otro módulo | Evolución | Hecho | API-16, API-17 | [SEC-10](Diagramas/SEC-10.png) | `ModulosApiTest` |

## Integraciones

| ID | Integración | Estado | Responsable de coordinar |
|---|---|---|---|
| INT-01 | Usuarios → Notificaciones (bienvenida) | Cliente HTTP real probado con el parche de Notificaciones; mock declarado por defecto | Usuarios y Notificaciones |
| INT-06 | Usuarios → Notificaciones (recuperación de contraseña) | Igual que INT-01 | Usuarios y Notificaciones |
| INT-02 | Portal Web → Usuarios | API disponible; falta probar con el Portal final | Portal |
| INT-03 | Pedidos y Logística → Usuarios | JWT de módulo (API-17) y `X-Api-Key` implementados; los consumidores publicados todavía usan mocks | Pedidos y Logística |
| INT-04 | Catálogo → Usuarios | JWT de Usuarios validado en Catálogo | Catálogo |
| INT-05 | Notificaciones, Pagos, Opiniones y Analítica → Usuarios | Notificaciones probado; el resto requiere ajustes | Cada módulo |

## Pendiente (evolución futura, fuera de lo visto en la cátedra)

| Tema | Detalle |
|---|---|
| Firma asimétrica del JWT | Acordar algoritmo y distribución de claves con todos los equipos |
| Revocación de JWT y token de renovación persistido | Hoy el token vence solo (DM-17) |
| Cola o broker para Notificaciones | Hoy la llamada es sincrónica con timeout y no se reintenta |
| Migraciones con Flyway | Hoy `ddl-auto=update` |
| Límite de pedidos de recuperación de contraseña | Conviene resolverlo en un gateway |
