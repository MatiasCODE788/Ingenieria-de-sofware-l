package cl.antucayen.model.service;

import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.PagoVentaDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.model.entity.ComprobanteVenta;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.security.Autorizacion;

import java.sql.SQLException;

/** RF-72: reconstruye el comprobante a partir de la venta persistida. */
public class ServicioComprobanteVenta {

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ItemVentaDAO itemVentaDAO = new ItemVentaDAO();
    private final PagoVentaDAO pagoVentaDAO = new PagoVentaDAO();

    public ComprobanteVenta obtener(int idVenta, int montoRecibido, int vuelto) throws SQLException {
        Autorizacion.verificarPuntoVenta();
        if (idVenta <= 0) throw new IllegalArgumentException("ID de venta inválido");
        if (montoRecibido < 0 || vuelto < 0) {
            throw new IllegalArgumentException("Los montos del comprobante no pueden ser negativos");
        }
        Venta venta = ventaDAO.buscarPorId(idVenta);
        if (venta == null) throw new IllegalArgumentException("No existe la venta #" + idVenta);
        if (montoRecibido < venta.getMontoTotal()) {
            throw new IllegalArgumentException("El monto recibido no puede ser menor al total de la venta");
        }
        if (montoRecibido - venta.getMontoTotal() != vuelto) {
            throw new IllegalArgumentException("El vuelto no coincide con el monto recibido y el total");
        }
        return new ComprobanteVenta(
                venta,
                itemVentaDAO.listarPorVenta(idVenta),
                pagoVentaDAO.listarPorVenta(idVenta),
                montoRecibido,
                vuelto);
    }
}
