package cl.antucayen.model.service;

import cl.antucayen.model.dao.ItemVentaDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.PagoVentaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.VentaDAO;
import cl.antucayen.model.domain.EstadoProducto;
import cl.antucayen.model.domain.EstadoVenta;
import cl.antucayen.model.entity.ItemVenta;
import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.PagoVenta;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.entity.Venta;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.security.SesionActual;
import cl.antucayen.util.DBConexion;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServicioVenta {

    private static final List<String> MEDIOS_PAGO_VALIDOS =
            List.of("Efectivo", "Débito", "Crédito");

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ItemVentaDAO itemVentaDAO = new ItemVentaDAO();
    private final PagoVentaDAO pagoVentaDAO = new PagoVentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

    /**
     * Prepara el módulo POS sin reservar un número de venta. Solo limpia
     * borradores "En curso" que pudieran haber quedado de versiones anteriores.
     *
     * La versión actual asigna el ID recién al confirmar un cobro exitoso,
     * evitando que entrar/salir del POS consuma números sin una venta real.
     */
    public void prepararPuntoVenta() throws SQLException {
        Autorizacion.verificarPuntoVenta();
        ventaDAO.cancelarVentasEnCursoPorUsuario(usuarioActualId());
    }

    /**
     * Registra una nueva venta de forma atómica. La cabecera se inserta una vez
     * validados carrito, pagos y stock, por lo que navegar por el POS no crea
     * filas ni incrementa el identificador de venta.
     */
    public int registrar(List<PagoVenta> pagos, List<ItemVenta> items) throws SQLException {
        Autorizacion.verificarPuntoVenta();
        return DBConexion.getInstancia().ejecutarEnTransaccion(
                () -> registrarNuevaVentaTransaccional(pagos, items));
    }

    private int registrarNuevaVentaTransaccional(
            List<PagoVenta> pagos,
            List<ItemVenta> items) throws SQLException {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }
        validarPagosBasicos(pagos);

        Map<String, Integer> cantidadPorSku = consolidarCantidades(items);
        Map<String, Producto> productosBloqueados = bloquearYValidarProductos(cantidadPorSku);

        int montoTotal = recalcularTotalConPreciosVigentes(items, productosBloqueados);
        int montoRecibido = sumarPagos(pagos);
        int vuelto = calcularVuelto(pagos, montoTotal);
        List<PagoVenta> pagosAplicados = ajustarPagosPorVuelto(pagos, vuelto);

        int totalAplicado = sumarPagos(pagosAplicados);
        if (totalAplicado != montoTotal) {
            throw new IllegalStateException(
                    "Los pagos aplicados no coinciden con el total de la venta");
        }

        List<String> mediosAplicados = pagosAplicados.stream()
                .map(PagoVenta::getMedioPago)
                .distinct()
                .toList();
        String medioPagoVenta = mediosAplicados.size() == 1
                ? mediosAplicados.get(0)
                : "Mixto";

        Venta venta = new Venta();
        venta.setIdUsuario(usuarioActualId());
        venta.setMedioPago(medioPagoVenta);
        venta.setMontoTotal(montoTotal);
        venta.setMontoRecibido(montoRecibido);
        venta.setVuelto(vuelto);
        venta.setEstado(EstadoVenta.PAGADA.valorDb());

        // El ID se obtiene únicamente aquí, una vez superadas las validaciones
        // funcionales. No existe una venta persistida por el solo hecho de abrir POS.
        int idVenta = ventaDAO.insertarPagada(venta);

        for (PagoVenta pago : pagosAplicados) {
            pago.setIdVenta(idVenta);
            pagoVentaDAO.insertar(pago);
        }

        Map<String, Integer> stockEnOperacion = new LinkedHashMap<>();
        productosBloqueados.forEach(
                (sku, producto) -> stockEnOperacion.put(sku, producto.getStockActual()));

        for (ItemVenta item : items) {
            item.setIdVenta(idVenta);
            int idItemVenta = itemVentaDAO.insertar(item);
            item.setIdItem(idItemVenta);

            int stockAnterior = stockEnOperacion.get(item.getSku());
            int delta = -item.getCantidad();
            int stockResultante;
            try {
                stockResultante = Math.addExact(stockAnterior, delta);
            } catch (ArithmeticException ex) {
                throw new IllegalStateException("El stock resultante excede el rango permitido", ex);
            }
            if (stockResultante < 0) {
                throw new IllegalStateException("El stock no puede quedar negativo");
            }

            productoDAO.actualizarStock(item.getSku(), stockResultante);
            stockEnOperacion.put(item.getSku(), stockResultante);
            registrarMovimientoVenta(item, delta, stockAnterior, stockResultante,
                    "Venta", null);
        }
        return idVenta;
    }

    private void validarPagosBasicos(List<PagoVenta> pagos) {
        if (pagos == null || pagos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes ingresar el monto pagado en al menos un medio de pago");
        }
        for (PagoVenta pago : pagos) {
            if (pago == null || !MEDIOS_PAGO_VALIDOS.contains(pago.getMedioPago())) {
                throw new IllegalArgumentException("Medio de pago no válido");
            }
            if (pago.getMonto() <= 0) {
                throw new IllegalArgumentException("El monto de cada pago debe ser mayor a cero");
            }
        }
    }

    private Map<String, Integer> consolidarCantidades(List<ItemVenta> items) {
        Map<String, Integer> cantidadPorSku = new LinkedHashMap<>();
        for (ItemVenta item : items) {
            if (item == null || item.getSku() == null || item.getSku().isBlank()) {
                throw new IllegalArgumentException("Todos los ítems deben tener un SKU válido");
            }
            if (item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de venta debe ser mayor a cero");
            }
            try {
                cantidadPorSku.merge(item.getSku(), item.getCantidad(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException(
                        "La cantidad total de un producto excede el rango permitido", ex);
            }
        }
        return cantidadPorSku;
    }

    private Map<String, Producto> bloquearYValidarProductos(
            Map<String, Integer> cantidadPorSku) throws SQLException {
        Map<String, Producto> productos = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entrada : cantidadPorSku.entrySet()) {
            Producto producto = productoDAO.buscarPorSkuParaActualizar(entrada.getKey());
            if (producto == null) {
                throw new IllegalArgumentException("Producto no encontrado: " + entrada.getKey());
            }
            if (!EstadoProducto.ACTIVO.coincide(producto.getEstado())) {
                throw new IllegalArgumentException(
                        "El producto '" + producto.getNombre() + "' está inactivo");
            }
            if (producto.getPrecioVenta() <= 0) {
                throw new IllegalArgumentException(
                        "El producto '" + producto.getNombre()
                                + "' no tiene un precio de venta válido configurado");
            }
            if (producto.getStockActual() < entrada.getValue()) {
                throw new IllegalArgumentException(
                        "Stock insuficiente de '" + producto.getNombre()
                                + "' (disponible: " + producto.getStockActual()
                                + ", solicitado: " + entrada.getValue() + ")");
            }
            productos.put(entrada.getKey(), producto);
        }
        return productos;
    }

    private int recalcularTotalConPreciosVigentes(
            List<ItemVenta> items,
            Map<String, Producto> productosBloqueados) {
        int montoTotal = 0;
        for (ItemVenta item : items) {
            Producto producto = productosBloqueados.get(item.getSku());
            try {
                int subtotal = Math.multiplyExact(
                        producto.getPrecioVenta(), item.getCantidad());
                montoTotal = Math.addExact(montoTotal, subtotal);
                item.setPrecioUnitarioVenta(producto.getPrecioVenta());
                item.setSubtotal(subtotal);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("El total de la venta excede el rango permitido", ex);
            }
        }
        return montoTotal;
    }

    /**
     * RF-68: el total recibido puede ser mayor al total; todo excedente debe
     * poder devolverse a partir del efectivo recibido.
     */
    public int calcularVuelto(List<PagoVenta> pagos, int montoTotal) {
        if (montoTotal < 0) {
            throw new IllegalArgumentException("El total de la venta no puede ser negativo");
        }
        validarPagosBasicos(pagos);

        int montoPagado = 0;
        int efectivoRecibido = 0;
        try {
            for (PagoVenta pago : pagos) {
                montoPagado = Math.addExact(montoPagado, pago.getMonto());
                if ("Efectivo".equals(pago.getMedioPago())) {
                    efectivoRecibido = Math.addExact(efectivoRecibido, pago.getMonto());
                }
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "La suma de los pagos excede el rango permitido", ex);
        }

        if (montoPagado < montoTotal) {
            throw new IllegalArgumentException(
                    "El monto pagado ($" + montoPagado
                            + ") es insuficiente para el total de la venta ($"
                            + montoTotal + ")");
        }

        int vuelto = montoPagado - montoTotal;
        if (vuelto > efectivoRecibido) {
            throw new IllegalArgumentException(
                    "El excedente de los pagos ($" + vuelto
                            + ") no puede devolverse como vuelto porque no proviene completamente de efectivo");
        }
        return vuelto;
    }

    private int sumarPagos(List<PagoVenta> pagos) {
        int total = 0;
        try {
            for (PagoVenta pago : pagos) total = Math.addExact(total, pago.getMonto());
            return total;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("La suma de los pagos excede el rango permitido", ex);
        }
    }

    /**
     * Persiste solo el importe aplicado a la venta. El excedente queda
     * almacenado explícitamente como venta.vuelto y venta.monto_recibido.
     */
    private List<PagoVenta> ajustarPagosPorVuelto(List<PagoVenta> pagos, int vuelto) {
        List<PagoVenta> pagosAplicados = new ArrayList<>();
        int vueltoPendiente = vuelto;

        for (PagoVenta pago : pagos) {
            int montoAplicado = pago.getMonto();
            if ("Efectivo".equals(pago.getMedioPago()) && vueltoPendiente > 0) {
                int descuento = Math.min(montoAplicado, vueltoPendiente);
                montoAplicado -= descuento;
                vueltoPendiente -= descuento;
            }
            if (montoAplicado > 0) {
                pagosAplicados.add(new PagoVenta(pago.getMedioPago(), montoAplicado));
            }
        }

        if (vueltoPendiente != 0) {
            throw new IllegalStateException(
                    "No fue posible aplicar correctamente el vuelto a los pagos en efectivo");
        }
        return pagosAplicados;
    }

    /** Anula una venta pagada y devuelve el stock de todos sus ítems. */
    public void anular(int idVenta) throws SQLException {
        Autorizacion.verificarAdministrador("Solo un Administrador puede anular una venta");
        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            Venta venta = ventaDAO.buscarPorIdParaActualizar(idVenta);
            if (venta == null) throw new IllegalArgumentException("La venta no existe");
            if (EstadoVenta.ANULADA.coincide(venta.getEstado())) {
                throw new IllegalStateException("La venta ya está anulada");
            }
            if (!EstadoVenta.PAGADA.coincide(venta.getEstado())) {
                throw new IllegalStateException(
                        "Solo se puede anular una venta confirmada y pagada");
            }

            for (ItemVenta item : itemVentaDAO.listarPorVenta(idVenta)) {
                Producto producto = productoDAO.buscarPorSkuParaActualizar(item.getSku());
                if (producto == null) {
                    throw new IllegalStateException(
                            "No existe el producto asociado al ítem de venta " + item.getIdItem());
                }
                int stockAnterior = producto.getStockActual();
                int delta = item.getCantidad();
                int stockResultante;
                try {
                    stockResultante = Math.addExact(stockAnterior, delta);
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException(
                            "La reversión excede el rango permitido de stock", ex);
                }
                productoDAO.actualizarStock(item.getSku(), stockResultante);

                int invalidados = movimientoDAO.marcarVentaNoVigentePorItemVenta(item.getIdItem());
                if (invalidados != 1) {
                    throw new IllegalStateException(
                            "No se encontró el movimiento vigente del ítem de venta #" + item.getIdItem());
                }

                registrarMovimientoVenta(
                        item, delta, stockAnterior, stockResultante,
                        "Reversión", "Anulación de venta");
            }
            ventaDAO.actualizarEstado(idVenta, EstadoVenta.ANULADA.valorDb());
            return null;
        });
    }

    private void registrarMovimientoVenta(ItemVenta item,
                                          int delta,
                                          int stockAnterior,
                                          int stockResultante,
                                          String tipoMovimiento,
                                          String motivo) throws SQLException {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSku(item.getSku());
        movimiento.setIdUsuario(usuarioActualId());
        movimiento.setIdItemVenta(item.getIdItem());
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setCantidadAplicada(delta);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo(motivo);
        movimiento.setVigente(true);
        movimientoDAO.insertar(movimiento);
    }

    private int usuarioActualId() {
        if (SesionActual.getUsuario() == null) {
            throw new SecurityException("No existe una sesión autenticada");
        }
        return SesionActual.getUsuario().getIdUsuario();
    }

    public List<Venta> listarDelDia() throws SQLException {
        Autorizacion.verificarPuntoVenta();
        if (SesionActual.esAdministrador()) return ventaDAO.listarDelDia();
        return ventaDAO.listarDelDiaPorUsuario(usuarioActualId());
    }

}
