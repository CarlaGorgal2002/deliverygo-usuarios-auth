package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.domain.TokenRecuperacion;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.integration.NotificacionesClient;
import ar.edu.uade.api.deliverygo.usuarios.repository.TokenRecuperacionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-08 (API-12 y API-13) y la regla RN-13.
class RecuperacionApiTest extends ApiTestSupport {

    private static final String FORGOT = "/api/v1/auth/password/forgot";
    private static final String RESET = "/api/v1/auth/password/reset";
    private static final String NUEVA = "NuevaClave2026!";

    @MockitoBean
    private NotificacionesClient notificaciones;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    private void pedir(String email) throws Exception {
        mvc.perform(conJson(post(FORGOT), cuerpo("email", email)))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    // Pide la recuperación y devuelve el token que se le entregó a Notificaciones.
    private String pedirYObtenerToken(String email) throws Exception {
        pedir(email);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(notificaciones, atLeastOnce()).enviarRecuperacion(anyLong(), any(), eq(email), token.capture());
        return token.getAllValues().getLast();
    }

    private static String huella(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    // ------------------------------------------------------------------ paso 1

    @Test
    void laSolicitudRespondeIgualExistaONoLaCuentaYSoloAvisaSiExiste() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion inactiva = registrarYLoguear();
        marcarInactivo(inactiva.email());

        var existente = mvc.perform(conJson(post(FORGOT), cuerpo("email", a.email()))).andReturn().getResponse();
        var inexistente = mvc.perform(conJson(post(FORGOT), cuerpo("email", emailUnico()))).andReturn().getResponse();
        var desactivada = mvc.perform(conJson(post(FORGOT), cuerpo("email", inactiva.email()))).andReturn().getResponse();

        // RN-13: misma respuesta en los tres casos, para no revelar qué emails existen ni su estado
        assertThat(existente.getStatus()).isEqualTo(202);
        assertThat(inexistente.getStatus()).isEqualTo(202);
        assertThat(desactivada.getStatus()).isEqualTo(202);
        assertThat(inexistente.getContentAsString()).isEqualTo(existente.getContentAsString());
        assertThat(desactivada.getContentAsString()).isEqualTo(existente.getContentAsString());

        verify(notificaciones).enviarRecuperacion(eq(a.id()), any(), eq(a.email()), any());
        verify(notificaciones, never()).enviarRecuperacion(eq(inactiva.id()), any(), any(), any());
    }

    @Test
    void elEmailSeNormalizaYSeValidaSuFormato() throws Exception {
        Sesion a = registrarYLoguear();

        // DC-02: se normaliza a minúsculas; el formato no admite espacios, igual que en el login
        mvc.perform(conJson(post(FORGOT), cuerpo("email", a.email().toUpperCase())))
                .andExpect(status().isAccepted());
        verify(notificaciones).enviarRecuperacion(eq(a.id()), any(), eq(a.email()), any());

        assertError(mvc.perform(conJson(post(FORGOT), cuerpo("email", "no-es-un-email"))), 400, "VALIDATION_ERROR", FORGOT)
                .andExpect(jsonPath("$.details[0].campo").value("email"));
        assertError(mvc.perform(conJson(post(FORGOT), "{}")), 400, "VALIDATION_ERROR", FORGOT);
        assertError(mvc.perform(conJson(post(FORGOT), cuerpo("email", a.email(), "rol", "ADMIN"))),
                400, "VALIDATION_ERROR", FORGOT);
    }

    @Test
    void siFallaNotificacionesLaRespuestaSigueSiendo202() throws Exception {
        Sesion a = registrarYLoguear();
        doThrow(new IllegalStateException("Notificaciones no disponible"))
                .when(notificaciones).enviarRecuperacion(anyLong(), any(), any(), any());

        pedir(a.email());
    }

    @Test
    void esPublicoAunConUnTokenViejoYSoloSeGuardaLaHuella() throws Exception {
        Sesion a = registrarYLoguear();

        mvc.perform(conJson(post(FORGOT).header("Authorization", "Bearer token.viejo.vencido"), cuerpo("email", a.email())))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(notificaciones).enviarRecuperacion(eq(a.id()), any(), eq(a.email()), token.capture());
        // RN-13: en la base está el hash, no el token; no se puede buscar por el valor entregado
        assertThat(tokenRepository.findByTokenHash(token.getValue())).isEmpty();
        assertThat(tokenRepository.findByTokenHash(huella(token.getValue()))).isPresent();
    }

    // ------------------------------------------------------------------ paso 2

    @Test
    void elTokenCambiaLaContraseniaUnaSolaVez() throws Exception {
        Sesion a = registrarYLoguear();
        String token = pedirYObtenerToken(a.email());

        mvc.perform(conJson(post(RESET), cuerpo("token", token, "password", NUEVA)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        login(a.email(), NUEVA).andExpect(status().isOk());
        login(a.email(), PASSWORD).andExpect(status().isUnauthorized());

        // un solo uso (RN-13)
        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", token, "password", "OtraClave2026!"))),
                400, "INVALID_RESET_TOKEN", RESET);
        login(a.email(), NUEVA).andExpect(status().isOk());
    }

    @Test
    void unPedidoNuevoDejaSinEfectoElAnterior() throws Exception {
        Sesion a = registrarYLoguear();
        String primero = pedirYObtenerToken(a.email());
        String segundo = pedirYObtenerToken(a.email());
        assertThat(segundo).isNotEqualTo(primero);

        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", primero, "password", NUEVA))),
                400, "INVALID_RESET_TOKEN", RESET);
        mvc.perform(conJson(post(RESET), cuerpo("token", segundo, "password", NUEVA))).andExpect(status().isNoContent());
    }

    @Test
    void tokenInexistenteVencidoOdeCuentaInactivaDanElMismoError() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion b = registrarYLoguear();

        // inexistente
        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", "no-existe", "password", NUEVA))),
                400, "INVALID_RESET_TOKEN", RESET);

