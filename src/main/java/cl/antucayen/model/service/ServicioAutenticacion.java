package cl.antucayen.model.service;

import cl.antucayen.model.dao.UsuarioDAO;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;

public class ServicioAutenticacion {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Usuario autenticar(String username, String password) throws SQLException {
        if (username == null || username.isEmpty())
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("La contraseña no puede estar vacía");

        Usuario u = usuarioDAO.autenticar(username, password);
        if (u == null)
            throw new SecurityException("Credenciales inválidas");

        SesionActual.iniciar(u);
        return u;
    }
}