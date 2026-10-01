package cl.antucayen.model.dao;

import cl.antucayen.model.entity.AjusteInventario;
import cl.antucayen.model.entity.ItemAjuste;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        try (PreparedStatement ps = getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    public AjusteInventario buscarPorId(int idAjuste) throws SQLException {
        String sql = "SELECT * FROM ajuste_inventario WHERE id_ajuste=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idAjuste);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearAjuste(rs) : null;
            }
        }
    }

    /**
     * Obtiene y bloquea la cabecera del ajuste para impedir dobles reversiones
     * concurrentes.
     */
    public AjusteInventario buscarPorIdParaActualizar(int idAjuste) throws SQLException {
        String sql = "SELECT * FROM ajuste_inventario WHERE id_ajuste=? FOR UPDATE";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idAjuste);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapearAjuste(rs) : null;
            }
        }
    }

    public List<ItemAjuste> listarItems(int idAjuste) throws SQLException {
        String sql = """
                SELECT id_item_ajuste, id_ajuste, sku, cantidad_aplicada,
                       stock_anterior, stock_resultante
                FROM item_ajuste
                WHERE id_ajuste=?
                ORDER BY id_item_ajuste
                """;
        List<ItemAjuste> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idAjuste);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ItemAjuste(
                            rs.getInt("id_item_ajuste"),
                            rs.getInt("id_ajuste"),
                            rs.getString("sku"),
                            rs.getInt("cantidad_aplicada"),
                            rs.getInt("stock_anterior"),
                            rs.getInt("stock_resultante")
                    ));
                }
            }
        }
        return lista;
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

    public void insertarItem(int idAjuste, String sku, int cantidad,
                             int stockAnterior, int stockResultante) throws SQLException {
        String sql = """
                INSERT INTO item_ajuste
                (id_ajuste, sku, cantidad_aplicada, stock_anterior, stock_resultante)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idAjuste);
            ps.setString(2, sku);
            ps.setInt(3, cantidad);
            ps.setInt(4, stockAnterior);
            ps.setInt(5, stockResultante);
            ps.executeUpdate();
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
