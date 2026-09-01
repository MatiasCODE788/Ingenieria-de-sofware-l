package cl.antucayen.controller;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.service.ServicioFactura;
import cl.antucayen.model.service.ServicioProcesamientoFactura;
import cl.antucayen.model.service.ServicioProcesamientoFactura.ResumenProcesamiento;
import cl.antucayen.util.SesionActual;
import cl.antucayen.view.VProcesamientoFactura;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ControladorProcesamientoFactura {

    private final ServicioProcesamientoFactura servicio        = new ServicioProcesamientoFactura();
    private final ServicioFactura              servicioFactura = new ServicioFactura();

    private final VProcesamientoFactura vista;
    private final int idFactura;
    private final int idProveedor;

    public ControladorProcesamientoFactura(VProcesamientoFactura vista, int idFactura, int idProveedor) {
        this.vista       = vista;
        this.idFactura   = idFactura;
        this.idProveedor = idProveedor;

        vista.habilitarCorreccion(SesionActual.esAdministrador());
        cargarCabeceraYResumen();
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnCorregir().addActionListener(e -> corregirManual());
        vista.getBtnReprocesar().addActionListener(e -> reprocesar());
    }

    private void cargarCabeceraYResumen() {
        try {
            Factura f = servicioFactura.buscarPorId(idFactura);
            if (f != null) vista.cargarCabecera(f);

            actualizarResumenYObservados();
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void actualizarResumenYObservados() throws SQLException {
        ResumenProcesamiento resumen = servicio.obtenerResumen(idFactura);
        vista.cargarResumen(resumen);

        List<ItemFactura> observados = servicioFactura.obtenerItems(idFactura).stream()
                .filter(i -> "Observado".equals(i.getEstadoItem()))
                .collect(Collectors.toList());
        vista.cargarObservados(observados);
    }

    private void corregirManual() {
        int idItem = vista.getIdItemSeleccionado();
        if (idItem < 0) {
            JOptionPane.showMessageDialog(vista, "Selecciona un ítem observado primero");
            return;
        }
        // Buscamos el código a mostrar en el diálogo de entrada
        String codigo;
        try {
            codigo = servicioFactura.obtenerItems(idFactura).stream()
                    .filter(i -> i.getIdItem() == idItem)
                    .findFirst()
                    .map(ItemFactura::getCodigoInternoProveedor)
                    .orElse("?");
        } catch (SQLException ex) {
            mostrarError(ex);
            return;
        }

        String sku = vista.pedirSkuCorreccion(codigo);
        if (sku == null || sku.isBlank()) return; // cancelado

        try {
            servicio.corregirEquivalenciaManual(idItem, idProveedor, sku.trim());
            actualizarResumenYObservados();
            JOptionPane.showMessageDialog(vista, "Equivalencia corregida correctamente");
        } catch (SecurityException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void reprocesar() {
        int confirmar = JOptionPane.showConfirmDialog(vista,
                "¿Reprocesar todos los ítems de esta factura? Se volverá a buscar la equivalencia de cada uno.",
                "Confirmar reproceso", JOptionPane.YES_NO_OPTION);
        if (confirmar != JOptionPane.YES_OPTION) return;

        try {
            servicio.reprocesar(idFactura, idProveedor);
            actualizarResumenYObservados();
            JOptionPane.showMessageDialog(vista, "Factura reprocesada correctamente");
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(SQLException ex) {
        JOptionPane.showMessageDialog(vista, "Error: " + ex.getMessage());
    }
}