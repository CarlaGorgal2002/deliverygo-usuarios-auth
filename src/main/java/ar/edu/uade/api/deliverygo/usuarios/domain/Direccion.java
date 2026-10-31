package ar.edu.uade.api.deliverygo.usuarios.domain;

import jakarta.persistence.*;

// ENT-03. REL-01: muchas direcciones pertenecen a un usuario. La baja es lógica (activa = false)
// para conservar las referencias históricas.
@Entity
@Table(name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String calle;

    // Texto, no entero: admite "S/N" o numeración alfanumérica.
    @Column(nullable = false, length = 10)
    private String numero;

    @Column(length = 10)
    private String piso;

    @Column(length = 10)
    private String departamento;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Column(nullable = false, length = 80)
    private String provincia;

    @Column(nullable = false, length = 10)
    private String codigoPostal;

    // Previsto en el DER; todavía no forma parte del contrato CORE ni de los DTO.
    @Column(length = 150)
    private String referencia;

    @Column(nullable = false)
    private boolean predeterminada;

    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    protected Direccion() {}

    public Direccion(Usuario usuario, String calle, String numero, String piso, String departamento,
                     String ciudad, String provincia, String codigoPostal) {
        this.usuario = usuario;
        actualizarDatos(calle, numero, piso, departamento, ciudad, provincia, codigoPostal);
    }

    // Reemplaza los datos editables; no toca predeterminada ni activa.
    public void actualizarDatos(String calle, String numero, String piso, String departamento,
                                String ciudad, String provincia, String codigoPostal) {
        this.calle = calle;
        this.numero = numero;
        this.piso = piso;
        this.departamento = departamento;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.codigoPostal = codigoPostal;
    }

    public Long getId() {
        return id;
    }

    public String getCalle() {
        return calle;
    }

    public String getNumero() {
        return numero;
    }

    public String getPiso() {
        return piso;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getProvincia() {
        return provincia;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public String getReferencia() {
        return referencia;
    }

    public boolean isPredeterminada() {
        return predeterminada;
    }

    public void setPredeterminada(boolean predeterminada) {
        this.predeterminada = predeterminada;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
