# Trazabilidad y fuentes del contrato

Se inventariaron las **23 referencias adjuntas** y se comprobó su existencia. Corresponden a **19 archivos distintos por SHA-256**: algunas guías, plantillas y ejemplos fueron adjuntados desde más de una carpeta. Se extrajo texto de documentos y PDF; se inspeccionaron contratos y diagramas relevantes; los ZIP se revisaron como ejemplos de código/configuración sin ejecutar sus aplicaciones. Se identificaron 100 archivos únicos de código/configuración en los laboratorios, excluyendo dependencias, compilados y metadatos de macOS.

La transcripción contiene fragmentos repetidos y notas de audio no reconocido; C9 se identifica como parcial. Por eso las especificaciones de los documentos y los patrones comprobables del código tienen más peso que una frase aislada o una transcripción dudosa. La teoría también es una extracción con errores de OCR. No se atribuyen al profesor las precisiones nuevas DC-01–DC-07.

## Criterio para resolver diferencias

Se usan conjuntamente la consigna, las aclaraciones de clases/Teams, la guía funcional y el diseño específico del Grupo 02. Para los nombres y comportamientos ya desarrollados del módulo se conserva F22. Los laboratorios aportan ejemplos de implementación; no tienen prioridad sobre el contrato particular de Usuarios. Las ambigüedades y diferencias con el código actual se hacen explícitas en `ACUERDOS_Y_BRECHAS.md`.

## Matriz de operaciones

Las rutas de esta tabla tienen el prefijo `/api/v1`. DTO-08 se utiliza para los errores de todas las operaciones.

| API | Método y ruta | CU / SEC | SRV | DTO entrada → salida | RN aplicadas |
|---|---|---|---|---|---|
| API-01 | `POST /auth/register` | CU-01 / SEC-01 | SRV-01 | DTO-01 → DTO-02 | RN-01, RN-02, RN-03, RN-06 |
| API-02 | `POST /auth/login` | CU-02 / SEC-02 | SRV-02 | DTO-03 → DTO-04 | RN-01, RN-03, RN-04, RN-08, RN-09 |
| API-03 | `GET /users/{id}` | CU-03 / SEC-03 | SRV-03 | Sin cuerpo → DTO-02 | RN-03, RN-04, RN-05, RN-09 |
| API-04 | `PUT /users/{id}` | CU-04 / SEC-04 | SRV-04 | DTO-05 → DTO-02 | RN-01, RN-02, RN-03, RN-04, RN-05, RN-09, RN-10 |
| API-05 | `GET /users/{id}/addresses` | CU-05 / SEC-05 | SRV-09 | Sin cuerpo → lista DTO-07 | RN-04, RN-05, RN-09 |
| API-06 | `POST /users/{id}/addresses` | CU-05 / SEC-05 | SRV-05 | DTO-06 → DTO-07 | RN-01, RN-04, RN-05, RN-07, RN-09 |
| API-07 | `PUT /users/{id}/addresses/{addressId}` | CU-05 / SEC-05 | SRV-06 | DTO-09 → DTO-07 | RN-01, RN-04, RN-05, RN-09 |
| API-08 | `DELETE /users/{id}/addresses/{addressId}` | CU-05 / SEC-05 | SRV-07 | Sin cuerpo → sin cuerpo | RN-04, RN-05, RN-07, RN-09 |
| API-09 | `PATCH /users/{id}/addresses/{addressId}/default` | CU-05 / SEC-05 | SRV-08 | Sin cuerpo → DTO-07 | RN-04, RN-05, RN-07, RN-09 |

La matriz detalla reglas por operación: por eso RN-07 aparece en baja y selección, pero no se aplica como escritura en el listado ni en el PUT de dirección. La ficha general CU-05 conserva su conjunto de reglas RN-01, RN-04, RN-05, RN-07 y RN-09.

## Correspondencia con persistencia e integración

| Servicio | Repositorios del diseño | Entidades y relaciones | Integración |
|---|---|---|---|
| SRV-01 | REP-01, REP-04, REP-03 | ENT-01 Usuario, ENT-02 Rol, REL-02 | INT-01 bienvenida; INT-02 Portal |
| SRV-02 | REP-01 | ENT-01, ENT-02, REL-02 | INT-02; token para el ecosistema |
| SRV-03 | REP-02 | ENT-01, ENT-02, REL-02 | INT-02, INT-03, INT-04, INT-05 según autorización |
| SRV-04 | REP-02, REP-01, REP-03 | ENT-01; rol sólo de lectura | INT-02 |
| SRV-05 | REP-02, REP-08, REP-07 | ENT-01, ENT-03 Direccion, REL-01 | INT-02 |
| SRV-06 y SRV-07 | REP-02, REP-06, REP-07 | ENT-01, ENT-03, REL-01 | INT-02 |
| SRV-08 | REP-02, REP-06, REP-08, REP-07 | ENT-01, ENT-03, REL-01 | INT-02 |
| SRV-09 | REP-02, REP-05 | ENT-01, ENT-03, REL-01 | INT-02; Pedidos en INT-03, credencial propia pendiente |

