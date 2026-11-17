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

Para regenerarlos desde la raíz del repositorio, con Java y PlantUML instalado:

```powershell
java -jar C:/herramientas/plantuml.jar -charset UTF-8 -tpng "doc/Diagramas/*.puml"
```

Se verificaron con PlantUML 1.2025.2. No se necesita enviar las fuentes a un servidor de diagramas. Revisar las imágenes después de modificar una fuente.

Los diagramas usan los nombres actuales (`JwtService`, `JwtAuthenticationFilter`, `TokenRecuperacion`), 403 para usuario inactivo, permisos distintos entre persona y módulo, datos de perfil sin contraseña y baja lógica de direcciones. Registro notifica después del commit; recuperación también confirma el token antes de notificar. Direcciones y recuperación bloquean la fila del usuario dentro de su transacción para mantener las reglas ante solicitudes simultáneas.

Las flechas de contexto muestran contratos. El estado comprobado de cada integración, incluido el parche necesario de Notificaciones, se encuentra en [la guía de integración](../integracion/README.md).
