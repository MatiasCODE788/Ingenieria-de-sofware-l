package cl.antucayen.model.dao;

import cl.antucayen.model.entity.Perfil;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.security.PasswordHasher;
import cl.antucayen.util.DBConexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private Connection getConexion() throws SQLException {
        return DBConexion.getInstancia().getConexion();
    }

    public Usuario autenticar(String username, String password) throws SQLException {
        Connection conexion = getConexion();

        String sql = """
            SELECT u.id_usuario,
                   u.nombre_completo,
                   u.username,
                   u.password_hash,
                   u.estado_activo,
                   u.id_perfil,
                   p.nombre_perfil
            FROM usuario u
            JOIN perfil p ON u.id_perfil = p.id_perfil
            WHERE u.username = ? AND u.estado_activo = 1
            """;

        Usuario usuario;
        String hashAlmacenado;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                hashAlmacenado = rs.getString("password_hash");
                if (!PasswordHasher.verificar(password, hashAlmacenado)) return null;
                usuario = mapear(rs);
            }
        }

        // La migración de hash es posterior a una autenticación ya válida.
        // Si este UPDATE falla, no se invalida el login correcto.
        if (PasswordHasher.esLegado(hashAlmacenado)) {
            try {
                actualizarPasswordHash(usuario.getIdUsuario(), PasswordHasher.hash(password));
            } catch (SQLException migracionEx) {
                System.err.println("No se pudo migrar el hash legado del usuario "
                        + usuario.getUsername() + ": " + migracionEx.getMessage());
            }
        }
        return usuario;
    }

    public void insertar(String nombreCompleto, String username,
                         String password, int idPerfil) throws SQLException {
        String sql = """
            INSERT INTO usuario
                (nombre_completo, username, password_hash, estado_activo, id_perfil)
            VALUES (?, ?, ?, 1, ?)
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, nombreCompleto);
            ps.setString(2, username);
            ps.setString(3, PasswordHasher.hash(password));
            ps.setInt(4, idPerfil);
            ps.executeUpdate();
        }
    }

    private void actualizarPasswordHash(int idUsuario, String hash) throws SQLException {
        String sql = "UPDATE usuario SET password_hash=? WHERE id_usuario=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public Usuario buscarPorId(int idUsuario) throws SQLException {
        String sql = """
            SELECT u.id_usuario,
                   u.nombre_completo,
                   u.username,
                   u.estado_activo,
                   u.id_perfil,
                   p.nombre_perfil
            FROM usuario u
            JOIN perfil p ON u.id_perfil = p.id_perfil
            WHERE u.id_usuario = ?
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void actualizar(Usuario usuario) throws SQLException {
        String sql = """
            UPDATE usuario
            SET nombre_completo=?, username=?, id_perfil=?
            WHERE id_usuario=?
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, usuario.getNombreCompleto());
            ps.setString(2, usuario.getUsername());
            ps.setInt(3, usuario.getIdPerfil());
            ps.setInt(4, usuario.getIdUsuario());
            ps.executeUpdate();
        }
    }

    public void desactivar(int idUsuario) throws SQLException {
        String sql = "UPDATE usuario SET estado_activo=0 WHERE id_usuario=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    public List<Usuario> listarTodos() throws SQLException {
        String sql = """
            SELECT u.id_usuario,
                   u.nombre_completo,
                   u.username,
                   u.estado_activo,
                   u.id_perfil,
                   p.nombre_perfil
            FROM usuario u
            JOIN perfil p ON u.id_perfil = p.id_perfil
            ORDER BY u.nombre_completo, u.username
            """;
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public boolean existeUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE username=?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean existeUsernameEnOtroUsuario(String username, int idUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE username=? AND id_usuario<>?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean esPerfilPermitido(int idPerfil) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM perfil
            WHERE id_perfil = ?
              AND nombre_perfil IN ('Administrador', 'Bodeguero', 'Cajero')
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idPerfil);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    public List<Perfil> listarPerfiles() throws SQLException {
        String sql = """
            SELECT id_perfil, nombre_perfil
            FROM perfil
            WHERE nombre_perfil IN ('Administrador', 'Bodeguero', 'Cajero')
            ORDER BY id_perfil
            """;
        List<Perfil> lista = new ArrayList<>();
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Perfil(rs.getInt("id_perfil"), rs.getString("nombre_perfil")));
            }
        }
        return lista;
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("nombre_completo"),
                rs.getString("username"),
                rs.getInt("estado_activo") == 1,
                rs.getInt("id_perfil"),
                rs.getString("nombre_perfil")
        );
    }
}
