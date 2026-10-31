# deliverygo-usuarios-auth

Módulo **Usuarios y Autenticación** del TPO DeliveryGo (Aplicaciones Interactivas, UADE, 2C 2026). Grupo 02, Equipo 1 del ecosistema.

Centraliza la identidad de los usuarios de DeliveryGo: registro, inicio de sesión con JWT, perfil, rol y direcciones de entrega. Los demás módulos referencian al usuario por su id y consumen este servicio por API REST; ninguno accede a su base de datos.

## Estado

**Etapa 2: backend de los cinco CU CORE implementado y verificado.** El servicio expone las nueve operaciones del contrato, con JWT, validación en la frontera, formato de error común y correlación. Se comprueba con 47 pruebas automáticas de integración (incluidas las de concurrencia de email y de dirección predeterminada); ver [VERIFICACION.md](docs/openapi/VERIFICACION.md).

La entrega Swagger está en [`docs/openapi/`](docs/openapi/README.md): [YAML completo](docs/openapi/deliverygo-usuarios.yaml), tres vistas por área, trazabilidad y validación reproducible. Quedan pendientes de acuerdo con el ecosistema (no implementados) la firma asimétrica del JWT, la credencial propia de los módulos consumidores y los CU-06 a CU-10; ver [ACUERDOS_Y_BRECHAS.md](docs/openapi/ACUERDOS_Y_BRECHAS.md). Los PDF y diagramas anteriores se conservan en [`docs/`](docs/).

Paquete para entregar: [G02 Usuarios Swagger — 30/10/2026](entregables/G02_Usuarios_Swagger_2026-10-30.zip).

## Tecnologías

- Java 25 y Maven
- Spring Boot (Web, Data JPA, Validation, Security)
- JWT para la sesión
- H2 durante el desarrollo; base relacional propia del módulo
- OpenAPI / Swagger UI para documentar los contratos

## Casos de uso CORE

| ID | Caso de uso |
|---|---|
| CU-01 | Registrar usuario validando email único |
| CU-02 | Iniciar sesión y generar JWT |
| CU-03 | Consultar perfil de usuario |
| CU-04 | Modificar datos del perfil |
| CU-05 | Administrar direcciones: alta, modificación, baja y selección de predeterminada |

## Endpoints

Base URL: `/api/v1`. Todos, salvo registro y login, requieren `Authorization: Bearer <token>` y que el token pertenezca al usuario de la ruta.

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

Los errores siguen el formato normalizado del ecosistema: `timestamp`, `status`, `code`, `message`, `path`, `correlationId` (y `details` en `VALIDATION_ERROR`). Cada respuesta devuelve la cabecera `X-Correlation-Id`.

## Integraciones

| ID | Módulo | Dirección | Estado |
|---|---|---|---|
| INT-01 | Notificaciones (bienvenida) | Saliente | Mock declarado hasta contar con el contrato real |
| INT-02 | Portal Web | Entrante | Real |
| INT-03 | Pedidos y Logística | Entrante | Real |
| INT-04 | Catálogo | Entrante | Real |
| INT-05 | Notificaciones, Pagos, Opiniones y Analítica | Entrante | Real |

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
mvn test                 # 47 pruebas automáticas
mvn spring-boot:run      # levanta el servicio en http://localhost:8089
```

Configuración por variables de entorno (todas opcionales en desarrollo):

| Variable | Uso | Valor por defecto |
|---|---|---|
| `JWT_SECRET` | Secreto de firma del JWT, mínimo 32 caracteres | Valor de desarrollo local; **no usarlo en ambientes compartidos** |
| `JWT_EXPIRATION_MINUTES` | Vigencia del token | `60` |
| `CORS_ALLOWED_ORIGINS` | Orígenes del Portal, separados por coma | `http://localhost:5173,http://localhost:5174` |

`src/main/resources/application.properties.example` documenta el resto de la configuración. No se deben subir secretos reales al repositorio.

- Swagger UI: `http://localhost:8089/swagger-ui.html` (JSON en `/v3/api-docs`).
- Consola H2 de desarrollo: `http://localhost:8089/h2-console` (URL `jdbc:h2:mem:deliverygo-usuarios`, usuario `sa`, sin contraseña). La base se vacía al reiniciar; al arrancar se crean los roles CLIENTE, COMERCIO, REPARTIDOR y ADMIN.
- INT-01 (bienvenida de Notificaciones) está resuelta con un mock declarado que sólo escribe en el log; su falla nunca revierte el registro.
