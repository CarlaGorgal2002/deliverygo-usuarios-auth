package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-01 (API-01) y CU-02 (API-02)
class AuthApiTest extends ApiTestSupport {

    @Test
    void registroCreaClienteActivoSinExponerLaContrasenia() throws Exception {
        String email = emailUnico();

        MvcResult resultado = registrar(email)
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaAlta").value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        long id = json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
        assertThat(resultado.getResponse().getHeader("Location")).isEqualTo("/api/v1/users/" + id);

        // RN-03: en la base queda un hash BCrypt, nunca la contraseña
        Usuario guardado = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(guardado.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void registroNormalizaElEmailYRechazaElDuplicadoSinImportarLaCapitalizacion() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());

        assertError(registrar(email.toUpperCase()), 409, "EMAIL_ALREADY_REGISTERED", "/api/v1/auth/register");
        assertThat(usuarioRepository.findAll().stream().filter(u -> u.getEmail().equals(email))).hasSize(1);
    }

    @Test
    void registroConDatosInvalidosDevuelveDetallesPorCampo() throws Exception {
        assertError(mvc.perform(conJson(post("/api/v1/auth/register"),
                        cuerpo("nombre", "  ", "apellido", "Pérez", "email", "no-es-un-email", "password", "corta"))),
                400, "VALIDATION_ERROR", "/api/v1/auth/register")
                .andExpect(jsonPath("$.details[?(@.campo=='nombre')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='password')]").exists())
                // el error nunca repite lo que envió el cliente
                .andExpect(jsonPath("$..mensaje", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("corta"))));
    }

    @Test
    void registroRechazaCamposQueElClienteNoPuedeAsignar() throws Exception {
        // RN-06 / DC-01: ni rol, ni estado, ni id
        for (String campo : List.of("rol", "estado", "id")) {
            assertError(mvc.perform(conJson(post("/api/v1/auth/register"),
                            cuerpo("nombre", "Ana", "apellido", "Pérez", "email", emailUnico(), "password", PASSWORD,
                                    campo, "ADMIN"))),
                    400, "VALIDATION_ERROR", "/api/v1/auth/register")
                    .andExpect(jsonPath("$.details[0].campo").value(campo));
        }
    }

    @Test
    void registroConCuerpoIlegibleOTipoIncorrectoDevuelve400() throws Exception {
        assertError(mvc.perform(conJson(post("/api/v1/auth/register"), "{ no es json")),
                400, "VALIDATION_ERROR", "/api/v1/auth/register")
                .andExpect(jsonPath("$.details[0].campo").value("body"));

        assertError(mvc.perform(conJson(post("/api/v1/auth/register"),
                        cuerpo("nombre", 123, "apellido", "Pérez", "email", emailUnico(), "password", PASSWORD))),
                400, "VALIDATION_ERROR", "/api/v1/auth/register")
                .andExpect(jsonPath("$.details[0].campo").value("nombre"));
    }

    @Test
    void registroRechazaContraseniasQueBCryptTruncaria() throws Exception {
        assertError(mvc.perform(conJson(post("/api/v1/auth/register"),
                        cuerpo("nombre", "Ana", "apellido", "Pérez", "email", emailUnico(), "password", "a".repeat(73)))),
                400, "VALIDATION_ERROR", "/api/v1/auth/register")
                .andExpect(jsonPath("$.details[0].campo").value("password"));
    }

    @Test
    void loginDevuelveTokenConIdYRolComoClaims() throws Exception {
        String email = emailUnico();
        long id = json.readTree(registrar(email).andReturn().getResponse().getContentAsString()).get("id").asLong();

        MvcResult resultado = login(email.toUpperCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuarioId").value(id))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.expiraEn").value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.expiresInSeconds").doesNotExist())
                .andReturn();

        JsonNode sesion = json.readTree(resultado.getResponse().getContentAsString());
        String payload = new String(java.util.Base64.getUrlDecoder().decode(sesion.get("token").asText().split("\\.")[1]));
        JsonNode claims = json.readTree(payload);
        assertThat(claims.get("sub").asText()).isEqualTo(String.valueOf(id));
        assertThat(claims.get("role").asText()).isEqualTo("CLIENTE");
        assertThat(claims.get("exp").asLong()).isGreaterThan(claims.get("iat").asLong());
        // exp y expiraEn describen el mismo instante
        assertThat(java.time.OffsetDateTime.parse(sesion.get("expiraEn").asText()).toEpochSecond())
                .isEqualTo(claims.get("exp").asLong());
        // el payload del JWT no es secreto: no lleva email ni contraseña
        assertThat(payload).doesNotContain(email).doesNotContain(PASSWORD);
    }

    @Test
    void loginConEmailInexistenteOContraseniaIncorrectaDaElMismoError() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());

        String inexistente = assertError(login(emailUnico(), PASSWORD), 401, "INVALID_CREDENTIALS", "/api/v1/auth/login")
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andReturn().getResponse().getContentAsString();
        String incorrecta = assertError(login(email, "otra-clave-123"), 401, "INVALID_CREDENTIALS", "/api/v1/auth/login")
                .andReturn().getResponse().getContentAsString();

        // RN-08: no se distingue la causa
        assertThat(json.readTree(inexistente).get("message")).isEqualTo(json.readTree(incorrecta).get("message"));
    }

    @Test
    void loginDeCuentaInactivaInformaElEstadoSoloConLaContraseniaCorrecta() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());
        marcarInactivo(email);

        assertError(login(email, PASSWORD), 403, "USER_INACTIVE", "/api/v1/auth/login");
        // RN-04 + RN-08: con la contraseña equivocada no se revela que la cuenta está inactiva
        assertError(login(email, "otra-clave-123"), 401, "INVALID_CREDENTIALS", "/api/v1/auth/login");
    }

    @Test
    void loginAdmiteContraseniasLargasSinFallar() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());

        assertError(login(email, "x".repeat(200)), 401, "INVALID_CREDENTIALS", "/api/v1/auth/login");
    }

    @Test
    void loginValidaElFormatoDeLosDatos() throws Exception {
        assertError(mvc.perform(conJson(post("/api/v1/auth/login"), cuerpo("email", "nada", "password", ""))),
                400, "VALIDATION_ERROR", "/api/v1/auth/login")
                .andExpect(jsonPath("$.details[?(@.campo=='email')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='password')]").exists());

        // el contrato usa email, no username
        assertError(mvc.perform(conJson(post("/api/v1/auth/login"), cuerpo("username", "x@example.com", "password", PASSWORD))),
                400, "VALIDATION_ERROR", "/api/v1/auth/login");
    }

    @Test
    void registroYLoginSonPublicosAunConUnTokenViejo() throws Exception {
        mvc.perform(conJson(post("/api/v1/auth/login"), cuerpo("email", emailUnico(), "password", PASSWORD))
                        .header("Authorization", "Bearer token.vencido.o-basura"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void dosRegistrosSimultaneosConElMismoEmailCreanUnaSolaCuenta() throws Exception {
        String email = emailUnico();
        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            Callable<Integer> tarea = () -> {
                largada.await();
                return registrar(email).andReturn().getResponse().getStatus();
            };
            resultados.add(pool.submit(tarea));
        }
        largada.countDown();

        int creados = 0;
        int conflictos = 0;
        for (Future<Integer> resultado : resultados) {
            int status = resultado.get();
            if (status == 201) creados++;
            else if (status == 409) conflictos++;
        }
        pool.shutdown();

        assertThat(creados).isEqualTo(1);
        assertThat(conflictos).isEqualTo(hilos - 1);
        assertThat(usuarioRepository.findAll().stream().filter(u -> u.getEmail().equals(email))).hasSize(1);
    }
}
