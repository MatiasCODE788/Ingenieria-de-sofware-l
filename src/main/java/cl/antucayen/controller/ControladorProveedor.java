package cl.antucayen.controller;

import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.entity.Proveedor;
import cl.antucayen.model.exception.EquivalenciaDuplicadaException;
import cl.antucayen.model.service.ServicioEquivalencia;
import cl.antucayen.model.service.ServicioProveedor;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.view.VBuscadorProveedores;
import cl.antucayen.view.VConsultaEquivalencias;
import cl.antucayen.view.VFormularioProveedor;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ControladorProveedor {

    private final ServicioProveedor    servicio            = new ServicioProveedor();
    private final ServicioEquivalencia servicioEquivalencia = new ServicioEquivalencia();

    private VBuscadorProveedores    vistaBuscador;
    private VConsultaEquivalencias  vistaConsultaEquiv;

    /** Modo "gestión de proveedores": buscador + alta/edición con equivalencias. */
    public ControladorProveedor(VBuscadorProveedores vista) {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        this.vistaBuscador = vista;
        vista.getBtnNuevo().setEnabled(!SesionActual.esCajero());
        cargarTodos();
        iniciarEventosBuscador();
    }

    /** Modo "consulta de equivalencias": filtro combinado proveedor/código/SKU. */
    public ControladorProveedor(VConsultaEquivalencias vista) {
        Autorizacion.verificarAdministradorOBodeguero(Autorizacion.ACCESO_DENEGADO);
        this.vistaConsultaEquiv = vista;
        iniciarEventosConsulta();
        buscarEquivalencias(); // carga inicial sin filtros
    }

    // ── BUSCADOR / GESTIÓN DE PROVEEDORES ──────────────────────────

    private void iniciarEventosBuscador() {
        vistaBuscador.getBtnBuscar().addActionListener(e -> buscar());
        vistaBuscador.getBtnNuevo().addActionListener(e -> abrirNuevo());
        vistaBuscador.getTblProveedores().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) abrirEdicion();
            }
        });
    }

    private void cargarTodos() {
        try {
            vistaBuscador.limpiarTabla();
            for (Proveedor p : servicio.listarTodos())
                vistaBuscador.agregarFila(new Object[]{
                        p.getIdProveedor(), p.getRut(), p.getNombre(),
                        p.getTelefono(), p.getCorreoElectronico()
                });
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void buscar() {
        String texto = vistaBuscador.getTextoBusqueda().toLowerCase();
        try {
            vistaBuscador.limpiarTabla();
            for (Proveedor p : servicio.listarTodos()) {
                if (texto.isEmpty()
                        || p.getNombre().toLowerCase().contains(texto)
                        || p.getRut().contains(texto))
                    vistaBuscador.agregarFila(new Object[]{
                            p.getIdProveedor(), p.getRut(), p.getNombre(),
                            p.getTelefono(), p.getCorreoElectronico()
                    });
            }
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void abrirNuevo() {
        if (SesionActual.esCajero()) return;
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
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error al guardar: " + ex.getMessage());
        }
    }

    private void abrirEdicion() {
        if (SesionActual.esCajero()) return;
        int fila = vistaBuscador.getTblProveedores().getSelectedRow();
        if (fila < 0) return;
        int idProveedor = (int) vistaBuscador.getModeloTabla().getValueAt(fila, 0);
        try {
            Proveedor p = servicio.buscarPorId(idProveedor);
            if (p == null) return;
            VFormularioProveedor form = new VFormularioProveedor(null, true);
            form.setDatos(p.getRut(), p.getNombre(), p.getTelefono(), p.getCorreoElectronico());
            form.configurarPermisosEquivalencias(
                    SesionActual.esAdministrador() || SesionActual.esBodeguero(),
                    SesionActual.esAdministrador());

            for (Equivalencia eq : servicioEquivalencia.listarPorProveedor(idProveedor))
                form.agregarEquivalencia(eq.getCodigoInternoProveedor(), eq.getSku());

            form.getBtnGuardar().addActionListener(e -> guardarEdicion(form, p));
            form.getBtnAgregarEquiv().addActionListener(e -> agregarEquivalencia(form, idProveedor));
            form.getBtnEditarEquiv().addActionListener(e -> editarEquivalencia(form, idProveedor));
            form.getBtnEliminarEquiv().addActionListener(e -> eliminarEquivalencia(form, idProveedor));
            form.setVisible(true);
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void guardarEdicion(VFormularioProveedor form, Proveedor original) {
        try {
            original.setRut(form.getRut());
            original.setNombre(form.getNombre());
            original.setTelefono(form.getTelefono());
            original.setCorreoElectronico(form.getCorreo());
            servicio.modificar(original);
            form.dispose();
            cargarTodos();
            javax.swing.JOptionPane.showMessageDialog(null, "Proveedor actualizado correctamente");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            form.mostrarError(ex.getMessage());
        } catch (SQLException ex) {
            form.mostrarError("Error: " + ex.getMessage());
        }
    }

    private void agregarEquivalencia(VFormularioProveedor form, int idProveedor) {
        List<String> codigosExistentes = codigosDeLaTabla(form);
        String[] datos = form.mostrarDialogoEquivalencia(null, null, codigosExistentes);
        if (datos == null) return; // cancelado o inválido

        try {
            Equivalencia eq = new Equivalencia(idProveedor, datos[0], datos[1]);
            servicioEquivalencia.registrar(eq);
            form.agregarEquivalencia(datos[0], datos[1]);
        } catch (EquivalenciaDuplicadaException | IllegalArgumentException | SecurityException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void editarEquivalencia(VFormularioProveedor form, int idProveedor) {
        int fila = form.getFilaEquivSeleccionada();
        if (fila < 0) {
            javax.swing.JOptionPane.showMessageDialog(null, "Selecciona una equivalencia primero");
            return;
        }
        String codigoActual = (String) form.getModeloEquiv().getValueAt(fila, 0);
        String skuActual    = (String) form.getModeloEquiv().getValueAt(fila, 1);

        List<String> codigosExistentes = codigosDeLaTabla(form);
        String[] datos = form.mostrarDialogoEquivalencia(codigoActual, skuActual, codigosExistentes);
        if (datos == null) return;

        try {
            if (!datos[0].equalsIgnoreCase(codigoActual)) {
                servicioEquivalencia.cambiarCodigo(idProveedor, codigoActual, datos[0], datos[1]);
            } else {
                servicioEquivalencia.modificar(idProveedor, codigoActual, datos[1]);
            }
            form.actualizarFilaEquivalencia(fila, datos[0], datos[1]);
        } catch (EquivalenciaDuplicadaException | IllegalArgumentException | SecurityException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
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
            servicioEquivalencia.eliminar(idProveedor, codigo);
            form.getModeloEquiv().removeRow(fila);
        } catch (SecurityException | IllegalArgumentException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage());
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private List<String> codigosDeLaTabla(VFormularioProveedor form) {
        var modelo = form.getModeloEquiv();
        return java.util.stream.IntStream.range(0, modelo.getRowCount())
                .mapToObj(i -> (String) modelo.getValueAt(i, 0))
                .collect(Collectors.toList());
    }

    // ── CONSULTA DE EQUIVALENCIAS (VConsultaEquivalencias) ─────────

    private void iniciarEventosConsulta() {
        vistaConsultaEquiv.getBtnBuscar().addActionListener(e -> buscarEquivalencias());
        vistaConsultaEquiv.getBtnLimpiar().addActionListener(e -> {
            vistaConsultaEquiv.limpiarFiltros();
            buscarEquivalencias();
        });
    }

    private void buscarEquivalencias() {
        try {
            vistaConsultaEquiv.limpiarTabla();
            List<Equivalencia> resultados = servicioEquivalencia.consultar(
                    vistaConsultaEquiv.getFiltroProveedor(),
                    vistaConsultaEquiv.getFiltroCodigo(),
                    vistaConsultaEquiv.getFiltroSku());
            for (Equivalencia eq : resultados)
                vistaConsultaEquiv.agregarFila(new Object[]{
                        eq.getNombreProveedor(), eq.getCodigoInternoProveedor(), eq.getSku()
                });
        } catch (SQLException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(SQLException ex) {
        javax.swing.JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
    }
}
