# Entrega Swagger del Grupo 02

Contrato de **Usuarios y Autenticación** de DeliveryGo, preparado el **30 de octubre de 2026**, versión **1.1.0**, en OpenAPI 3.0.3 y YAML. Incluye las dieciséis operaciones de los diez casos de uso (los cinco CORE, CU-01 a CU-05, y los cinco de evolución, CU-06 a CU-10), sus DTO, ejemplos, validaciones, seguridad y respuestas de error.

**Archivo principal para entregar: [deliverygo-usuarios.yaml](deliverygo-usuarios.yaml).** Es autónomo: todas sus referencias están dentro del mismo archivo. Corresponde al Grupo 02 académico, denominado Equipo 1 del ecosistema en la guía funcional.

Este contrato parte del diseño de la primera entrega y lo completa con CU-06 a CU-10 ([DISENO_CU_EVOLUCION.md](DISENO_CU_EVOLUCION.md)). El backend del repositorio implementa las dieciséis operaciones y la Swagger generada por Springdoc coincide con él (operaciones, códigos de respuesta y seguridad). La validación del YAML (`validar_contratos.py`) comprueba el documento; el comportamiento del servidor lo comprueban las pruebas automáticas descritas en [VERIFICACION.md](VERIFICACION.md). Lo que sigue pendiente de acuerdo externo está en [ACUERDOS_Y_BRECHAS.md](ACUERDOS_Y_BRECHAS.md).

## Archivos de la entrega

| Archivo | Contenido |
|---|---|
| `deliverygo-usuarios.yaml` | Contrato completo y única fuente editable; 16 operaciones |
| `vistas/autenticacion.yaml` | Vista autónoma de registro, login, recuperación de contraseña y renovación y validación de sesión; 6 operaciones |
| `vistas/usuarios.yaml` | Vista autónoma de perfil, estado, rol y consulta para módulos; 5 operaciones |
| `vistas/direcciones.yaml` | Vista autónoma de direcciones; 5 operaciones |
| `DISENO_CU_EVOLUCION.md` | Diseño de CU-06 a CU-10: reglas RN-11 a RN-15, entidad ENT-04, decisiones DM-13 a DM-17, fichas, secuencias y matriz de trazabilidad |
| `TRAZABILIDAD_Y_FUENTES.md` | Correspondencia con API, CU, RN, SRV, DTO y los 23 adjuntos |
| `ACUERDOS_Y_BRECHAS.md` | Decisiones de contrato, integración JWT, estado de la implementación y pendientes |
| `VERIFICACION.md` | Validación del contrato, pruebas automáticas del backend y escenarios aún por acordar |
| `exportar_vistas.py` | Regenera las vistas a partir del principal |
| `validar_contratos.py` | Valida especificaciones, ejemplos y restricciones del contrato |

Las vistas son formas de compartir partes del mismo servicio, no tres microservicios adicionales. No se deben editar independientemente ni importar las cuatro especificaciones como si fueran cuatro APIs distintas. Si el profesor pide un único YAML por módulo, entregar solamente el principal; si solicita separación por área, también están disponibles las tres vistas.

## Cómo abrir los YAML

1. Importar `deliverygo-usuarios.yaml` en Swagger Editor o en una herramienta que soporte OpenAPI 3.0.3. También puede importarse en Postman como definición OpenAPI.
2. Revisar las secciones **Autenticación**, **Usuarios** y **Direcciones**. El contrato puede leerse sin ejecutar Java ni Maven.
3. Para ejecutar solicitudes, seleccionar el servidor correcto. El puerto local **8089** coincide con `application.properties`; la URL de integración es configurable y todavía no identifica un despliegue real. Con `mvn spring-boot:run`, la Swagger UI del propio servicio está en `http://localhost:8089/swagger-ui.html`.
4. Registrar un usuario e iniciar sesión usando `email` y `password`. Copiar el `token` de la respuesta real y pegarlo en **Authorize**, sin escribir el prefijo `Bearer`.
5. Usar el `usuarioId` real en las rutas del perfil y direcciones. El token incluido en los ejemplos tiene una firma ficticia y no permite iniciar sesión.

Para probar los casos de evolución en local, definir antes de arrancar `ADMIN_EMAIL` y `ADMIN_PASSWORD` (primer administrador), `MODULE_KEY_PEDIDOS` y las demás `MODULE_KEY_*` (claves de módulo, de al menos 16 caracteres) y, para ver el token de recuperación sin Notificaciones real, `MOCK_LOG_RECOVERY_TOKEN=true`. En **Authorize** hay dos esquemas: `bearerAuth` (JWT) y `moduleApiKey` (cabecera `X-Api-Key`).

