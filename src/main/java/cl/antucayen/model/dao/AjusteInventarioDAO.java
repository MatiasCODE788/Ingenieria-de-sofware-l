package cl.antucayen.model.dao;

import cl.antucayen.model.entity.AjusteInventario;
import cl.antucayen.util.DBConexion;

import java.sql.*;

public class AjusteInventarioDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public int insertar(AjusteInventario a) throws SQLException {
        String sql = "INSERT INTO ajuste_inventario (modalidad_ajuste, estado_ajuste, id_usuario) VALUES (?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getModalidadAjuste());
            ps.setString(2, a.getEstadoAjuste());
            ps.setInt   (3, a.getIdUsuario());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void actualizarEstado(int idAjuste, String estado) throws SQLException {
        String sql = "UPDATE ajuste_inventario SET estado_ajuste=? WHERE id_ajuste=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt   (2, idAjuste);
            ps.executeUpdate();
        }
    }

    public void insertarItem(int idAjuste, String sku, int cantidad,
                             int stockAnterior, int stockResultante) throws SQLException {
        String sql = "INSERT INTO item_ajuste (id_ajuste, sku, cantidad_aplicada, stock_anterior, stock_resultante) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, idAjuste);
            ps.setString(2, sku);
            ps.setInt   (3, cantidad);
            ps.setInt   (4, stockAnterior);
            ps.setInt   (5, stockResultante);
            ps.executeUpdate();
        }
    }
}