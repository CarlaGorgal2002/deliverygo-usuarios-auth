package ar.edu.uade.api.deliverygo.usuarios.repository;

import ar.edu.uade.api.deliverygo.usuarios.domain.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    // Deja sin efecto los tokens vigentes de un usuario cuando se pide uno nuevo o se restablece la clave.
    @Modifying(flushAutomatically = true)
    @Query("update TokenRecuperacion t set t.usadoEn = :ahora where t.usuario.id = :usuarioId and t.usadoEn is null")
    int invalidarVigentes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
