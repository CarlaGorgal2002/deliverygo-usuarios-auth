package org.deliverygo.dto;

import java.time.LocalDateTime;

public class ErrorDto {

    private int status;
    private String mensaje;
    private LocalDateTime fecha;

    public ErrorDto() {
    }

    public ErrorDto(int status, String mensaje) {
        this.status = status;
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
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