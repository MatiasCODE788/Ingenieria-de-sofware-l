package cl.antucayen.controller;

import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.service.ServicioFactura;
import cl.antucayen.model.service.ServicioProcesamientoFactura;
import cl.antucayen.model.service.ServicioProcesamientoFactura.ResumenProcesamiento;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.SesionActual;
import cl.antucayen.view.VProcesamientoFactura;

import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ControladorProcesamientoFactura {

    private final ServicioProcesamientoFactura servicio = new ServicioProcesamientoFactura();
    private final ServicioFactura servicioFactura = new ServicioFactura();
    private final VProcesamientoFactura vista;
    private final int idFactura;
    private final int idProveedor;

    public ControladorProcesamientoFactura(VProcesamientoFactura vista,
                                           int idFactura,
                                           int idProveedor) {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        this.vista = vista;
        this.idFactura = idFactura;
        this.idProveedor = idProveedor;
        iniciarEventos();
        cargarYProcesarInicial();
    }

    private void iniciarEventos() {
        vista.getBtnCorregir().addActionListener(e -> corregirManual());
        vista.getBtnReprocesar().addActionListener(e -> reprocesar());
    }

    private void cargarYProcesarInicial() {
        try {
            Factura factura = servicioFactura.buscarPorId(idFactura);
            if (factura == null) {
                JOptionPane.showMessageDialog(vista, "La factura no existe");
                return;
            }
            if (!"Procesada".equals(factura.getEstado())) {
                servicio.procesarInicial(idFactura, idProveedor);
            }
            recargarEstadoYResumen();
        } catch (IllegalStateException | IllegalArgumentException | SecurityException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void recargarEstadoYResumen() throws SQLException {
        Factura actual = servicioFactura.buscarPorId(idFactura);
        if (actual == null) throw new IllegalStateException("La factura no existe");
        vista.cargarCabecera(actual);
        boolean procesada = "Procesada".equals(actual.getEstado());
        vista.habilitarCorreccion(SesionActual.esAdministrador() && !procesada);
        vista.configurarFacturaProcesada(procesada, SesionActual.esAdministrador());
        actualizarResumenObservadosYErrores();
    }

    private void actualizarResumenObservadosYErrores() throws SQLException {
        ResumenProcesamiento resumen = servicio.obtenerResumen(idFactura);
        vista.cargarResumen(resumen);

        List<ItemFactura> observados = servicioFactura.obtenerItems(idFactura).stream()
                .filter(i -> "Observado".equals(i.getEstadoItem()))
                .collect(Collectors.toList());
        vista.cargarObservados(observados);
        vista.cargarErrores(servicio.obtenerErroresProcesamiento(idFactura));
    }

    private void corregirManual() {
        int idItem = vista.getIdItemSeleccionado();
        if (idItem < 0) {
            JOptionPane.showMessageDialog(vista, "Selecciona un ítem observado primero");
            return;
        }

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
        if (sku == null || sku.isBlank()) return;

        try {
            servicio.corregirEquivalenciaManual(idItem, idProveedor, sku.trim());
            recargarEstadoYResumen();
            JOptionPane.showMessageDialog(vista,
                    "Equivalencia corregida. Reprocesa la factura para aplicar el cambio.");
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void reprocesar() {
        try {
            Factura factura = servicioFactura.buscarPorId(idFactura);
            if (factura == null) {
                JOptionPane.showMessageDialog(vista, "La factura no existe");
                return;
            }

            if ("Procesada".equals(factura.getEstado())) {
                JOptionPane.showMessageDialog(vista, ServicioFactura.MENSAJE_FACTURA_PROCESADA);
                if (!SesionActual.esAdministrador()) return;

                int confirmar = JOptionPane.showConfirmDialog(
                        vista,
                        "¿Autoriza explícitamente el reprocesamiento de esta factura?\n"
                                + "Se revertirán los ingresos anteriores, se recalcularán las "
                                + "equivalencias y se reaplicará el stock de forma transaccional.",
                        "Autorizar reprocesamiento",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirmar != JOptionPane.YES_OPTION) return;

                servicio.reprocesarProcesadaAutorizado(idFactura, idProveedor);
                recargarEstadoYResumen();
                JOptionPane.showMessageDialog(
                        vista, "Factura reprocesada con autorización de Administrador");
                return;
            }

            int confirmar = JOptionPane.showConfirmDialog(
                    vista,
                    "¿Reprocesar todos los ítems de esta factura? "
                            + "Se volverá a buscar la equivalencia de cada uno.",
                    "Confirmar reproceso",
                    JOptionPane.YES_NO_OPTION);
            if (confirmar != JOptionPane.YES_OPTION) return;

            servicio.reprocesar(idFactura, idProveedor);
            recargarEstadoYResumen();
            JOptionPane.showMessageDialog(vista, "Factura reprocesada correctamente");
        } catch (IllegalStateException | IllegalArgumentException | SecurityException ex) {
            JOptionPane.showMessageDialog(vista, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(SQLException ex) {
        JOptionPane.showMessageDialog(vista, "Error: " + ex.getMessage());
    }
}
