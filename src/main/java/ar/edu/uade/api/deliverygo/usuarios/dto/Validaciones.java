package ar.edu.uade.api.deliverygo.usuarios.dto;

// Mensajes y patrones compartidos por los DTO de entrada (RN-01).
final class Validaciones {

    static final String OBLIGATORIO = "Es obligatorio";
    static final String EMAIL_MENSAJE = "Debe tener un formato de email válido";
    // Sin espacios, una sola arroba y un dominio con punto.
    static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    static final String PASSWORD_MINIMO = "Debe tener al menos 8 caracteres";

    private Validaciones() {}
}
