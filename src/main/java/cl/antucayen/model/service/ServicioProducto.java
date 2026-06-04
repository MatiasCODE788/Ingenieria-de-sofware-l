package cl.antucayen.model.service;

import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Producto;

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
        if (productoDAO.existeSku(p.getSku()))
            throw new IllegalStateException("Ya existe un producto con el SKU: " + p.getSku());
        if (productoDAO.existeCodigoBarras(p.getCodigoBarras()))
            throw new IllegalStateException("Ya existe un producto con ese código de barras");

        productoDAO.insertar(p);
    }

    public void modificar(Producto p) throws SQLException {
        if (p.getNombre() == null || p.getNombre().isEmpty())
            throw new IllegalArgumentException("El nombre es obligatorio");
        productoDAO.actualizar(p);
    }

    public void inactivar(String sku) throws SQLException {
        productoDAO.inactivar(sku);
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

    public void actualizarStock(String sku, int nuevoStock) throws SQLException {
        if (nuevoStock < 0)
            throw new IllegalArgumentException("El stock no puede ser negativo");
        productoDAO.actualizarStock(sku, nuevoStock);
    }
}