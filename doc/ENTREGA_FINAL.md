# Entrega del módulo Usuarios y Autenticación

**Grupo 02 · Aplicaciones Interactivas · DeliveryGo · 16 de noviembre de 2026**

El módulo implementa los diez casos de uso y las dieciséis operaciones REST del contrato. Incluye persistencia PostgreSQL con Docker Compose, JWT, permisos por titular y rol, API Keys de módulos, validaciones y errores comunes. Esta entrega actualiza los diagramas, agrega las colecciones de Postman y corrige la consistencia de direcciones y recuperación ante solicitudes simultáneas.

El alcance propio está implementado y probado. La integración completa del ecosistema requiere además que los otros equipos incorporen las correcciones y acuerdos documentados. En particular, Notificaciones fue probado con un parche local incluido en esta entrega; no se afirma que ese parche ya esté publicado en su repositorio.

## Material para entregar y ejecutar

- [README principal](../README.md): requisitos, ejecución, configuración y datos de prueba.
- [Contrato OpenAPI completo](openapi/deliverygo-usuarios.yaml): 16 operaciones; vistas por área y validador en la misma carpeta.
- [Postman](postman/README.md): colección de Usuarios, environment local y colecciones entre servicios.
- [Diagramas](Diagramas/README.md): contexto, arquitectura, clases, DER, seguridad y diez secuencias; PNG y PlantUML.
- [Integraciones](integracion/README.md): comandos, versiones revisadas, parche de Notificaciones y bloqueos externos.
- [Verificación](openapi/VERIFICACION.md): resultados, cobertura, pasos de la demo y límites de las pruebas.
- [Decisiones](openapi/ACUERDOS_Y_BRECHAS.md) y [diseño de evolución](openapi/DISENO_CU_EVOLUCION.md): justificación y trazabilidad.

El PDF de primera entrega y el ZIP Swagger del 30/10 se conservan como antecedentes. No contienen los cambios de cierre y no reemplazan esta guía.

## Trazabilidad de los diez casos

| CU | API | Servicio principal | Secuencia | Prueba |
|---|---|---|---|---|
| CU-01 Registro | API-01 | UsuarioService, SRV-01 | [SEC-01](Diagramas/SEC-01.png) | AuthApiTest, BienvenidaApiTest |
| CU-02 Login | API-02 | AuthService, SRV-02 | [SEC-02](Diagramas/SEC-02.png) | AuthApiTest, JwtServiceTest |
| CU-03 Perfil | API-03 | UsuarioService, SRV-03 | [SEC-03](Diagramas/SEC-03.png) | PerfilApiTest, ModulosApiTest |
| CU-04 Modificar perfil | API-04 | UsuarioService, SRV-04 | [SEC-04](Diagramas/SEC-04.png) | PerfilApiTest |
| CU-05 Direcciones | API-05 a API-09 | DireccionService, SRV-05 a SRV-09 | [SEC-05](Diagramas/SEC-05.png) | DireccionesApiTest, ConcurrenciaApiTest |
| CU-06 Estado | API-10 | AdministracionService, SRV-10 | [SEC-06](Diagramas/SEC-06.png) | AdministracionApiTest |
| CU-07 Rol | API-11 | AdministracionService, SRV-11 | [SEC-07](Diagramas/SEC-07.png) | AdministracionApiTest |
| CU-08 Recuperación | API-12, API-13 | AuthService / RecuperacionService, SRV-12, SRV-13 | [SEC-08](Diagramas/SEC-08.png) | RecuperacionApiTest, ConcurrenciaApiTest |
| CU-09 Sesión | API-14, API-15 | AuthService, SRV-14, SRV-15 | [SEC-09](Diagramas/SEC-09.png) | SesionApiTest |
| CU-10 Consulta de módulo | API-16 | UsuarioService, SRV-16 | [SEC-10](Diagramas/SEC-10.png) | ModulosApiTest |

