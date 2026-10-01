package cl.antucayen.model.entity;

import java.util.List;

/** DTO inmutable de presentación para RF-72. */
public record ComprobanteVenta(
        Venta venta,
        List<ItemVenta> items,
        List<PagoVenta> pagosAplicados,
        int montoRecibido,
        int vuelto
) {
    public ComprobanteVenta {
        items = items == null ? List.of() : List.copyOf(items);
        pagosAplicados = pagosAplicados == null ? List.of() : List.copyOf(pagosAplicados);
    }
}
