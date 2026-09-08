package cl.antucayen.model.dao;

import cl.antucayen.model.entity.AuditoriaProveedor;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaProveedorDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(AuditoriaProveedor a) throws SQLException {
        String sql = """
            INSERT INTO auditoria_proveedor
            (id_proveedor, id_usuario, campo_modificado, valor_anterior, valor_nuevo)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt   (1, a.getIdProveedor());
            ps.setInt   (2, a.getIdUsuario());
            ps.setString(3, a.getCampoModificado());
            ps.setString(4, a.getValorAnterior());
            ps.setString(5, a.getValorNuevo());
            ps.executeUpdate();
        }
    }

    public List<AuditoriaProveedor> listarPorProveedor(int idProveedor) throws SQLException {
        String sql = """
            SELECT a.*, u.username AS nombre_usuario
            FROM auditoria_proveedor a
            JOIN usuario u ON a.id_usuario = u.id_usuario
            WHERE a.id_proveedor = ?
            ORDER BY a.fecha_hora DESC
            """;
        List<AuditoriaProveedor> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private AuditoriaProveedor mapear(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_hora");
        return new AuditoriaProveedor(
                rs.getInt   ("id_auditoria"),
                rs.getInt   ("id_proveedor"),
                rs.getInt   ("id_usuario"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("campo_modificado"),
                rs.getString("valor_anterior"),
                rs.getString("valor_nuevo"),
                rs.getString("nombre_usuario")
        );
    }
}
