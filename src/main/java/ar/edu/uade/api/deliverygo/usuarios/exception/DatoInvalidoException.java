package ar.edu.uade.api.deliverygo.usuarios.exception;

// Dato que supera las validaciones declarativas del DTO y se detecta en el servicio (RN-01).
public class DatoInvalidoException extends RuntimeException {

    private final String campo;

    public DatoInvalidoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
