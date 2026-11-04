package org.deliverygo.mapper;

import org.deliverygo.dto.DireccionResponse;
import org.deliverygo.model.Direccion;

public class DireccionMapper {
    private DireccionMapper() {}

    public static DireccionResponse toResponse(Direccion d) {
        return new DireccionResponse(
                d.getId(),
                d.getCalle(),
                d.getNumero(),
                d.getDepartamento(),
                d.getPiso(),
                d.getCiudad(),
                d.getProvincia(),
                d.getCodigoPostal(),
                d.getReferencia(),
                d.isPredeterminada()
        );
    }
}
