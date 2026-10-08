package org.deliverygo.services;

import jakarta.transaction.Transactional;
import org.example.dto.PruebaDto;
import org.example.model.Prueba;
import org.example.repository.PruebaRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class PruebaService {

    private final PruebaRepository pruebaRepository;

    public PruebaService(PruebaRepository pruebaRepository) {

        this.pruebaRepository = pruebaRepository;
    }

    @Transactional
    public PruebaDto crearPrueba(PruebaDto pruebaDto) {
        String text = pruebaDto.getText();

        if(Objects.isNull(text) || text.isEmpty()){
            throw new IllegalArgumentException("El texto no puede ser nulo o blank");
        }

        Prueba pruebaDto2 = new Prueba(text);
        pruebaRepository.save(pruebaDto2);

        return pruebaDto;
    }

}
