package org.deliverygo.repository;

import org.deliverygo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    public Usuario findByNombreContainingIgnoreCase(String nombre);

    public Usuario findByEmailContainingIgnoreCase(String email);


}
