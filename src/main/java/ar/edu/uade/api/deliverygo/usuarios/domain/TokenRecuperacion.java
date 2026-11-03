package ar.edu.uade.api.deliverygo.usuarios.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// ENT-04 (CU-08). Token de un solo uso para restablecer la contraseña. Sólo se guarda su hash (RN-13):
// quien lea la base no puede reconstruir un token vigente. Fechas en UTC (DC-04).
@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // SHA-256 en hexadecimal del token entregado al usuario.
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    // Null mientras el token no se usó ni se invalidó.
    private LocalDateTime usadoEn;

    protected TokenRecuperacion() {}

    public TokenRecuperacion(Usuario usuario, String tokenHash, LocalDateTime creadoEn, LocalDateTime expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public LocalDateTime getUsadoEn() {
        return usadoEn;
    }
}
