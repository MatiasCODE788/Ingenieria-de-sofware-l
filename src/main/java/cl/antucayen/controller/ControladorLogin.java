package cl.antucayen.controller;

import cl.antucayen.model.entity.Usuario;
import cl.antucayen.model.service.ServicioAutenticacion;
import cl.antucayen.view.VLogin;
import cl.antucayen.view.VPrincipal;

import java.sql.SQLException;

public class ControladorLogin {

    private final VLogin               vista;
    private final ServicioAutenticacion servicio = new ServicioAutenticacion();

    public ControladorLogin(VLogin vista) {
        this.vista = vista;
        vista.getBtnIngresar().addActionListener(e -> iniciarSesion());
    }

    private void iniciarSesion() {
        String username = vista.getUsername();
        String password = vista.getPassword();
        try {
            Usuario u = servicio.autenticar(username, password);
            vista.dispose();
            VPrincipal principal = new VPrincipal(u.getUsername(), u.getNombrePerfil());
            new ControladorPrincipal(principal);
            principal.setVisible(true);
        } catch (IllegalArgumentException ex) {
            vista.mostrarError(ex.getMessage());
        } catch (SecurityException ex) {
            vista.mostrarError("Usuario o contraseña incorrectos");
            vista.limpiarCampos();
        } catch (SQLException ex) {
            vista.mostrarError("Error de conexión: " + ex.getMessage());
        }
    }
}