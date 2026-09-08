package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Producto;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    /** Umbral por defecto (unidades) bajo el cual un producto se considera "stock bajo". */
    public static final int UMBRAL_STOCK_BAJO = 10;

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (sku, nombre, codigo_barras, unidad_medida, precio_venta, stock_actual, estado) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getCodigoBarras());
            ps.setString(4, p.getUnidadMedida());
            ps.setInt   (5, p.getPrecioVenta());
            ps.setInt   (6, p.getStockActual());
            ps.setString(7, p.getEstado());
            ps.executeUpdate();
        }
    }

    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre=?, codigo_barras=?, unidad_medida=?, precio_venta=?, estado=? WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getCodigoBarras());
            ps.setString(3, p.getUnidadMedida());
            ps.setInt   (4, p.getPrecioVenta());
            ps.setString(5, p.getEstado());
            ps.setString(6, p.getSku());
            ps.executeUpdate();
        }
    }

    public void actualizarStock(String sku, int nuevoStock) throws SQLException {
        String sql = "UPDATE producto SET stock_actual=? WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, nuevoStock);
            ps.setString(2, sku);
            ps.executeUpdate();
        }
    }

    public void inactivar(String sku) throws SQLException {
        String sql = "UPDATE producto SET estado='Inactivo' WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina físicamente el producto. Solo debe usarse cuando NO tiene
     * historial (ni movimientos, ni ítems de factura/venta) — si los tiene,
     * la FK rechaza el DELETE y se propaga la SQLException para que el
     * servicio lo traduzca en un mensaje amigable sugiriendo inactivar.
     */
    public void eliminarFisico(String sku) throws SQLException {
        String sql = "DELETE FROM producto WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.executeUpdate();
        }
    }

    public Producto buscarPorSku(String sku) throws SQLException {
        String sql = "SELECT * FROM producto WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, sku);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public Producto buscarPorCodigoBarras(String codigoBarras) throws SQLException {
        String sql = "SELECT * FROM producto WHERE codigo_barras=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public List<Producto> buscarPorNombre(String texto) throws SQLException {
        String sql = "SELECT * FROM producto WHERE nombre LIKE ? ORDER BY nombre";
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Producto> listarTodos() throws SQLException {
        String sql = "SELECT * FROM producto ORDER BY nombre";
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Solo productos activos (útil para el módulo de Ventas). */
    public List<Producto> listarActivos() throws SQLException {
        String sql = "SELECT * FROM producto WHERE estado='Activo' ORDER BY nombre";
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Productos activos cuyo stock actual es menor o igual al umbral indicado. */
    public List<Producto> listarStockBajo(int umbral) throws SQLException {
        String sql = "SELECT * FROM producto WHERE estado='Activo' AND stock_actual <= ? ORDER BY stock_actual ASC";
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, umbral);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /** Cantidad de productos activos en stock bajo (para la tarjeta del dashboard). */
    public int contarStockBajo(int umbral) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE estado='Activo' AND stock_actual <= ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, umbral);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public boolean existeSku(String sku) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, sku);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public boolean existeCodigoBarras(String codigoBarras) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE codigo_barras=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getString("sku"),
                rs.getString("nombre"),
                rs.getString("codigo_barras"),
                rs.getString("unidad_medida"),
                rs.getInt   ("precio_venta"),
                rs.getInt   ("stock_actual"),
                rs.getString("estado")
        );
    }
}