package ar.edu.uade.api.deliverygo.usuarios;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Estado del servicio: público, 200 con status ok, también con un token viejo o inválido.
class StatusApiTest extends ApiTestSupport {

    @Test
    void respondeOkSinCredencial() throws Exception {
        mvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void unTokenInvalidoNoLoRompe() throws Exception {
        mvc.perform(get("/api/v1/status").header("Authorization", "Bearer token-viejo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
