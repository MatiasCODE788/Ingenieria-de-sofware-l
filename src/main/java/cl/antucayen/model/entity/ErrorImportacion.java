package cl.antucayen.model.entity;

/**
 * Error funcional detectado durante una importación o procesamiento.
 * Mantiene separados el origen (fila/ítem), la columna afectada, el código/SKU
 * involucrado y la descripción, para satisfacer los reportes de validación.
 */
public class ErrorImportacion {
    private final int fila;
    private final String columna;
    private final String codigoSku;
    private final String descripcion;

    public ErrorImportacion(int fila, String columna, String descripcion) {
        this(fila, columna, "", descripcion);
    }

    public ErrorImportacion(int fila, String columna, String codigoSku, String descripcion) {
        this.fila = fila;
        this.columna = columna == null ? "" : columna;
        this.codigoSku = codigoSku == null ? "" : codigoSku;
        this.descripcion = descripcion == null ? "" : descripcion;
    }

    public int getFila() { return fila; }
    public String getColumna() { return columna; }
    public String getCodigoSku() { return codigoSku; }
    public String getDescripcion() { return descripcion; }
}
