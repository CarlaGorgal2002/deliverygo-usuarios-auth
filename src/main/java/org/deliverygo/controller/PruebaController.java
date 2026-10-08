package org.deliverygo.controller;

import org.example.dto.PruebaDto;
import org.example.services.PruebaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prueba")
public class PruebaController {
    private final PruebaService pruebaService;

    public PruebaController(PruebaService pruebaService) {
        this. pruebaService = pruebaService;
    }


    @PostMapping
    public ResponseEntity<PruebaDto> crearComercio(@RequestBody PruebaDto pruebaDto ) {

        pruebaService.crearPrueba(pruebaDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(pruebaDto);
    }
}
