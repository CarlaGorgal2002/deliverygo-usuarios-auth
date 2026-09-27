# deliverygo-usuarios-auth

Módulo **Usuarios y Autenticación** del TPO DeliveryGo (Aplicaciones Interactivas, UADE, 2C 2026). Grupo 02, Equipo 1 del ecosistema.

Centraliza la identidad de los usuarios de DeliveryGo: registro, inicio de sesión con JWT, perfil, rol y direcciones de entrega. Los demás módulos referencian al usuario por su id y consumen este servicio por API REST; ninguno accede a su base de datos.

## Estado

**Etapa 1: diseño y arquitectura.** El documento de la Primera Entrega está en [`docs/`](docs/). La implementación de los cinco casos de uso CORE corresponde a la Etapa 2.

## Integrantes

| Integrante | 
|---|---|
| Camacho Lucas | 
| Gorgal Carla Fátima | 
| Hanine Santiago | 
| Safadie Ezra | 

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

## Endpoints previstos

Base URL: `/api/v1`. Todos, salvo registro y login, requieren `Authorization: Bearer <token>`.

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

Los errores siguen el formato normalizado del ecosistema: `timestamp`, `status`, `code`, `message`, `path`, `correlationId`.

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
src/main/java/ar/edu/uade/api/deliverygo/usuarios/
  controller/  service/  repository/  domain/  dto/
  mapper/  exception/  config/  security/  integration/
src/test/
```

## Flujo de trabajo con Git

- `main`: versiones estables y entregas.
- `develop`: integración del equipo.
- `feature/<tema>` y `fix/<tema>`: se integran a `develop` por Pull Request con revisión de otro integrante.

## Instalación y ejecución

Se completará en la Etapa 2 junto con el código.
