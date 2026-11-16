package ar.edu.uade.api.deliverygo.usuarios.integration;

import ar.edu.uade.api.deliverygo.usuarios.config.AsyncConfig;
import ar.edu.uade.api.deliverygo.usuarios.security.JwtService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// INT-01 e INT-06 contra la API real de Notificaciones (repo DeliveryGo-Notificaciones). Se activa con
// app.notificaciones.modo=http; por defecto sigue el mock declarado (NotificacionesClientMock).
// Consume el contrato tal como lo publica Notificaciones:
//   GET  /api/v1/templates      -> se busca la plantilla activa por nombre (y se guarda su id en caché)
//   POST /api/v1/notifications  -> { destinatarioId, plantillaId, parametros } -> 201
// Cada llamada lleva un JWT de servicio corto (HS256, mismo JWT_SECRET, sub = destinatario, role = SISTEMA)
// y el X-Correlation-Id de la solicitud en curso.
// Los envíos corren en segundo plano (@Async, AsyncConfig): la respuesta no espera a Notificaciones, así que su
// tiempo no delata si un email está registrado (RN-13). Una falla sale como NotificacionesException y la
// registra AsyncConfig, sin afectar el registro ni la recuperación.
@Component
@ConditionalOnProperty(name = "app.notificaciones.modo", havingValue = "http")
public class NotificacionesClientHttp implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientHttp.class);

    static final String RUTA_PLANTILLAS = "/api/v1/templates";
    static final String RUTA_NOTIFICACIONES = "/api/v1/notifications";
    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final Duration VIGENCIA_TOKEN_SERVICIO = Duration.ofMinutes(2);

    private final RestClient restClient;
    private final JwtService jwtService;
    private final String plantillaBienvenida;
    private final String plantillaRecuperacion;
    private final Map<String, Long> plantillasPorNombre = new ConcurrentHashMap<>();

    @Autowired
    public NotificacionesClientHttp(RestClient.Builder builder,
                                    JwtService jwtService,
                                    @Value("${app.notificaciones.base-url}") String baseUrl,
                                    @Value("${app.notificaciones.timeout-ms}") int timeoutMs,
                                    @Value("${app.notificaciones.plantillas.bienvenida}") String plantillaBienvenida,
                                    @Value("${app.notificaciones.plantillas.recuperacion}") String plantillaRecuperacion,
                                    @Value("${server.port:8080}") int puertoPropio) {
        this(validarQueNoApunteASiMismo(builder, baseUrl, puertoPropio).baseUrl(baseUrl).requestFactory(fabricaConTimeout(timeoutMs)).build(),
                jwtService, plantillaBienvenida, plantillaRecuperacion);
    }

    // Para las pruebas: recibe el RestClient ya armado (por ejemplo, ligado a MockRestServiceServer).
    NotificacionesClientHttp(RestClient restClient, JwtService jwtService,
                             String plantillaBienvenida, String plantillaRecuperacion) {
        this.restClient = restClient;
        this.jwtService = jwtService;
        this.plantillaBienvenida = plantillaBienvenida;
        this.plantillaRecuperacion = plantillaRecuperacion;
    }

    @Async(AsyncConfig.EJECUTOR_NOTIFICACIONES)
    @Override
    public void enviarBienvenida(Long usuarioId, String nombre, String email) {
        enviar(usuarioId, plantillaBienvenida, Map.of("nombre", nombre), "INT-01");
    }

    @Async(AsyncConfig.EJECUTOR_NOTIFICACIONES)
    @Override
    public void enviarRecuperacion(Long usuarioId, String nombre, String email, String token) {
        enviar(usuarioId, plantillaRecuperacion, Map.of("nombre", nombre, "token", token), "INT-06");
    }

    private void enviar(Long destinatarioId, String nombrePlantilla, Map<String, String> parametros, String integracion) {
        try {
            Long plantillaId = resolverPlantilla(nombrePlantilla, destinatarioId);
            NotificacionResponse respuesta = restClient.post()
                    .uri(RUTA_NOTIFICACIONES)
                    .headers(h -> cabeceras(h, destinatarioId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotificacionRequest(destinatarioId, plantillaId, parametros))
                    .retrieve()
                    .body(NotificacionResponse.class);
            // No se registran email, nombre ni token: el log no los necesita.
            log.info("[{}] Notificaciones aceptó el envío para el usuario {}: notificación {} en estado {}",
                    integracion, destinatarioId,
                    respuesta == null ? null : respuesta.id(),
                    respuesta == null ? null : respuesta.estado());
        } catch (RestClientException e) {
            // Un 4xx puede deberse a un id de plantilla viejo (Notificaciones la recreó): se olvida para que el
            // próximo envío la vuelva a buscar por nombre.
            if (e instanceof HttpClientErrorException) {
                plantillasPorNombre.remove(nombrePlantilla);
            }
            throw new NotificacionesException(
                    "[" + integracion + "] Notificaciones no aceptó el envío para el usuario " + destinatarioId
                            + ": " + e.getMessage(), e);
        }
    }

    // Busca el id de la plantilla activa con ese nombre. Sólo se guarda en caché cuando se encuentra, y se
    // descarta si Notificaciones rechaza un envío con 4xx (ver enviar).
    private Long resolverPlantilla(String nombre, Long destinatarioId) {
        Long enCache = plantillasPorNombre.get(nombre);
        if (enCache != null) {
            return enCache;
        }
        List<PlantillaResponse> plantillas = restClient.get()
                .uri(RUTA_PLANTILLAS)
                .headers(h -> cabeceras(h, destinatarioId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PlantillaResponse>>() { });
        Long id = plantillas == null ? null : plantillas.stream()
                .filter(p -> nombre.equals(p.nombre()) && Boolean.TRUE.equals(p.activa()) && p.id() != null)
                .map(PlantillaResponse::id)
                .findFirst()
                .orElse(null);
        if (id == null) {
            throw new NotificacionesException("No existe en Notificaciones una plantilla activa con nombre '"
                    + nombre + "'. Debe crearla el equipo de Notificaciones.");
        }
        plantillasPorNombre.put(nombre, id);
        return id;
    }

    private void cabeceras(HttpHeaders headers, Long destinatarioId) {
        String token = jwtService.generarToken(destinatarioId, JwtService.ROL_SERVICIO, VIGENCIA_TOKEN_SERVICIO).token();
        headers.setBearerAuth(token);
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.set(CORRELATION_HEADER, correlationId);
        }
    }

    // Notificaciones usa por defecto el mismo puerto que Usuarios (8089). Si NOTIFICACIONES_URL apunta a este
    // mismo servicio, Usuarios se llamaría a sí mismo: la app no arranca y el mensaje dice cómo corregirlo.
    private static RestClient.Builder validarQueNoApunteASiMismo(RestClient.Builder builder, String baseUrl,
                                                                 int puertoPropio) {
        if (apuntaASiMismo(baseUrl, puertoPropio)) {
            throw new IllegalStateException("NOTIFICACIONES_URL (" + baseUrl + ") apunta a este mismo servicio, "
                    + "que escucha en el puerto " + puertoPropio + ". Levantar Usuarios en otro puerto "
                    + "(SERVER_PORT, por ejemplo 8080) o indicar la URL real de Notificaciones.");
        }
        return builder;
    }

    static boolean apuntaASiMismo(String baseUrl, int puertoPropio) {
        if (puertoPropio <= 0) {
            return false; // puerto aleatorio: no se puede comparar
        }
        URI uri = URI.create(baseUrl);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean local = host.equals("localhost") || host.equals("0.0.0.0") || host.startsWith("127.")
                || host.equals("[::1]") || host.equals("::1");
        int puerto = uri.getPort() != -1 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        return local && puerto == puertoPropio;
    }

    private static SimpleClientHttpRequestFactory fabricaConTimeout(int timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeoutMs);
        fabrica.setReadTimeout(timeoutMs);
        return fabrica;
    }

    record NotificacionRequest(Long destinatarioId, Long plantillaId, Map<String, String> parametros) {
    }

    // La app usa fail-on-unknown-properties=true: las respuestas ajenas ignoran los campos que no usamos.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlantillaResponse(Long id, String nombre, Boolean activa, String canal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NotificacionResponse(Long id, Long destinatarioId, Long plantillaId, String canal,
                                String estado, Integer cantidadIntentos) {
    }
}
