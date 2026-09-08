package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Venta;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VentaDAO{

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public int insertar(Venta v) throws SQLException {
        String sql = "INSERT INTO venta (id_usuario, medio_pago, monto_total, estado) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, v.getIdUsuario());
            ps.setString(2, v.getMedioPago());
            ps.setInt   (3, v.getMontoTotal());
            ps.setString(4, v.getEstado());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        throw new SQLException("No se pudo obtener el ID de la venta generada");
    }

    public void actualizarEstado(int idVenta, String estado) throws SQLException {
        String sql = "UPDATE venta SET estado=? WHERE id_venta=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt   (2, idVenta);
            ps.executeUpdate();
        }
    }

    public Venta buscarPorId(int idVenta) throws SQLException {
        String sql = """
            SELECT v.*, u.username AS nombre_usuario
            FROM venta v JOIN usuario u ON v.id_usuario = u.id_usuario
            WHERE v.id_venta=?
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public List<Venta> listarDelDia() throws SQLException {
        String sql = """
            SELECT v.*, u.username AS nombre_usuario
            FROM venta v JOIN usuario u ON v.id_usuario = u.id_usuario
            WHERE DATE(v.fecha_hora) = CURDATE()
            ORDER BY v.fecha_hora DESC
            """;
        return ejecutarLista(sql);
    }

    /** Total vendido HOY, agrupado por medio de pago real (según pago_venta; soporta pagos divididos). */
    public Map<String, Integer> totalHoyPorMedioPago() throws SQLException {
        String sql = """
            SELECT pv.medio_pago, COALESCE(SUM(pv.monto),0) AS total
            FROM pago_venta pv
            JOIN venta v ON pv.id_venta = v.id_venta
            WHERE DATE(v.fecha_hora) = CURDATE() AND v.estado = 'Pagada'
            GROUP BY pv.medio_pago
            """;
        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("Efectivo", 0);
        resultado.put("Débito", 0);
        resultado.put("Crédito", 0);
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultado.put(rs.getString("medio_pago"), rs.getInt("total"));
        }
        return resultado;
    }

    /** Total vendido en el MES actual (todas las formas de pago, solo Pagadas). */
    public int totalMesActual() throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(monto_total),0) AS total
            FROM venta
            WHERE YEAR(fecha_hora) = YEAR(CURDATE()) AND MONTH(fecha_hora) = MONTH(CURDATE())
              AND estado = 'Pagada'
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    public int cantidadVentasHoy() throws SQLException {
        String sql = "SELECT COUNT(*) FROM venta WHERE DATE(fecha_hora) = CURDATE() AND estado='Pagada'";
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private List<Venta> ejecutarLista(String sql) throws SQLException {
        List<Venta> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_hora");
        return new Venta(
                rs.getInt   ("id_venta"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getInt   ("id_usuario"),
                rs.getString("medio_pago"),
                rs.getInt   ("monto_total"),
                rs.getString("estado"),
                rs.getString("nombre_usuario")
        );
    }
}