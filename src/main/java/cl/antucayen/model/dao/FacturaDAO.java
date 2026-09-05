package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacturaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public int insertar(Factura f) throws SQLException {
        String sql = """
            INSERT INTO factura (numero_factura, fecha_emision, estado,
            ruta_archivo_digital, valor_total, id_proveedor, id_usuario)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNumeroFactura());
            ps.setDate  (2, Date.valueOf(f.getFechaEmision()));
            ps.setString(3, f.getEstado());
            ps.setString(4, f.getRutaArchivoDigital());
            ps.setInt   (5, f.getValorTotal());
            ps.setInt   (6, f.getIdProveedor());
            ps.setInt   (7, f.getIdUsuario());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void actualizarEstado(int idFactura, String estado) throws SQLException {
        String sql = "UPDATE factura SET estado=? WHERE id_factura=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt   (2, idFactura);
            ps.executeUpdate();
        }
    }

    public Factura buscarPorId(int idFactura) throws SQLException {
        String sql = """
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            WHERE f.id_factura=?
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idFactura);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public List<Factura> listarTodas() throws SQLException {
        String sql = """
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            ORDER BY f.fecha_emision DESC
            """;
        List<Factura> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Factura> listarPorEstado(String estado) throws SQLException {
        String sql = """
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            WHERE f.estado=?
            ORDER BY f.fecha_emision DESC
            """;
        List<Factura> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public boolean existeNumeroPorProveedor(int idProveedor, String numeroFactura) throws SQLException {
        String sql = "SELECT COUNT(*) FROM factura WHERE id_proveedor=? AND numero_factura=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, idProveedor);
            ps.setString(2, numeroFactura);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    /**
     * Consulta combinada por número (parcial), proveedor y rango de fechas de emisión.
     * Cualquier parámetro puede venir null/vacío; los filtros se combinan con AND.
     */
    public List<Factura> buscarConFiltro(String numero, Integer idProveedor,
                                         java.time.LocalDate desde, java.time.LocalDate hasta) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT f.*, p.nombre AS nombre_proveedor
            FROM factura f
            JOIN proveedor p ON f.id_proveedor = p.id_proveedor
            WHERE 1=1
            """);
        if (numero != null && !numero.isBlank())      sqlBuilder.append(" AND f.numero_factura LIKE ?");
        if (idProveedor != null && idProveedor > 0)    sqlBuilder.append(" AND f.id_proveedor = ?");
        if (desde != null)                             sqlBuilder.append(" AND f.fecha_emision >= ?");
        if (hasta != null)                             sqlBuilder.append(" AND f.fecha_emision <= ?");
        sqlBuilder.append(" ORDER BY f.fecha_emision DESC");

        List<Factura> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sqlBuilder.toString())) {
            int i = 1;
            if (numero != null && !numero.isBlank())   ps.setString(i++, "%" + numero + "%");
            if (idProveedor != null && idProveedor > 0) ps.setInt  (i++, idProveedor);
            if (desde != null)                          ps.setDate (i++, Date.valueOf(desde));
            if (hasta != null)                          ps.setDate (i,   Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        return new Factura(
                rs.getInt   ("id_factura"),
                rs.getString("numero_factura"),
                rs.getDate  ("fecha_emision").toLocalDate(),
                rs.getString("estado"),
                rs.getString("ruta_archivo_digital"),
                rs.getInt   ("valor_total"),
                rs.getInt   ("id_proveedor"),
                rs.getInt   ("id_usuario"),
                rs.getString("nombre_proveedor")
        );
    }
}