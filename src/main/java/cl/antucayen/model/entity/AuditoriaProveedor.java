package cl.antucayen.model.entity;

import java.time.LocalDateTime;

public class AuditoriaProveedor {
    private int            idAuditoria;
    private int            idProveedor;
    private int            idUsuario;
    private LocalDateTime  fechaHora;
    private String         campoModificado;
    private String         valorAnterior;
    private String         valorNuevo;
    private String         nombreUsuario; // se carga con JOIN, solo lectura

    public AuditoriaProveedor() {}

    public AuditoriaProveedor(int idProveedor, int idUsuario, String campoModificado,
                              String valorAnterior, String valorNuevo) {
        this.idProveedor     = idProveedor;
        this.idUsuario       = idUsuario;
        this.campoModificado = campoModificado;
        this.valorAnterior   = valorAnterior;
        this.valorNuevo      = valorNuevo;
    }

    public AuditoriaProveedor(int idAuditoria, int idProveedor, int idUsuario,
                              LocalDateTime fechaHora, String campoModificado,
                              String valorAnterior, String valorNuevo, String nombreUsuario) {
        this.idAuditoria     = idAuditoria;
        this.idProveedor     = idProveedor;
        this.idUsuario       = idUsuario;
        this.fechaHora       = fechaHora;
        this.campoModificado = campoModificado;
        this.valorAnterior   = valorAnterior;
        this.valorNuevo      = valorNuevo;
        this.nombreUsuario   = nombreUsuario;
    }

    public int           getIdAuditoria()     { return idAuditoria; }
    public int           getIdProveedor()     { return idProveedor; }
    public int           getIdUsuario()       { return idUsuario; }
    public LocalDateTime getFechaHora()       { return fechaHora; }
    public String        getCampoModificado() { return campoModificado; }
    public String        getValorAnterior()   { return valorAnterior; }
    public String        getValorNuevo()      { return valorNuevo; }
    public String        getNombreUsuario()   { return nombreUsuario; }

    public void setIdAuditoria(int idAuditoria)         { this.idAuditoria = idAuditoria; }
    public void setIdProveedor(int idProveedor)         { this.idProveedor = idProveedor; }
    public void setIdUsuario(int idUsuario)             { this.idUsuario = idUsuario; }
    public void setFechaHora(LocalDateTime fechaHora)   { this.fechaHora = fechaHora; }
    public void setCampoModificado(String campo)        { this.campoModificado = campo; }
    public void setValorAnterior(String valorAnterior)  { this.valorAnterior = valorAnterior; }
    public void setValorNuevo(String valorNuevo)        { this.valorNuevo = valorNuevo; }
    public void setNombreUsuario(String nombreUsuario)  { this.nombreUsuario = nombreUsuario; }
}
