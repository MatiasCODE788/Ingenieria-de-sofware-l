package cl.antucayen.controller;

import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.ErrorImportacion;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.service.ServicioImportacionInventario;
import cl.antucayen.model.service.ServicioImportacionInventario.FilaCruda;
import cl.antucayen.model.service.ServicioImportacionInventario.ResultadoLectura;
import cl.antucayen.model.service.ServicioInventario;
import cl.antucayen.view.VAjusteInventario;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ControladorAjusteInventario {

    private final VAjusteInventario vista;
    private final ServicioInventario servicio = new ServicioInventario();
    private final ServicioImportacionInventario servicioImportacion = new ServicioImportacionInventario();
    private final ProductoDAO productoDAO = new ProductoDAO();

    /** Ítem cargado en el preview, con toda la información necesaria para aplicar o resolver duplicados. */
    private static class ItemPreview {
        String  sku, nombre, estado;
        int     stockActual, cantidad, stockProyectado, numeroFila;
        boolean esError;
    }

    private List<ItemPreview> itemsCargados = new ArrayList<>();

    public ControladorAjusteInventario(VAjusteInventario vista) {
        this.vista = vista;
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnSeleccionar().addActionListener(e -> seleccionarArchivo());
        vista.getBtnCargar().addActionListener(e -> cargarPreview());
        vista.getBtnConfirmar().addActionListener(e -> confirmarAjuste());
        vista.getBtnConsolidarDuplicados().addActionListener(e -> resolverDuplicados(true));
        vista.getBtnExcluirDuplicados().addActionListener(e -> resolverDuplicados(false));
        vista.getBtnVolver().addActionListener(e -> {
            vista.mostrarPaso1();
            vista.limpiarPreview();
            vista.getPanelErroresEstructura().limpiar();
            itemsCargados.clear();
        });
    }

    private void seleccionarArchivo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo Excel o CSV");
        int resultado = chooser.showOpenDialog(null);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = chooser.getSelectedFile();
            if (archivo.length() > 10 * 1024 * 1024) {
                vista.mostrarError("El archivo supera el límite de 10 MB");
                return;
            }
            vista.setNombreArchivo(archivo.getAbsolutePath());
            vista.limpiarError();
            vista.getPanelErroresEstructura().limpiar();
        }
    }

    private void cargarPreview() {
        String ruta = vista.getNombreArchivo();
        if (ruta == null || ruta.isEmpty()) {
            vista.mostrarError("Selecciona un archivo primero");
            return;
        }

        itemsCargados.clear();
        vista.limpiarPreview();
        vista.getPanelErroresEstructura().limpiar();

        ResultadoLectura resultado;
        try {
            resultado = servicioImportacion.leerYValidar(ruta);
        } catch (IOException ex) {
            vista.mostrarError("Error al leer el archivo: " + ex.getMessage());
            return;
        }

        // Validación de ESTRUCTURA primero: si falla, no se toca el inventario ni se avanza de paso.
        if (!resultado.estructuraValida()) {
            vista.mostrarError("Archivo con estructura inválida, revisa el detalle abajo");
            vista.getPanelErroresEstructura().cargarErrores(resultado.erroresEstructura());
            return;
        }

        boolean correccionAutorizada = vista.isCorreccionAutorizada();
        String modalidad = vista.getModalidad();
        List<ErrorImportacion> erroresFila = new ArrayList<>();

        for (FilaCruda fila : resultado.filas()) {
            ItemPreview item = new ItemPreview();
            item.numeroFila = fila.numeroFila();
            item.sku = fila.sku();

            int cantidad;
            try {
                cantidad = Integer.parseInt(fila.cantidadTexto());
            } catch (NumberFormatException ex) {
                item.estado = "❌ Cantidad no numérica";
                item.esError = true;
                itemsCargados.add(item);
                erroresFila.add(new ErrorImportacion(fila.numeroFila(), fila.sku(),
                        "Cantidad no numérica: '" + fila.cantidadTexto() + "'"));
                continue;
            }
            item.cantidad = cantidad;

            if (cantidad == 0) {
                item.estado = "❌ Cantidad cero no permitida";
                item.esError = true;
                itemsCargados.add(item);
                erroresFila.add(new ErrorImportacion(fila.numeroFila(), fila.sku(), "Cantidad no puede ser cero"));
                continue;
            }
            if (cantidad < 0 && !correccionAutorizada) {
                item.estado = "❌ Negativo (requiere Corrección autorizada)";
                item.esError = true;
                itemsCargados.add(item);
                erroresFila.add(new ErrorImportacion(fila.numeroFila(), fila.sku(),
                        "Cantidad negativa sin autorización de Administrador"));
                continue;
            }

            try {
                Producto p = productoDAO.buscarPorSku(fila.sku());
                if (p == null) {
                    item.estado = "❌ SKU no encontrado";
                    item.esError = true;
                    itemsCargados.add(item);
                    erroresFila.add(new ErrorImportacion(fila.numeroFila(), fila.sku(), "SKU no encontrado"));
                    continue;
                }

                item.nombre = p.getNombre();
                item.stockActual = p.getStockActual();
                if (modalidad.contains("Sumar")) {
                    item.stockProyectado = item.stockActual + cantidad;
                } else if (modalidad.contains("Restar")) {
                    item.stockProyectado = item.stockActual - cantidad;
                } else {
                    item.stockProyectado = cantidad;
                }
                item.estado = item.stockProyectado < 0 ? "⚠ Stock negativo" : "✅ OK";
                itemsCargados.add(item);
            } catch (SQLException ex) {
                erroresFila.add(new ErrorImportacion(fila.numeroFila(), fila.sku(), "Error de BD: " + ex.getMessage()));
            }
        }

        marcarDuplicados();
        refrescarTabla();

        if (!erroresFila.isEmpty()) {
            vista.mostrarError(erroresFila.size() + " error(es) de fila encontrados");
        } else {
            vista.limpiarError();
        }
        vista.getPanelErroresEstructura().cargarErrores(erroresFila);
        vista.mostrarPaso2();
    }

    /** Marca visualmente (🔁) los ítems cuyo SKU se repite más de una vez en el archivo. */
    private void marcarDuplicados() {
        Map<String, List<ItemPreview>> porSku = new LinkedHashMap<>();
        for (ItemPreview it : itemsCargados) {
            if (it.esError) continue; // los ítems con error ya se muestran con su propio motivo
            porSku.computeIfAbsent(it.sku.toUpperCase(), k -> new ArrayList<>()).add(it);
        }
        int grupos = 0;
        for (List<ItemPreview> lista : porSku.values()) {
            if (lista.size() > 1) {
                grupos++;
                for (ItemPreview it : lista) it.estado = "🔁 Duplicado (fila " + it.numeroFila + ")";
            }
        }
        vista.mostrarAvisoDuplicados(grupos);
    }

    /**
     * Resuelve los grupos de SKU duplicado: si consolidar=true, suma las
     * cantidades de cada grupo en un solo ítem; si es false, excluye todos
     * los ítems del grupo duplicado del ajuste.
     */
    private void resolverDuplicados(boolean consolidar) {
        Map<String, List<ItemPreview>> porSku = new LinkedHashMap<>();
        for (ItemPreview it : itemsCargados) {
            if (it.estado != null && it.estado.startsWith("🔁"))
                porSku.computeIfAbsent(it.sku.toUpperCase(), k -> new ArrayList<>()).add(it);
        }

        List<ItemPreview> nuevaLista = new ArrayList<>();
        for (ItemPreview it : itemsCargados) {
            boolean esDuplicado = it.estado != null && it.estado.startsWith("🔁");
            if (!esDuplicado) {
                nuevaLista.add(it);
            }
        }

        if (consolidar) {
            for (List<ItemPreview> grupo : porSku.values()) {
                ItemPreview base = grupo.get(0);
                int total = grupo.stream().mapToInt(i -> i.cantidad).sum();
                base.cantidad = total;
                String modalidad = vista.getModalidad();
                if (modalidad.contains("Sumar")) base.stockProyectado = base.stockActual + total;
                else if (modalidad.contains("Restar")) base.stockProyectado = base.stockActual - total;
                else base.stockProyectado = total;
                base.estado = base.stockProyectado < 0 ? "⚠ Stock negativo" : "✅ OK (consolidado)";
                nuevaLista.add(base);
            }
        }
        // si no consolida (excluir), simplemente no se vuelven a agregar

        itemsCargados = nuevaLista;
        vista.mostrarAvisoDuplicados(0);
        refrescarTabla();
    }

    private void refrescarTabla() {
        vista.limpiarPreview();
        for (ItemPreview it : itemsCargados) {
            vista.agregarFilaPreview(new Object[]{
                    it.sku, it.nombre == null ? "" : it.nombre, it.stockActual,
                    it.cantidad, it.stockProyectado, it.estado
            });
        }
    }

    private void confirmarAjuste() {
        long errores = itemsCargados.stream().filter(i -> i.esError).count();
        long advertencias = itemsCargados.stream()
                .filter(i -> i.estado != null && i.estado.startsWith("⚠")).count();
        if (errores > 0 || advertencias > 0) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Hay " + (errores + advertencias) + " ítem(s) con error o advertencia. ¿Continuar solo con los válidos?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        String modalidad = vista.getModalidad();
        boolean correccionAutorizada = vista.isCorreccionAutorizada();
        try {
            int idAjuste = servicio.crearCabeceraAjuste(modalidad);
            int aplicados = 0;
            for (ItemPreview item : itemsCargados) {
                if (item.estado != null && (item.estado.startsWith("✅"))) {
                    servicio.ajustarStock(item.sku, item.cantidad, modalidad, correccionAutorizada);
                    servicio.registrarItemAjuste(idAjuste, item.sku, item.cantidad,
                            item.stockActual, item.stockProyectado);
                    aplicados++;
                }
            }
            JOptionPane.showMessageDialog(null,
                    "Ajuste aplicado correctamente a " + aplicados + " producto(s).");
            vista.mostrarPaso1();
            vista.limpiarPreview();
            vista.getPanelErroresEstructura().limpiar();
            itemsCargados.clear();
        } catch (SQLException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, "Error al aplicar ajuste: " + ex.getMessage());
        }
    }
}
