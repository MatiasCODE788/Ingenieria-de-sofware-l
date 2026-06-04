package cl.antucayen.controller;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.service.ServicioFactura;
import cl.antucayen.view.VFacturas;

import java.sql.SQLException;
import java.util.List;

public class ControladorFactura {

    private final VFacturas      vista;
    private final ServicioFactura servicio = new ServicioFactura();

    public ControladorFactura(VFacturas vista) {
        this.vista = vista;
        cargarTodas();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnFiltrar().addActionListener(e -> filtrar());
        vista.getBtnNueva().addActionListener(e ->
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Módulo de nueva factura disponible en próximo incremento."));
        vista.getTblFacturas().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) procesarFactura();
            }
        });
    }

    private void cargarTodas() {
        try {
            vista.limpiarTabla();
            for (Factura f : servicio.listarTodas())
                vista.agregarFila(new Object[]{
                        f.getIdFactura(), f.getNumeroFactura(),
                        f.getFechaEmision(), f.getNombreProveedor(), f.getEstado()
                });
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void filtrar() {
        String estado = vista.getFiltroEstado();
        try {
            vista.limpiarTabla();
            List<Factura> lista = "Todos".equals(estado)
                    ? servicio.listarTodas()
                    : servicio.listarPorEstado(estado);
            for (Factura f : lista)
                vista.agregarFila(new Object[]{
                        f.getIdFactura(), f.getNumeroFactura(),
                        f.getFechaEmision(), f.getNombreProveedor(), f.getEstado()
                });
        } catch (SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private void procesarFactura() {
        int fila = vista.getTblFacturas().getSelectedRow();
        if (fila < 0) return;
        int id     = (int)    vista.getModeloTabla().getValueAt(fila, 0);
        String est = (String) vista.getModeloTabla().getValueAt(fila, 4);
        if ("Procesada".equals(est)) {
            javax.swing.JOptionPane.showMessageDialog(null, "Esta factura ya fue procesada.");
            return;
        }
        int confirm = javax.swing.JOptionPane.showConfirmDialog(null,
                "¿Marcar la factura como Procesada?", "Confirmar",
                javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            try {
                servicio.actualizarEstado(id, "Procesada");
                cargarTodas();
            } catch (SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
            }
        }
    }

    private List<Factura> listarPorEstado(String estado) throws SQLException {
        return servicio.listarPorEstado(estado);
    }
}