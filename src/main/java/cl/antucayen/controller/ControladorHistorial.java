package cl.antucayen.controller;

import cl.antucayen.model.entity.MovimientoInventario;
import cl.antucayen.model.service.ServicioInventario;
import cl.antucayen.view.VHistorial;

import java.sql.SQLException;
import java.util.List;

public class ControladorHistorial {

    private final VHistorial        vista;
    private final ServicioInventario servicio = new ServicioInventario();

    public ControladorHistorial(VHistorial vista) {
        this.vista = vista;
        cargarTodos();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnFiltrar().addActionListener(e -> filtrar());
        vista.getBtnLimpiar().addActionListener(e -> {
            vista.limpiarFiltros();
            cargarTodos();
        });
    }

    private void cargarTodos() {
        try {
            vista.limpiarTabla();
            for (MovimientoInventario m : servicio.listarMovimientos())
                vista.agregarFila(toFila(m));
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void filtrar() {
        String sku  = vista.getSku().isEmpty() ? null : vista.getSku();
        String tipo = "Todos".equals(vista.getTipo()) ? null : vista.getTipo();
        try {
            vista.limpiarTabla();
            for (MovimientoInventario m : servicio.filtrarMovimientos(sku, tipo, null, null))
                vista.agregarFila(toFila(m));
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private Object[] toFila(MovimientoInventario m) {
        return new Object[]{
                m.getTipoMovimiento(),
                m.getFechaHora() != null ? m.getFechaHora().toString().replace("T", " ") : "",
                m.getSku(),
                m.getNombreProducto(),
                m.getStockAnterior(),
                m.getCantidadAplicada(),
                m.getStockResultante(),
                String.valueOf(m.getIdUsuario())
        };
    }
}