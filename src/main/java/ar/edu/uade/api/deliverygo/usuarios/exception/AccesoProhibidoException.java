package ar.edu.uade.api.deliverygo.usuarios.exception;

// Operación para la que el solicitante no tiene permiso aunque esté autenticado (RN-05, RN-11, RN-12).
public class AccesoProhibidoException extends RuntimeException {
    public AccesoProhibidoException(String message) {
        super(message);
    }
}
