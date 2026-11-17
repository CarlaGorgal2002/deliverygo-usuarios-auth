package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RN-02 / DM-11: la restricción única de la base es la segunda barrera. Se simula que el control del service no
// vio el email repetido (como pasaría con dos altas simultáneas) y se comprueba que la base lo rechaza con 409.
class IntegridadApiTest extends ApiTestSupport {

    @MockitoSpyBean
    private UsuarioRepository repositorioEspiado;

    @Test
    void unEmailRepetidoQueEvitaElControlDelServiceSeInformaComo409() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());
        doReturn(false).when(repositorioEspiado).existsByEmail(email);

        registrar(email)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }
}
