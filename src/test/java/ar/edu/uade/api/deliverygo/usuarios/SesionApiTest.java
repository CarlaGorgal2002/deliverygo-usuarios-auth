package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-09 (API-14 y API-15) y la regla RN-14.
class SesionApiTest extends ApiTestSupport {

    private static final String REFRESH = "/api/v1/auth/refresh";
    private static final String SESSION = "/api/v1/auth/session";

    @Autowired
    private JwtService jwtService;
    @Value("${app.jwt.secret}")
    private String secreto;

    private String tokenVencido(long usuarioId) {
        return Jwts.builder().subject(String.valueOf(usuarioId)).claim("role", "CLIENTE")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8))).compact();
    }

    // ------------------------------------------------------------------ validar

    @Test
    void validarDevuelveElRolYElEstadoVigentesYElVencimientoDelToken() throws Exception {
        Sesion a = registrarYLoguear();

        mvc.perform(get(SESSION).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(a.id()))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.expiraEn", endsWith("Z")))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void validarMuestraElRolActualAunqueElTokenTengaOtro() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();
        mvc.perform(conJson(put("/api/v1/users/" + a.id() + "/role").header("Authorization", admin.bearer()),
                cuerpo("rol", "REPARTIDOR"))).andExpect(status().isOk());

        // el token emitido antes del cambio dice CLIENTE; la base dice REPARTIDOR
        assertThat(jwtService.leer(a.token()).rol()).isEqualTo("CLIENTE");
        mvc.perform(get(SESSION).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("REPARTIDOR"));
    }

    // ------------------------------------------------------------------ renovar

    @Test
    void renovarEmiteUnTokenQueSirveYTraeElRolActual() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();
        mvc.perform(conJson(put("/api/v1/users/" + a.id() + "/role").header("Authorization", admin.bearer()),
                cuerpo("rol", "COMERCIO"))).andExpect(status().isOk());

        JsonNode renovada = json.readTree(mvc.perform(post(REFRESH).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuarioId").value(a.id()))
                .andExpect(jsonPath("$.rol").value("COMERCIO"))
                .andExpect(jsonPath("$.expiraEn", endsWith("Z")))
                .andReturn().getResponse().getContentAsString());

        String nuevo = renovada.get("token").asText();
        assertThat(jwtService.leer(nuevo).rol()).isEqualTo("COMERCIO");
        assertThat(jwtService.leer(nuevo).id()).isEqualTo(a.id());
        // el token nuevo opera sobre el perfil propio
        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", "Bearer " + nuevo))
                .andExpect(status().isOk());
    }

    @Test
    void unaCuentaInactivaNoRenuevaNiValida() throws Exception {
        Sesion a = registrarYLoguear();
        marcarInactivo(a.email());

        assertError(mvc.perform(post(REFRESH).header("Authorization", a.bearer())), 403, "USER_INACTIVE", REFRESH);
        assertError(mvc.perform(get(SESSION).header("Authorization", a.bearer())), 403, "USER_INACTIVE", SESSION);
    }

    @Test
    void unTokenDeUnaCuentaQueYaNoExisteEs401() throws Exception {
        Sesion a = registrarYLoguear();
        usuarioRepository.deleteById(a.id());

        assertError(mvc.perform(post(REFRESH).header("Authorization", a.bearer())), 401, "UNAUTHENTICATED", REFRESH);
        assertError(mvc.perform(get(SESSION).header("Authorization", a.bearer())), 401, "UNAUTHENTICATED", SESSION);
    }

    // ------------------------------------------------------------------ credenciales

    @Test
    void sinTokenVencidoOAlteradoLaRespuestaEs401() throws Exception {
        Sesion a = registrarYLoguear();

        assertError(mvc.perform(post(REFRESH)), 401, "UNAUTHENTICATED", REFRESH);
        assertError(mvc.perform(get(SESSION)), 401, "UNAUTHENTICATED", SESSION);

        // RN-14: un token vencido no se renueva; hay que volver a iniciar sesión
        String vencido = "Bearer " + tokenVencido(a.id());
        assertError(mvc.perform(post(REFRESH).header("Authorization", vencido)), 401, "UNAUTHENTICATED", REFRESH);
        assertError(mvc.perform(get(SESSION).header("Authorization", vencido)), 401, "UNAUTHENTICATED", SESSION);

        String alterado = "Bearer " + a.token().substring(0, a.token().length() - 3) + "abc";
        assertError(mvc.perform(post(REFRESH).header("Authorization", alterado)), 401, "UNAUTHENTICATED", REFRESH);
    }

    @Test
    void unaApiKeyDeModuloNoRenuevaNiValidaSesionesDeUsuario() throws Exception {
        assertError(mvc.perform(post(REFRESH).header("X-Api-Key", CLAVE_PEDIDOS)), 403, "FORBIDDEN", REFRESH);
        assertError(mvc.perform(get(SESSION).header("X-Api-Key", CLAVE_PEDIDOS)), 403, "FORBIDDEN", SESSION);
    }
}
