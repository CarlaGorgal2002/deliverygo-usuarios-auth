package ar.edu.uade.api.deliverygo.usuarios.integration;

// Usuarios le pide a Notificaciones que envíe mensajes. El envío efectivo es de Notificaciones.
// El contrato real (POST /api/v1/notifications) lo define ese equipo.
public interface NotificacionesClient {

    // INT-01: bienvenida de un usuario recién registrado.
    void enviarBienvenida(Long usuarioId, String nombre, String email);

    // INT-06: enlace o código para restablecer la contraseña. El token viaja sólo hacia Notificaciones.
    void enviarRecuperacion(Long usuarioId, String nombre, String email, String token);
}
