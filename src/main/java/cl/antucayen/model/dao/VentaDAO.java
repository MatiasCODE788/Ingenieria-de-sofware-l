package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Venta;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VentaDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    /**
     * Persiste una venta ya validada y pagada. El ID se asigna recién al
     * confirmar el cobro, por lo que abrir/cerrar el POS no consume números.
     */
    public int insertarPagada(Venta v) throws SQLException {
        String sql = """
                INSERT INTO venta
                (fecha_confirmacion, id_usuario, medio_pago, monto_total,
                 monto_recibido, vuelto, estado)
                VALUES (CURRENT_TIMESTAMP,?,?,?,?,?,'Pagada')
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getIdUsuario());
            ps.setString(2, v.getMedioPago());
            ps.setInt(3, v.getMontoTotal());
            ps.setInt(4, v.getMontoRecibido());
            ps.setInt(5, v.getVuelto());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el ID de la venta generada");
    }

    /**
     * Compatibilidad de datos: limpia operaciones En curso creadas por
     * versiones anteriores. La versión actual ya no crea borradores al abrir POS.
     */
    public int cancelarVentasEnCursoPorUsuario(int idUsuario) throws SQLException {
        String sql = """
                UPDATE venta
                SET estado='Cancelada'
                WHERE id_usuario=? AND estado='En curso'
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate();
        }
    }

    public void actualizarEstado(int idVenta, String estado) throws SQLException {
        String sql = "UPDATE venta SET estado=? WHERE id_venta=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idVenta);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo actualizar el estado de la venta #" + idVenta);
            }
        }
    }

    public Venta buscarPorIdParaActualizar(int idVenta) throws SQLException {
        String sql = SELECT_VENTA + " WHERE v.id_venta=? FOR UPDATE";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Venta buscarPorId(int idVenta) throws SQLException {
        String sql = SELECT_VENTA + " WHERE v.id_venta=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Venta> listarDelDia() throws SQLException {
        RangoFechas rango = rangoDiaActual();
        String sql = SELECT_VENTA + """
                WHERE v.fecha_confirmacion >= ?
                  AND v.fecha_confirmacion < ?
                  AND v.estado IN ('Pagada','Anulada')
                ORDER BY v.fecha_confirmacion DESC, v.id_venta DESC
                """;
        return ejecutarListaRango(sql, rango);
    }

    public List<Venta> listarDelDiaPorUsuario(int idUsuario) throws SQLException {
        RangoFechas rango = rangoDiaActual();
        String sql = SELECT_VENTA + """
                WHERE v.fecha_confirmacion >= ?
                  AND v.fecha_confirmacion < ?
                  AND v.id_usuario = ?
                  AND v.estado IN ('Pagada','Anulada')
                ORDER BY v.fecha_confirmacion DESC, v.id_venta DESC
                """;
        List<Venta> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, rango.desde());
            ps.setTimestamp(2, rango.hasta());
            ps.setInt(3, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Total de hoy por medio realmente aplicado en pago_venta. */
    public Map<String, Integer> totalHoyPorMedioPago() throws SQLException {
        RangoFechas rango = rangoDiaActual();
        String sql = """
                SELECT pv.medio_pago, COALESCE(SUM(pv.monto),0) AS total
                FROM venta v
                JOIN pago_venta pv ON pv.id_venta = v.id_venta
                WHERE v.fecha_confirmacion >= ?
                  AND v.fecha_confirmacion < ?
                  AND v.estado = 'Pagada'
                GROUP BY pv.medio_pago
                """;
        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("Efectivo", 0);
        resultado.put("Débito", 0);
        resultado.put("Crédito", 0);
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, rango.desde());
            ps.setTimestamp(2, rango.hasta());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultado.put(rs.getString("medio_pago"), rs.getInt("total"));
            }
        }
        return resultado;
    }

    /** Total vendido en el mes actual, usando límites de rango indexables. */
    public int totalMesActual() throws SQLException {
        RangoFechas rango = rangoMesActual();
        String sql = """
                SELECT COALESCE(SUM(monto_total),0) AS total
                FROM venta
                WHERE fecha_confirmacion >= ?
                  AND fecha_confirmacion < ?
                  AND estado = 'Pagada'
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, rango.desde());
            ps.setTimestamp(2, rango.hasta());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }


    private static final String SELECT_VENTA = """
            SELECT v.*,
                   COALESCE(NULLIF(u.nombre_completo,''), u.username) AS nombre_usuario
            FROM venta v
            JOIN usuario u ON v.id_usuario = u.id_usuario
            """;

    private List<Venta> ejecutarListaRango(String sql, RangoFechas rango) throws SQLException {
        List<Venta> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setTimestamp(1, rango.desde());
            ps.setTimestamp(2, rango.hasta());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private RangoFechas rangoDiaActual() {
        LocalDate hoy = LocalDate.now();
        return new RangoFechas(
                Timestamp.valueOf(hoy.atStartOfDay()),
                Timestamp.valueOf(hoy.plusDays(1).atStartOfDay()));
    }

    private RangoFechas rangoMesActual() {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        return new RangoFechas(
                Timestamp.valueOf(inicio.atStartOfDay()),
                Timestamp.valueOf(inicio.plusMonths(1).atStartOfDay()));
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Timestamp inicio = rs.getTimestamp("fecha_inicio");
        Timestamp confirmacion = rs.getTimestamp("fecha_confirmacion");
        return new Venta(
                rs.getInt("id_venta"),
                inicio == null ? null : inicio.toLocalDateTime(),
                confirmacion == null ? null : confirmacion.toLocalDateTime(),
                rs.getInt("id_usuario"),
                rs.getString("medio_pago"),
                rs.getInt("monto_total"),
                rs.getInt("monto_recibido"),
                rs.getInt("vuelto"),
                rs.getString("estado"),
                rs.getString("nombre_usuario"));
    }

    private record RangoFechas(Timestamp desde, Timestamp hasta) {}
}
