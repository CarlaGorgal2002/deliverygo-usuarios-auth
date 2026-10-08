package org.deliverygo.repository;

import org.example.model.Prueba;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PruebaRepository extends JpaRepository<Prueba, Long> {

    public Prueba findByTextContainingIgnoreCase(String text);

}
