package cl.antucayen.model.service;

import cl.antucayen.model.dao.EquivalenciaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.exception.EquivalenciaDuplicadaException;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;

import java.sql.SQLException;
import java.util.List;

public class ServicioEquivalencia {

    private final EquivalenciaDAO equivalenciaDAO = new EquivalenciaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    public void registrar(Equivalencia equivalencia) throws SQLException, EquivalenciaDuplicadaException {
        Autorizacion.verificarGestionEquivalencias();
        validarDatos(equivalencia.getCodigoInternoProveedor(), equivalencia.getSku());
        if (equivalenciaDAO.existe(equivalencia.getIdProveedor(), equivalencia.getCodigoInternoProveedor()))
            throw new EquivalenciaDuplicadaException(equivalencia.getCodigoInternoProveedor());
        equivalenciaDAO.insertar(equivalencia);
    }

    public void modificar(int idProveedor, String codigoInterno, String nuevoSku) throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede modificar equivalencias existentes");
        validarDatos(codigoInterno, nuevoSku);
        if (!equivalenciaDAO.existe(idProveedor, codigoInterno))
            throw new IllegalArgumentException("La equivalencia no existe, no se puede modificar");
        equivalenciaDAO.actualizarSku(idProveedor, codigoInterno, nuevoSku);
    }

    /**
     * Cambia la clave compuesta de una equivalencia sin dejar un hueco entre
     * DELETE e INSERT. Si cualquier paso falla, se revierte el cambio completo.
     */
    public void cambiarCodigo(int idProveedor, String codigoActual, String nuevoCodigo,
                              String nuevoSku) throws SQLException, EquivalenciaDuplicadaException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede modificar equivalencias existentes");
        validarDatos(nuevoCodigo, nuevoSku);
        try {
            DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
                if (!equivalenciaDAO.existe(idProveedor, codigoActual))
                    throw new IllegalArgumentException("La equivalencia original no existe");
                if (!codigoActual.equalsIgnoreCase(nuevoCodigo)
                        && equivalenciaDAO.existe(idProveedor, nuevoCodigo)) {
                    throw new DuplicadoDuranteTransaccion(new EquivalenciaDuplicadaException(nuevoCodigo));
                }
                equivalenciaDAO.eliminar(idProveedor, codigoActual);
                equivalenciaDAO.insertar(new Equivalencia(idProveedor, nuevoCodigo, nuevoSku));
                return null;
            });
        } catch (DuplicadoDuranteTransaccion ex) {
            throw ex.causa;
        }
    }

    public void eliminar(int idProveedor, String codigoInterno) throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede eliminar equivalencias");
        equivalenciaDAO.eliminar(idProveedor, codigoInterno);
    }

    public List<Equivalencia> listarPorProveedor(int idProveedor) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return equivalenciaDAO.listarPorProveedor(idProveedor);
    }

    public List<Equivalencia> consultar(String nombreProveedor, String codigoInterno,
                                        String sku) throws SQLException {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        return equivalenciaDAO.buscarConFiltro(nombreProveedor, codigoInterno, sku);
    }

    private static final class DuplicadoDuranteTransaccion extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final EquivalenciaDuplicadaException causa;

        private DuplicadoDuranteTransaccion(EquivalenciaDuplicadaException causa) {
            super(causa);
            this.causa = causa;
        }
    }

    private void validarDatos(String codigoInterno, String sku) throws SQLException {
        if (codigoInterno == null || codigoInterno.isBlank())
            throw new IllegalArgumentException("El código interno del proveedor es obligatorio");
        if (sku == null || sku.isBlank())
            throw new IllegalArgumentException("El SKU es obligatorio");
        if (!productoDAO.existeSku(sku))
            throw new IllegalArgumentException("No existe un producto con el SKU: " + sku);
    }
}
