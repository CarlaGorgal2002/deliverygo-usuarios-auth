package ar.edu.uade.api.deliverygo.usuarios.exception;

// El JWT es válido pero ya no corresponde a una sesión utilizable (por ejemplo, el usuario dejó de existir).
public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException(String message) {
        super(message);
    }
}
