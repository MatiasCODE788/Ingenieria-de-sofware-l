package cl.antucayen.model.entity;

public class ItemFactura {
    private int    idItem;
    private int    idFactura;
    private String codigoInternoProveedor; // código leído del proveedor, antes de resolver el SKU
    private String sku;                 // null hasta que se resuelva la equivalencia
    private int    cantidadFacturada;
    private int    precioUnitarioCompra;
    private String estadoItem;          // Válido, Observado, No Procesado

    public ItemFactura() {}

    public ItemFactura(int idItem, int idFactura, String codigoInternoProveedor, String sku,
                       int cantidadFacturada, int precioUnitarioCompra, String estadoItem) {
        this.idItem                 = idItem;
        this.idFactura              = idFactura;
        this.codigoInternoProveedor = codigoInternoProveedor;
        this.sku                    = sku;
        this.cantidadFacturada      = cantidadFacturada;
        this.precioUnitarioCompra   = precioUnitarioCompra;
        this.estadoItem             = estadoItem;
    }

    public int    getIdItem()                { return idItem; }
    public int    getIdFactura()             { return idFactura; }
    public String getCodigoInternoProveedor(){ return codigoInternoProveedor; }
    public String getSku()                   { return sku; }
    public int    getCantidadFacturada()     { return cantidadFacturada; }
    public int    getPrecioUnitarioCompra()  { return precioUnitarioCompra; }
    public String getEstadoItem()            { return estadoItem; }

    public void setIdItem(int idItem)                                { this.idItem = idItem; }
    public void setIdFactura(int idFactura)                          { this.idFactura = idFactura; }
    public void setCodigoInternoProveedor(String codigo)             { this.codigoInternoProveedor = codigo; }
    public void setSku(String sku)                                   { this.sku = sku; }
    public void setCantidadFacturada(int cantidadFacturada)          { this.cantidadFacturada = cantidadFacturada; }
    public void setPrecioUnitarioCompra(int precioUnitarioCompra)    { this.precioUnitarioCompra = precioUnitarioCompra; }
    public void setEstadoItem(String estadoItem)                     { this.estadoItem = estadoItem; }
}