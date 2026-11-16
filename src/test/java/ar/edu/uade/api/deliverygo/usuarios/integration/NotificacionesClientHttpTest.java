package ar.edu.uade.api.deliverygo.usuarios.integration;

import ar.edu.uade.api.deliverygo.usuarios.security.JwtService;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// INT-01 e INT-06 contra el contrato publicado por Notificaciones, sin levantar Spring ni la red.
class NotificacionesClientHttpTest {

    private static final String BASE = "http://notificaciones.test";
    private static final String SECRETO = "secreto-de-prueba-con-mas-de-treinta-y-dos-caracteres";
    private static final String BIENVENIDA = "USUARIO_BIENVENIDA_EMAIL";
    private static final String RECUPERACION = "RECUPERACION_PASSWORD_EMAIL";

    // Respuesta real de Notificaciones: trae campos que no usamos (asunto, cuerpo, variables) y una plantilla
    // inactiva con el mismo nombre que la de recuperación.
    private static final String PLANTILLAS = """
            [
              {"id": 7, "nombre": "USUARIO_BIENVENIDA_EMAIL", "asunto": "Bienvenido", "cuerpo": "Hola {{nombre}}",
               "activa": true, "canal": "EMAIL", "variables": ["nombre"]},
              {"id": 8, "nombre": "RECUPERACION_PASSWORD_EMAIL", "asunto": "Viejo", "cuerpo": "-",
               "activa": false, "canal": "EMAIL", "variables": []},
              {"id": 9, "nombre": "RECUPERACION_PASSWORD_EMAIL", "asunto": "Recuperar", "cuerpo": "{{token}}",
               "activa": true, "canal": "EMAIL", "variables": ["nombre", "token"]}
            ]
            """;

    private static final String NOTIFICACION_CREADA = """
            {"id": 100, "destinatarioId": 42, "plantillaId": 7, "canal": "EMAIL", "estado": "PENDIENTE",
             "cantidadIntentos": 0, "fechaCreacion": "2026-11-16T12:00:00Z"}
            """;

    private final JwtService jwtService = new JwtService(SECRETO, 60);
    private MockRestServiceServer servidor;
    private NotificacionesClientHttp cliente;

