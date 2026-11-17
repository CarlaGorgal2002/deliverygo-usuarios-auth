# Diagramas del módulo Usuarios

Actualizados el **16/11/2026** contra el código de CU-01 a CU-10. Los PNG se generan desde los archivos PlantUML de esta carpeta. El PDF de primera entrega conserva el diseño histórico; para el estado implementado usar estas versiones.

| Diagrama | Imagen | Fuente |
|---|---|---|
| Contexto | [PNG](contexto.png) | [PlantUML](contexto.puml) |
| Arquitectura por capas | [PNG](arquitectura.png) | [PlantUML](arquitectura.puml) |
| Clases del dominio | [PNG](clases.png) | [PlantUML](clases.puml) |
| DER de PostgreSQL | [PNG](der.png) | [PlantUML](der.puml) |
| Seguridad e integración | [PNG](seguridad.png) | [PlantUML](seguridad.puml) |
| SEC-01 Registro | [PNG](SEC-01.png) | [PlantUML](SEC-01.puml) |
| SEC-02 Login | [PNG](SEC-02.png) | [PlantUML](SEC-02.puml) |
| SEC-03 Consulta de perfil | [PNG](SEC-03.png) | [PlantUML](SEC-03.puml) |
| SEC-04 Modificación de perfil | [PNG](SEC-04.png) | [PlantUML](SEC-04.puml) |
| SEC-05 Direcciones | [PNG](SEC-05.png) | [PlantUML](SEC-05.puml) |
| SEC-06 Estado de usuario | [PNG](SEC-06.png) | [PlantUML](SEC-06.puml) |
| SEC-07 Rol de usuario | [PNG](SEC-07.png) | [PlantUML](SEC-07.puml) |
| SEC-08 Recuperación de contraseña | [PNG](SEC-08.png) | [PlantUML](SEC-08.puml) |
| SEC-09 Validación y renovación | [PNG](SEC-09.png) | [PlantUML](SEC-09.puml) |
| SEC-10 Consulta desde módulos | [PNG](SEC-10.png) | [PlantUML](SEC-10.puml) |

Los diagramas se generan desde los archivos PlantUML de esta carpeta. Para regenerar las imágenes desde la raíz del repositorio, con Java y el `plantuml.jar` descargado de https://plantuml.com/download:

```powershell
java -jar C:/herramientas/plantuml.jar -charset UTF-8 -tpng "doc/Diagramas/*.puml"
```

Cada secuencia usa los participantes que pide la plantilla de la primera entrega (actor, filtro de seguridad cuando corresponde, Controller, Service, Repository, base de datos e integración), los mismos nombres de operaciones que el código (`registrarUsuario`, `consultarPerfil`, `crearDireccion`...) y los identificadores de trazabilidad (CU, RN, SRV, REP, API, DTO, INT, DM). Las respuestas de error vuelven siempre por el Controller con el formato normalizado `ErrorResponse` (DTO-08).

Si un PNG no coincide con su archivo `.puml` (por ejemplo, después de editar el diagrama sin regenerar las imágenes), manda el `.puml`.

Las flechas del diagrama de contexto muestran contratos. El estado comprobado de cada integración, incluido el parche necesario de Notificaciones, está en [la guía de integración](../integracion/README.md).
