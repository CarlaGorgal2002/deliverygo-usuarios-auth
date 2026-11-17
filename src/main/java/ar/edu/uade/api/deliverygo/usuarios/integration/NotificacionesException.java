package ar.edu.uade.api.deliverygo.usuarios.integration;

// Falla al pedirle un envío a Notificaciones. Es RuntimeException para que AuthService la atrape y
// la registre sin afectar el registro ni la recuperación de contraseña.
public class NotificacionesException extends RuntimeException {

    public NotificacionesException(String mensaje) {
        super(mensaje);
    }

    public NotificacionesException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
