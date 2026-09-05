package cl.antucayen.model.entity;

import java.time.LocalDateTime;

public class Venta {
    private int            idVenta;
    private LocalDateTime  fechaHora;
    private int            idUsuario;
    private String         medioPago;
    private int            montoTotal;
    private String         estado;
    private String         nombreUsuario;

    public Venta() {}

    public Venta(int idVenta, LocalDateTime fechaHora, int idUsuario,
                 String medioPago, int montoTotal, String estado, String nombreUsuario) {
        this.idVenta       = idVenta;
        this.fechaHora     = fechaHora;
        this.idUsuario     = idUsuario;
        this.medioPago     = medioPago;
        this.montoTotal    = montoTotal;
        this.estado        = estado;
        this.nombreUsuario = nombreUsuario;
    }

    public int           getIdVenta()       { return idVenta; }
    public LocalDateTime getFechaHora()     { return fechaHora; }
    public int           getIdUsuario()     { return idUsuario; }
    public String        getMedioPago()     { return medioPago; }
    public int           getMontoTotal()    { return montoTotal; }
    public String        getEstado()        { return estado; }
    public String        getNombreUsuario() { return nombreUsuario; }

    public void setIdVenta(int idVenta)               { this.idVenta = idVenta; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setIdUsuario(int idUsuario)           { this.idUsuario = idUsuario; }
    public void setMedioPago(String medioPago)        { this.medioPago = medioPago; }
    public void setMontoTotal(int montoTotal)         { this.montoTotal = montoTotal; }
    public void setEstado(String estado)              { this.estado = estado; }
    public void setNombreUsuario(String nombreUsuario){ this.nombreUsuario = nombreUsuario; }

    @Override
    public String toString() {
        return "Venta #" + idVenta + " — " + estado;
    }
}
