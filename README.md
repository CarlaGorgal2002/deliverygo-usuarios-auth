# deliverygo-usuarios-auth

Módulo **Usuarios y Autenticación** del TPO DeliveryGo (Aplicaciones Interactivas, UADE, 2C 2026). Grupo 02, Equipo 1 del ecosistema.

Centraliza la identidad de los usuarios de DeliveryGo: registro, inicio de sesión con JWT, perfil, rol y direcciones de entrega. Los demás módulos referencian al usuario por su id y consumen este servicio por API REST; ninguno accede a su base de datos.

## Estado

**Backend de los diez casos de uso implementado y verificado.** El servicio expone las dieciséis operaciones del contrato (los cinco CU CORE y los cinco de evolución), con JWT, API Key para módulos, validación en la frontera, formato de error común y correlación. Se comprueba con 95 pruebas automáticas (integración y unitarias); ver [VERIFICACION.md](doc/openapi/VERIFICACION.md).

La entrega Swagger está en [`doc/openapi/`](doc/openapi/README.md): [YAML completo](doc/openapi/deliverygo-usuarios.yaml), tres vistas por área, el [diseño de CU-06 a CU-10](doc/openapi/DISENO_CU_EVOLUCION.md), trazabilidad y validación reproducible. La integración HTTP de bienvenida y recuperación se probó con Notificaciones aplicando el parche incluido para ese módulo; Catálogo se probó con JWT emitidos por Usuarios. Los resultados y bloqueos de los demás consumidores están en [Integración](doc/integracion/README.md). La firma asimétrica y la distribución de credenciales siguen requiriendo acuerdo entre equipos. La [entrega actual](doc/ENTREGA_FINAL.md) reúne trazabilidad, evidencia y diagramas; el PDF de la primera entrega se conserva como antecedente.

Entrega actual: [cierre del módulo — 16/11/2026](doc/ENTREGA_FINAL.md). El [paquete Swagger del 30/10/2026](entregables/G02_Usuarios_Swagger_2026-10-30.zip) es histórico y no incluye los cambios de noviembre.

Las escrituras de direcciones y recuperación bloquean la fila del usuario durante su transacción: solicitudes simultáneas no dejan varias predeterminadas ni consumen dos veces el mismo token. Se verifica con tres pruebas de concurrencia.

## Tecnologías

- Java 25 y Maven
- Spring Boot (Web, Data JPA, Validation, Security)
- JWT para la sesión
- PostgreSQL 16 como base propia del módulo (levantada con Docker Compose); H2 en memoria sólo para las pruebas automáticas
- OpenAPI / Swagger UI para documentar los contratos

## Casos de uso

CU-01 a CU-05 son los CORE; CU-06 a CU-10, los de evolución.

| ID | Caso de uso |
|---|---|
| CU-01 | Registrar usuario validando email único |
| CU-02 | Iniciar sesión y generar JWT |
| CU-03 | Consultar perfil de usuario |
| CU-04 | Modificar datos del perfil |
| CU-05 | Administrar direcciones: alta, modificación, baja y selección de predeterminada |
| CU-06 | Desactivar o reactivar usuario (ADMIN) |
| CU-07 | Asignar o modificar rol (ADMIN) |
| CU-08 | Recuperar o restablecer contraseña |
| CU-09 | Renovar o validar sesión/token |
| CU-10 | Consultar estado y rol de un usuario para otro módulo |

## Endpoints

Base URL: `/api/v1`. Son públicos el registro, el login y la recuperación de contraseña. El resto requiere `Authorization: Bearer <token>`; sobre `/users/{id}` el token debe ser del usuario de la ruta, salvo la administración (ADMIN) y la lectura de los módulos consumidores, que se identifican con `X-Api-Key`.

| Método | Ruta | CU |
|---|---|---|
| POST | `/api/v1/auth/register` | CU-01 |
| POST | `/api/v1/auth/login` | CU-02 |
| GET | `/api/v1/users/{id}` | CU-03 |
| PUT | `/api/v1/users/{id}` | CU-04 |
| GET | `/api/v1/users/{id}/addresses` | CU-05 |
| POST | `/api/v1/users/{id}/addresses` | CU-05 |
| PUT | `/api/v1/users/{id}/addresses/{addressId}` | CU-05 |
| DELETE | `/api/v1/users/{id}/addresses/{addressId}` | CU-05 |
| PATCH | `/api/v1/users/{id}/addresses/{addressId}/default` | CU-05 |
| PUT | `/api/v1/users/{id}/status` | CU-06 |
| PUT | `/api/v1/users/{id}/role` | CU-07 |
| POST | `/api/v1/auth/password/forgot` | CU-08 |
| POST | `/api/v1/auth/password/reset` | CU-08 |
| POST | `/api/v1/auth/refresh` | CU-09 |
| GET | `/api/v1/auth/session` | CU-09 |
| GET | `/api/v1/users/{id}/access` | CU-10 |

