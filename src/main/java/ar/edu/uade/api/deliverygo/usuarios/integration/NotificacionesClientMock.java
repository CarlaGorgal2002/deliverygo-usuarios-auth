package ar.edu.uade.api.deliverygo.usuarios.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// MOCK DECLARADO de INT-01: no hay llamada HTTP real porque todavía no existe el contrato de
// Notificaciones. Cuando exista, se reemplaza por un cliente REST que implemente NotificacionesClient.
@Component
public class NotificacionesClientMock implements NotificacionesClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesClientMock.class);

    @Override
    public void enviarBienvenida(Long usuarioId, String nombre, String email) {
        // No se registra el email: es un dato personal que el log no necesita.
        log.info("[MOCK INT-01] Bienvenida solicitada a Notificaciones para el usuario {}", usuarioId);
    }
}
