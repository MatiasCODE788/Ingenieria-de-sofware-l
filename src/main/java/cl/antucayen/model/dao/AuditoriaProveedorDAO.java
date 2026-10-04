package cl.antucayen.model.dao;

import cl.antucayen.model.entity.AuditoriaProveedor;
import cl.antucayen.util.DBConexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Persistencia del historial de cambios de proveedores. */
public class AuditoriaProveedorDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public void insertar(AuditoriaProveedor auditoria) throws SQLException {
        String sql = """
                INSERT INTO auditoria_proveedor
                (id_proveedor, id_usuario, campo_modificado, valor_anterior, valor_nuevo)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, auditoria.getIdProveedor());
            ps.setInt(2, auditoria.getIdUsuario());
            ps.setString(3, auditoria.getCampoModificado());
            ps.setString(4, auditoria.getValorAnterior());
            ps.setString(5, auditoria.getValorNuevo());
            ps.executeUpdate();
        }
    }

    /** Devuelve la trazabilidad completa de un proveedor, del cambio más reciente al más antiguo. */
    public List<AuditoriaProveedor> listarPorProveedor(int idProveedor) throws SQLException {
        String sql = """
                SELECT a.id_auditoria, a.id_proveedor, a.id_usuario, a.fecha_hora,
                       a.campo_modificado, a.valor_anterior, a.valor_nuevo,
                       COALESCE(NULLIF(u.nombre_completo,''), u.username) AS nombre_usuario
                FROM auditoria_proveedor a
                JOIN usuario u ON u.id_usuario = a.id_usuario
                WHERE a.id_proveedor = ?
                ORDER BY a.fecha_hora DESC, a.id_auditoria DESC
                """;
        List<AuditoriaProveedor> resultado = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new AuditoriaProveedor(
                            rs.getInt("id_auditoria"),
                            rs.getInt("id_proveedor"),
                            rs.getInt("id_usuario"),
                            rs.getTimestamp("fecha_hora").toLocalDateTime(),
                            rs.getString("campo_modificado"),
                            rs.getString("valor_anterior"),
                            rs.getString("valor_nuevo"),
                            rs.getString("nombre_usuario")
                    ));
                }
            }
        }
        return resultado;
    }
}
