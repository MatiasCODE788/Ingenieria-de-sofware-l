package cl.antucayen.model.entity;

public class Producto {
    private String sku;
    private String nombre;
    private String categoria;
    private String unidadMedida;
    private double precioCompra;
    private double precioVenta;
    private int stockActual;

    public Producto() {}

    public Producto(String sku, String nombre, String categoria,
                    String unidadMedida, double precioCompra,
                    double precioVenta, int stockActual) {
        this.sku          = sku;
        this.nombre       = nombre;
        this.categoria    = categoria;
        this.unidadMedida = unidadMedida;
        this.precioCompra = precioCompra;
        this.precioVenta  = precioVenta;
        this.stockActual  = stockActual;
    }

    public String getSku()            { return sku; }
    public String getNombre()         { return nombre; }
    public String getCategoria()      { return categoria; }
    public String getUnidadMedida()   { return unidadMedida; }
    public double getPrecioCompra()   { return precioCompra; }
    public double getPrecioVenta()    { return precioVenta; }
    public int    getStockActual()    { return stockActual; }

    public void setSku(String sku)                  { this.sku = sku; }
    public void setNombre(String nombre)            { this.nombre = nombre; }
    public void setCategoria(String categoria)      { this.categoria = categoria; }
    public void setUnidadMedida(String unidadMedida){ this.unidadMedida = unidadMedida; }
    public void setPrecioCompra(double precioCompra){ this.precioCompra = precioCompra; }
    public void setPrecioVenta(double precioVenta)  { this.precioVenta = precioVenta; }
    public void setStockActual(int stockActual)     { this.stockActual = stockActual; }

    @Override
    public String toString() {
        return "Producto{sku='" + sku + "', nombre='" + nombre + "', stock=" + stockActual + "}";
    }
}