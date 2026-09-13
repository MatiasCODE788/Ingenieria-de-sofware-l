package cl.antucayen.controller;

import cl.antucayen.model.entity.Perfil;
import cl.antucayen.model.entity.Usuario;
import cl.antucayen.model.service.ServicioUsuario;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VGestionUsuarios;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ControladorUsuario {

    private final VGestionUsuarios vista;
    private final ServicioUsuario servicio = new ServicioUsuario();

    public ControladorUsuario(VGestionUsuarios vista) {
        Autorizacion.verificarAdministrador("Solo el Administrador puede gestionar usuarios");
        this.vista = vista;
        cargarUsuarios();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnNuevo().addActionListener(e -> crearUsuario());
        vista.getBtnEditar().addActionListener(e -> editarUsuario());
        vista.getBtnDesactivar().addActionListener(e -> desactivarUsuario());
    }

    private void cargarUsuarios() {
        try {
            vista.limpiarTabla();
            for (Usuario usuario : servicio.listarUsuarios()) {
                vista.agregarFila(new Object[]{
                        usuario.getIdUsuario(),
                        usuario.getNombreCompleto(),
                        usuario.getUsername(),
                        usuario.getNombrePerfil(),
                        usuario.isEstadoActivo() ? "Activo" : "Inactivo"
                });
            }
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(vista, "Error: " + ex.getMessage());
        }
    }

    private void crearUsuario() {
        try {
            List<Perfil> perfiles = servicio.listarPerfiles();
            FormularioUsuario formulario = new FormularioUsuario(perfiles, null, null, null);
            int resultado = JOptionPane.showConfirmDialog(
                    vista, formulario.panel, "Crear usuario",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resultado != JOptionPane.OK_OPTION) return;

            String password = new String(formulario.password.getPassword());
            String repeticion = new String(formulario.repetirPassword.getPassword());
            if (!password.equals(repeticion)) {
                throw new IllegalArgumentException("Las contraseñas no coinciden");
            }

            Perfil perfil = formulario.perfilSeleccionado();
            servicio.crearUsuario(
                    formulario.nombreCompleto.getText(),
                    formulario.username.getText(),
                    password,
                    perfil.getIdPerfil());

            cargarUsuarios();
            JOptionPane.showMessageDialog(vista, "Usuario creado correctamente");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(vista, "Error: " + ex.getMessage());
        }
    }

    private void editarUsuario() {
        int fila = vista.getFilaSeleccionada();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, "Selecciona un usuario primero");
            return;
        }

        int idUsuario = (int) vista.getValorFila(fila, 0);
        String nombreActual = (String) vista.getValorFila(fila, 1);
        String usernameActual = (String) vista.getValorFila(fila, 2);
        String perfilActual = (String) vista.getValorFila(fila, 3);

        try {
            List<Perfil> perfiles = servicio.listarPerfiles();
            FormularioUsuario formulario = new FormularioUsuario(
                    perfiles, nombreActual, usernameActual, perfilActual);
            formulario.ocultarPassword();

            int resultado = JOptionPane.showConfirmDialog(
                    vista, formulario.panel, "Editar usuario",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resultado != JOptionPane.OK_OPTION) return;

            Perfil perfil = formulario.perfilSeleccionado();
            Usuario usuario = new Usuario();
            usuario.setIdUsuario(idUsuario);
            usuario.setNombreCompleto(formulario.nombreCompleto.getText());
            usuario.setUsername(formulario.username.getText());
            usuario.setIdPerfil(perfil.getIdPerfil());
            servicio.modificarUsuario(usuario);

            cargarUsuarios();
            JOptionPane.showMessageDialog(vista, "Usuario actualizado correctamente");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(vista, "Error: " + ex.getMessage());
        }
    }

    private void desactivarUsuario() {
        int fila = vista.getFilaSeleccionada();
        if (fila < 0) {
            JOptionPane.showMessageDialog(vista, "Selecciona un usuario primero");
            return;
        }
        int id = (int) vista.getValorFila(fila, 0);
        String username = (String) vista.getValorFila(fila, 2);
        int confirm = JOptionPane.showConfirmDialog(
                vista, "¿Desactivar al usuario '" + username + "'?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                servicio.desactivarUsuario(id);
                cargarUsuarios();
            } catch (SecurityException | SQLException ex) {
                JOptionPane.showMessageDialog(vista, ex.getMessage());
            }
        }
    }

    private static final class FormularioUsuario {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JTextField nombreCompleto = new JTextField(22);
        private final JTextField username = new JTextField(22);
        private final JPasswordField password = new JPasswordField(22);
        private final JPasswordField repetirPassword = new JPasswordField(22);
        private final JComboBox<Perfil> perfil;
        private JLabel lblPassword;
        private JLabel lblRepetir;

        private FormularioUsuario(List<Perfil> perfiles, String nombre,
                                  String usuario, String perfilActual) {
            perfil = new JComboBox<>(perfiles.toArray(Perfil[]::new));
            perfil.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Perfil p) setText(p.getNombrePerfil());
                    return this;
                }
            });

            if (nombre != null) nombreCompleto.setText(nombre);
            if (usuario != null) username.setText(usuario);
            if (perfilActual != null) {
                for (int i = 0; i < perfil.getItemCount(); i++) {
                    if (perfilActual.equals(perfil.getItemAt(i).getNombrePerfil())) {
                        perfil.setSelectedIndex(i);
                        break;
                    }
                }
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            agregarFila(gbc, 0, new JLabel("Nombre completo:"), nombreCompleto);
            agregarFila(gbc, 1, new JLabel("Nombre de usuario:"), username);
            lblPassword = new JLabel("Contraseña:");
            agregarFila(gbc, 2, lblPassword, password);
            lblRepetir = new JLabel("Repetir contraseña:");
            agregarFila(gbc, 3, lblRepetir, repetirPassword);
            agregarFila(gbc, 4, new JLabel("Perfil:"), perfil);
        }

        private void agregarFila(GridBagConstraints gbc, int fila, JComponent etiqueta, JComponent campo) {
            gbc.gridy = fila;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(etiqueta, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(campo, gbc);
        }

        private void ocultarPassword() {
            lblPassword.setVisible(false);
            lblRepetir.setVisible(false);
            password.setVisible(false);
            repetirPassword.setVisible(false);
        }

        private Perfil perfilSeleccionado() {
            Perfil seleccionado = (Perfil) perfil.getSelectedItem();
            if (seleccionado == null) throw new IllegalArgumentException("Debes seleccionar un perfil");
            return seleccionado;
        }
    }
}
