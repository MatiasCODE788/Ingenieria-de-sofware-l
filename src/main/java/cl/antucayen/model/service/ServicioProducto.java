package cl.antucayen.model.service;

import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.ProductoProveedorDAO;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;

import java.sql.SQLException;
import java.util.List;

public class ServicioProducto {

    public static final String MENSAJE_CODIGO_BARRAS_DUPLICADO =
            "Código de barras ya registrado para otro producto";

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProductoProveedorDAO productoProveedorDAO = new ProductoProveedorDAO();

    public void registrar(Producto producto) throws SQLException {
        registrar(producto, List.of());
    }

    /** Registra producto y asociaciones a proveedores en una sola transacción. */
    public void registrar(Producto producto, List<Integer> idsProveedor) throws SQLException {
        Autorizacion.verificarGestionProductos();
        validarProducto(producto, true);
        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            if (productoDAO.existeSku(producto.getSku())) {
                throw new IllegalStateException("Ya existe un producto con el SKU: " + producto.getSku());
            }
            if (productoDAO.existeCodigoBarras(producto.getCodigoBarras())) {
                throw new IllegalStateException(MENSAJE_CODIGO_BARRAS_DUPLICADO);
            }
            productoDAO.insertar(producto);
            productoProveedorDAO.reemplazarAsociaciones(producto.getSku(), idsProveedor);
            return null;
        });
    }

    public void modificar(Producto producto) throws SQLException {
        Autorizacion.verificarGestionProductos();
        modificar(producto, productoProveedorDAO.listarIdsPorSku(producto.getSku()));
    }

    /** Modifica ficha y asociaciones a proveedores de forma atómica. */
    public void modificar(Producto producto, List<Integer> idsProveedor) throws SQLException {
        Autorizacion.verificarGestionProductos();
        validarProducto(producto, false);
        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {
            Producto existente = productoDAO.buscarPorSkuParaActualizar(producto.getSku());
            if (existente == null) throw new IllegalArgumentException("El producto no existe");
            if (productoDAO.existeCodigoBarrasEnOtroProducto(
                    producto.getCodigoBarras(), producto.getSku())) {
                throw new IllegalStateException(MENSAJE_CODIGO_BARRAS_DUPLICADO);
            }
            productoDAO.actualizar(producto);
            productoProveedorDAO.reemplazarAsociaciones(producto.getSku(), idsProveedor);
            return null;
        });
    }

    /** RF-03: la baja es lógica; nunca se elimina historial físicamente. */
    public void inactivar(String sku) throws SQLException {
        Autorizacion.verificarGestionProductos();
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("El SKU es obligatorio");
        Producto producto = productoDAO.buscarPorSku(sku.trim());
        if (producto == null) throw new IllegalArgumentException("El producto no existe");
        productoDAO.inactivar(sku.trim());
    }

    private void validarProducto(Producto producto, boolean validarSku) {
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        if (validarSku && (producto.getSku() == null || producto.getSku().isBlank())) {
            throw new IllegalArgumentException("El SKU es obligatorio");
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (producto.getCodigoBarras() == null || producto.getCodigoBarras().isBlank()) {
            throw new IllegalArgumentException("El código de barras es obligatorio");
        }
        if (producto.getUnidadMedida() == null || producto.getUnidadMedida().isBlank()) {
            throw new IllegalArgumentException("La unidad de medida es obligatoria");
        }
        if (producto.getPrecioVenta() < 0) {
            throw new IllegalArgumentException("El precio de venta no puede ser negativo");
        }
        if (producto.getStockActual() < 0) {
            throw new IllegalArgumentException("El stock actual no puede ser negativo");
        }
        if (producto.getEstado() == null
                || (!"Activo".equals(producto.getEstado()) && !"Inactivo".equals(producto.getEstado()))) {
            throw new IllegalArgumentException("Estado de producto no válido");
        }

        if (producto.getSku() != null) producto.setSku(producto.getSku().trim());
        producto.setNombre(producto.getNombre().trim());
        producto.setCodigoBarras(producto.getCodigoBarras().trim());
        producto.setUnidadMedida(producto.getUnidadMedida().trim());
    }

    public Producto buscarPorSku(String sku) throws SQLException {
        Autorizacion.verificarConsultaStock();
        return productoDAO.buscarPorSku(sku);
    }

    public Producto buscarPorCodigoBarras(String codigoBarras) throws SQLException {
        Autorizacion.verificarConsultaStock();
        return productoDAO.buscarPorCodigoBarras(codigoBarras);
    }

    public List<Producto> buscarPorNombre(String texto) throws SQLException {
        Autorizacion.verificarConsultaStock();
        return productoDAO.buscarPorNombre(texto);
    }

    public List<Producto> listarTodos() throws SQLException {
        Autorizacion.verificarConsultaStock();
        return productoDAO.listarTodos();
    }

    public List<Producto> listarActivos() throws SQLException {
        Autorizacion.verificarConsultaStock();
        return productoDAO.listarActivos();
    }

    public List<Integer> listarIdsProveedores(String sku) throws SQLException {
        Autorizacion.verificarGestionProductos();
        return productoProveedorDAO.listarIdsPorSku(sku);
    }

    public List<Proveedor> listarProveedores(String sku) throws SQLException {
        Autorizacion.verificarGestionProductos();
        return productoProveedorDAO.listarProveedoresPorSku(sku);
    }
}