Las matrices de RN, ENT, REL, REP, DTO e INT se conservan en [trazabilidad y fuentes](openapi/TRAZABILIDAD_Y_FUENTES.md) y en [el diseño de CU-06 a CU-10](openapi/DISENO_CU_EVOLUCION.md). El dominio actual contiene Usuario, Rol, Dirección y TokenRecuperacion; los consumidores conservan referencias por ID y no acceden a esta base.

## Resultados de cierre

| Comprobación | Resultado |
|---|---|
| Maven, backend de Usuarios | 95 pruebas; 0 fallas, errores u omisiones |
| Postman, Usuarios sobre PostgreSQL | 45 solicitudes; 155 comprobaciones; 0 fallas |
| Postman, Notificaciones con parche local | 12 solicitudes; 40 comprobaciones; 0 fallas |
| Postman, Catálogo sin modificar su código | 7 solicitudes; 8 comprobaciones; 0 fallas |
| Concurrencia por HTTP sobre PostgreSQL | Seis altas dejan una sola predeterminada; seis restablecimientos del mismo token producen un 204 y cinco 400 |
| Suite de Notificaciones con el parche | 103 pruebas; 0 fallas, errores u omisiones |
| OpenAPI | Cuatro YAML válidos, 70 escenarios de esquema y vistas sincronizadas |
| YAML frente a Springdoc en ejecución | Coinciden las 16 operaciones, sus respuestas y seguridad |
| Diagramas | 15 fuentes renderizadas y revisadas visualmente |

Los resúmenes de Postman están en [evidencia](postman/evidencia/). Las pruebas de contratos no sustituyen a las pruebas HTTP, y las llamadas con una API Key desde Postman no se presentan como clientes implementados por otros equipos.

La [evidencia de concurrencia en PostgreSQL](postman/evidencia/concurrencia-postgres-2026-11-16.json) corresponde a llamadas HTTP simultáneas sobre los servicios reales, con un usuario ficticio nuevo. Los tres escenarios automatizados reproducibles se encuentran en `ConcurrenciaApiTest` y forman parte de `mvn test` sobre H2.

## Explicación para la defensa

El controller valida el DTO y delega. El service aplica las reglas y delimita la transacción. Los repositories leen y escriben entidades JPA; los mappers producen DTO sin datos sensibles. El registro crea CLIENTE y guarda BCrypt, no la contraseña. JWT lleva identidad, rol y vencimiento; los permisos de administración se vuelven a verificar contra la base.

Una dirección pertenece a un usuario y se elimina de forma lógica. La selección de predeterminada desmarca la anterior en la misma transacción. Si dos solicitudes intentan modificar direcciones del mismo usuario al mismo tiempo, una espera a que termine la otra mediante el bloqueo de su fila. Esto mantiene una sola predeterminada.

El token de recuperación es distinto del JWT: es aleatorio, vence y sólo se conserva su hash. Las solicitudes de recuperación y restablecimiento también bloquean al usuario antes de operar sobre sus tokens. Así un pedido nuevo invalida los anteriores y dos restablecimientos no pueden consumir el mismo token. No se agregaron brokers, gateways, OAuth ni infraestructura adicional.

Notificaciones se solicita después de persistir y en segundo plano. Un servicio lento o caído no revierte el registro ni cambia el 202 de recuperación. Esa disponibilidad tiene un límite: no hay una cola persistente con reintento garantizado; si falla el envío queda registrado y el usuario puede solicitar otra recuperación.

## Trabajo de coordinación que sigue abierto

El equipo de Notificaciones debe incorporar el parche o una solución equivalente. Pedidos y Pagos deben adaptar las referencias de usuarios/direcciones a Long y sus credenciales; Logística debe reemplazar su mock; Analítica debe resolver la selección de implementaciones y probar su cliente; falta el repositorio del Portal final para verificarlo. El detalle, commits revisados y acciones concretas están en [Integración](integracion/README.md).

La firma asimétrica mencionada en la teoría requiere un acuerdo de algoritmo y distribución de claves con los consumidores. El código actual usa HS256, coherente con los laboratorios y los módulos probados. Estos puntos no se marcan como completos ni como aprobados por la cátedra.
