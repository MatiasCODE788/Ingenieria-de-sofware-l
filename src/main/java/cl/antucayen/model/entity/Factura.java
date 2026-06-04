package cl.antucayen.model.entity;

import java.time.LocalDate;

public class Factura {
    private int       idFactura;
    private String    numeroFactura;
    private LocalDate fechaEmision;
    private String    estado;
    private String    rutaArchivoDigital;
    private int       idProveedor;
    private int       idUsuario;
    private String    nombreProveedor; // se carga con JOIN

    public Factura() {}

    public Factura(int idFactura, String numeroFactura, LocalDate fechaEmision,
                   String estado, String rutaArchivoDigital,
                   int idProveedor, int idUsuario, String nombreProveedor) {
        this.idFactura          = idFactura;
        this.numeroFactura      = numeroFactura;
        this.fechaEmision       = fechaEmision;
        this.estado             = estado;
        this.rutaArchivoDigital = rutaArchivoDigital;
        this.idProveedor        = idProveedor;
        this.idUsuario          = idUsuario;
        this.nombreProveedor    = nombreProveedor;
    }

    public int       getIdFactura()          { return idFactura; }
    public String    getNumeroFactura()      { return numeroFactura; }
    public LocalDate getFechaEmision()       { return fechaEmision; }
    public String    getEstado()             { return estado; }
    public String    getRutaArchivoDigital() { return rutaArchivoDigital; }
    public int       getIdProveedor()        { return idProveedor; }
    public int       getIdUsuario()          { return idUsuario; }
    public String    getNombreProveedor()    { return nombreProveedor; }

    public void setIdFactura(int idFactura)                    { this.idFactura = idFactura; }
    public void setNumeroFactura(String numeroFactura)         { this.numeroFactura = numeroFactura; }
    public void setFechaEmision(LocalDate fechaEmision)        { this.fechaEmision = fechaEmision; }
    public void setEstado(String estado)                       { this.estado = estado; }
    public void setRutaArchivoDigital(String ruta)             { this.rutaArchivoDigital = ruta; }
    public void setIdProveedor(int idProveedor)                { this.idProveedor = idProveedor; }
    public void setIdUsuario(int idUsuario)                    { this.idUsuario = idUsuario; }
    public void setNombreProveedor(String nombreProveedor)     { this.nombreProveedor = nombreProveedor; }
}