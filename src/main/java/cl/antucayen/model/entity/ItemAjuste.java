package cl.antucayen.model.entity;

public class ItemAjuste {
    private int idItemAjuste;
    private int idAjuste;
    private String sku;
    private int cantidadAplicada;
    private int stockAnterior;
    private int stockResultante;

    public ItemAjuste() {}

    public ItemAjuste(int idItemAjuste, int idAjuste, String sku,
                      int cantidadAplicada, int stockAnterior, int stockResultante) {
        this.idItemAjuste = idItemAjuste;
        this.idAjuste = idAjuste;
        this.sku = sku;
        this.cantidadAplicada = cantidadAplicada;
        this.stockAnterior = stockAnterior;
        this.stockResultante = stockResultante;
    }

    public int getIdItemAjuste() { return idItemAjuste; }
    public int getIdAjuste() { return idAjuste; }
    public String getSku() { return sku; }
    public int getCantidadAplicada() { return cantidadAplicada; }
    public int getStockAnterior() { return stockAnterior; }
    public int getStockResultante() { return stockResultante; }

    public void setIdItemAjuste(int idItemAjuste) { this.idItemAjuste = idItemAjuste; }
    public void setIdAjuste(int idAjuste) { this.idAjuste = idAjuste; }
    public void setSku(String sku) { this.sku = sku; }
    public void setCantidadAplicada(int cantidadAplicada) { this.cantidadAplicada = cantidadAplicada; }
    public void setStockAnterior(int stockAnterior) { this.stockAnterior = stockAnterior; }
    public void setStockResultante(int stockResultante) { this.stockResultante = stockResultante; }
}
