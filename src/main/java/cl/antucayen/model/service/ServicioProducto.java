package cl.antucayen.model.service;

import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;

public class ServicioProducto {

    private final ProductoDAO productoDAO = new ProductoDAO();

    public void registrar(Producto p) throws SQLException {
        if (p.getSku() == null || p.getSku().isEmpty())
            throw new IllegalArgumentException("El SKU es obligatorio");
        if (p.getNombre() == null || p.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (p.getCodigoBarras() == null || p.getCodigoBarras().isEmpty())
            throw new IllegalArgumentException("El código de barras es obligatorio");
        if (p.getPrecioVenta() < 0)
            throw new IllegalArgumentException("El precio de venta no puede ser negativo");
        if (productoDAO.existeSku(p.getSku()))
            throw new IllegalStateException("Ya existe un producto con el SKU: " + p.getSku());
        if (productoDAO.existeCodigoBarras(p.getCodigoBarras()))
            throw new IllegalStateException("Ya existe un producto con ese código de barras");

        productoDAO.insertar(p);
    }

    public void modificar(Producto p) throws SQLException {
        if (p.getNombre() == null || p.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (p.getPrecioVenta() < 0)
            throw new IllegalArgumentException("El precio de venta no puede ser negativo");
        productoDAO.actualizar(p);
    }

    public void inactivar(String sku) throws SQLException {
        productoDAO.inactivar(sku);
    }

    /**
     * Elimina definitivamente un producto. Solo el Administrador puede
     * hacerlo. Si el producto ya tiene historial (movimientos, facturas o
     * ventas asociadas), no se puede borrar físicamente sin perder
     * trazabilidad: se lanza un mensaje claro sugiriendo inactivar en su lugar.
     */
    public void eliminarProducto(String sku) throws SQLException {
        if (!SesionActual.esAdministrador())
            throw new SecurityException("Solo un Administrador puede eliminar productos");

        Producto p = productoDAO.buscarPorSku(sku);
        if (p == null)
            throw new IllegalArgumentException("El producto no existe");

        try {
            productoDAO.eliminarFisico(sku);
        } catch (SQLException ex) {
            // Violación de llave foránea (el producto tiene historial asociado)
            throw new IllegalStateException(
                    "No se puede eliminar '" + sku + "': ya tiene movimientos, facturas o "
                            + "ventas registradas. Usa 'Inactivar' en su lugar para conservar el historial.");
        }
    }

    public Producto buscarPorSku(String sku) throws SQLException {
        return productoDAO.buscarPorSku(sku);
    }

    public Producto buscarPorCodigoBarras(String cb) throws SQLException {
        return productoDAO.buscarPorCodigoBarras(cb);
    }

    public List<Producto> buscarPorNombre(String texto) throws SQLException {
        return productoDAO.buscarPorNombre(texto);
    }

    public List<Producto> listarTodos() throws SQLException {
        return productoDAO.listarTodos();
    }

    public List<Producto> listarActivos() throws SQLException {
        return productoDAO.listarActivos();
    }

    public void actualizarStock(String sku, int nuevoStock) throws SQLException {
        if (nuevoStock < 0)
            throw new IllegalArgumentException("El stock no puede ser negativo");
        productoDAO.actualizarStock(sku, nuevoStock);
    }
}