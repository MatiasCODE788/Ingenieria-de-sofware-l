package cl.antucayen.model.entity;

import java.time.LocalDate;

public class Factura {
    private int       idFactura;
    private String    nroFactura;
    private LocalDate fechaEmision;
    private String    rutProveedor;
    private String    estado;

    public Factura() {}

    public Factura(int idFactura, String nroFactura, LocalDate fechaEmision,
                   String rutProveedor, String estado) {
        this.idFactura    = idFactura;
        this.nroFactura   = nroFactura;
        this.fechaEmision = fechaEmision;
        this.rutProveedor = rutProveedor;
        this.estado       = estado;
    }

    public int       getIdFactura()    { return idFactura; }
    public String    getNroFactura()   { return nroFactura; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public String    getRutProveedor() { return rutProveedor; }
    public String    getEstado()       { return estado; }

    public void setIdFactura(int idFactura)          { this.idFactura = idFactura; }
    public void setNroFactura(String nroFactura)     { this.nroFactura = nroFactura; }
    public void setFechaEmision(LocalDate fecha)     { this.fechaEmision = fecha; }
    public void setRutProveedor(String rutProveedor) { this.rutProveedor = rutProveedor; }
    public void setEstado(String estado)             { this.estado = estado; }
}