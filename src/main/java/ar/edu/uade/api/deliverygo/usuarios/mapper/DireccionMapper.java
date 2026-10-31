package ar.edu.uade.api.deliverygo.usuarios.mapper;

import ar.edu.uade.api.deliverygo.usuarios.domain.Direccion;
import ar.edu.uade.api.deliverygo.usuarios.dto.DireccionResponse;
import org.springframework.stereotype.Component;

@Component
public class DireccionMapper {

    public DireccionResponse toResponse(Direccion direccion) {
        return new DireccionResponse(
                direccion.getId(),
                direccion.getCalle(),
                direccion.getNumero(),
                direccion.getPiso(),
                direccion.getDepartamento(),
                direccion.getCiudad(),
                direccion.getProvincia(),
                direccion.getCodigoPostal(),
                direccion.isPredeterminada());
    }
}
