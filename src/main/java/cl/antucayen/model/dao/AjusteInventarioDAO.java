package cl.antucayen.model.dao;

import cl.antucayen.model.entity.AjusteInventario;
import cl.antucayen.util.DBConexion;

import java.sql.*;

/** DAO de la cabecera de ajustes. El detalle vive en movimiento_inventario. */
public class AjusteInventarioDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public int insertar(AjusteInventario a) throws SQLException {
        String sql = """
                INSERT INTO ajuste_inventario
                (modalidad_ajuste, estado_ajuste, id_usuario, nombre_usuario)
                VALUES (?,?,?,?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getModalidadAjuste());
            ps.setString(2, a.getEstadoAjuste());
            ps.setInt(3, a.getIdUsuario());
            ps.setString(4, a.getNombreUsuario());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el ID del ajuste generado");
    }


    /** Bloquea la cabecera hasta finalizar la transacción actual. */
    public AjusteInventario buscarPorIdParaActualizar(int idAjuste) throws SQLException {
        String sql = "SELECT * FROM ajuste_inventario WHERE id_ajuste=? FOR UPDATE";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idAjuste);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearAjuste(rs) : null;
            }
        }
    }

    public void actualizarEstado(int idAjuste, String estado) throws SQLException {
        String sql = "UPDATE ajuste_inventario SET estado_ajuste=? WHERE id_ajuste=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idAjuste);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("No se pudo actualizar el estado del ajuste " + idAjuste);
            }
        }
    }

    private AjusteInventario mapearAjuste(ResultSet rs) throws SQLException {
        AjusteInventario ajuste = new AjusteInventario();
        ajuste.setIdAjuste(rs.getInt("id_ajuste"));
        Timestamp ts = rs.getTimestamp("fecha_hora");
        ajuste.setFechaHora(ts == null ? null : ts.toLocalDateTime());
        ajuste.setModalidadAjuste(rs.getString("modalidad_ajuste"));
        ajuste.setEstadoAjuste(rs.getString("estado_ajuste"));
        ajuste.setIdUsuario(rs.getInt("id_usuario"));
        ajuste.setNombreUsuario(rs.getString("nombre_usuario"));
        return ajuste;
    }
}
