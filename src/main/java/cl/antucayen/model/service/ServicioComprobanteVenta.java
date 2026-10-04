package cl.antucayen.model.service;

import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.PagoVentaDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.model.domain.EstadoVenta;
import cl.antucayen.model.entity.ComprobanteVenta;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.security.SesionActual;

import java.sql.SQLException;

/** RF-72: reconstruye íntegramente el comprobante desde la base de datos. */
public class ServicioComprobanteVenta {

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ItemVentaDAO itemVentaDAO = new ItemVentaDAO();
    private final PagoVentaDAO pagoVentaDAO = new PagoVentaDAO();

    public ComprobanteVenta obtener(int idVenta) throws SQLException {
        Autorizacion.verificarPuntoVenta();
        if (idVenta <= 0) throw new IllegalArgumentException("ID de venta inválido");

        Venta venta = ventaDAO.buscarPorId(idVenta);
        if (venta == null) throw new IllegalArgumentException("No existe la venta #" + idVenta);
        if (!EstadoVenta.PAGADA.coincide(venta.getEstado())
                && !EstadoVenta.ANULADA.coincide(venta.getEstado())) {
            throw new IllegalStateException("La venta aún no posee un comprobante definitivo");
        }
        if (SesionActual.esCajero()
                && venta.getIdUsuario() != SesionActual.getUsuario().getIdUsuario()) {
            throw new SecurityException("El Cajero solo puede consultar comprobantes de sus propias ventas");
        }
        if (venta.getMontoRecibido() < venta.getMontoTotal()) {
            throw new IllegalStateException("La venta persistida contiene un monto recibido inconsistente");
        }
        if (venta.getMontoRecibido() - venta.getMontoTotal() != venta.getVuelto()) {
            throw new IllegalStateException("La venta persistida contiene un vuelto inconsistente");
        }

        return new ComprobanteVenta(
                venta,
                itemVentaDAO.listarPorVenta(idVenta),
                pagoVentaDAO.listarPorVenta(idVenta),
                venta.getMontoRecibido(),
                venta.getVuelto());
    }
}
