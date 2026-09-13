package cl.antucayen.model.service;

import cl.antucayen.model.dao.UsuarioDAO;
import cl.antucayen.model.entity.Perfil;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.security.Autorizacion;

import java.sql.SQLException;
import java.util.List;

public class ServicioUsuario {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public void crearUsuario(String nombreCompleto, String username,
                             String password, int idPerfil) throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
        String nombre = normalizarObligatorio(nombreCompleto, "El nombre completo es obligatorio");
        String usuario = normalizarObligatorio(username, "El username es obligatorio");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("La contraseña es obligatoria");
        if (!usuarioDAO.esPerfilPermitido(idPerfil))
            throw new IllegalArgumentException("El perfil seleccionado no está permitido");
        if (usuarioDAO.existeUsername(usuario))
            throw new IllegalStateException("Ya existe un usuario con ese username");
        usuarioDAO.insertar(nombre, usuario, password, idPerfil);
    }

    public void modificarUsuario(Usuario usuario) throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
        if (usuario == null || usuario.getIdUsuario() <= 0)
            throw new IllegalArgumentException("Usuario inválido");

        String nombre = normalizarObligatorio(
                usuario.getNombreCompleto(), "El nombre completo es obligatorio");
        String username = normalizarObligatorio(
                usuario.getUsername(), "El username es obligatorio");

        if (!usuarioDAO.esPerfilPermitido(usuario.getIdPerfil()))
            throw new IllegalArgumentException("El perfil seleccionado no está permitido");
        if (usuarioDAO.existeUsernameEnOtroUsuario(username, usuario.getIdUsuario()))
            throw new IllegalStateException("Ya existe otro usuario con ese username");

        usuario.setNombreCompleto(nombre);
        usuario.setUsername(username);
        usuarioDAO.actualizar(usuario);
    }

    private String normalizarObligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
        return valor.trim();
    }

    public void desactivarUsuario(int idUsuario) throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
        usuarioDAO.desactivar(idUsuario);
    }

    public List<Usuario> listarUsuarios() throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
        return usuarioDAO.listarTodos();
    }

    public List<Perfil> listarPerfiles() throws SQLException {
        Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
        return usuarioDAO.listarPerfiles();
    }
}
