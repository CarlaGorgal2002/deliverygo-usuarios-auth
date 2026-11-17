package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.integration.NotificacionesClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// INT-01: la bienvenida es no bloqueante y se pide una vez confirmado el alta.
class BienvenidaApiTest extends ApiTestSupport {

    @MockitoBean
    private NotificacionesClient notificaciones;

    @Autowired
    private PlatformTransactionManager transactionManager;

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

    // Notificaciones verifica al destinatario con GET /users/{id}, en otra conexión y otra transacción. Si la
    // bienvenida se pidiera antes del commit del alta, esa consulta todavía no vería al usuario (RECIPIENT_NOT_FOUND).
    @Test
    void laBienvenidaSePideRecienCuandoElAltaYaEsVisibleParaOtraTransaccion() throws Exception {
        TransactionTemplate otraTransaccion = new TransactionTemplate(transactionManager);
        otraTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicReference<Boolean> visible = new AtomicReference<>();
        doAnswer(invocacion -> {
            Long id = invocacion.getArgument(0);
            visible.set(otraTransaccion.execute(estado -> usuarioRepository.existsById(id)));
            return null;
        }).when(notificaciones).enviarBienvenida(anyLong(), any(), any());

        registrar(emailUnico()).andExpect(status().isCreated());

        assertThat(visible.get()).as("el usuario ya está confirmado cuando se pide la bienvenida").isTrue();
    }
}