        // vencido
        Usuario usuarioA = usuarioRepository.findByEmail(a.email()).orElseThrow();
        LocalDateTime ahora = LocalDateTime.now(ZoneOffset.UTC);
        String vencido = "token-vencido-" + a.id();
        tokenRepository.save(new TokenRecuperacion(usuarioA, huella(vencido), ahora.minusHours(2), ahora.minusHours(1)));
        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", vencido, "password", NUEVA))),
                400, "INVALID_RESET_TOKEN", RESET);

        // la cuenta se desactivó después de pedir el token
        String deB = pedirYObtenerToken(b.email());
        marcarInactivo(b.email());
        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", deB, "password", NUEVA))),
                400, "INVALID_RESET_TOKEN", RESET);

        login(a.email(), PASSWORD).andExpect(status().isOk());
    }

    @Test
    void laNuevaContraseniaSigueLaPoliticaDelAlta() throws Exception {
        Sesion a = registrarYLoguear();
        String token = pedirYObtenerToken(a.email());

        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", token, "password", "corta"))),
                400, "VALIDATION_ERROR", RESET).andExpect(jsonPath("$.details[0].campo").value("password"));
        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", token, "password", "a".repeat(73)))),
                400, "VALIDATION_ERROR", RESET).andExpect(jsonPath("$.details[0].campo").value("password"));
        assertError(mvc.perform(conJson(post(RESET), cuerpo("password", NUEVA))), 400, "VALIDATION_ERROR", RESET)
                .andExpect(jsonPath("$.details[0].campo").value("token"));
        assertError(mvc.perform(conJson(post(RESET), cuerpo("token", token, "password", NUEVA, "email", a.email()))),
                400, "VALIDATION_ERROR", RESET);

        // los rechazos no consumieron el token
        mvc.perform(conJson(post(RESET), cuerpo("token", token, "password", NUEVA))).andExpect(status().isNoContent());
    }

    @Test
    void dosRestablecimientosSimultaneosConElMismoTokenSoloUnoSeAplica() throws Exception {
        Sesion a = registrarYLoguear();
        String token = pedirYObtenerToken(a.email());

        List<Integer> estados = enParalelo(6, indice -> mvc.perform(conJson(post(RESET),
                        cuerpo("token", token, "password", "Clave" + indice + "-Paralela2026!")))
                .andReturn().getResponse().getStatus());

        assertThat(estados).filteredOn(e -> e == 204).hasSize(1);
        assertThat(estados).filteredOn(e -> e == 400).hasSize(5);
    }
}
