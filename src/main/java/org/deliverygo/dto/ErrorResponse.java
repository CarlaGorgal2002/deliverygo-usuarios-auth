package org.deliverygo.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    private int status;
    private String code;
    private String mensaje;
    private LocalDateTime fecha;

    public ErrorResponse() {
    }

    public ErrorResponse(int status,String code, String mensaje) {
        this.status = status;
        this.code = code;
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
    }


    public String  getCodigo() {
        return code;
    }
    public void setCodigo(String code) {
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMensaje() { return mensaje; }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

}