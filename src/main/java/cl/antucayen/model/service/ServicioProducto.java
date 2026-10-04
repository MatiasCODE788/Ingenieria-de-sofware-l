package cl.antucayen.model.service;

import cl.antucayen.model.dao.MovimientoInventarioDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.dao.ProductoProveedorDAO;
import cl.antucayen.model.domain.EstadoProducto;
import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.security.SesionActual;
import cl.antucayen.util.DBConexion;

import java.sql.SQLException;
import java.util.List;

public class ServicioProducto {

    public static final String MENSAJE_CODIGO_BARRAS_DUPLICADO =
            "Código de barras ya registrado para otro producto";

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProductoProveedorDAO productoProveedorDAO = new ProductoProveedorDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();

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

            // El stock inicial debe formar parte del kardex. Si el producto se
            // crea con stock mayor a cero, se registra su origen explícitamente.
            if (producto.getStockActual() > 0) {
                if (SesionActual.getUsuario() == null) {
                    throw new SecurityException("No existe una sesión autenticada");
                }
                MovimientoInventario inicial = new MovimientoInventario();
                inicial.setSku(producto.getSku());
                inicial.setIdUsuario(SesionActual.getUsuario().getIdUsuario());
                inicial.setTipoMovimiento("Stock inicial");
                inicial.setStockAnterior(0);
                inicial.setCantidadAplicada(producto.getStockActual());
                inicial.setStockResultante(producto.getStockActual());
                inicial.setVigente(true);
                movimientoDAO.insertar(inicial);
            }
            return null;
        });
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

    /** Reactiva un producto previamente inactivado sin alterar su historial. */
    public void reactivar(String sku) throws SQLException {
        Autorizacion.verificarGestionProductos();
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("El SKU es obligatorio");
        Producto producto = productoDAO.buscarPorSku(sku.trim());
        if (producto == null) throw new IllegalArgumentException("El producto no existe");
        productoDAO.reactivar(sku.trim());
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
                || (!EstadoProducto.ACTIVO.coincide(producto.getEstado())
                && !EstadoProducto.INACTIVO.coincide(producto.getEstado()))) {
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

    public List<Producto> buscarSugerenciasActivas(String texto, int limite) throws SQLException {
        Autorizacion.verificarConsultaStock();
        if (texto == null || texto.isBlank()) return List.of();
        int limiteSeguro = Math.max(1, Math.min(limite, 20));
        return productoDAO.buscarSugerenciasActivas(texto.trim(), limiteSeguro);
    }

    public List<Producto> listarTodos() throws SQLException {
        Autorizacion.verificarConsultaStock();
        return productoDAO.listarTodos();
    }

    public List<Integer> listarIdsProveedores(String sku) throws SQLException {
        Autorizacion.verificarGestionProductos();
        return productoProveedorDAO.listarIdsPorSku(sku);
    }

}
