package cl.antucayen.model.service;

import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.dao.ItemFacturaDAO;
import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ServicioFactura {

    public static final String MENSAJE_FACTURA_DUPLICADA =
            "Número de factura ya registrado para este proveedor";
    public static final String MENSAJE_FACTURA_PROCESADA =
            "Factura ya procesada anteriormente";

    private static final List<String> ESTADOS_VALIDOS =
            List.of("Pendiente", "Procesada", "Observada");

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final ItemFacturaDAO itemFacturaDAO = new ItemFacturaDAO();
    private final ServicioInventario servicioInventario = new ServicioInventario();

    /** Registra de forma atómica una factura junto con todos sus ítems. */
    public int registrar(Factura factura, List<ItemFactura> items) throws SQLException {
        Autorizacion.verificarGestionFacturas();
        validarNuevaFactura(factura, items);

        try {
            return DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
                if (facturaDAO.existeNumeroPorProveedor(
                        factura.getIdProveedor(), factura.getNumeroFactura())) {
                    throw new IllegalStateException(MENSAJE_FACTURA_DUPLICADA);
                }

                factura.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
                factura.setEstado("Pendiente");
                int idFactura = facturaDAO.insertar(factura);
                if (idFactura <= 0) {
                    throw new SQLException("No se pudo obtener el ID de la factura generada");
                }

                for (ItemFactura item : items) {
                    if (item.getEstadoItem() == null || item.getEstadoItem().isBlank()) {
                        item.setEstadoItem(item.getSku() != null ? "Válido" : "Observado");
                    }
                    if ("No Procesado".equals(item.getEstadoItem())) {
                        if (item.getCantidadFacturada() < 0) {
                            throw new IllegalArgumentException(
                                    "La cantidad de un ítem No Procesado no puede ser negativa");
                        }
                    } else if (item.getCantidadFacturada() <= 0) {
                        throw new IllegalArgumentException(
                                "La cantidad de cada ítem legible debe ser mayor a cero");
                    }
                    item.setIdFactura(idFactura);
                    itemFacturaDAO.insertar(item);
                }
                return idFactura;
            });
        } catch (SQLException ex) {
            // Cierra también una posible carrera entre la prevalidación Java y
            // la restricción UNIQUE(id_proveedor, numero_factura) de MariaDB.
            if (esViolacionDuplicidad(ex)) {
                throw new IllegalStateException(MENSAJE_FACTURA_DUPLICADA, ex);
            }
            throw ex;
        }
    }

    private boolean esViolacionDuplicidad(SQLException ex) {
        SQLException actual = ex;
        while (actual != null) {
            if (actual.getErrorCode() == 1062 || "23000".equals(actual.getSQLState())) return true;
            actual = actual.getNextException();
        }
        return false;
    }

    private void validarNuevaFactura(Factura factura, List<ItemFactura> items) {
        if (factura == null) throw new IllegalArgumentException("La factura es obligatoria");
        if (factura.getNumeroFactura() == null || factura.getNumeroFactura().isBlank()) {
            throw new IllegalArgumentException("El número de factura es obligatorio");
        }
        if (factura.getFechaEmision() == null) {
            throw new IllegalArgumentException("La fecha de emisión es obligatoria");
        }
        if (factura.getIdProveedor() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un proveedor");
        }
        if (factura.getValorTotal() < 0) {
            throw new IllegalArgumentException("El valor total no puede ser negativo");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La factura debe tener al menos un ítem");
        }
    }

    /**
     * Cambia el estado de forma atómica. El stock se aplica solamente cuando
     * todos los ítems están resueltos como Válidos.
     */
    public void cambiarEstado(int idFactura, String nuevoEstado) throws SQLException {
        Autorizacion.verificarGestionFacturas();
        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado);
        }

        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            Factura actual = facturaDAO.buscarPorIdParaActualizar(idFactura);
            if (actual == null) throw new IllegalArgumentException("La factura no existe");
            if ("Procesada".equals(actual.getEstado())) {
                throw new IllegalStateException(MENSAJE_FACTURA_PROCESADA);
            }
            if (actual.getEstado().equals(nuevoEstado)) return null;
            if ("Observada".equals(actual.getEstado())) {
                throw new IllegalStateException(
                        "Una factura Observada solo puede cambiar de estado tras corregir equivalencias y reprocesar");
            }

            if ("Procesada".equals(nuevoEstado)) {
                List<ItemFactura> items = itemFacturaDAO.listarPorFactura(idFactura);
                if (items.isEmpty()) {
                    throw new IllegalStateException("La factura no tiene ítems para procesar");
                }
                boolean hayPendientes = items.stream().anyMatch(item ->
                        item.getSku() == null || !"Válido".equals(item.getEstadoItem()));
                if (hayPendientes) {
                    throw new IllegalStateException(
                            "No se puede marcar como Procesada mientras existan ítems Observados o No Procesados");
                }

                for (ItemFactura item : items) {
                    servicioInventario.registrarIngresoPorCompra(
                            item.getSku(), item.getCantidadFacturada(), idFactura, item.getIdItem());
                }
            }
            facturaDAO.actualizarEstado(idFactura, nuevoEstado);
            return null;
        });
    }

    public List<ItemFactura> obtenerItems(int idFactura) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return itemFacturaDAO.listarPorFactura(idFactura);
    }

    public Factura buscarPorId(int id) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return facturaDAO.buscarPorId(id);
    }

    public List<Factura> listarTodas() throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return facturaDAO.listarTodas();
    }

    public List<Factura> listarPorEstado(String estado) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return facturaDAO.listarPorEstado(estado);
    }

    public List<Factura> consultar(String numero, Integer idProveedor,
                                   LocalDate desde, LocalDate hasta) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return facturaDAO.buscarConFiltro(numero, idProveedor, desde, hasta);
    }
}
