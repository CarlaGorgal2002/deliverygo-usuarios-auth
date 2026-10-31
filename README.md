# deliverygo-usuarios-auth

Módulo **Usuarios y Autenticación** del TPO DeliveryGo (Aplicaciones Interactivas, UADE, 2C 2026). Grupo 02, Equipo 1 del ecosistema.

Centraliza la identidad de los usuarios de DeliveryGo: registro, inicio de sesión con JWT, perfil, rol y direcciones de entrega. Los demás módulos referencian al usuario por su id y consumen este servicio por API REST; ninguno accede a su base de datos.

## Estado

**Backend de los diez casos de uso implementado y verificado.** El servicio expone las dieciséis operaciones del contrato (los cinco CU CORE y los cinco de evolución), con JWT, API Key para módulos, validación en la frontera, formato de error común y correlación. Se comprueba con 82 pruebas automáticas de integración, incluidas las de concurrencia; ver [VERIFICACION.md](docs/openapi/VERIFICACION.md).

La entrega Swagger está en [`docs/openapi/`](docs/openapi/README.md): [YAML completo](docs/openapi/deliverygo-usuarios.yaml), tres vistas por área, el [diseño de CU-06 a CU-10](docs/openapi/DISENO_CU_EVOLUCION.md), trazabilidad y validación reproducible. Quedan pendientes de acuerdo con el ecosistema (no implementados) la firma asimétrica del JWT, la entrega y rotación de las API Keys de los módulos y el contrato real de Notificaciones, hoy un mock declarado; ver [ACUERDOS_Y_BRECHAS.md](docs/openapi/ACUERDOS_Y_BRECHAS.md). Los PDF y diagramas anteriores se conservan en [`docs/`](docs/).

Paquete para entregar: [G02 Usuarios Swagger — 30/10/2026](entregables/G02_Usuarios_Swagger_2026-10-30.zip).

## Tecnologías

- Java 25 y Maven
- Spring Boot (Web, Data JPA, Validation, Security)
- JWT para la sesión
- H2 durante el desarrollo; base relacional propia del módulo
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
| INT-01 | Notificaciones (bienvenida) | Saliente | Mock declarado hasta contar con el contrato real |
| INT-06 | Notificaciones (recuperación de contraseña) | Saliente | Mock declarado hasta contar con el contrato real |
| INT-02 | Portal Web | Entrante | Real |
| INT-03 | Pedidos y Logística | Entrante | Real, con API Key de módulo |
| INT-04 | Catálogo | Entrante | Real, con API Key de módulo |
| INT-05 | Notificaciones, Pagos, Opiniones y Analítica | Entrante | Real, con API Key de módulo |

## Estructura del repositorio

```
docs/                     Documento de entrega y diagramas (fuentes .puml e imágenes)
docs/openapi/             Contrato OpenAPI, vistas, trazabilidad y verificación
entregables/              Paquete Swagger para entregar
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

Requisitos: JDK 25 y Maven (ver el instructivo de la cátedra).

```bash
mvn test                 # 82 pruebas automáticas
mvn spring-boot:run      # levanta el servicio en http://localhost:8089
```

Configuración por variables de entorno (todas opcionales en desarrollo):

| Variable | Uso | Valor por defecto |
|---|---|---|
| `JWT_SECRET` | Secreto de firma del JWT, mínimo 32 caracteres | Valor de desarrollo local; **no usarlo en ambientes compartidos** |
| `JWT_EXPIRATION_MINUTES` | Vigencia del token | `60` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del Portal, separados por coma | `http://localhost:5173,http://localhost:5174` |
| `ADMIN_EMAIL` y `ADMIN_PASSWORD` | Primer administrador (CU-06 y CU-07); se definen juntos, la contraseña tiene entre 8 caracteres y 72 bytes | Sin ellos no se crea ningún administrador |
| `MODULE_KEY_PEDIDOS`, `MODULE_KEY_LOGISTICA`, `MODULE_KEY_CATALOGO`, `MODULE_KEY_NOTIFICACIONES`, `MODULE_KEY_PAGOS`, `MODULE_KEY_OPINIONES_ANALITICA` | API Key de cada módulo consumidor, de al menos 16 caracteres (CU-10) | Vacías: el módulo no tiene acceso |
| `RECOVERY_TOKEN_MINUTES` | Vigencia del token de recuperación de contraseña | `30` |
| `MOCK_LOG_RECOVERY_TOKEN` | Sólo para demostrar CU-08 en local: el mock de Notificaciones escribe el token en el log | `false`; no activar en ambientes compartidos |

`src/main/resources/application.properties.example` documenta el resto de la configuración. No se deben subir secretos reales al repositorio.

- Swagger UI: `http://localhost:8089/swagger-ui.html` (JSON en `/v3/api-docs`).
- Consola H2 de desarrollo: `http://localhost:8089/h2-console` (URL `jdbc:h2:mem:deliverygo-usuarios`, usuario `sa`, sin contraseña). La base se vacía al reiniciar; al arrancar se crean los roles CLIENTE, COMERCIO, REPARTIDOR y ADMIN.
- INT-01 (bienvenida) e INT-06 (recuperación de contraseña) con Notificaciones están resueltas con un mock declarado que sólo escribe en el log; su falla nunca revierte el registro ni cambia la respuesta.
- Para probar la administración y los módulos en local, definir antes de arrancar, por ejemplo: `ADMIN_EMAIL=admin@deliverygo.test`, `ADMIN_PASSWORD=<una contraseña propia>` y `MODULE_KEY_PEDIDOS=<una clave de 16 caracteres o más>`. En Swagger UI, **Authorize** acepta el JWT (`bearerAuth`) y la API Key (`moduleApiKey`). Las claves y contraseñas no se suben al repositorio.
