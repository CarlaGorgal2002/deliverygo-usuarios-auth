package ar.edu.uade.api.deliverygo.usuarios.mapper;

import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class UsuarioMapper {

    // fechaAlta se guarda en UTC (DC-04), por eso se le agrega el offset UTC sin convertir la hora.
    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol().getNombre(),
                usuario.getEstado(),
                usuario.getFechaAlta().atOffset(ZoneOffset.UTC));
    }
}
