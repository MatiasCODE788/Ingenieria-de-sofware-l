package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Producto;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (sku, nombre, codigo_barras, unidad_medida, stock_actual, estado) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getCodigoBarras());
            ps.setString(4, p.getUnidadMedida());
            ps.setInt   (5, p.getStockActual());
            ps.setString(6, p.getEstado());
            ps.executeUpdate();
        }
    }

    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre=?, codigo_barras=?, unidad_medida=?, estado=? WHERE sku=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getCodigoBarras());
            ps.setString(3, p.getUnidadMedida());
            ps.setString(4, p.getEstado());
            ps.setString(5, p.getSku());
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
                rs.getInt   ("stock_actual"),
                rs.getString("estado")
        );
    }
}