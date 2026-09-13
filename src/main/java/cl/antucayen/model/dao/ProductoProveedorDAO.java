package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.util.DBConexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persistencia de la relación muchos-a-muchos producto/proveedor (RF-07). */
public class ProductoProveedorDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void reemplazarAsociaciones(String sku, List<Integer> idsProveedor) throws SQLException {
        try (PreparedStatement ps = getConexion().prepareStatement(
                "DELETE FROM producto_proveedor WHERE sku=?")) {
            ps.setString(1, sku);
            ps.executeUpdate();
        }

        if (idsProveedor == null || idsProveedor.isEmpty()) return;
        Set<Integer> unicos = new LinkedHashSet<>(idsProveedor);
        String sql = "INSERT INTO producto_proveedor (sku, id_proveedor) VALUES (?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            for (Integer idProveedor : unicos) {
                if (idProveedor == null || idProveedor <= 0) continue;
                ps.setString(1, sku);
                ps.setInt(2, idProveedor);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Integer> listarIdsPorSku(String sku) throws SQLException {
        String sql = "SELECT id_proveedor FROM producto_proveedor WHERE sku=? ORDER BY id_proveedor";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("id_proveedor"));
            }
        }
        return ids;
    }

    public List<Proveedor> listarProveedoresPorSku(String sku) throws SQLException {
        String sql = """
                SELECT p.*
                FROM proveedor p
                JOIN producto_proveedor pp ON pp.id_proveedor = p.id_proveedor
                WHERE pp.sku=?
                ORDER BY p.nombre
                """;
        List<Proveedor> proveedores = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    proveedores.add(new Proveedor(
                            rs.getInt("id_proveedor"),
                            rs.getString("rut"),
                            rs.getString("nombre"),
                            rs.getString("telefono"),
                            rs.getString("correo_electronico")));
                }
            }
        }
        return proveedores;
    }
}