No se exponen entidades JPA ni repositorios como contratos. El servicio es responsable de las reglas y transacciones; el controlador valida la entrada y adapta HTTP; la capa de seguridad verifica identidad y permisos. Cada módulo mantiene su base de datos propia.

## Reglas y evidencia contractual

| RN | Aplicación en YAML |
|---|---|
| RN-01 | Campos required, formatos, minLength, patrón no vacío, tipos y respuesta 400 con details |
| RN-02 | Email normalizado, 409 y descripción de unicidad concurrente en registro/modificación |
| RN-03 | password writeOnly; sin password/passwordHash en salidas; hash irreversible indicado |
| RN-04 | 403 USER_INACTIVE; verificación posterior a contraseña en login; distinción futura de consumidor de módulo |
| RN-05 | sub frente a id, autorización por titular, 403 y ocultación de direcciones ajenas con 404 |
| RN-06 | Registro sin rol/estado de entrada; respuesta CLIENTE/ACTIVO |
| RN-07 | predeterminada false por omisión, selección atómica, PUT sin ese atributo y baja que la desmarca |
| RN-08 | Un único error genérico para email inexistente/contraseña incorrecta |
| RN-09 | Bearer en operaciones protegidas, claims y vencimiento; 401 ante falta o invalidez |
| RN-10 | PUT de perfil limitado a nombre, apellido, email y telefono |

La definición de esquemas permite validar la estructura. Las reglas de estado, pertenencia, persistencia y concurrencia necesitan las pruebas de ejecución propuestas en `VERIFICACION.md`.

## Inventario de adjuntos

Las rutas se expresan relativas al Escritorio del usuario para identificar los originales sin copiarlos al repositorio. El archivo `fuentes-manifest.json` guarda tamaños y SHA-256 completos para comprobar identidad.

| ID | Archivo original relativo al Escritorio | Aporte y tratamiento |
|---|---|---|
| F01 | `Transcripción TODAS las clases de API.txt` | Clases C1–C13: capas, DTO, HTTP, Swagger, JWT e integración. C13 insiste en publicar Swagger, uniformar errores y responder 204 sin cuerpo; también recuerda los 10 CU de la entrega final. |
| F02 | `Publicaciones Teams API.docx` | Publicaciones Teams: distinción CORE/evolución, Java 25 y plantilla de error común publicada al final del documento. |
| F03 | `Teoría API.docx` | Teoría: páginas etiquetadas 4–12 (seguridad/OpenAPI), 21–28 (contrato, validaciones, emisor Usuarios y firma asimétrica), DTO, persistencia y laboratorio E2E. |
| F04 | `Archivos Teams API/DeliveryGo_Guia_Funcional_y_Hoja_de_Ruta_TPO_2026.docx` | Guía funcional, secciones 5 y 15: cinco CU CORE de Usuarios, evolución, integración y criterios de implementación. |
| F05 | `Archivos Teams API/Instructivo_Instalacion_Java25_Maven.md` | Java 25 y Maven como entorno del backend. Abrir y validar los YAML no requiere levantar Java. |
| F06 | `Archivos Teams API/Template_Guia_Primera_Entrega_TPO_DeliveryGo.docx.pdf` | Plantilla: trazabilidad bidireccional CU/RN/ENT/SEC/SRV/REP/API/DTO/INT, separación de responsabilidades y reglas justificadas. |
| F07 | `Archivos Teams API/Template_Guia_Primera_Entrega_TPO_DeliveryGo_2.0.pdf` | Misma plantilla y mismo hash que F06; el nombre 2.0 no introduce diferencias de contenido en estos adjuntos. |
| F08 | `Laboratorios/deliverygo-persistence 2-290926.zip` | Laboratorio del 29/09: controladores REST, DTO, manejo de errores, baja lógica, servicios transaccionales y repositorios. Las entidades de ejemplo no amplían el alcance de Usuarios. |
| F09 | `Laboratorios/App/backend-reference.zip` | Laboratorio de seguridad: OpenApiConfig, SecurityFilterChain, AuthController, JwtService, JWT Bearer y rol. LoginDto/TokenDto son contratos de la demo de Comercio. |
| F10 | `Laboratorios/App/frontend-react.zip` | Cliente React inicial: fetch, envío de Authorization, manejo de 401/403 y consumo de DELETE sin cuerpo. Sus rutas de Comercio no sustituyen /api/v1/users. |
| F11 | `Laboratorios/Clase 13-10-26/backend-reference_2.zip` | Copia exacta de F09: se verifica por hash; no es una versión backend distinta pese al sufijo _2. |
| F12 | `Laboratorios/Clase 13-10-26/DeliveryGo_React_Publico_Privado_2.zip` | React público/privado: token en memoria, decodificación para presentación y validación real en backend, origen CORS y separación de navegación/permisos. |
| F13 | `Laboratorios/Clase 13-10-26/descarga.svg` | Diagrama SVG: Grupo 2 Usuarios autentica y emite JWT; Portal transporta; los otros módulos validan JWT y rol. |
| F14 | `Laboratorios/Old/deliverygo_clase_2_v2.zip` | Laboratorio inicial de persistencia: JPA, repositorios, H2 y separación del modelo; antecedente histórico. |
| F15 | `Laboratorios/Old/deliverygo-persistence 2.zip` | Evolución del laboratorio: relaciones entre Usuario, Direccion, Perfil y Promocion; se conserva el diseño particular ENT-01/02/03 del equipo. |
| F16 | `Laboratorios/Old/deliverygo-persistence 2-150926.zip` | Laboratorio del 15/09: DTO, validación en servicios y uso de transacciones; antecedente del manejo posterior de errores. |
| F17 | `Laboratorios/Old/deliverygo-persistence.zip` | Laboratorio de persistencia inicial: entidades y JpaRepository; no define los DTO finales de Usuarios. |
| F18 | `Laboratorios/Old/DiagramaDERClases.drawio` | Diagrama drawio: relaciones de dominio, capas y protocolo. Referencia didáctica; no se copia el dominio completo de Comercio/Pedidos/Promociones. |
| F19 | `TPO/DeliveryGo_Guia_Funcional_y_Hoja_de_Ruta_TPO_2026.docx` | Copia exacta de la guía F04. |
| F20 | `TPO/Template_Guia_Primera_Entrega_TPO_DeliveryGo.docx.pdf` | Copia exacta de las plantillas F06/F07. |
| F21 | `TPO/TPO DeliveryGo Aplicaciones Interactivas 2026.pdf` | Consigna, pp. 7–8 y 18–20: base /api/v1, cinco rutas obligatorias, JSON, IDs consistentes, errores, Bearer y contratos entre módulos. |
| F22 | `G02_Usuarios_PrimeraEntrega_v1.5.pdf` | Referencia principal del equipo: nueve APIs, nueve DTO, reglas RN-01–10, CU-01–05, PUT completo, 403 para inactivos, baja lógica y credencial de módulo pendiente. |
| F23 | `TPO/Seguridad.jpg` | Imagen de seguridad: alternativas de topología directa y gateway, emisor Usuarios y validación distribuida. Se mantiene acceso directo sin imponer un gateway nuevo. |

