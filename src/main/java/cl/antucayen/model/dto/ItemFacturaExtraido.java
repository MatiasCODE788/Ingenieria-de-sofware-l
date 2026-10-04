package cl.antucayen.model.dto;

/**
 * Ítem detectado durante el análisis de una factura digital antes de su
 * persistencia como ItemFactura. La vista y el servicio comparten este DTO
 * sin acoplar la interfaz Swing a una clase interna del servicio OCR.
 */
public record ItemFacturaExtraido(
        String codigoInterno,
        String descripcion,
        int cantidad,
        int precioUnitario,
        String estado) {
}
