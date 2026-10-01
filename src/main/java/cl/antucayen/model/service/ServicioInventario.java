package cl.antucayen.model.service;

import cl.antucayen.model.dao.AjusteInventarioDAO;
import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.AjusteInventario;
import cl.antucayen.model.entity.ItemAjuste;
import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class ServicioInventario {

    public static final String MODALIDAD_SUMAR = "Sumar al stock actual";
    public static final String MODALIDAD_REEMPLAZAR = "Reemplazar stock actual";

    public record SolicitudAjuste(String sku, int cantidad) {}

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final AjusteInventarioDAO ajusteDAO = new AjusteInventarioDAO();

    /** Ingresa stock una sola vez por ítem de factura mientras el movimiento esté vigente. */
    public void registrarIngresoPorCompra(String sku, int cantidad, int idFactura,
                                          int idItemFactura) throws SQLException {
        Autorizacion.verificarGestionFacturas();
        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad a ingresar debe ser mayor que cero");
            }
            if (idItemFactura <= 0) {
                throw new IllegalArgumentException(
                        "El ítem de factura es obligatorio para registrar el ingreso");
            }
            if (movimientoDAO.existeIngresoPorItemFactura(idItemFactura)) return null;

            Producto producto = productoDAO.buscarPorSkuParaActualizar(sku);
            if (producto == null) throw new IllegalArgumentException("SKU no encontrado: " + sku);
            if (!"Activo".equals(producto.getEstado())) {
                throw new IllegalArgumentException(
                        "El producto está Inactivo y no puede recibir nuevas facturas: " + sku);
            }

            int stockAnterior = producto.getStockActual();
            int stockResultante;
            try {
                stockResultante = Math.addExact(stockAnterior, cantidad);
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("El ingreso excede el rango permitido de stock", ex);
            }
            productoDAO.actualizarStock(sku, stockResultante);

            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setSku(sku);
            movimiento.setIdUsuario(usuarioActual().getIdUsuario());
            movimiento.setIdFactura(idFactura);
            movimiento.setIdItemFactura(idItemFactura);
            movimiento.setTipoMovimiento("Ingreso por compra");
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setCantidadAplicada(cantidad);
            movimiento.setStockResultante(stockResultante);
            movimiento.setVigente(true);
            movimientoDAO.insertar(movimiento);
            return null;
        });
    }

    /**
     * Revierte de forma auditable el ingreso vigente de un ítem antes de un
     * reprocesamiento autorizado.
     */
    public void revertirIngresoPorCompraParaReproceso(int idFactura,
                                                       int idItemFactura) throws SQLException {
        Autorizacion.verificarAdministrador(
                "Solo un Administrador puede autorizar el reprocesamiento de una factura Procesada");
        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            MovimientoInventario ingreso = movimientoDAO.buscarIngresoVigentePorItemFactura(idItemFactura);
            if (ingreso == null) {
                throw new IllegalStateException(
                        "No existe un ingreso de inventario vigente para el ítem " + idItemFactura);
            }
            if (ingreso.getIdFactura() == null || ingreso.getIdFactura() != idFactura) {
                throw new IllegalStateException(
                        "El movimiento de inventario no corresponde a la factura indicada");
            }

            Producto producto = productoDAO.buscarPorSkuParaActualizar(ingreso.getSku());
            if (producto == null) {
                throw new IllegalStateException(
                        "No existe el producto asociado al movimiento: " + ingreso.getSku());
            }

            int stockAnterior = producto.getStockActual();
            int stockResultante;
            try {
                stockResultante = Math.subtractExact(stockAnterior, ingreso.getCantidadAplicada());
            } catch (ArithmeticException ex) {
                throw new IllegalStateException(
                        "No se pudo revertir el ingreso por desbordamiento de stock", ex);
            }
            if (stockResultante < 0) {
                throw new IllegalStateException(
                        "No se puede reprocesar la factura porque revertir el ítem "
                                + idItemFactura + " dejaría stock negativo para " + ingreso.getSku());
            }

            productoDAO.actualizarStock(ingreso.getSku(), stockResultante);
            movimientoDAO.marcarNoVigente(ingreso.getIdMovimiento());

            MovimientoInventario reversion = new MovimientoInventario();
            reversion.setSku(ingreso.getSku());
            reversion.setIdUsuario(usuarioActual().getIdUsuario());
            reversion.setIdFactura(idFactura);
            reversion.setIdItemFactura(idItemFactura);
            reversion.setTipoMovimiento("Reversión");
            reversion.setStockAnterior(stockAnterior);
            reversion.setCantidadAplicada(-ingreso.getCantidadAplicada());
            reversion.setStockResultante(stockResultante);
            reversion.setModalidadAjuste("Reproceso de factura");
            reversion.setVigente(false);
            movimientoDAO.insertar(reversion);
            return null;
        });
    }

    /**
     * Aplica un ajuste ordinario. Administrador y Bodeguero pueden ejecutar
     * ajustes; una cantidad negativa exige autorización explícita de Admin.
     */
    public int aplicarAjuste(String modalidad, boolean correccionAutorizada,
                             List<SolicitudAjuste> solicitudes) throws SQLException {
        Autorizacion.verificarAjustesInventario();
        validarModalidad(modalidad);
        if (solicitudes == null || solicitudes.isEmpty()) return 0;
        if (correccionAutorizada) {
            Autorizacion.verificarCorreccionNegativaInventario();
        }

        return DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            Usuario usuario = usuarioActual();
            AjusteInventario ajuste = new AjusteInventario();
            ajuste.setModalidadAjuste(modalidad);
            ajuste.setEstadoAjuste("Pendiente");
            ajuste.setIdUsuario(usuario.getIdUsuario());
            ajuste.setNombreUsuario(nombreVisible(usuario));
            int idAjuste = ajusteDAO.insertar(ajuste);

            int aplicados = 0;
            for (SolicitudAjuste solicitud : solicitudes) {
                validarSolicitud(solicitud, correccionAutorizada);

                Producto producto = productoDAO.buscarPorSkuParaActualizar(solicitud.sku());
                if (producto == null) {
                    throw new IllegalArgumentException("SKU no encontrado: " + solicitud.sku());
                }
                if (!"Activo".equals(producto.getEstado())) {
                    throw new IllegalArgumentException(
                            "El producto está Inactivo y no puede usarse en nuevas importaciones: "
                                    + solicitud.sku());
                }

                int stockAnterior = producto.getStockActual();
                int stockResultante = calcularStockResultante(
                        stockAnterior, solicitud.cantidad(), modalidad);
                if (stockResultante < 0) {
                    throw new IllegalArgumentException(
                            "El stock resultante no puede ser negativo para SKU: " + solicitud.sku());
                }

                int delta;
                try {
                    delta = Math.subtractExact(stockResultante, stockAnterior);
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException("El ajuste excede el rango permitido de stock", ex);
                }
                if (delta == 0) continue;

                productoDAO.actualizarStock(solicitud.sku(), stockResultante);

                MovimientoInventario movimiento = new MovimientoInventario();
                movimiento.setSku(solicitud.sku());
                movimiento.setIdUsuario(usuario.getIdUsuario());
                movimiento.setIdAjuste(idAjuste);
                movimiento.setTipoMovimiento(delta > 0 ? "Ajuste positivo" : "Ajuste negativo");
                movimiento.setStockAnterior(stockAnterior);
                movimiento.setCantidadAplicada(delta);
                movimiento.setStockResultante(stockResultante);
                movimiento.setModalidadAjuste(modalidad);
                movimiento.setVigente(true);
                movimientoDAO.insertar(movimiento);

                ajusteDAO.insertarItem(
                        idAjuste, solicitud.sku(), delta, stockAnterior, stockResultante);
                aplicados++;
            }

            if (aplicados == 0) {
                throw new IllegalArgumentException("El ajuste no produce cambios de stock");
            }
            ajusteDAO.actualizarEstado(idAjuste, "Aplicado");
            return aplicados;
        });
    }

    /**
     * RF-46. Revierte atómicamente el efecto de un ajuste aplicado. La
     * reversión usa el delta histórico del ajuste y no sobrescribe movimientos
     * posteriores: stock_resultante = stock_actual - delta_original. Así se
     * restaura el efecto previo del ajuste sin borrar ventas/compras posteriores.
     */
    public void revertirAjuste(int idAjuste) throws SQLException {
        Autorizacion.verificarReversionAjuste();
        if (idAjuste <= 0) throw new IllegalArgumentException("Debes indicar un ajuste válido");

        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            AjusteInventario ajuste = ajusteDAO.buscarPorIdParaActualizar(idAjuste);
            if (ajuste == null) {
                throw new IllegalArgumentException("No existe el ajuste #" + idAjuste);
            }
            if ("Revertido".equals(ajuste.getEstadoAjuste())) {
                throw new IllegalStateException("El ajuste #" + idAjuste + " ya fue revertido");
            }
            if (!"Aplicado".equals(ajuste.getEstadoAjuste())) {
                throw new IllegalStateException(
                        "Solo se puede revertir un ajuste en estado Aplicado");
            }

            List<ItemAjuste> items = ajusteDAO.listarItems(idAjuste);
            if (items.isEmpty()) {
                throw new IllegalStateException("El ajuste no contiene ítems que puedan revertirse");
            }

            Usuario usuario = usuarioActual();
            movimientoDAO.marcarNoVigentesPorAjuste(idAjuste);

            for (ItemAjuste item : items) {
                Producto producto = productoDAO.buscarPorSkuParaActualizar(item.getSku());
                if (producto == null) {
                    throw new IllegalStateException(
                            "No existe el producto del ajuste: " + item.getSku());
                }

                int stockActual = producto.getStockActual();
                int stockResultante;
                try {
                    stockResultante = Math.subtractExact(stockActual, item.getCantidadAplicada());
                } catch (ArithmeticException ex) {
                    throw new IllegalStateException(
                            "La reversión excede el rango permitido para " + item.getSku(), ex);
                }
                if (stockResultante < 0) {
                    throw new IllegalStateException(
                            "No se puede revertir el ajuste #" + idAjuste
                                    + " porque el SKU " + item.getSku()
                                    + " quedaría con stock negativo (" + stockResultante + ")");
                }

                productoDAO.actualizarStock(item.getSku(), stockResultante);

                MovimientoInventario reversion = new MovimientoInventario();
                reversion.setSku(item.getSku());
                reversion.setIdUsuario(usuario.getIdUsuario());
                reversion.setIdAjuste(idAjuste);
                reversion.setTipoMovimiento("Reversión");
                reversion.setStockAnterior(stockActual);
                reversion.setCantidadAplicada(-item.getCantidadAplicada());
                reversion.setStockResultante(stockResultante);
                reversion.setModalidadAjuste("Reversión de ajuste");
                reversion.setVigente(true);
                movimientoDAO.insertar(reversion);
            }

            ajusteDAO.actualizarEstado(idAjuste, "Revertido");
            return null;
        });
    }

    private void validarSolicitud(SolicitudAjuste solicitud, boolean correccionAutorizada) {
        if (solicitud == null || solicitud.sku() == null || solicitud.sku().isBlank()) {
            throw new IllegalArgumentException("Todos los ajustes deben tener un SKU válido");
        }
        if (solicitud.cantidad() == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero");
        }
        if (solicitud.cantidad() < 0 && !correccionAutorizada) {
            throw new IllegalArgumentException(
                    "Cantidad negativa no permitida sin Corrección autorizada (Administrador)");
        }
    }

    private void validarModalidad(String modalidad) {
        if (!MODALIDAD_SUMAR.equals(modalidad) && !MODALIDAD_REEMPLAZAR.equals(modalidad)) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una modalidad de ajuste: 'Sumar al stock actual' o 'Reemplazar stock actual'");
        }
    }

    private int calcularStockResultante(int stockAnterior, int cantidad, String modalidad) {
        try {
            return switch (modalidad) {
                case MODALIDAD_SUMAR -> Math.addExact(stockAnterior, cantidad);
                case MODALIDAD_REEMPLAZAR -> cantidad;
                default -> throw new IllegalArgumentException(
                        "Debes seleccionar una modalidad de ajuste válida");
            };
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("El ajuste excede el rango permitido de stock", ex);
        }
    }

    private Usuario usuarioActual() {
        Usuario usuario = SesionActual.getUsuario();
        if (usuario == null) throw new SecurityException("No existe una sesión autenticada");
        return usuario;
    }

    private String nombreVisible(Usuario usuario) {
        String nombre = usuario.getNombreCompleto();
        if (nombre != null && !nombre.isBlank()) return nombre.trim();
        return usuario.getUsername();
    }

    public List<MovimientoInventario> listarMovimientos() throws SQLException {
        Autorizacion.verificarHistorialInventario();
        return movimientoDAO.listarTodos();
    }

    public List<MovimientoInventario> filtrarMovimientos(String sku, String tipo,
                                                         Timestamp desde,
                                                         Timestamp hasta) throws SQLException {
        Autorizacion.verificarHistorialInventario();
        return movimientoDAO.filtrar(sku, tipo, desde, hasta);
    }

    /** RF-56: últimos diez movimientos asociados a un producto. */
    public List<MovimientoInventario> listarUltimosMovimientos(String sku) throws SQLException {
        Autorizacion.verificarConsultaStock();
        if (sku == null || sku.isBlank()) return List.of();
        return movimientoDAO.listarUltimosPorSku(sku.trim(), 10);
    }
}
