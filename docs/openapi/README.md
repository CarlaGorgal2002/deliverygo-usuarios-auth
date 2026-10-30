# Entrega Swagger del Grupo 02

Contrato de **Usuarios y Autenticación** de DeliveryGo, preparado el **30 de octubre de 2026**, versión **1.0.0**, en OpenAPI 3.0.3 y YAML. Incluye las nueve operaciones de los cinco casos de uso CORE de la primera entrega, sus DTO, ejemplos, validaciones, seguridad y respuestas de error.

**Archivo principal para entregar: [deliverygo-usuarios.yaml](deliverygo-usuarios.yaml).** Es autónomo: todas sus referencias están dentro del mismo archivo. Corresponde al Grupo 02 académico, denominado Equipo 1 del ecosistema en la guía funcional.

Este es el **contrato objetivo** basado en la primera entrega. El backend del repositorio es parcial y todavía difiere del contrato. Validar estos archivos no demuestra que el servidor ya implemente las nueve operaciones. El detalle está en [ACUERDOS_Y_BRECHAS.md](ACUERDOS_Y_BRECHAS.md).

## Archivos de la entrega

| Archivo | Contenido |
|---|---|
| `deliverygo-usuarios.yaml` | Contrato completo y única fuente editable; 9 operaciones |
| `vistas/autenticacion.yaml` | Vista autónoma de registro y login; 2 operaciones |
| `vistas/usuarios.yaml` | Vista autónoma de consulta y modificación de perfil; 2 operaciones |
| `vistas/direcciones.yaml` | Vista autónoma de direcciones; 5 operaciones |
| `TRAZABILIDAD_Y_FUENTES.md` | Correspondencia con API, CU, RN, SRV, DTO y los 23 adjuntos |
| `ACUERDOS_Y_BRECHAS.md` | Decisiones de contrato, integración JWT y diferencias con Java |
| `VERIFICACION.md` | Validación ejecutada y escenarios HTTP pendientes de implementación |
| `exportar_vistas.py` | Regenera las vistas a partir del principal |
| `validar_contratos.py` | Valida especificaciones, ejemplos y restricciones del contrato |

Las vistas son formas de compartir partes del mismo servicio, no tres microservicios adicionales. No se deben editar independientemente ni importar las cuatro especificaciones como si fueran cuatro APIs distintas. Si el profesor pide un único YAML por módulo, entregar solamente el principal; si solicita separación por área, también están disponibles las tres vistas.

## Cómo abrir los YAML

1. Importar `deliverygo-usuarios.yaml` en Swagger Editor o en una herramienta que soporte OpenAPI 3.0.3. También puede importarse en Postman como definición OpenAPI.
2. Revisar las secciones **Autenticación**, **Usuarios** y **Direcciones**. El contrato puede leerse sin ejecutar Java ni Maven.
3. Para ejecutar solicitudes, seleccionar el servidor correcto. El puerto local **8089** coincide con `application.properties`; la URL de integración es configurable y todavía no identifica un despliegue real.
4. Cuando el backend cumpla el contrato, registrar un usuario e iniciar sesión usando `email` y `password`. Copiar el `token` de la respuesta real y pegarlo en **Authorize**, sin escribir el prefijo `Bearer`.
5. Usar el `usuarioId` real en las rutas del perfil y direcciones. El token incluido en los ejemplos tiene una firma ficticia y no permite iniciar sesión.

Swagger UI permite explorar la API, mientras que OpenAPI es la definición formal de sus operaciones. La autenticación HTTP Bearer se declara con `type: http`, `scheme: bearer`; registro y login la desactivan mediante `security: []`. Referencias técnicas: [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3) y [Swagger Bearer Authentication](https://swagger.io/docs/specification/v3_0/authentication/bearer-authentication/).

## Operaciones incluidas

Todas las rutas comienzan con `/api/v1`. Sólo registro y login son públicos. Las demás exigen JWT y autorización sobre el usuario de la ruta.

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

Los dos `PUT` reciben el conjunto completo de campos modificables. La baja de direcciones es lógica. Elegir una predeterminada debe desmarcar la anterior de forma atómica; eliminarla deja al usuario sin predeterminada. Cada operación contiene su trazabilidad en `x-trazabilidad`.

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

Adaptar el backend al contrato antes de usarlo en la integración real. En particular: completar direcciones; corregir login y errores; aplicar validación en la frontera; distinguir titular y módulo consumidor; coordinar claves y claims del JWT. Después, ejecutar los escenarios de [VERIFICACION.md](VERIFICACION.md) y comparar la documentación generada por Springdoc con este YAML.

No debe reemplazarse este archivo por una exportación automática del backend parcial: se perderían operaciones, ejemplos y restricciones ya diseñadas. Tampoco basta con configurar Swagger UI para mostrarlo si Java sigue devolviendo otro JSON.