    @BeforeEach
    void armarCliente() {
        // Igual que la app: fail-on-unknown-properties activo, para comprobar @JsonIgnoreProperties en los DTO.
        ObjectMapper estricto = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE)
                .messageConverters(c -> {
                    c.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    c.add(new MappingJackson2HttpMessageConverter(estricto));
                });
        servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        cliente = new NotificacionesClientHttp(builder.build(), jwtService, BIENVENIDA, RECUPERACION);
        MDC.put("correlationId", "corr-123");
    }

    @AfterEach
    void limpiar() {
        MDC.remove("correlationId");
    }

    @Test
    void enviaLaBienvenidaConPlantillaTokenDeServicioYCorrelacion() {
        servidor.expect(requestTo(BASE + "/api/v1/templates"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(tokenDeServicioPara(42L))
                .andExpect(header("X-Correlation-Id", "corr-123"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/api/v1/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(tokenDeServicioPara(42L))
                .andExpect(header("X-Correlation-Id", "corr-123"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"destinatarioId": 42, "plantillaId": 7, "parametros": {"nombre": "Ana"}}
                        """, true))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(NOTIFICACION_CREADA));

        cliente.enviarBienvenida(42L, "Ana", "ana@example.com");

        servidor.verify();
    }

    @Test
    void enviaLaRecuperacionConNombreYTokenUsandoLaPlantillaActiva() {
        servidor.expect(requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/api/v1/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(tokenDeServicioPara(42L))
                .andExpect(content().json("""
                        {"destinatarioId": 42, "plantillaId": 9, "parametros": {"nombre": "Ana", "token": "tok-abc"}}
                        """, true))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(NOTIFICACION_CREADA));

        cliente.enviarRecuperacion(42L, "Ana", "ana@example.com", "tok-abc");

        servidor.verify();
    }

    @Test
    void sinCorrelacionEnElMdcNoEnviaLaCabecera() {
        MDC.remove("correlationId");
        servidor.expect(requestTo(BASE + "/api/v1/templates"))
                .andExpect(headerDoesNotExist("X-Correlation-Id"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/api/v1/notifications"))
                .andExpect(headerDoesNotExist("X-Correlation-Id"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(NOTIFICACION_CREADA));

        cliente.enviarBienvenida(42L, "Ana", "ana@example.com");

        servidor.verify();
    }

    @Test
    void siLaPlantillaNoExisteOEstaInactivaLanzaUnErrorClaroYNoEnvia() {
        servidor.expect(requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess("""
                        [{"id": 8, "nombre": "RECUPERACION_PASSWORD_EMAIL", "activa": false, "canal": "EMAIL"},
                         {"id": 3, "nombre": "OTRA", "activa": true, "canal": "EMAIL"}]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.enviarRecuperacion(42L, "Ana", "ana@example.com", "tok-abc"))
                .isInstanceOf(NotificacionesException.class)
                .hasMessageContaining("No existe en Notificaciones una plantilla activa con nombre 'RECUPERACION_PASSWORD_EMAIL'");

        servidor.verify();
    }

    @Test
    void siNotificacionesResponde503LanzaNotificacionesException() {
        servidor.expect(requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/api/v1/notifications"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> cliente.enviarBienvenida(42L, "Ana", "ana@example.com"))
                .isInstanceOf(NotificacionesException.class)
                .hasMessageContaining("INT-01")
                .hasMessageContaining("503");

        servidor.verify();
    }

    @Test
    void siNotificacionesEstaCaidoLanzaNotificacionesException() {
        servidor.expect(requestTo(BASE + "/api/v1/templates"))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> cliente.enviarBienvenida(42L, "Ana", "ana@example.com"))
                .isInstanceOf(NotificacionesException.class)
                .hasRootCauseInstanceOf(ConnectException.class);

        servidor.verify();
    }

    @Test
    void laPlantillaSeBuscaUnaSolaVezGraciasALaCache() {
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.times(2), requestTo(BASE + "/api/v1/notifications"))
                .andExpect(content().json("""
                        {"plantillaId": 7}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(NOTIFICACION_CREADA));

        cliente.enviarBienvenida(42L, "Ana", "ana@example.com");
        cliente.enviarBienvenida(43L, "Luis", "luis@example.com");

        servidor.verify();
    }

    @Test
    void siElEnvioRespondeUn4xxLaPlantillaSeVuelveABuscarEnElProximoEnvio() {
        // Notificaciones recreó la plantilla: el id 7 ya no existe y la nueva es la 70.
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/notifications"))
                .andExpect(content().json("""
                        {"plantillaId": 7}
                        """))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> cliente.enviarBienvenida(42L, "Ana", "ana@example.com"))
                .isInstanceOf(NotificacionesException.class)
                .hasMessageContaining("404");
        servidor.verify();
        servidor.reset();

        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess("""
                        [{"id": 70, "nombre": "USUARIO_BIENVENIDA_EMAIL", "activa": true, "canal": "EMAIL"}]
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/notifications"))
                .andExpect(content().json("""
                        {"plantillaId": 70}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(NOTIFICACION_CREADA));

        cliente.enviarBienvenida(43L, "Luis", "luis@example.com");

        servidor.verify();
    }

    @Test
    void siElEnvioRespondeUn5xxLaPlantillaSigueEnCache() {
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/templates"))
                .andRespond(withSuccess(PLANTILLAS, MediaType.APPLICATION_JSON));
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/notifications"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        servidor.expect(ExpectedCount.once(), requestTo(BASE + "/api/v1/notifications"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(NOTIFICACION_CREADA));

        assertThatThrownBy(() -> cliente.enviarBienvenida(42L, "Ana", "ana@example.com"))
                .isInstanceOf(NotificacionesException.class);
        cliente.enviarBienvenida(43L, "Luis", "luis@example.com");

        servidor.verify();
    }

    // El Bearer es un JWT HS256 con el mismo secreto: sub = destinatario, role = SISTEMA, vigencia de 2 minutos.
    private RequestMatcher tokenDeServicioPara(Long destinatarioId) {
        return request -> {
            String autorizacion = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            assertThat(autorizacion).startsWith("Bearer ");
            UsuarioAutenticado servicio = jwtService.leer(autorizacion.substring("Bearer ".length()));
            assertThat(servicio.id()).isEqualTo(destinatarioId);
            assertThat(servicio.rol()).isEqualTo("SISTEMA");
            assertThat(servicio.expiraEn())
                    .isAfter(Instant.now().plusSeconds(60))
                    .isBeforeOrEqualTo(Instant.now().plusSeconds(120));
        };
    }
}
