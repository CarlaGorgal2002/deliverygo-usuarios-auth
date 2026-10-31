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

    // Consumo atómico (RN-13): sólo uno de dos pedidos simultáneos con el mismo token obtiene 1.
    @Modifying(flushAutomatically = true)
    @Query("update TokenRecuperacion t set t.usadoEn = :ahora "
            + "where t.tokenHash = :hash and t.usadoEn is null and t.expiraEn > :ahora")
    int consumir(@Param("hash") String hash, @Param("ahora") LocalDateTime ahora);

    // Deja sin efecto los tokens vigentes de un usuario cuando se pide uno nuevo o se restablece la clave.
    @Modifying(flushAutomatically = true)
    @Query("update TokenRecuperacion t set t.usadoEn = :ahora where t.usuario.id = :usuarioId and t.usadoEn is null")
    int invalidarVigentes(@Param("usuarioId") Long usuarioId, @Param("ahora") LocalDateTime ahora);
}
