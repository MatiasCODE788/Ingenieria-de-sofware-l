package cl.antucayen.model.service;

import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.PagoVentaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.model.dao.ItemVentaDAO.ProductoVendido;
import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ServicioVenta {

    private static final List<String> MEDIOS_PAGO_VALIDOS = List.of("Efectivo", "Débito", "Crédito");

    private final VentaDAO      ventaDAO      = new VentaDAO();
    private final ItemVentaDAO  itemVentaDAO  = new ItemVentaDAO();
    private final PagoVentaDAO  pagoVentaDAO  = new PagoVentaDAO();
    private final ProductoDAO   productoDAO   = new ProductoDAO();
    private final MovimientoInventarioDaoWrapper movWrapper = new MovimientoInventarioDaoWrapper();

    /**
     * Registra una venta que puede pagarse con un único medio de pago o con
     * una combinación de varios (ej.: $5.000 Efectivo + $3.000 Débito). La
     * suma de los pagos debe coincidir exactamente con el total del carrito.
     * Valida stock suficiente de cada ítem ANTES de aplicar cualquier
     * cambio, descuenta stock, y deja registro en movimiento_inventario
     * (tipo 'Salida por venta') vinculado a la venta.
     */
    public int registrar(List<PagoVenta> pagos, List<ItemVenta> items) throws SQLException {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("El carrito está vacío");
        if (pagos == null || pagos.isEmpty())
            throw new IllegalArgumentException("Debes ingresar el monto pagado en al menos un medio de pago");

        for (PagoVenta pago : pagos) {
            if (!MEDIOS_PAGO_VALIDOS.contains(pago.getMedioPago()))
                throw new IllegalArgumentException("Medio de pago no válido: " + pago.getMedioPago());
            if (pago.getMonto() <= 0)
                throw new IllegalArgumentException("El monto de cada pago debe ser mayor a cero");
        }

        // Validar stock disponible de todos los ítems antes de tocar nada
        for (ItemVenta item : items) {
            Producto p = productoDAO.buscarPorSku(item.getSku());
            if (p == null)
                throw new IllegalArgumentException("Producto no encontrado: " + item.getSku());
            if (!"Activo".equals(p.getEstado()))
                throw new IllegalArgumentException("El producto '" + p.getNombre() + "' está inactivo");
            if (p.getStockActual() < item.getCantidad())
                throw new IllegalArgumentException("Stock insuficiente de '" + p.getNombre()
                        + "' (disponible: " + p.getStockActual() + ", solicitado: " + item.getCantidad() + ")");
        }

        int montoTotal  = items.stream().mapToInt(ItemVenta::getSubtotal).sum();
        int montoPagado = pagos.stream().mapToInt(PagoVenta::getMonto).sum();
        if (montoPagado != montoTotal)
            throw new IllegalArgumentException("La suma de los pagos ($" + montoPagado
                    + ") no coincide con el total de la venta ($" + montoTotal + ")");

        String medioPagoVenta = pagos.size() == 1 ? pagos.get(0).getMedioPago() : "Mixto";

        Venta v = new Venta();
        v.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        v.setMedioPago(medioPagoVenta);
        v.setMontoTotal(montoTotal);
        v.setEstado("Pagada");
        int idVenta = ventaDAO.insertar(v);

        for (PagoVenta pago : pagos) {
            pago.setIdVenta(idVenta);
            pagoVentaDAO.insertar(pago);
        }

        for (ItemVenta item : items) {
            item.setIdVenta(idVenta);
            itemVentaDAO.insertar(item);

            Producto p = productoDAO.buscarPorSku(item.getSku());
            int stockAnterior = p.getStockActual();
            int stockResultante = stockAnterior - item.getCantidad();
            productoDAO.actualizarStock(item.getSku(), stockResultante);

            movWrapper.registrarSalidaPorVenta(item.getSku(), item.getCantidad(),
                    stockAnterior, stockResultante, idVenta);
        }
        return idVenta;
    }

    /**
     * Anula una venta ya pagada: devuelve el stock de cada ítem y marca la
     * venta como Anulada. Solo un Administrador puede anular.
     */
    public void anular(int idVenta) throws SQLException {
        if (!SesionActual.esAdministrador())
            throw new SecurityException("Solo un Administrador puede anular una venta");

        Venta v = ventaDAO.buscarPorId(idVenta);
        if (v == null) throw new IllegalArgumentException("La venta no existe");
        if ("Anulada".equals(v.getEstado())) throw new IllegalStateException("La venta ya está anulada");

        List<ItemVenta> items = itemVentaDAO.listarPorVenta(idVenta);
        for (ItemVenta item : items) {
            Producto p = productoDAO.buscarPorSku(item.getSku());
            if (p == null) continue;
            int stockAnterior = p.getStockActual();
            int stockResultante = stockAnterior + item.getCantidad();
            productoDAO.actualizarStock(item.getSku(), stockResultante);
            movWrapper.registrarReversion(item.getSku(), item.getCantidad(),
                    stockAnterior, stockResultante, idVenta);
        }
        ventaDAO.actualizarEstado(idVenta, "Anulada");
    }

    public List<Venta> listarDelDia() throws SQLException { return ventaDAO.listarDelDia(); }

    public List<ItemVenta> obtenerItems(int idVenta) throws SQLException {
        return itemVentaDAO.listarPorVenta(idVenta);
    }

    /** Detalle de los pagos (medio + monto) que componen una venta. */
    public List<PagoVenta> obtenerPagos(int idVenta) throws SQLException {
        return pagoVentaDAO.listarPorVenta(idVenta);
    }

    // ── Datos para el Dashboard ─────────────────────────────────
    public Map<String, Integer> totalHoyPorMedioPago() throws SQLException {
        return ventaDAO.totalHoyPorMedioPago();
    }

    public int totalMesActual() throws SQLException { return ventaDAO.totalMesActual(); }

    public int cantidadVentasHoy() throws SQLException { return ventaDAO.cantidadVentasHoy(); }

    public List<ProductoVendido> productosMasVendidosDelMes(int top) throws SQLException {
        return itemVentaDAO.productosMasVendidosDelMes(top);
    }

    /** Pequeño ayudante interno para no repetir el armado del movimiento en cada método. */
    private static class MovimientoInventarioDaoWrapper {
        private final cl.antucayen.model.dao.MovimientoInventarioDAO movDAO =
                new cl.antucayen.model.dao.MovimientoInventarioDAO();

        void registrarSalidaPorVenta(String sku, int cantidad, int stockAnterior,
                                     int stockResultante, int idVenta) throws SQLException {
            MovimientoInventario mov = new MovimientoInventario();
            mov.setSku(sku);
            mov.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
            mov.setIdVenta(idVenta);
            mov.setTipoMovimiento("Salida por venta");
            mov.setStockAnterior(stockAnterior);
            mov.setCantidadAplicada(cantidad);
            mov.setStockResultante(stockResultante);
            movDAO.insertar(mov);
        }

        void registrarReversion(String sku, int cantidad, int stockAnterior,
                                int stockResultante, int idVenta) throws SQLException {
            MovimientoInventario mov = new MovimientoInventario();
            mov.setSku(sku);
            mov.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
            mov.setIdVenta(idVenta);
            mov.setTipoMovimiento("Reversión");
            mov.setStockAnterior(stockAnterior);
            mov.setCantidadAplicada(cantidad);
            mov.setStockResultante(stockResultante);
            movDAO.insertar(mov);
        }
    }
}