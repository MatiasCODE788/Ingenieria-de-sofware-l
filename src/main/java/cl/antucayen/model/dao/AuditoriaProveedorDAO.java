package cl.antucayen.model.dao;

import cl.antucayen.model.entity.AuditoriaProveedor;
import cl.antucayen.util.DBConexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
}
