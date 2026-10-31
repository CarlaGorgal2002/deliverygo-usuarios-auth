package ar.edu.uade.api.deliverygo.usuarios;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DC-06: X-Correlation-Id en todas las respuestas, también las de seguridad. Además CORS del Portal.
class CorrelacionApiTest extends ApiTestSupport {

    @Test
    void seReutilizaElIdValidoRecibidoEnCabeceraYEnLosErrores() throws Exception {
        assertError(mvc.perform(get("/api/v1/users/1").header("X-Correlation-Id", "g02-demo.001_A")),
                401, "UNAUTHENTICATED", "/api/v1/users/1")
                .andExpect(header().string("X-Correlation-Id", "g02-demo.001_A"))
                .andExpect(jsonPath("$.correlationId").value("g02-demo.001_A"));

        assertError(login(emailUnico(), PASSWORD), 401, "INVALID_CREDENTIALS", "/api/v1/auth/login");
    }

    @Test
    void siFaltaSeGeneraUnoNuevoYCoincideEnCabeceraYCuerpo() throws Exception {
        String cabecera = assertError(mvc.perform(get("/api/v1/users/1")), 401, "UNAUTHENTICATED", "/api/v1/users/1")
                .andExpect(header().exists("X-Correlation-Id"))
                .andReturn().getResponse().getHeader("X-Correlation-Id");
        assertThat(cabecera).matches("^[A-Za-z0-9._-]{1,128}$");

        registrar(emailUnico())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void unaCabeceraInvalidaProduce400ConUnIdNuevo() throws Exception {
        for (String invalido : new String[]{"tiene espacios", "a".repeat(129), "con/barra", "ñandú"}) {
            String generado = assertError(mvc.perform(get("/api/v1/users/1").header("X-Correlation-Id", invalido)),
                    400, "VALIDATION_ERROR", "/api/v1/users/1")
                    .andExpect(jsonPath("$.details[0].campo").value("X-Correlation-Id"))
                    .andReturn().getResponse().getHeader("X-Correlation-Id");
            assertThat(generado).isNotEqualTo(invalido).matches("^[A-Za-z0-9._-]{1,128}$");
        }
    }

    @Test
    void elPortalPuedeHacerPreflightConPatchYCorrelacion() throws Exception {
        mvc.perform(options("/api/v1/users/1/addresses/1/default")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,x-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("PATCH")));
    }

    @Test
    void unOrigenNoAutorizadoNoRecibeCabecerasCors() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://sitio-ajeno.example")
                        .contentType("application/json").content(cuerpo("email", emailUnico(), "password", PASSWORD)))
                .andExpect(status().isForbidden());
    }
}
