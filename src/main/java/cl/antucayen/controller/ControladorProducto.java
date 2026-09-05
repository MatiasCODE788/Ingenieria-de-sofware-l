package cl.antucayen.controller;

import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.service.ServicioProducto;
import cl.antucayen.view.VBuscadorProductos;
import cl.antucayen.view.VFormularioProducto;

import java.sql.SQLException;
import java.util.List;

public class ControladorProducto {

    private final VBuscadorProductos vista;
    private final ServicioProducto   servicio = new ServicioProducto();

    public ControladorProducto(VBuscadorProductos vista) {
        this.vista = vista;
        cargarTodos();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnBuscar().addActionListener(e -> buscar());
        vista.getBtnNuevo().addActionListener(e -> abrirNuevo());
        vista.getTblProductos().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) abrirEdicion();
            }
        });
    }

    private void cargarTodos() {
        try {
            vista.limpiarTabla();
            for (Producto p : servicio.listarTodos())
                vista.agregarFila(new Object[]{
                        p.getSku(), p.getNombre(), p.getCodigoBarras(),
                        p.getUnidadMedida(), p.getStockActual(), p.getEstado()
                });
        } catch (SQLException ex) {
            mostrarError("Error al cargar productos: " + ex.getMessage());
        }
    }

    private void buscar() {
        String texto = vista.getTextoBusqueda();
        String tipo  = vista.getTipoBusqueda();
        try {
            vista.limpiarTabla();
            List<Producto> lista;
            if (texto.isEmpty()) {
                lista = servicio.listarTodos();
            } else if ("SKU".equals(tipo)) {
                Producto p = servicio.buscarPorSku(texto);
                lista = p != null ? List.of(p) : List.of();
            } else if ("Código de barras".equals(tipo)) {
                Producto p = servicio.buscarPorCodigoBarras(texto);
                lista = p != null ? List.of(p) : List.of();
            } else {
                lista = servicio.buscarPorNombre(texto);
            }
            for (Producto p : lista)
                vista.agregarFila(new Object[]{
                        p.getSku(), p.getNombre(), p.getCodigoBarras(),
                        p.getUnidadMedida(), p.getStockActual(), p.getEstado()
                });
            if (lista.isEmpty() && !texto.isEmpty())
                javax.swing.JOptionPane.showMessageDialog(null,
                        "No se encontraron productos para la búsqueda: " + texto);
        } catch (SQLException ex) {
            mostrarError("Error al buscar: " + ex.getMessage());
        }
    }

    private void abrirNuevo() {
        VFormularioProducto form = new VFormularioProducto(null, false);
        form.getBtnGuardar().addActionListener(e -> guardarNuevo(form));
        form.setVisible(true);
    }

    private void guardarNuevo(VFormularioProducto form) {
        try {
            int precio = Integer.parseInt(form.getPrecioVenta());
            Producto p = new Producto(
                    form.getSku(), form.getNombre(), form.getCodigoBarras(),
                    form.getUnidad(), precio, Integer.parseInt(form.getStock()), "Activo"
            );
            servicio.registrar(p);
            form.dispose();
            cargarTodos();
            javax.swing.JOptionPane.showMessageDialog(null, "Producto registrado correctamente");
        } catch (NumberFormatException ex) {
            form.mostrarError("El stock y el precio deben ser números enteros");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al guardar: " + ex.getMessage());
        }
    }

    private void abrirEdicion() {
        int fila = vista.getTblProductos().getSelectedRow();
        if (fila < 0) return;
        String sku = (String) vista.getModeloTabla().getValueAt(fila, 0);
        try {
            Producto p = servicio.buscarPorSku(sku);
            if (p == null) return;
            VFormularioProducto form = new VFormularioProducto(null, true);
            form.setDatos(p.getSku(), p.getNombre(), p.getCodigoBarras(),
                    p.getUnidadMedida(), p.getPrecioVenta(), p.getStockActual(), p.getEstado());
            form.getBtnGuardar().addActionListener(e -> guardarEdicion(form, p));
            form.getBtnInactivar().addActionListener(e -> inactivar(form, sku));
            if (form.getBtnEliminar() != null)
                form.getBtnEliminar().addActionListener(e -> eliminar(form, sku));
            form.setVisible(true);
        } catch (SQLException ex) {
            mostrarError("Error al cargar producto: " + ex.getMessage());
        }
    }

    private void guardarEdicion(VFormularioProducto form, Producto original) {
        try {
            original.setNombre(form.getNombre());
            original.setCodigoBarras(form.getCodigoBarras());
            original.setUnidadMedida(form.getUnidad());
            original.setPrecioVenta(Integer.parseInt(form.getPrecioVenta()));
            original.setEstado(form.getEstado());
            servicio.modificar(original);
            form.dispose();
            cargarTodos();
            javax.swing.JOptionPane.showMessageDialog(null, "Producto actualizado correctamente");
        } catch (NumberFormatException ex) {
            form.mostrarError("El precio de venta debe ser un número entero");
        } catch (IllegalArgumentException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al actualizar: " + ex.getMessage());
        }
    }

    private void inactivar(VFormularioProducto form, String sku) {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                null, "¿Inactivar el producto " + sku + "?",
                "Confirmar", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            try {
                servicio.inactivar(sku);
                form.dispose();
                cargarTodos();
            } catch (SQLException ex) {
                mostrarError("Error al inactivar: " + ex.getMessage());
            }
        }
    }

    private void eliminar(VFormularioProducto form, String sku) {
        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                null, "¿ELIMINAR definitivamente el producto " + sku + "?\nEsta acción no se puede deshacer.",
                "Confirmar eliminación", javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            try {
                servicio.eliminarProducto(sku);
                form.dispose();
                cargarTodos();
                javax.swing.JOptionPane.showMessageDialog(null, "Producto eliminado correctamente");
            } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) {
                javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
            } catch (SQLException ex) {
                mostrarError("Error al eliminar: " + ex.getMessage());
            }
        }
    }

    private void mostrarError(String msg) {
        javax.swing.JOptionPane.showMessageDialog(null, msg);
    }
}