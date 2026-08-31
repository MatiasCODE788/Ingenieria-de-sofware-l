package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquivalenciaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(Equivalencia e) throws SQLException {
        String sql = "INSERT INTO equivalencia (id_proveedor, codigo_interno_proveedor, sku) VALUES (?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, e.getIdProveedor());
            ps.setString(2, e.getCodigoInternoProveedor());
            ps.setString(3, e.getSku());
            ps.executeUpdate();
        }
    }

    /** Modifica el SKU al que apunta un código interno de proveedor ya existente. */
    public void actualizarSku(int idProveedor, String codigoInterno, String nuevoSku) throws SQLException {
        String sql = "UPDATE equivalencia SET sku=? WHERE id_proveedor=? AND codigo_interno_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, nuevoSku);
            ps.setInt   (2, idProveedor);
            ps.setString(3, codigoInterno);
            ps.executeUpdate();
        }
    }

    public void eliminar(int idProveedor, String codigoInterno) throws SQLException {
        String sql = "DELETE FROM equivalencia WHERE id_proveedor=? AND codigo_interno_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, idProveedor);
            ps.setString(2, codigoInterno);
            ps.executeUpdate();
        }
    }

    public boolean existe(int idProveedor, String codigoInterno) throws SQLException {
        String sql = "SELECT COUNT(*) FROM equivalencia WHERE id_proveedor=? AND codigo_interno_proveedor=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, idProveedor);
            ps.setString(2, codigoInterno);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public List<Equivalencia> listarPorProveedor(int idProveedor) throws SQLException {
        String sql = """
            SELECT e.*, p.nombre AS nombre_proveedor
            FROM equivalencia e
            JOIN proveedor p ON e.id_proveedor = p.id_proveedor
            WHERE e.id_proveedor = ?
            ORDER BY e.codigo_interno_proveedor
            """;
        List<Equivalencia> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    /**
     * Consulta combinada por nombre de proveedor, código interno y/o SKU.
     * Cualquier parámetro puede venir vacío o null; los filtros se combinan con AND.
     */
    public List<Equivalencia> buscarConFiltro(String nombreProveedor, String codigoInterno,
                                              String sku) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT e.*, p.nombre AS nombre_proveedor
            FROM equivalencia e
            JOIN proveedor p ON e.id_proveedor = p.id_proveedor
            WHERE 1=1
            """);
        if (nombreProveedor != null && !nombreProveedor.isBlank())
            sqlBuilder.append(" AND p.nombre LIKE ?");
        if (codigoInterno != null && !codigoInterno.isBlank())
            sqlBuilder.append(" AND e.codigo_interno_proveedor LIKE ?");
        if (sku != null && !sku.isBlank())
            sqlBuilder.append(" AND e.sku LIKE ?");
        sqlBuilder.append(" ORDER BY p.nombre, e.codigo_interno_proveedor");

        List<Equivalencia> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sqlBuilder.toString())) {
            int i = 1;
            if (nombreProveedor != null && !nombreProveedor.isBlank())
                ps.setString(i++, "%" + nombreProveedor + "%");
            if (codigoInterno != null && !codigoInterno.isBlank())
                ps.setString(i++, "%" + codigoInterno + "%");
            if (sku != null && !sku.isBlank())
                ps.setString(i, "%" + sku + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Equivalencia mapear(ResultSet rs) throws SQLException {
        return new Equivalencia(
                rs.getInt   ("id_proveedor"),
                rs.getString("codigo_interno_proveedor"),
                rs.getString("sku"),
                rs.getString("nombre_proveedor")
        );
    }
}
