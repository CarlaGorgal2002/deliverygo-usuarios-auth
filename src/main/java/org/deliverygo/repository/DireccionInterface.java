package org.deliverygo.repository;

import org.deliverygo.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionInterface extends JpaRepository<Direccion, Long> {
}