Los errores siguen el formato normalizado del ecosistema: `timestamp`, `status`, `code`, `message`, `path`, `correlationId` (y `details` en `VALIDATION_ERROR`). Cada respuesta devuelve la cabecera `X-Correlation-Id`.

## Integraciones

| ID | Módulo | Dirección | Estado |
|---|---|---|---|
| INT-01 | Notificaciones (bienvenida) | Saliente | Cliente HTTP real (`NOTIFICACIONES_MODO=http`); mock declarado por defecto |
| INT-06 | Notificaciones (recuperación de contraseña) | Saliente | Cliente HTTP real (`NOTIFICACIONES_MODO=http`); mock declarado por defecto |
| INT-02 | Portal Web | Entrante | API disponible; prueba desde el Portal final pendiente |
| INT-03 | Pedidos y Logística | Entrante | API Key implementada; consumidores publicados todavía usan mocks de Usuarios |
| INT-04 | Catálogo | Entrante | JWT de Usuarios validado en Catálogo; API Key disponible para consultas |
| INT-05 | Notificaciones, Pagos, Opiniones y Analítica | Entrante | Notificaciones probado con el parche; otros consumidores requieren ajustes |

## Estructura del repositorio

```
doc/                     Documento de entrega y diagramas (fuentes .puml e imágenes)
doc/openapi/             Contrato OpenAPI, vistas, trazabilidad y verificación
doc/postman/             Colecciones, environment local y evidencia sin credenciales
doc/integracion/         Guía entre módulos, hallazgos y parche para Notificaciones
entregables/              Paquete Swagger histórico de primera entrega
src/main/java/ar/edu/uade/api/deliverygo/usuarios/
  controller/  service/  repository/  domain/  dto/
  mapper/  exception/  config/  security/  integration/
src/test/java/ar/edu/uade/api/deliverygo/usuarios/   Pruebas de integración y unitarias
```

## Flujo de trabajo con Git

- `main`: versiones estables y entregas.
- `develop`: integración del equipo.
- `feature/<tema>` y `fix/<tema>`: se integran a `develop` por Pull Request con revisión de otro integrante.

## Instalación y ejecución

Requisitos: JDK 25, Maven (ver el instructivo de la cátedra) y Docker para la base de datos.

```bash
docker compose up -d     # levanta PostgreSQL en localhost:5432 (los datos persisten en un volumen)
mvn spring-boot:run      # levanta el servicio en http://localhost:8089
mvn test                 # 95 pruebas automáticas; usan H2 en memoria, no necesitan Docker
```

`docker compose down` apaga la base sin perder los datos; `docker compose down -v` también los borra. Al arrancar, Hibernate crea las tablas que falten (`ddl-auto=update`) y la aplicación carga los roles y, si se configuró, el administrador inicial sólo si no existen. Cambiar `ADMIN_PASSWORD` no cambia la contraseña de una cuenta ya creada.

Configuración por variables de entorno (todas opcionales en desarrollo):

