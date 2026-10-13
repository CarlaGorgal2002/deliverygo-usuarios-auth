package org.deliverygo.repository;

import org.deliverygo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    public Optional<Usuario> findByNombre(String nombre);

    public Optional<Usuario> findById(Long id);

    public Optional<Usuario> findByEmail(String email);

    public boolean existsByEmail(String email);

}
