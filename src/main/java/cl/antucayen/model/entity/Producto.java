package cl.antucayen.model.entity;

public class Producto {
    private String sku;
    private String nombre;
    private String codigoBarras;
    private String unidadMedida;
    private int    stockActual;
    private String estado;

    public Producto() {}

    public Producto(String sku, String nombre, String codigoBarras,
                    String unidadMedida, int stockActual, String estado) {
        this.sku          = sku;
        this.nombre       = nombre;
        this.codigoBarras = codigoBarras;
        this.unidadMedida = unidadMedida;
        this.stockActual  = stockActual;
        this.estado       = estado;
    }

    public String getSku()           { return sku; }
    public String getNombre()        { return nombre; }
    public String getCodigoBarras()  { return codigoBarras; }
    public String getUnidadMedida()  { return unidadMedida; }
    public int    getStockActual()   { return stockActual; }
    public String getEstado()        { return estado; }

    public void setSku(String sku)                  { this.sku = sku; }
    public void setNombre(String nombre)            { this.nombre = nombre; }
    public void setCodigoBarras(String codigoBarras){ this.codigoBarras = codigoBarras; }
    public void setUnidadMedida(String unidadMedida){ this.unidadMedida = unidadMedida; }
    public void setStockActual(int stockActual)     { this.stockActual = stockActual; }
    public void setEstado(String estado)            { this.estado = estado; }

    @Override
    public String toString() {
        return sku + " — " + nombre;
    }
}