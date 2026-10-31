package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.integration.NotificacionesClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INT-01: la bienvenida es no bloqueante y se pide una vez confirmado el alta.
class BienvenidaApiTest extends ApiTestSupport {

    @MockitoBean
    private NotificacionesClient notificaciones;

    @Test
    void elRegistroPideLaBienvenidaDelUsuarioCreado() throws Exception {
        String email = emailUnico();

        String cuerpo = registrar(email).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        long id = json.readTree(cuerpo).get("id").asLong();
        verify(notificaciones).enviarBienvenida(eq(id), eq("Ana"), eq(email));
    }

    @Test
    void siFallaNotificacionesElRegistroSeMantieneYResponde201() throws Exception {
        doThrow(new IllegalStateException("Notificaciones no disponible"))
                .when(notificaciones).enviarBienvenida(anyLong(), any(), any());
        String email = emailUnico();

        registrar(email).andExpect(status().isCreated());

        assertThat(usuarioRepository.findByEmail(email)).isPresent();
        login(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void siElRegistroFallaNoSePideLaBienvenida() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());

        registrar(email).andExpect(status().isConflict());

        // sólo la del primer alta
        verify(notificaciones, times(1)).enviarBienvenida(anyLong(), any(), eq(email));
    }
}
