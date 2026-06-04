package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Usuario;
import cl.antucayen.util.DBConexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public Usuario autenticar(String username, String password) throws SQLException {
        String sql = """
            SELECT u.*, p.nombre_perfil
            FROM usuario u
            JOIN perfil p ON u.id_perfil = p.id_perfil
            WHERE u.username = ?
            AND u.password_hash = SHA2(?, 256)
            AND u.estado_activo = 1
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        }
        return null;
    }

    public void insertar(String username, String password, int idPerfil) throws SQLException {
        String sql = "INSERT INTO usuario (username, password_hash, estado_activo, id_perfil) VALUES (?, SHA2(?,256), 1, ?)";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setInt   (3, idPerfil);
            ps.executeUpdate();
        }
    }

    public void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET username=?, id_perfil=? WHERE id_usuario=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setInt   (2, u.getIdPerfil());
            ps.setInt   (3, u.getIdUsuario());
            ps.executeUpdate();
        }
    }

    public void desactivar(int idUsuario) throws SQLException {
        String sql = "UPDATE usuario SET estado_activo = 0 WHERE id_usuario = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    public List<Usuario> listarTodos() throws SQLException {
        String sql = """
            SELECT u.*, p.nombre_perfil
            FROM usuario u
            JOIN perfil p ON u.id_perfil = p.id_perfil
            ORDER BY u.username
            """;
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public boolean existeUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE username = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    public List<cl.antucayen.model.entity.Perfil> listarPerfiles() throws SQLException {
        String sql = "SELECT * FROM perfil ORDER BY id_perfil";
        List<cl.antucayen.model.entity.Perfil> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new cl.antucayen.model.entity.Perfil(
                        rs.getInt("id_perfil"),
                        rs.getString("nombre_perfil")
                ));
            }
        }
        return lista;
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt    ("id_usuario"),
                rs.getString ("username"),
                rs.getString ("password_hash"),
                rs.getInt    ("estado_activo") == 1,
                rs.getInt    ("id_perfil"),
                rs.getString ("nombre_perfil")
        );
    }
}