| Variable | Uso | Valor por defecto |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Ubicación de PostgreSQL | `localhost`, `5432`, `deliverygo_usuarios` |
| `DB_USERNAME` y `DB_PASSWORD` | Credenciales de PostgreSQL; `docker-compose.yml` usa las mismas variables para crear la base | `deliverygo` y `deliverygo` (sólo desarrollo local) |
| `JWT_SECRET` | Secreto de firma del JWT, mínimo 32 caracteres | Valor de desarrollo local; **no usarlo en ambientes compartidos** |
| `JWT_EXPIRATION_MINUTES` | Vigencia del token | `60` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del Portal, separados por coma | `http://localhost:5173,http://localhost:5174` |
| `ADMIN_EMAIL` y `ADMIN_PASSWORD` | Primer administrador (CU-06 y CU-07); se definen juntos, la contraseña tiene entre 8 caracteres y 72 bytes | Sin ellos no se crea ningún administrador |
| `MODULE_KEY_PEDIDOS`, `MODULE_KEY_LOGISTICA`, `MODULE_KEY_CATALOGO`, `MODULE_KEY_NOTIFICACIONES`, `MODULE_KEY_PAGOS`, `MODULE_KEY_OPINIONES_ANALITICA` | API Key de cada módulo consumidor, de al menos 16 caracteres (CU-10) | Vacías: el módulo no tiene acceso |
| `RECOVERY_TOKEN_MINUTES` | Vigencia del token de recuperación de contraseña | `30` |
| `MOCK_LOG_RECOVERY_TOKEN` | Sólo para demostrar CU-08 en local: el mock de Notificaciones escribe el token en el log | `false`; no activar en ambientes compartidos |
| `NOTIFICACIONES_MODO` | `mock` (sólo log) o `http` (API real de Notificaciones) | `mock` |
| `NOTIFICACIONES_URL` y `NOTIFICACIONES_TIMEOUT_MS` | URL base y tiempo de espera del cliente HTTP de Notificaciones. Si la URL apunta a este mismo servicio (mismo puerto en localhost), la app no arranca | `http://localhost:8089` y `2000` |
| `NOTIFICACIONES_PLANTILLA_BIENVENIDA` y `NOTIFICACIONES_PLANTILLA_RECUPERACION` | Nombres de las plantillas en Notificaciones | `USUARIO_BIENVENIDA_EMAIL` y `RECUPERACION_PASSWORD_EMAIL` |

`src/main/resources/application.properties.example` documenta el resto de la configuración. No se deben subir secretos reales al repositorio.

- Swagger UI: `http://localhost:8089/swagger-ui.html` (JSON en `/v3/api-docs`).
- Base de datos: `docker compose exec postgres psql -U deliverygo -d deliverygo_usuarios` abre una consola SQL. Al arrancar se crean los roles CLIENTE, COMERCIO, REPARTIDOR y ADMIN si no existen.
- INT-01 (bienvenida) e INT-06 (recuperación de contraseña) con Notificaciones usan por defecto un mock declarado que sólo escribe en el log; con `NOTIFICACIONES_MODO=http` llaman a la API real. Su falla nunca revierte el registro ni cambia la respuesta.
- Para probar la administración y los módulos en local, definir antes de arrancar, por ejemplo: `ADMIN_EMAIL=admin@deliverygo.test`, `ADMIN_PASSWORD=<una contraseña propia>` y `MODULE_KEY_PEDIDOS=<una clave de 16 caracteres o más>`. En Swagger UI, **Authorize** acepta el JWT (`bearerAuth`) y la API Key (`moduleApiKey`). Las claves y contraseñas no se suben al repositorio.

## Datos de prueba

Importar [la colección de Postman y el environment local](doc/postman/README.md). Los siguientes valores son públicos y exclusivos de desarrollo local; coinciden con `Local.postman_environment.json`.

En PowerShell, antes de iniciar Usuarios:

```powershell
$env:ADMIN_EMAIL = 'admin@deliverygo.test'
$env:ADMIN_PASSWORD = 'AdminDemo2026!'
$env:MODULE_KEY_PEDIDOS = 'pedidos-demo-local-2026'
$env:MODULE_KEY_CATALOGO = 'catalogo-demo-local-2026'
$env:MODULE_KEY_NOTIFICACIONES = 'notificaciones-demo-local-2026'
# Opcional, sólo para ver el token de recuperación en el log del mock local:
$env:MOCK_LOG_RECOVERY_TOKEN = 'true'
docker compose up -d
mvn spring-boot:run
```

La colección crea sus propios usuarios con emails únicos, contraseña `ClaveDemo2026!` y contraseña nueva `NuevaClaveDemo2026!`. No usa IDs fijos. Cubre validaciones, acceso cruzado, administración, direcciones, sesión y permisos de módulos. El restablecimiento exitoso se prueba con el token recibido o automáticamente mediante la colección de integración con Notificaciones.

### Usuarios y otros módulos juntos

Ver [la guía de integración](doc/integracion/README.md): incluye comandos PowerShell para Usuarios en **8080**, Notificaciones en **8089** y Catálogo en **8082**, con el mismo secreto JWT. También incluye el parche necesario para el commit revisado de Notificaciones y las incompatibilidades de los otros consumidores.

Las pruebas de Notificaciones usan HTTP real entre los servicios y envío de correo **simulado** del módulo externo. El modo por defecto de Usuarios continúa siendo `mock`; el modo HTTP se activa con `NOTIFICACIONES_MODO=http`. No hay que cambiar los endpoints ni las entidades de Usuarios para ejecutar la demo.