Duplicados binarios verificados: **F04 = F19**, **F06 = F07 = F20**, **F09 = F11**. Se conservaron todos en el inventario para que ninguna referencia adjunta desaparezca del relevamiento.

## Localizadores para revisar las decisiones

Los números de F22 son páginas físicas del PDF, contadas desde 1, no sus pies de página, que están desplazados.

| Decisión | Fuente y localizador |
|---|---|
| Cinco rutas obligatorias y separación de módulos | F21, páginas físicas 7–8 |
| Nueve operaciones y autenticación por actor | F22, páginas físicas 41–42, sección 12.2 |
| Error normalizado y USER_INACTIVE=403 | F22, páginas físicas 42–43; F02, publicación «Template Swagger - Respuesta» |
| Campos y validaciones de los nueve DTO | F22, páginas físicas 44–45, sección 13 |
| Titular inactivo versus módulo autorizado | F22, página física 15, CU-03; páginas 38–40, SRV-03 y SRV-09 |
| Baja lógica y predeterminada | F22, página física 17, CU-05; páginas 31–32, SEC-05 |
| Login verifica contraseña antes de informar estado | F22, páginas físicas 14–15 y 40 |
| DTO/capas/transacciones y validación de frontera | F03, bloques de persistencia y mapeo avanzado; F08/F16, controladores y servicios |
| Emisión centralizada y firma asimétrica | F03, «Página 27 del PDF»; F13 y F23 para flujo de token |
| 204 sin contenido y error común del ecosistema | F01, bloque C13, segmentos que revisan DELETE y correlation ID; F02, última plantilla |
| Contrato intercambiable con el Portal y YAML en docs | F01, C13, discusión sobre exportar Swagger/OpenAPI y compartirlo en la carpeta docs |

F01 no incluye marcas temporales de todos los fragmentos y la transcripción no es una fuente exacta para fijar fechas de entregas. No se inventan timestamps ni se convierten menciones como «el 3» en un cronograma confirmado.

## Referencias técnicas complementarias

La sintaxis y las reglas de OpenAPI se contrastaron con la [especificación oficial 3.0.3](https://spec.openapis.org/oas/v3.0.3). La declaración de seguridad sigue [Swagger Bearer Authentication](https://swagger.io/docs/specification/v3_0/authentication/bearer-authentication/). Los tipos de claims y precauciones de validación se contrastaron con [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html) y [RFC 8725](https://www.rfc-editor.org/rfc/rfc8725.html). Estas fuentes aclaran estándares técnicos; no agregan funcionalidades al alcance académico.
