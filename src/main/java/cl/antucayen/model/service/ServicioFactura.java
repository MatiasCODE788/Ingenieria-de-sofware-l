package cl.antucayen.model.service;

import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.dao.ItemFacturaDAO;
import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ServicioFactura {

    private static final List<String> ESTADOS_VALIDOS =
            List.of("Pendiente", "Procesada", "Observada");

    private final FacturaDAO           facturaDAO      = new FacturaDAO();
    private final ItemFacturaDAO       itemFacturaDAO  = new ItemFacturaDAO();
    private final ServicioInventario   servicioInventario = new ServicioInventario();

    /**
     * Registra una factura junto a sus ítems. Valida que el número de
     * factura no esté repetido para el mismo proveedor (además de la
     * restricción UNIQUE de BD, que actúa como respaldo).
     */
    public int registrar(Factura f, List<ItemFactura> items) throws SQLException {
        if (f.getNumeroFactura() == null || f.getNumeroFactura().isBlank())
            throw new IllegalArgumentException("El número de factura es obligatorio");
        if (f.getFechaEmision() == null)
            throw new IllegalArgumentException("La fecha de emisión es obligatoria");
        if (f.getIdProveedor() <= 0)
            throw new IllegalArgumentException("Debe seleccionar un proveedor");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("La factura debe tener al menos un ítem");
        if (facturaDAO.existeNumeroPorProveedor(f.getIdProveedor(), f.getNumeroFactura()))
            throw new IllegalStateException(
                    "Ya existe una factura con el número '" + f.getNumeroFactura()
                            + "' para este proveedor");

        f.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
        f.setEstado("Pendiente");
        int idFactura = facturaDAO.insertar(f);

        for (ItemFactura item : items) {
            item.setIdFactura(idFactura);
            if (item.getEstadoItem() == null)
                item.setEstadoItem(item.getSku() != null ? "Válido" : "Observado");
            itemFacturaDAO.insertar(item);
        }
        return idFactura;
    }

    /** Cambia el estado de la factura, sin permitir reprocesar una ya Procesada. */
    public void cambiarEstado(int idFactura, String nuevoEstado) throws SQLException {
        cambiarEstado(idFactura, nuevoEstado, false);
    }

    /**
     * Cambia el estado de la factura. Al pasar a 'Procesada', vincula de
     * forma permanente cada ítem Válido con un movimiento de ingreso de
     * stock ('Ingreso por compra'), asociado a esta factura.
     *
     * @param permitirReprocesar si es true, permite volver a aplicar el
     *                           cambio aunque la factura ya esté Procesada
     *                           (reservado para el flujo de reproceso con
     *                           autorización de Administrador, incremento 1.7).
     */
    public void cambiarEstado(int idFactura, String nuevoEstado, boolean permitirReprocesar) throws SQLException {
        if (!ESTADOS_VALIDOS.contains(nuevoEstado))
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstado);

        Factura actual = facturaDAO.buscarPorId(idFactura);
        if (actual == null)
            throw new IllegalArgumentException("La factura no existe");
        if ("Procesada".equals(actual.getEstado()) && !permitirReprocesar)
            throw new IllegalStateException("Factura ya procesada anteriormente");

        if ("Procesada".equals(nuevoEstado)) {
            for (ItemFactura item : itemFacturaDAO.listarPorFactura(idFactura)) {
                if (item.getSku() != null && "Válido".equals(item.getEstadoItem())) {
                    servicioInventario.registrarIngresoPorCompra(
                            item.getSku(), item.getCantidadFacturada(), idFactura);
                }
            }
        }
        facturaDAO.actualizarEstado(idFactura, nuevoEstado);
    }

    public List<ItemFactura> obtenerItems(int idFactura) throws SQLException {
        return itemFacturaDAO.listarPorFactura(idFactura);
    }

    public Factura buscarPorId(int id) throws SQLException {
        return facturaDAO.buscarPorId(id);
    }

    public List<Factura> listarTodas() throws SQLException {
        return facturaDAO.listarTodas();
    }

    public List<Factura> listarPorEstado(String estado) throws SQLException {
        return facturaDAO.listarPorEstado(estado);
    }

    /** Consulta combinada por número (parcial), proveedor y rango de fechas de emisión. */
    public List<Factura> consultar(String numero, Integer idProveedor,
                                   LocalDate desde, LocalDate hasta) throws SQLException {
        return facturaDAO.buscarConFiltro(numero, idProveedor, desde, hasta);
    }
}
