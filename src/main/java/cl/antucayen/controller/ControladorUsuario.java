package cl.antucayen.controller;

import cl.antucayen.model.entity.Perfil;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.model.service.ServicioUsuario;
import cl.antucayen.view.VGestionUsuarios;

import java.sql.SQLException;
import java.util.List;

public class ControladorUsuario {

    private final VGestionUsuarios vista;
    private final ServicioUsuario  servicio = new ServicioUsuario();

    public ControladorUsuario(VGestionUsuarios vista) {
        this.vista = vista;
        cargarUsuarios();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnNuevo().addActionListener(e -> crearUsuario());
        vista.getBtnEditar().addActionListener(e ->
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Editar usuario disponible en próximo incremento."));
        vista.getBtnDesactivar().addActionListener(e -> desactivarUsuario());
    }

    private void cargarUsuarios() {
        try {
            vista.limpiarTabla();
            for (Usuario u : servicio.listarUsuarios())
                vista.agregarFila(new Object[]{
                        u.getIdUsuario(), u.getUsername(),
                        u.getNombrePerfil(), u.isEstadoActivo() ? "Activo" : "Inactivo"
                });
        } catch (SecurityException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void crearUsuario() {
        String username = javax.swing.JOptionPane.showInputDialog("Nombre de usuario:");
        if (username == null || username.isEmpty()) return;
        String password = javax.swing.JOptionPane.showInputDialog("Contraseña:");
        if (password == null || password.isEmpty()) return;

        try {
            List<Perfil> perfiles = servicio.listarPerfiles();
            String[] opciones = perfiles.stream()
                    .map(Perfil::getNombrePerfil).toArray(String[]::new);
            String perfilElegido = (String) javax.swing.JOptionPane.showInputDialog(
                    null, "Selecciona el perfil:", "Perfil",
                    javax.swing.JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
            if (perfilElegido == null) return;

            int idPerfil = perfiles.stream()
                    .filter(p -> p.getNombrePerfil().equals(perfilElegido))
                    .findFirst().get().getIdPerfil();

            servicio.crearUsuario(username, password, idPerfil);
            cargarUsuarios();
            javax.swing.JOptionPane.showMessageDialog(null, "Usuario creado correctamente");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void desactivarUsuario() {
        int fila = vista.getFilaSeleccionada();
        if (fila < 0) {
            javax.swing.JOptionPane.showMessageDialog(null, "Selecciona un usuario primero");
            return;
        }
        int    id       = (int)    vista.getValorFila(fila, 0);
        String username = (String) vista.getValorFila(fila, 1);
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                null, "¿Desactivar al usuario '" + username + "'?",
                "Confirmar", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            try {
                servicio.desactivarUsuario(id);
                cargarUsuarios();
            } catch (SecurityException | SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
            }
        }
    }
}