package ar.edu.uade.api.deliverygo.usuarios.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// MOCK DECLARADO de INT-01 e INT-06: no hace ninguna llamada HTTP, sólo escribe en el log. Es el modo por
// defecto (app.notificaciones.modo=mock). Con app.notificaciones.modo=http se usa NotificacionesClientHttp.
@Component
@ConditionalOnProperty(name = "app.notificaciones.modo", havingValue = "mock", matchIfMissing = true)
public class NotificacionesClientMock implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientMock.class);

    // Sólo para demostrar la recuperación en un ambiente local sin Notificaciones real: el mock escribe el
    // token en el log, que de otro modo nadie recibiría. Apagado por defecto; nunca activarlo en un ambiente compartido.
    private final boolean registrarTokenDeRecuperacion;

    public NotificacionesClientMock(
            @Value("${app.notificaciones.mock.log-recovery-token:false}") boolean registrarTokenDeRecuperacion) {
        this.registrarTokenDeRecuperacion = registrarTokenDeRecuperacion;
    }

    @Override
    public void enviarBienvenida(Long usuarioId, String nombre, String email) {
        // No se registra el email: es un dato personal que el log no necesita.
        log.info("[MOCK INT-01] Bienvenida solicitada a Notificaciones para el usuario {}", usuarioId);
    }

    @Override
    public void enviarRecuperacion(Long usuarioId, String nombre, String email, String token) {
        if (registrarTokenDeRecuperacion) {
            log.warn("[MOCK INT-06] Recuperación solicitada para el usuario {}. TOKEN (sólo demo local): {}", usuarioId, token);
        } else {
            log.info("[MOCK INT-06] Recuperación de contraseña solicitada a Notificaciones para el usuario {}", usuarioId);
        }
    }
}
