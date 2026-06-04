package cl.antucayen.model.service;

import cl.antucayen.model.dao.UsuarioDAO;
import cl.antucayen.model.entity.Perfil;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;

public class ServicioUsuario {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private void verificarAdmin() {
        if (!SesionActual.esAdministrador())
            throw new SecurityException("Solo el Administrador puede gestionar usuarios");
    }

    public void crearUsuario(String username, String password, int idPerfil) throws SQLException {
        verificarAdmin();
        if (username == null || username.isEmpty())
            throw new IllegalArgumentException("El username es obligatorio");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("La contraseña es obligatoria");
        if (usuarioDAO.existeUsername(username))
            throw new IllegalStateException("Ya existe un usuario con ese username");
        usuarioDAO.insertar(username, password, idPerfil);
    }

    public void modificarUsuario(Usuario u) throws SQLException {
        verificarAdmin();
        usuarioDAO.actualizar(u);
    }

    public void desactivarUsuario(int idUsuario) throws SQLException {
        verificarAdmin();
        usuarioDAO.desactivar(idUsuario);
    }

    public List<Usuario> listarUsuarios() throws SQLException {
        verificarAdmin();
        return usuarioDAO.listarTodos();
    }

    public List<Perfil> listarPerfiles() throws SQLException {
        return usuarioDAO.listarPerfiles();
    }
}