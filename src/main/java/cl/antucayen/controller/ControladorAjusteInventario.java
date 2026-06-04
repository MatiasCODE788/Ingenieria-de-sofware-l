package cl.antucayen.controller;

import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.model.service.ServicioInventario;
import cl.antucayen.view.VAjusteInventario;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ControladorAjusteInventario {

    private final VAjusteInventario vista;
    private final ServicioInventario servicio    = new ServicioInventario();
    private final ProductoDAO        productoDAO = new ProductoDAO();

    // items cargados: [sku, cantidad, nombreProducto, stockActual, stockProyectado, estado]
    private List<Object[]> itemsCargados = new ArrayList<>();

    public ControladorAjusteInventario(VAjusteInventario vista) {
        this.vista = vista;
        iniciarEventos();
    }

    private void iniciarEventos() {
        vista.getBtnSeleccionar().addActionListener(e -> seleccionarArchivo());
        vista.getBtnCargar().addActionListener(e -> cargarPreview());
        vista.getBtnConfirmar().addActionListener(e -> confirmarAjuste());
        vista.getBtnVolver().addActionListener(e -> {
            vista.mostrarPaso1();
            vista.limpiarPreview();
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
        List<String> errores = new ArrayList<>();

        try {
            List<String[]> filas;
            if (ruta.endsWith(".xlsx") || ruta.endsWith(".xls")) {
                filas = leerExcel(ruta);
            } else if (ruta.endsWith(".csv")) {
                filas = leerCSV(ruta);
            } else {
                vista.mostrarError("Formato no soportado. Usa .xlsx o .csv");
                return;
            }

            String modalidad = vista.getModalidad();
            for (String[] fila : filas) {
                String sku      = fila[0].trim();
                String cantStr  = fila[1].trim();
                int    cantidad;

                try {
                    cantidad = Integer.parseInt(cantStr);
                } catch (NumberFormatException ex) {
                    errores.add("SKU " + sku + ": cantidad inválida (" + cantStr + ")");
                    continue;
                }

                if (cantidad == 0) {
                    errores.add("SKU " + sku + ": cantidad no puede ser cero");
                    itemsCargados.add(new Object[]{
                            sku, "", 0, cantidad, 0, "❌ Cantidad cero no permitida"
                    });
                    vista.agregarFilaPreview(new Object[]{
                            sku, "", 0, cantidad, 0, "❌ Cantidad cero no permitida"
                    });
                    continue;
                }
                if (cantidad < 0 && !vista.getModalidad().contains("Restar")) {
                    itemsCargados.add(new Object[]{
                            sku, "", 0, cantidad, 0, "❌ Cantidad negativa no permitida"
                    });
                    vista.agregarFilaPreview(new Object[]{
                            sku, "", 0, cantidad, 0, "❌ Cantidad negativa no permitida"
                    });
                    continue;
                }

                try {
                    Producto p = productoDAO.buscarPorSku(sku);
                    if (p == null) {
                        itemsCargados.add(new Object[]{
                                sku, "", 0, cantidad, 0, "❌ SKU no encontrado"
                        });
                        continue;
                    }

                    int stockActual = p.getStockActual();
                    int stockProyectado;
                    if (modalidad.contains("Sumar")) {
                        stockProyectado = stockActual + cantidad;
                    } else if (modalidad.contains("Restar")) {
                        stockProyectado = stockActual - cantidad;
                    } else {
                        stockProyectado = cantidad;
                    }

                    String estado = stockProyectado < 0 ? "⚠ Stock negativo" : "✅ OK";
                    itemsCargados.add(new Object[]{
                            sku, p.getNombre(), stockActual, cantidad, stockProyectado, estado
                    });

                    vista.agregarFilaPreview(new Object[]{
                            sku, p.getNombre(), stockActual, cantidad, stockProyectado, estado
                    });
                } catch (SQLException ex) {
                    errores.add("SKU " + sku + ": error BD - " + ex.getMessage());
                }
            }

            if (!errores.isEmpty()) {
                vista.mostrarError("⚠ " + errores.size() + " error(es) encontrados");
            } else {
                vista.limpiarError();
            }
            vista.mostrarPaso2();

        } catch (IOException ex) {
            vista.mostrarError("Error al leer el archivo: " + ex.getMessage());
        }
    }

    private List<String[]> leerExcel(String ruta) throws IOException {
        List<String[]> filas = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(ruta);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Cell cSku = row.getCell(0);
                Cell cCant = row.getCell(1);
                if (cSku == null || cCant == null) continue;
                String sku  = cSku.getStringCellValue().trim();
                String cant = cCant.getCellType() == CellType.NUMERIC
                        ? String.valueOf((int) cCant.getNumericCellValue())
                        : cCant.getStringCellValue().trim();
                if (!sku.isEmpty()) filas.add(new String[]{sku, cant});
            }
        }
        return filas;
    }

    private List<String[]> leerCSV(String ruta) throws IOException {
        List<String[]> filas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            String linea;
            boolean primera = true;
            while ((linea = br.readLine()) != null) {
                if (primera) { primera = false; continue; } // saltar encabezado
                String[] cols = linea.split("[,;]");
                if (cols.length >= 2) {
                    filas.add(new String[]{cols[0].trim(), cols[1].trim()});
                }
            }
        }
        return filas;
    }

    private void confirmarAjuste() {
        long errores = itemsCargados.stream()
                .filter(i -> i[5].toString().startsWith("❌") || i[5].toString().startsWith("⚠"))
                .count();
        if (errores > 0) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Hay " + errores + " ítem(s) con error. ¿Continuar solo con los válidos?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        String modalidad = vista.getModalidad();
        try {
            int aplicados = 0;
            for (Object[] item : itemsCargados) {
                if (item[5].toString().startsWith("✅")) {
                    String sku      = (String) item[0];
                    int    cantidad = (int)    item[3];
                    servicio.ajustarStock(sku, cantidad, modalidad);
                    aplicados++;
                }
            }
            JOptionPane.showMessageDialog(null,
                    "Ajuste aplicado correctamente a " + aplicados + " producto(s).");
            vista.mostrarPaso1();
            vista.limpiarPreview();
            itemsCargados.clear();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al aplicar ajuste: " + ex.getMessage());
        }
    }
}