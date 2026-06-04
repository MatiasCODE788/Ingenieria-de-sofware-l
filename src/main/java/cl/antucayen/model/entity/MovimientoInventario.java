package cl.antucayen.model.entity;

import java.time.LocalDateTime;

public class MovimientoInventario {
    private int           idMovimiento;
    private String        sku;
    private int           idUsuario;
    private Integer       idFactura;
    private String        tipoMovimiento;
    private LocalDateTime fechaHora;
    private int           stockAnterior;
    private int           cantidadAplicada;
    private int           stockResultante;
    private String        modalidadAjuste;
    private String        nombreProducto; // se carga con JOIN

    public MovimientoInventario() {}

    public MovimientoInventario(int idMovimiento, String sku, int idUsuario,
                                Integer idFactura, String tipoMovimiento,
                                LocalDateTime fechaHora, int stockAnterior,
                                int cantidadAplicada, int stockResultante,
                                String modalidadAjuste, String nombreProducto) {
        this.idMovimiento     = idMovimiento;
        this.sku              = sku;
        this.idUsuario        = idUsuario;
        this.idFactura        = idFactura;
        this.tipoMovimiento   = tipoMovimiento;
        this.fechaHora        = fechaHora;
        this.stockAnterior    = stockAnterior;
        this.cantidadAplicada = cantidadAplicada;
        this.stockResultante  = stockResultante;
        this.modalidadAjuste  = modalidadAjuste;
        this.nombreProducto   = nombreProducto;
    }

    public int           getIdMovimiento()     { return idMovimiento; }
    public String        getSku()              { return sku; }
    public int           getIdUsuario()        { return idUsuario; }
    public Integer       getIdFactura()        { return idFactura; }
    public String        getTipoMovimiento()   { return tipoMovimiento; }
    public LocalDateTime getFechaHora()        { return fechaHora; }
    public int           getStockAnterior()    { return stockAnterior; }
    public int           getCantidadAplicada() { return cantidadAplicada; }
    public int           getStockResultante()  { return stockResultante; }
    public String        getModalidadAjuste()  { return modalidadAjuste; }
    public String        getNombreProducto()   { return nombreProducto; }

    public void setIdMovimiento(int idMovimiento)          { this.idMovimiento = idMovimiento; }
    public void setSku(String sku)                         { this.sku = sku; }
    public void setIdUsuario(int idUsuario)                { this.idUsuario = idUsuario; }
    public void setIdFactura(Integer idFactura)            { this.idFactura = idFactura; }
    public void setTipoMovimiento(String tipoMovimiento)   { this.tipoMovimiento = tipoMovimiento; }
    public void setFechaHora(LocalDateTime fechaHora)      { this.fechaHora = fechaHora; }
    public void setStockAnterior(int stockAnterior)        { this.stockAnterior = stockAnterior; }
    public void setCantidadAplicada(int cantidadAplicada)  { this.cantidadAplicada = cantidadAplicada; }
    public void setStockResultante(int stockResultante)    { this.stockResultante = stockResultante; }
    public void setModalidadAjuste(String modalidadAjuste) { this.modalidadAjuste = modalidadAjuste; }
    public void setNombreProducto(String nombreProducto)   { this.nombreProducto = nombreProducto; }
}