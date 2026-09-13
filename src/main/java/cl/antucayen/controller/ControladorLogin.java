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
        vista.setOnTemaCambiado(this::recargarLoginConTemaActual);
    }

    private void recargarLoginConTemaActual() {
        vista.dispose();
        VLogin nuevaVista = new VLogin();
        new ControladorLogin(nuevaVista);
        nuevaVista.setVisible(true);
    }

    private void iniciarSesion() {
        String username = vista.getUsername();
        String password = vista.getPassword();
        try {
            Usuario u = servicio.autenticar(username, password);
            vista.dispose();
            VPrincipal principal = new VPrincipal(u.getNombreCompleto(), u.getNombrePerfil());
            new ControladorPrincipal(principal);
            principal.setVisible(true);
        } catch (IllegalArgumentException ex) {
            vista.mostrarError(ex.getMessage());
        } catch (SecurityException ex) {
            vista.mostrarError("Usuario o contraseña incorrectos");
            // no llames limpiarCampos() aquí, o el mensaje se oculta al instante
        } catch (SQLException ex) {
            ex.printStackTrace();
            vista.mostrarError("Error de conexión: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}