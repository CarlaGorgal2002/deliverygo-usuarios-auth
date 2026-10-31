package ar.edu.uade.api.deliverygo.usuarios.security;

import org.springframework.stereotype.Component;

import java.util.Arrays;

// Decisiones de autorización que los controllers invocan desde @PreAuthorize (RN-05, RN-11, RN-15):
//   @PreAuthorize("@acceso.titular(principal, #id)")
// Se evalúan siempre con el tipo de identidad correcto, así que un módulo que llama a una ruta de usuario
// recibe 403 en lugar de un error interno.
@Component("acceso")
public class Acceso {

    // El solicitante es una persona autenticada y es el titular de la ruta.
    public boolean titular(Object principal, Long id) {
        return principal instanceof UsuarioAutenticado usuario && usuario.id().equals(id);
    }

    // El titular, o un módulo consumidor. Sin nombres, vale cualquier módulo con credencial; con nombres,
    // sólo esos (por ejemplo 'pedidos').
    public boolean titularOModulo(Object principal, Long id, String... modulosPermitidos) {
        if (titular(principal, id)) {
            return true;
        }
        if (principal instanceof ModuloAutenticado modulo) {
            return modulosPermitidos.length == 0 || Arrays.asList(modulosPermitidos).contains(modulo.nombre());
        }
        return false;
    }

    // Sólo un módulo consumidor autenticado.
    public boolean modulo(Object principal) {
        return principal instanceof ModuloAutenticado;
    }

    // Una persona autenticada (no un módulo).
    public boolean usuario(Object principal) {
        return principal instanceof UsuarioAutenticado;
    }

    // Filtro rápido por el rol del token. El servicio vuelve a comprobarlo contra la base (RN-11),
    // porque el rol del token puede estar desactualizado hasta que venza.
    public boolean administrador(Object principal) {
        return principal instanceof UsuarioAutenticado usuario && "ADMIN".equals(usuario.rol());
    }
}
