package org.deliverygo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String calle;

    @Column(nullable = false)
    private Integer  numero;

    // cambiaria del DER de Sring a Integer (Ej: piso: 2)
    private Integer piso;

    private String departamento;

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private String provincia;

    @Column(nullable = false)
    private String codigoPostal;

    private String referencia;

    private boolean predeterminada;

    private boolean activa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Direccion() {}

    public Direccion(String calle, Integer numero, Integer piso, String departamento, String ciudad, String provincia, String codigoPostal, String referencia, boolean predeterminada, boolean activa, Usuario usuario) {
        this.calle = calle;
        this.numero = numero;
        this.piso = piso;
        this.departamento = departamento;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.codigoPostal = codigoPostal;
        this.referencia = referencia;
        this.predeterminada = predeterminada;
        this.activa = activa;
        this.usuario = usuario;
    }
}
