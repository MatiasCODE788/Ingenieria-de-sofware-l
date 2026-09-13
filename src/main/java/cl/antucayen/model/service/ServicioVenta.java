package cl.antucayen.model.service;

import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.PagoVentaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.model.dto.ProductoVendido;
import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServicioVenta {

    private static final List<String> MEDIOS_PAGO_VALIDOS = List.of("Efectivo", "Débito", "Crédito");

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ItemVentaDAO itemVentaDAO = new ItemVentaDAO();
    private final PagoVentaDAO pagoVentaDAO = new PagoVentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

    /**
     * Registra de forma atómica una venta y todos sus efectos de inventario.
     * Los productos se bloquean durante la operación para evitar vender más
     * unidades que las disponibles ante accesos concurrentes.
     */
    public int registrar(List<PagoVenta> pagos, List<ItemVenta> items) throws SQLException {
        Autorizacion.verificarPuntoVenta();
        return DBConexion.getInstancia().ejecutarEnTransaccion(() -> registrarTransaccional(pagos, items));
    }

    private int registrarTransaccional(List<PagoVenta> pagos, List<ItemVenta> items) throws SQLException {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("El carrito está vacío");
        if (pagos == null || pagos.isEmpty())
            throw new IllegalArgumentException("Debes ingresar el monto pagado en al menos un medio de pago");

        for (PagoVenta pago : pagos) {
            if (pago == null || !MEDIOS_PAGO_VALIDOS.contains(pago.getMedioPago()))
                throw new IllegalArgumentException("Medio de pago no válido");
            if (pago.getMonto() <= 0)
                throw new IllegalArgumentException("El monto de cada pago debe ser mayor a cero");
        }

        Map<String, Integer> cantidadPorSku = new LinkedHashMap<>();
        for (ItemVenta item : items) {
            if (item == null || item.getSku() == null || item.getSku().isBlank())
                throw new IllegalArgumentException("Todos los ítems deben tener un SKU válido");
            if (item.getCantidad() <= 0)
                throw new IllegalArgumentException("La cantidad de venta debe ser mayor a cero");
            try {
                cantidadPorSku.merge(item.getSku(), item.getCantidad(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("La cantidad total de un producto excede el rango permitido", ex);
            }
        }

        Map<String, Producto> productosBloqueados = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entrada : cantidadPorSku.entrySet()) {
            Producto producto = productoDAO.buscarPorSkuParaActualizar(entrada.getKey());
            if (producto == null)
                throw new IllegalArgumentException("Producto no encontrado: " + entrada.getKey());
            if (!"Activo".equals(producto.getEstado()))
                throw new IllegalArgumentException("El producto '" + producto.getNombre() + "' está inactivo");
            if (producto.getPrecioVenta() <= 0)
                throw new IllegalArgumentException("El producto '" + producto.getNombre()
                        + "' no tiene un precio de venta válido configurado");
            if (producto.getStockActual() < entrada.getValue())
                throw new IllegalArgumentException("Stock insuficiente de '" + producto.getNombre()
                        + "' (disponible: " + producto.getStockActual()
                        + ", solicitado: " + entrada.getValue() + ")");
            productosBloqueados.put(entrada.getKey(), producto);
        }

        int montoTotal = 0;
        for (ItemVenta item : items) {
            Producto producto = productosBloqueados.get(item.getSku());
            int subtotal;
            try {
                subtotal = Math.multiplyExact(producto.getPrecioVenta(), item.getCantidad());
                montoTotal = Math.addExact(montoTotal, subtotal);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("El total de la venta excede el rango permitido");
            }
            // El servicio usa el precio oficial actual del producto como fuente de verdad.
            item.setPrecioUnitarioVenta(producto.getPrecioVenta());
            item.setSubtotal(subtotal);
        }

        int montoPagado = 0;
        try {
            for (PagoVenta pago : pagos) montoPagado = Math.addExact(montoPagado, pago.getMonto());
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("La suma de los pagos excede el rango permitido", ex);
        }
        if (montoPagado != montoTotal)
            throw new IllegalArgumentException("La suma de los pagos ($" + montoPagado
                    + ") no coincide con el total de la venta ($" + montoTotal + ")");

        String medioPagoVenta = pagos.size() == 1 ? pagos.get(0).getMedioPago() : "Mixto";
        Venta venta = new Venta();
        venta.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        venta.setMedioPago(medioPagoVenta);
        venta.setMontoTotal(montoTotal);
        venta.setEstado("Pagada");
        int idVenta = ventaDAO.insertar(venta);

        for (PagoVenta pago : pagos) {
            pago.setIdVenta(idVenta);
            pagoVentaDAO.insertar(pago);
        }

        Map<String, Integer> stockEnOperacion = new LinkedHashMap<>();
        productosBloqueados.forEach((sku, producto) -> stockEnOperacion.put(sku, producto.getStockActual()));

        for (ItemVenta item : items) {
            item.setIdVenta(idVenta);
            itemVentaDAO.insertar(item);

            int stockAnterior = stockEnOperacion.get(item.getSku());
            int stockResultante = stockAnterior - item.getCantidad();
            productoDAO.actualizarStock(item.getSku(), stockResultante);
            stockEnOperacion.put(item.getSku(), stockResultante);
            registrarMovimientoVenta(item.getSku(), item.getCantidad(), stockAnterior, stockResultante,
                    idVenta, "Salida por venta");
        }
        return idVenta;
    }

    /** Anula una venta de forma atómica y devuelve el stock de todos sus ítems. */
    public void anular(int idVenta) throws SQLException {
        Autorizacion.verificarAdministrador("Solo un Administrador puede anular una venta");
        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            Venta venta = ventaDAO.buscarPorIdParaActualizar(idVenta);
            if (venta == null) throw new IllegalArgumentException("La venta no existe");
            if ("Anulada".equals(venta.getEstado())) throw new IllegalStateException("La venta ya está anulada");

            for (ItemVenta item : itemVentaDAO.listarPorVenta(idVenta)) {
                Producto producto = productoDAO.buscarPorSkuParaActualizar(item.getSku());
                if (producto == null) continue;
                int stockAnterior = producto.getStockActual();
                int stockResultante;
                try {
                    stockResultante = Math.addExact(stockAnterior, item.getCantidad());
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException("La reversión excede el rango permitido de stock", ex);
                }
                productoDAO.actualizarStock(item.getSku(), stockResultante);
                registrarMovimientoVenta(item.getSku(), item.getCantidad(), stockAnterior, stockResultante,
                        idVenta, "Reversión");
            }
            ventaDAO.actualizarEstado(idVenta, "Anulada");
            return null;
        });
    }

    private void registrarMovimientoVenta(String sku, int cantidad, int stockAnterior,
                                          int stockResultante, int idVenta,
                                          String tipoMovimiento) throws SQLException {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSku(sku);
        movimiento.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        movimiento.setIdVenta(idVenta);
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setCantidadAplicada(cantidad);
        movimiento.setStockResultante(stockResultante);
        movimientoDAO.insertar(movimiento);
    }

    public List<Venta> listarDelDia() throws SQLException {
        Autorizacion.verificarPuntoVenta();
        if (SesionActual.esAdministrador()) return ventaDAO.listarDelDia();
        return ventaDAO.listarDelDiaPorUsuario(SesionActual.getUsuario().getIdUsuario());
    }

    public List<ItemVenta> obtenerItems(int idVenta) throws SQLException {
        Autorizacion.verificarPuntoVenta();
        if (SesionActual.esCajero()) {
            Venta venta = ventaDAO.buscarPorId(idVenta);
            if (venta == null) throw new IllegalArgumentException("La venta no existe");
            if (venta.getIdUsuario() != SesionActual.getUsuario().getIdUsuario()) {
                throw new SecurityException(
                        "El Cajero solo puede consultar el detalle de sus propias ventas");
            }
        }
        return itemVentaDAO.listarPorVenta(idVenta);
    }

    public Map<String, Integer> totalHoyPorMedioPago() throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede consultar reportes financieros globales");
        return ventaDAO.totalHoyPorMedioPago();
    }

    public int totalMesActual() throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede consultar reportes financieros globales");
        return ventaDAO.totalMesActual();
    }

    public int cantidadVentasHoy() throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede consultar reportes financieros globales");
        return ventaDAO.cantidadVentasHoy();
    }

    public List<ProductoVendido> productosMasVendidosDelMes(int top) throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede consultar reportes financieros globales");
        return itemVentaDAO.productosMasVendidosDelMes(top);
    }
}
