package cl.antucayen.controller;

import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.model.service.ServicioProveedor;
import cl.antucayen.view.VBuscadorProveedores;
import cl.antucayen.view.VFormularioProveedor;

import java.sql.SQLException;
import java.util.List;

public class ControladorProveedor {

    private final VBuscadorProveedores vista;
    private final ServicioProveedor    servicio = new ServicioProveedor();

    public ControladorProveedor(VBuscadorProveedores vista) {
        this.vista = vista;
        cargarTodos();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnBuscar().addActionListener(e -> buscar());
        vista.getBtnNuevo().addActionListener(e -> abrirNuevo());
        vista.getTblProveedores().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) abrirEdicion();
            }
        });
    }

    private void cargarTodos() {
        try {
            vista.limpiarTabla();
            for (Proveedor p : servicio.listarTodos())
                vista.agregarFila(new Object[]{
                        p.getIdProveedor(), p.getRut(), p.getNombre(),
                        p.getTelefono(), p.getCorreoElectronico()
                });
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void buscar() {
        String texto = vista.getTextoBusqueda().toLowerCase();
        try {
            vista.limpiarTabla();
            for (Proveedor p : servicio.listarTodos()) {
                if (texto.isEmpty()
                        || p.getNombre().toLowerCase().contains(texto)
                        || p.getRut().contains(texto))
                    vista.agregarFila(new Object[]{
                            p.getIdProveedor(), p.getRut(), p.getNombre(),
                            p.getTelefono(), p.getCorreoElectronico()
                    });
            }
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void abrirNuevo() {
        VFormularioProveedor form = new VFormularioProveedor(null, false);
        form.getBtnGuardar().addActionListener(e -> guardarNuevo(form));
        form.getBtnAgregarEquiv().addActionListener(e ->
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Guarda el proveedor primero para agregar equivalencias."));
        form.setVisible(true);
    }

    private void guardarNuevo(VFormularioProveedor form) {
        try {
            Proveedor p = new Proveedor(0, form.getRut(), form.getNombre(),
                    form.getTelefono(), form.getCorreo());
            servicio.registrar(p);
            form.dispose();
            cargarTodos();
            javax.swing.JOptionPane.showMessageDialog(null, "Proveedor registrado correctamente");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al guardar: " + ex.getMessage());
        }
    }

    private void abrirEdicion() {
        int fila = vista.getTblProveedores().getSelectedRow();
        if (fila < 0) return;
        int idProveedor = (int) vista.getModeloTabla().getValueAt(fila, 0);
        try {
            Proveedor p = servicio.buscarPorId(idProveedor);
            if (p == null) return;
            VFormularioProveedor form = new VFormularioProveedor(null, true);
            form.setDatos(p.getRut(), p.getNombre(), p.getTelefono(), p.getCorreoElectronico());

            // Cargar equivalencias
            for (Equivalencia eq : servicio.listarEquivalencias(idProveedor))
                form.agregarEquivalencia(eq.getCodigoInternoProveedor(), eq.getSku());

            form.getBtnGuardar().addActionListener(e -> guardarEdicion(form, p));
            form.getBtnAgregarEquiv().addActionListener(e -> agregarEquivalencia(form, idProveedor));
            form.getBtnEliminarEquiv().addActionListener(e -> eliminarEquivalencia(form, idProveedor));
            form.setVisible(true);
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void guardarEdicion(VFormularioProveedor form, Proveedor original) {
        try {
            original.setNombre(form.getNombre());
            original.setTelefono(form.getTelefono());
            original.setCorreoElectronico(form.getCorreo());
            servicio.modificar(original);
            form.dispose();
            cargarTodos();
            javax.swing.JOptionPane.showMessageDialog(null, "Proveedor actualizado correctamente");
        } catch (IllegalArgumentException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error: " + ex.getMessage());
        }
    }

    private void agregarEquivalencia(VFormularioProveedor form, int idProveedor) {
        String codigo = javax.swing.JOptionPane.showInputDialog("Código interno del proveedor:");
        if (codigo == null || codigo.isEmpty()) return;
        String sku = javax.swing.JOptionPane.showInputDialog("SKU interno del sistema:");
        if (sku == null || sku.isEmpty()) return;
        try {
            Equivalencia eq = new Equivalencia(idProveedor, codigo, sku);
            servicio.agregarEquivalencia(eq);
            form.agregarEquivalencia(codigo, sku);
        } catch (IllegalStateException | SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

    private void eliminarEquivalencia(VFormularioProveedor form, int idProveedor) {
        int fila = form.getFilaEquivSeleccionada();
        if (fila < 0) {
            javax.swing.JOptionPane.showMessageDialog(null, "Selecciona una equivalencia primero");
            return;
        }
        String codigo = (String) form.getModeloEquiv().getValueAt(fila, 0);
        try {
            servicio.eliminarEquivalencia(idProveedor, codigo);
            form.getModeloEquiv().removeRow(fila);
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }
}