package cl.antucayen.model.service;

import cl.antucayen.model.dao.EquivalenciaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.exception.EquivalenciaDuplicadaException;

import java.sql.SQLException;
import java.util.List;

public class ServicioEquivalencia {

    private final EquivalenciaDAO equivalenciaDAO = new EquivalenciaDAO();
    private final ProductoDAO     productoDAO     = new ProductoDAO();

    /**
     * Registra un código interno de proveedor y lo mapea a un único SKU.
     * La unicidad del código interno por proveedor la garantiza además la
     * llave primaria compuesta (id_proveedor, codigo_interno_proveedor) en BD.
     */
    public void registrar(Equivalencia e) throws SQLException, EquivalenciaDuplicadaException {
        if (e.getCodigoInternoProveedor() == null || e.getCodigoInternoProveedor().isBlank())
            throw new IllegalArgumentException("El código interno del proveedor es obligatorio");
        if (e.getSku() == null || e.getSku().isBlank())
            throw new IllegalArgumentException("El SKU es obligatorio");
        if (!productoDAO.existeSku(e.getSku()))
            throw new IllegalArgumentException("No existe un producto con el SKU: " + e.getSku());
        if (equivalenciaDAO.existe(e.getIdProveedor(), e.getCodigoInternoProveedor()))
            throw new EquivalenciaDuplicadaException(e.getCodigoInternoProveedor());

        equivalenciaDAO.insertar(e);
    }

    /** Modifica el SKU al que apunta una equivalencia ya registrada. */
    public void modificar(int idProveedor, String codigoInterno, String nuevoSku) throws SQLException {
        if (nuevoSku == null || nuevoSku.isBlank())
            throw new IllegalArgumentException("El SKU es obligatorio");
        if (!productoDAO.existeSku(nuevoSku))
            throw new IllegalArgumentException("No existe un producto con el SKU: " + nuevoSku);
        if (!equivalenciaDAO.existe(idProveedor, codigoInterno))
            throw new IllegalArgumentException("La equivalencia no existe, no se puede modificar");

        equivalenciaDAO.actualizarSku(idProveedor, codigoInterno, nuevoSku);
    }

    public void eliminar(int idProveedor, String codigoInterno) throws SQLException {
        equivalenciaDAO.eliminar(idProveedor, codigoInterno);
    }

    public List<Equivalencia> listarPorProveedor(int idProveedor) throws SQLException {
        return equivalenciaDAO.listarPorProveedor(idProveedor);
    }

    /** Consulta combinada por nombre de proveedor, código interno y/o SKU (todos opcionales). */
    public List<Equivalencia> consultar(String nombreProveedor, String codigoInterno,
                                        String sku) throws SQLException {
        return equivalenciaDAO.buscarConFiltro(nombreProveedor, codigoInterno, sku);
    }

    /** Detección local de duplicado dentro de un conjunto ya cargado (para validación visual en UI). */
    public boolean esDuplicadoLocal(List<Equivalencia> existentes, String codigoInterno,
                                    String codigoExcluido) {
        if (codigoInterno == null) return false;
        return existentes.stream().anyMatch(eq ->
                eq.getCodigoInternoProveedor().equalsIgnoreCase(codigoInterno)
                        && !eq.getCodigoInternoProveedor().equalsIgnoreCase(
                                codigoExcluido == null ? "" : codigoExcluido));
    }
}
