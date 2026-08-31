package cl.antucayen.model.entity;

public class ItemFactura {
    private int    idItem;
    private int    idFactura;
    private String sku;                 // puede ser null si no tiene equivalencia (Observado)
    private int    cantidadFacturada;
    private int    precioUnitarioCompra;
    private String estadoItem;          // Válido, Observado, No Procesado
    private String codigoInternoOriginal; // código leído del archivo/proveedor, no persistido (solo UI)

    public ItemFactura() {}

    public ItemFactura(int idItem, int idFactura, String sku, int cantidadFacturada,
                       int precioUnitarioCompra, String estadoItem) {
        this.idItem               = idItem;
        this.idFactura             = idFactura;
        this.sku                   = sku;
        this.cantidadFacturada     = cantidadFacturada;
        this.precioUnitarioCompra  = precioUnitarioCompra;
        this.estadoItem            = estadoItem;
    }

    public int    getIdItem()               { return idItem; }
    public int    getIdFactura()            { return idFactura; }
    public String getSku()                  { return sku; }
    public int    getCantidadFacturada()    { return cantidadFacturada; }
    public int    getPrecioUnitarioCompra() { return precioUnitarioCompra; }
    public String getEstadoItem()           { return estadoItem; }
    public String getCodigoInternoOriginal(){ return codigoInternoOriginal; }

    public void setIdItem(int idItem)                          { this.idItem = idItem; }
    public void setIdFactura(int idFactura)                    { this.idFactura = idFactura; }
    public void setSku(String sku)                             { this.sku = sku; }
    public void setCantidadFacturada(int cantidadFacturada)    { this.cantidadFacturada = cantidadFacturada; }
    public void setPrecioUnitarioCompra(int precioUnitarioCompra) { this.precioUnitarioCompra = precioUnitarioCompra; }
    public void setEstadoItem(String estadoItem)               { this.estadoItem = estadoItem; }
    public void setCodigoInternoOriginal(String codigo)        { this.codigoInternoOriginal = codigo; }
}
