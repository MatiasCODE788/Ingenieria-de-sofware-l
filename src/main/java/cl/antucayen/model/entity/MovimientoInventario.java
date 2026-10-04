package cl.antucayen.model.entity;

import java.time.LocalDateTime;

/**
 * Movimiento auditable del kardex. cantidadAplicada siempre representa un delta
 * firmado: positivo aumenta stock y negativo lo disminuye.
 */
public class MovimientoInventario {
    private int idMovimiento;
    private String sku;
    private int idUsuario;
    private Integer idItemFactura;
    private Integer idItemVenta;
    private Integer idAjuste;
    private String tipoMovimiento;
    private LocalDateTime fechaHora;
    private int stockAnterior;
    private int cantidadAplicada;
    private int stockResultante;
    private String motivo;
    private boolean vigente = true;

    // Campos derivados de JOIN; no se persisten desde esta entidad.
    private String nombreProducto;
    private String nombreUsuario;
    private String usernameUsuario;
    private Integer idFacturaOrigen;

    public MovimientoInventario() {}

    public int getIdMovimiento() { return idMovimiento; }
    public String getSku() { return sku; }
    public int getIdUsuario() { return idUsuario; }
    public Integer getIdItemFactura() { return idItemFactura; }
    public Integer getIdItemVenta() { return idItemVenta; }
    public Integer getIdAjuste() { return idAjuste; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public int getStockAnterior() { return stockAnterior; }
    public int getCantidadAplicada() { return cantidadAplicada; }
    public int getStockResultante() { return stockResultante; }
    public String getMotivo() { return motivo; }
    public boolean isVigente() { return vigente; }
    public String getNombreProducto() { return nombreProducto; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getUsernameUsuario() { return usernameUsuario; }
    public Integer getIdFacturaOrigen() { return idFacturaOrigen; }

    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    public void setSku(String sku) { this.sku = sku; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public void setIdItemFactura(Integer idItemFactura) { this.idItemFactura = idItemFactura; }
    public void setIdItemVenta(Integer idItemVenta) { this.idItemVenta = idItemVenta; }
    public void setIdAjuste(Integer idAjuste) { this.idAjuste = idAjuste; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setStockAnterior(int stockAnterior) { this.stockAnterior = stockAnterior; }
    public void setCantidadAplicada(int cantidadAplicada) { this.cantidadAplicada = cantidadAplicada; }
    public void setStockResultante(int stockResultante) { this.stockResultante = stockResultante; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public void setUsernameUsuario(String usernameUsuario) { this.usernameUsuario = usernameUsuario; }
    public void setIdFacturaOrigen(Integer idFacturaOrigen) { this.idFacturaOrigen = idFacturaOrigen; }
}
