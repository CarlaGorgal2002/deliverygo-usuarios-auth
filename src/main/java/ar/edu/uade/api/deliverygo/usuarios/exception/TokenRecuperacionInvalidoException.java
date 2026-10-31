package ar.edu.uade.api.deliverygo.usuarios.exception;

// CU-08 (RN-13): el token no existe, venció, ya se usó o su cuenta está inactiva. No se distingue la causa.
public class TokenRecuperacionInvalidoException extends RuntimeException {
    public TokenRecuperacionInvalidoException(String message) {
        super(message);
    }
}
