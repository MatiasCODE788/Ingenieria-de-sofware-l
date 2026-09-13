package cl.antucayen.model.entity;

import java.time.LocalDateTime;

public class MovimientoInventario {
    private int idMovimiento;
    private String sku;
    private int idUsuario;
    private Integer idFactura;
    private Integer idItemFactura;
    private Integer idVenta;
    private Integer idAjuste;
    private String tipoMovimiento;
    private LocalDateTime fechaHora;
    private int stockAnterior;
    private int cantidadAplicada;
    private int stockResultante;
    private String modalidadAjuste;
    private boolean vigente = true;
    private String nombreProducto;
    private String nombreUsuario;
    private String usernameUsuario;

    public MovimientoInventario() {}

    public int getIdMovimiento() { return idMovimiento; }
    public String getSku() { return sku; }
    public int getIdUsuario() { return idUsuario; }
    public Integer getIdFactura() { return idFactura; }
    public Integer getIdItemFactura() { return idItemFactura; }
    public Integer getIdVenta() { return idVenta; }
    public Integer getIdAjuste() { return idAjuste; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public int getStockAnterior() { return stockAnterior; }
    public int getCantidadAplicada() { return cantidadAplicada; }
    public int getStockResultante() { return stockResultante; }
    public String getModalidadAjuste() { return modalidadAjuste; }
    public boolean isVigente() { return vigente; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getUsernameUsuario() { return usernameUsuario; }

    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public void setSku(String sku) { this.sku = sku; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setIdFactura(Integer idFactura) { this.idFactura = idFactura; }
    public void setIdItemFactura(Integer idItemFactura) { this.idItemFactura = idItemFactura; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }
    public void setIdAjuste(Integer idAjuste) { this.idAjuste = idAjuste; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setStockAnterior(int stockAnterior) { this.stockAnterior = stockAnterior; }
    public void setCantidadAplicada(int cantidadAplicada) { this.cantidadAplicada = cantidadAplicada; }
    public void setStockResultante(int stockResultante) { this.stockResultante = stockResultante; }
    public void setModalidadAjuste(String modalidadAjuste) { this.modalidadAjuste = modalidadAjuste; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public void setUsernameUsuario(String usernameUsuario) { this.usernameUsuario = usernameUsuario; }
}