Swagger UI permite explorar la API, mientras que OpenAPI es la definición formal de sus operaciones. La autenticación HTTP Bearer se declara con `type: http`, `scheme: bearer`; las cuatro operaciones públicas (registro, login y las dos de recuperación de contraseña) la desactivan mediante `security: []`; API-03 y API-05 admiten Bearer o API Key de módulo, y API-16 sólo la API Key. Referencias técnicas: [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3) y [Swagger Bearer Authentication](https://swagger.io/docs/specification/v3_0/authentication/bearer-authentication/).

## Operaciones incluidas

Todas las rutas comienzan con `/api/v1`. Son públicos el registro, el login y la recuperación de contraseña. API-14, API-15 y las demás exigen JWT; las escrituras sobre un perfil o sus direcciones exigen además ser el titular; la administración (API-10 y API-11) exige ser ADMIN. Los módulos consumidores, con su API Key, sólo leen (API-03, API-05 para Pedidos, y API-16).

| API | Método y ruta | Éxito | Caso de uso |
|---|---|---|---|
| API-01 | `POST /auth/register` | 201 + usuario | CU-01 |
| API-02 | `POST /auth/login` | 200 + sesión | CU-02 |
| API-03 | `GET /users/{id}` | 200 + usuario | CU-03 |
| API-04 | `PUT /users/{id}` | 200 + usuario actualizado | CU-04 |
| API-05 | `GET /users/{id}/addresses` | 200 + lista, incluida `[]` | CU-05 |
| API-06 | `POST /users/{id}/addresses` | 201 + dirección | CU-05 |
| API-07 | `PUT /users/{id}/addresses/{addressId}` | 200 + dirección actualizada | CU-05 |
| API-08 | `DELETE /users/{id}/addresses/{addressId}` | 204 sin cuerpo | CU-05 |
| API-09 | `PATCH /users/{id}/addresses/{addressId}/default` | 200 + dirección seleccionada | CU-05 |
| API-10 | `PUT /users/{id}/status` | 200 + usuario | CU-06 |
| API-11 | `PUT /users/{id}/role` | 200 + usuario | CU-07 |
| API-12 | `POST /auth/password/forgot` | 202 sin cuerpo | CU-08 |
| API-13 | `POST /auth/password/reset` | 204 sin cuerpo | CU-08 |
| API-14 | `POST /auth/refresh` | 200 + sesión | CU-09 |
| API-15 | `GET /auth/session` | 200 + rol y estado vigentes | CU-09 |
| API-16 | `GET /users/{id}/access` | 200 + estado y rol | CU-10 |

Los dos `PUT` reciben el conjunto completo de campos modificables. La baja de direcciones es lógica. Elegir una predeterminada debe desmarcar la anterior de forma atómica; eliminarla deja al usuario sin predeterminada. Cada operación contiene su trazabilidad en `x-trazabilidad`. Las operaciones de administración, recuperación de contraseña y sesión se explican en [DISENO_CU_EVOLUCION.md](DISENO_CU_EVOLUCION.md).

## Validación reproducible

Desde la raíz del repositorio, con Python 3.10 o posterior:

```powershell
python -m venv target/openapi-venv
.\target\openapi-venv\Scripts\python.exe -m pip install -r docs/openapi/requirements.txt
.\target\openapi-venv\Scripts\python.exe docs/openapi/validar_contratos.py
```

Si `python` apunta a otra instalación en Windows, se puede crear el entorno con `py -3.14 -m venv target/openapi-venv`. La verificación de esta entrega se ejecutó con Python 3.14.

Después de modificar el archivo principal:

```powershell
.\target\openapi-venv\Scripts\python.exe docs/openapi/exportar_vistas.py
.\target\openapi-venv\Scripts\python.exe docs/openapi/validar_contratos.py
```

Para verificar sólo la sincronización de las vistas sin reescribirlas:

```powershell
.\target\openapi-venv\Scripts\python.exe docs/openapi/exportar_vistas.py --check
```

## Próximo paso de implementación

Coordinar con los demás grupos lo que no depende de este módulo: algoritmo y distribución de claves del JWT (hoy HS256 con secreto compartido; la firma asimétrica es una propuesta), la entrega y rotación de las API Keys de cada módulo y el contrato real de Notificaciones (hoy un mock declarado para la bienvenida y la recuperación de contraseña). Los claims actuales son `sub` = id del usuario, `role`, `iat` y `exp`; los consumidores no deben leer `sub` como email, y el rol de un token emitido puede quedar desactualizado hasta que venza: el vigente se consulta con API-15 o API-16.

No debe reemplazarse este archivo por una exportación automática de Springdoc: se perderían ejemplos, trazabilidad y restricciones ya diseñadas. Ante un cambio de comportamiento, actualizar el YAML, regenerar las vistas y volver a correr `validar_contratos.py` y `mvn test`.
