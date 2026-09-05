package cl.antucayen.model.entity;

public class ItemVenta {
    private int    idItem;
    private int    idVenta;
    private String sku;
    private int    cantidad;
    private int    precioUnitarioVenta;
    private int    subtotal;
    private String nombreProducto; // se carga con JOIN, solo lectura

    public ItemVenta() {}

    public ItemVenta(int idItem, int idVenta, String sku, int cantidad,
                     int precioUnitarioVenta, int subtotal, String nombreProducto) {
        this.idItem              = idItem;
        this.idVenta             = idVenta;
        this.sku                 = sku;
        this.cantidad            = cantidad;
        this.precioUnitarioVenta = precioUnitarioVenta;
        this.subtotal            = subtotal;
        this.nombreProducto      = nombreProducto;
    }

    public int    getIdItem()              { return idItem; }
    public int    getIdVenta()             { return idVenta; }
    public String getSku()                 { return sku; }
    public int    getCantidad()            { return cantidad; }
    public int    getPrecioUnitarioVenta() { return precioUnitarioVenta; }
    public int    getSubtotal()            { return subtotal; }
    public String getNombreProducto()      { return nombreProducto; }

    public void setIdItem(int idItem)                           { this.idItem = idItem; }
    public void setIdVenta(int idVenta)                         { this.idVenta = idVenta; }
    public void setSku(String sku)                              { this.sku = sku; }
    public void setCantidad(int cantidad)                       { this.cantidad = cantidad; }
    public void setPrecioUnitarioVenta(int precioUnitarioVenta) { this.precioUnitarioVenta = precioUnitarioVenta; }
    public void setSubtotal(int subtotal)                       { this.subtotal = subtotal; }
    public void setNombreProducto(String nombreProducto)        { this.nombreProducto = nombreProducto; }
}