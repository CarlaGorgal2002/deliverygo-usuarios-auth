package ar.edu.uade.api.deliverygo.usuarios.integration;

// INT-01: Usuarios solicita a Notificaciones la bienvenida de un usuario recién registrado.
// El contrato real (POST /api/v1/notifications) lo define el equipo de Notificaciones.
public interface NotificacionesClient {

    void enviarBienvenida(Long usuarioId, String nombre, String email);
}
