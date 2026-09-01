package cl.antucayen.model.service;

import cl.antucayen.model.dao.EquivalenciaDAO;
import cl.antucayen.model.dao.ItemFacturaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.util.SesionActual;

import java.sql.SQLException;
import java.util.List;

public class ServicioProcesamientoFactura {

    private final ItemFacturaDAO   itemFacturaDAO   = new ItemFacturaDAO();
    private final EquivalenciaDAO  equivalenciaDAO  = new EquivalenciaDAO();
    private final ProductoDAO      productoDAO      = new ProductoDAO();

    /**
     * Ejecuta la búsqueda de equivalencia para TODOS los ítems de la factura
     * (tanto los que ya tenían SKU como los observados), recalculando su
     * estado según lo que exista hoy en la tabla equivalencia.
     */
    public ResumenProcesamiento reprocesar(int idFactura, int idProveedor) throws SQLException {
        List<ItemFactura> items = itemFacturaDAO.listarPorFactura(idFactura);
        return procesarItems(items, idProveedor);
    }

    /** Primer procesamiento (misma lógica que reprocesar, alias semántico). */
    public ResumenProcesamiento procesar(int idFactura, int idProveedor) throws SQLException {
        return reprocesar(idFactura, idProveedor);
    }

    private ResumenProcesamiento procesarItems(List<ItemFactura> items, int idProveedor) throws SQLException {
        int leidos = items.size();
        int validos = 0, observados = 0, noProcesados = 0;

        for (ItemFactura item : items) {
            if (item.getCodigoInternoProveedor() == null) {
                // Ítem sin código de proveedor asociado: no hay nada que buscar
                if (item.getSku() != null) {
                    validos++;
                } else {
                    noProcesados++;
                    itemFacturaDAO.actualizarSkuYEstado(item.getIdItem(), null, "No Procesado");
                }
                continue;
            }

            Equivalencia eq = buscarEquivalencia(idProveedor, item.getCodigoInternoProveedor());
            if (eq != null) {
                itemFacturaDAO.actualizarSkuYEstado(item.getIdItem(), eq.getSku(), "Válido");
                validos++;
            } else {
                itemFacturaDAO.actualizarSkuYEstado(item.getIdItem(), null, "Observado");
                observados++;
            }
        }
        return new ResumenProcesamiento(leidos, validos, observados, noProcesados);
    }

    private Equivalencia buscarEquivalencia(int idProveedor, String codigoInterno) throws SQLException {
        return equivalenciaDAO.listarPorProveedor(idProveedor).stream()
                .filter(e -> e.getCodigoInternoProveedor().equalsIgnoreCase(codigoInterno))
                .findFirst()
                .orElse(null);
    }

    /**
     * Corrige manualmente la equivalencia de un ítem Observado, dejándolo
     * Válido. Además crea o actualiza la equivalencia real (proveedor +
     * código -> SKU) para que futuros ítems con el mismo código se resuelvan
     * solos. Solo el Administrador puede ejecutar esta corrección.
     */
    public void corregirEquivalenciaManual(int idItem, int idProveedor, String nuevoSku) throws SQLException {
        if (!SesionActual.esAdministrador())
            throw new SecurityException("Solo un Administrador puede corregir equivalencias manualmente");

        ItemFactura item = itemFacturaDAO.buscarPorId(idItem);
        if (item == null)
            throw new IllegalArgumentException("El ítem no existe");
        if (item.getCodigoInternoProveedor() == null)
            throw new IllegalArgumentException("El ítem no tiene código de proveedor asociado");
        if (nuevoSku == null || nuevoSku.isBlank())
            throw new IllegalArgumentException("Debes indicar un SKU válido");
        if (!productoDAO.existeSku(nuevoSku))
            throw new IllegalArgumentException("No existe un producto con el SKU: " + nuevoSku);

        if (equivalenciaDAO.existe(idProveedor, item.getCodigoInternoProveedor())) {
            equivalenciaDAO.actualizarSku(idProveedor, item.getCodigoInternoProveedor(), nuevoSku);
        } else {
            equivalenciaDAO.insertar(new Equivalencia(idProveedor, item.getCodigoInternoProveedor(), nuevoSku));
        }
        itemFacturaDAO.actualizarSkuYEstado(idItem, nuevoSku, "Válido");
    }

    /** Resumen (leídos, válidos, observados, no procesados) del estado actual de la factura. */
    public ResumenProcesamiento obtenerResumen(int idFactura) throws SQLException {
        List<ItemFactura> items = itemFacturaDAO.listarPorFactura(idFactura);
        int leidos = items.size();
        int validos = (int) items.stream().filter(i -> "Válido".equals(i.getEstadoItem())).count();
        int observados = (int) items.stream().filter(i -> "Observado".equals(i.getEstadoItem())).count();
        int noProcesados = (int) items.stream().filter(i -> "No Procesado".equals(i.getEstadoItem())).count();
        return new ResumenProcesamiento(leidos, validos, observados, noProcesados);
    }

    public record ResumenProcesamiento(int leidos, int validos, int observados, int noProcesados) {}
}