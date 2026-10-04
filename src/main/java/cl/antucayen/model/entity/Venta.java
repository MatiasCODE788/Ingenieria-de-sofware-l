package cl.antucayen.model.entity;

import java.time.LocalDateTime;

/** Cabecera persistente de una operación de Punto de Venta. */
public class Venta {
    private int idVenta;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaConfirmacion;
    private int idUsuario;
    private String medioPago;
    private int montoTotal;
    private int montoRecibido;
    private int vuelto;
    private String estado;
    private String nombreUsuario;

    public Venta() {}

    public Venta(int idVenta,
                 LocalDateTime fechaInicio,
                 LocalDateTime fechaConfirmacion,
                 int idUsuario,
                 String medioPago,
                 int montoTotal,
                 int montoRecibido,
                 int vuelto,
                 String estado,
                 String nombreUsuario) {
        this.idVenta = idVenta;
        this.fechaInicio = fechaInicio;
        this.fechaConfirmacion = fechaConfirmacion;
        this.idUsuario = idUsuario;
        this.medioPago = medioPago;
        this.montoTotal = montoTotal;
        this.montoRecibido = montoRecibido;
        this.vuelto = vuelto;
        this.estado = estado;
        this.nombreUsuario = nombreUsuario;
    }

    public int getIdVenta() { return idVenta; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }

    /**
     * Compatibilidad de presentación: para una venta cerrada devuelve la hora
     * de confirmación; para una venta en curso devuelve la hora de inicio.
     */
    public LocalDateTime getFechaHora() {
        return fechaConfirmacion != null ? fechaConfirmacion : fechaInicio;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getMedioPago() { return medioPago; }
    public int getMontoTotal() { return montoTotal; }
    public int getMontoRecibido() { return montoRecibido; }
    public int getVuelto() { return vuelto; }
    public String getEstado() { return estado; }
    public String getNombreUsuario() { return nombreUsuario; }

    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
    public void setMontoTotal(int montoTotal) { this.montoTotal = montoTotal; }
    public void setMontoRecibido(int montoRecibido) { this.montoRecibido = montoRecibido; }
    public void setVuelto(int vuelto) { this.vuelto = vuelto; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    @Override
    public String toString() {
        return "Venta #" + idVenta + " — " + estado;
    }
}
