package cl.antucayen.model.service;

import cl.antucayen.model.dao.EquivalenciaDAO;
import cl.antucayen.model.dao.FacturaDAO;
import cl.antucayen.model.dao.ItemFacturaDAO;
import cl.antucayen.model.dao.ProductoDAO;
import cl.antucayen.model.entity.Equivalencia;
import cl.antucayen.model.entity.ErrorImportacion;
import cl.antucayen.model.entity.Factura;
import cl.antucayen.model.entity.ItemFactura;
import cl.antucayen.model.entity.Producto;
import cl.antucayen.security.Autorizacion;
import cl.antucayen.util.DBConexion;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServicioProcesamientoFactura {

    private final ItemFacturaDAO itemFacturaDAO = new ItemFacturaDAO();
    private final EquivalenciaDAO equivalenciaDAO = new EquivalenciaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final ServicioInventario servicioInventario = new ServicioInventario();

    public ResumenProcesamiento resolverEquivalenciasAlRegistrar(
            int idFactura,
            int idProveedor) throws SQLException {

        Autorizacion.verificarGestionFacturas();

        return DBConexion.getInstancia().ejecutarEnTransaccion(() -> {

            Factura factura =
                    facturaDAO.buscarPorIdParaActualizar(idFactura);

            validarFacturaEditable(factura, idProveedor);

            List<ItemFactura> items =
                    itemFacturaDAO.listarPorFactura(idFactura);

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        "La factura no tiene ítems"
                );
            }

            List<ResolucionItem> resoluciones =
                    resolverItems(items, idProveedor);

            for (ResolucionItem resolucion : resoluciones) {
                itemFacturaDAO.actualizarSkuYEstado(
                        resolucion.item().getIdItem(),
                        resolucion.sku(),
                        resolucion.estado()
                );
            }

            ResumenProcesamiento resumen =
                    resumir(resoluciones);

            if (resumen.observados() > 0
                    || resumen.noProcesados() > 0) {

                facturaDAO.actualizarEstado(
                        idFactura,
                        "Observada"
                );

            } else {

                facturaDAO.actualizarEstado(
                        idFactura,
                        "Pendiente"
                );
            }

            return resumen;
        });
    }

    public ResumenProcesamiento procesarInicial(
            int idFactura,
            int idProveedor) throws SQLException {

        Autorizacion.verificarGestionFacturas();

        return procesarFacturaEditable(
                idFactura,
                idProveedor
        );
    }

    public ResumenProcesamiento reprocesar(
            int idFactura,
            int idProveedor) throws SQLException {

        Autorizacion.verificarGestionFacturas();

        return procesarFacturaEditable(
                idFactura,
                idProveedor
        );
    }

    private ResumenProcesamiento procesarFacturaEditable(
            int idFactura,
            int idProveedor) throws SQLException {

        return DBConexion.getInstancia().ejecutarEnTransaccion(() -> {

            Factura factura =
                    facturaDAO.buscarPorIdParaActualizar(idFactura);

            validarFacturaEditable(
                    factura,
                    idProveedor
            );

            List<ItemFactura> items =
                    itemFacturaDAO.listarPorFactura(idFactura);

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        "La factura no tiene ítems"
                );
            }

            List<ResolucionItem> resoluciones =
                    resolverItems(
                            items,
                            idProveedor
                    );

            for (ResolucionItem resolucion : resoluciones) {
                itemFacturaDAO.actualizarSkuYEstado(
                        resolucion.item().getIdItem(),
                        resolucion.sku(),
                        resolucion.estado()
                );
            }

            ResumenProcesamiento resumen =
                    resumir(resoluciones);

            if (resumen.observados() > 0
                    || resumen.noProcesados() > 0) {

                facturaDAO.actualizarEstado(
                        idFactura,
                        "Observada"
                );

                return resumen;
            }

            for (ResolucionItem resolucion : resoluciones) {

                ItemFactura item =
                        resolucion.item();

                servicioInventario.registrarIngresoPorCompra(
                        resolucion.sku(),
                        item.getCantidadFacturada(),
                        idFactura,
                        item.getIdItem()
                );
            }

            facturaDAO.actualizarEstado(
                    idFactura,
                    "Procesada"
            );

            return resumen;
        });
    }

    public ResumenProcesamiento reprocesarProcesadaAutorizado(
            int idFactura,
            int idProveedor) throws SQLException {

        Autorizacion.verificarAdministrador(
                "Solo un Administrador puede autorizar el reprocesamiento de una factura Procesada"
        );

        return DBConexion.getInstancia().ejecutarEnTransaccion(() -> {

            Factura factura =
                    facturaDAO.buscarPorIdParaActualizar(idFactura);

            if (factura == null) {
                throw new IllegalArgumentException(
                        "La factura no existe"
                );
            }

            if (factura.getIdProveedor()
                    != idProveedor) {

                throw new IllegalArgumentException(
                        "El proveedor indicado no corresponde a la factura"
                );
            }

            if (!"Procesada".equals(
                    factura.getEstado())) {

                throw new IllegalStateException(
                        "La autorización especial solo corresponde a una factura Procesada"
                );
            }

            List<ItemFactura> items =
                    itemFacturaDAO.listarPorFactura(idFactura);

            if (items.isEmpty()) {
                throw new IllegalStateException(
                        "La factura no tiene ítems"
                );
            }

            List<ResolucionItem> resoluciones =
                    resolverItems(
                            items,
                            idProveedor
                    );

            ResumenProcesamiento resumen =
                    resumir(resoluciones);

            if (resumen.observados() > 0
                    || resumen.noProcesados() > 0) {

                throw new IllegalStateException(
                        "Reprocesamiento cancelado: existen ítems sin equivalencia o SKU válido. "
                                + "Corrige las equivalencias y vuelve a autorizar el reprocesamiento."
                );
            }

            for (ItemFactura item : items) {
                servicioInventario
                        .revertirIngresoPorCompraParaReproceso(
                                idFactura,
                                item.getIdItem()
                        );
            }

            for (ResolucionItem resolucion : resoluciones) {
                itemFacturaDAO.actualizarSkuYEstado(
                        resolucion.item().getIdItem(),
                        resolucion.sku(),
                        resolucion.estado()
                );
            }

            for (ResolucionItem resolucion : resoluciones) {

                ItemFactura item =
                        resolucion.item();

                servicioInventario.registrarIngresoPorCompra(
                        resolucion.sku(),
                        item.getCantidadFacturada(),
                        idFactura,
                        item.getIdItem()
                );
            }

            facturaDAO.actualizarEstado(
                    idFactura,
                    "Procesada"
            );

            return resumen;
        });
    }

    private void validarFacturaEditable(
            Factura factura,
            int idProveedor) {

        if (factura == null) {
            throw new IllegalArgumentException(
                    "La factura no existe"
            );
        }

        if (factura.getIdProveedor()
                != idProveedor) {

            throw new IllegalArgumentException(
                    "El proveedor indicado no corresponde a la factura"
            );
        }

        if ("Procesada".equals(
                factura.getEstado())) {

            throw new IllegalStateException(
                    ServicioFactura.MENSAJE_FACTURA_PROCESADA
            );
        }

        if (!"Pendiente".equals(
                factura.getEstado())
                && !"Observada".equals(
                factura.getEstado())) {

            throw new IllegalStateException(
                    "La factura no se encuentra en un estado procesable"
            );
        }
    }

    private List<ResolucionItem> resolverItems(
            List<ItemFactura> items,
            int idProveedor) throws SQLException {

        List<Equivalencia> equivalencias =
                equivalenciaDAO.listarPorProveedor(
                        idProveedor
                );

        List<ResolucionItem> resoluciones =
                new ArrayList<>();

        for (ItemFactura item : items) {

            String codigo =
                    item.getCodigoInternoProveedor();

            if (codigo == null
                    || codigo.isBlank()) {

                String skuActual =
                        item.getSku();

                if (skuActual != null
                        && !skuActual.isBlank()
                        && esProductoActivo(
                        skuActual)) {

                    resoluciones.add(
                            new ResolucionItem(
                                    item,
                                    skuActual,
                                    "Válido"
                            )
                    );

                } else {

                    resoluciones.add(
                            new ResolucionItem(
                                    item,
                                    null,
                                    "No Procesado"
                            )
                    );
                }

                continue;
            }

            Equivalencia equivalencia =
                    buscarEquivalencia(
                            equivalencias,
                            codigo
                    );

            if (equivalencia != null
                    && esProductoActivo(
                    equivalencia.getSku())) {

                resoluciones.add(
                        new ResolucionItem(
                                item,
                                equivalencia.getSku(),
                                "Válido"
                        )
                );

            } else {

                resoluciones.add(
                        new ResolucionItem(
                                item,
                                null,
                                "Observado"
                        )
                );
            }
        }

        return resoluciones;
    }

    private boolean esProductoActivo(
            String sku) throws SQLException {

        Producto producto =
                productoDAO.buscarPorSku(
                        sku
                );

        return producto != null
                && "Activo".equals(
                producto.getEstado()
        );
    }

    private ResumenProcesamiento resumir(
            List<ResolucionItem> resoluciones) {

        int validos =
                (int) resoluciones.stream()
                        .filter(
                                r ->
                                        "Válido".equals(
                                                r.estado()
                                        )
                        )
                        .count();

        int observados =
                (int) resoluciones.stream()
                        .filter(
                                r ->
                                        "Observado".equals(
                                                r.estado()
                                        )
                        )
                        .count();

        int noProcesados =
                (int) resoluciones.stream()
                        .filter(
                                r ->
                                        "No Procesado".equals(
                                                r.estado()
                                        )
                        )
                        .count();

        return new ResumenProcesamiento(
                resoluciones.size(),
                validos,
                observados,
                noProcesados
        );
    }

    private Equivalencia buscarEquivalencia(
            List<Equivalencia> equivalencias,
            String codigoInterno) {

        return equivalencias.stream()
                .filter(
                        e ->
                                e.getCodigoInternoProveedor()
                                        .equalsIgnoreCase(
                                                codigoInterno
                                        )
                )
                .findFirst()
                .orElse(null);
    }

    public void corregirEquivalenciaManual(
            int idItem,
            int idProveedor,
            String nuevoSku) throws SQLException {

        Autorizacion.verificarAdministrador(
                "Solo un Administrador puede corregir equivalencias manualmente"
        );

        DBConexion.getInstancia().ejecutarEnTransaccion(() -> {

            ItemFactura item =
                    itemFacturaDAO.buscarPorId(
                            idItem
                    );

            if (item == null) {
                throw new IllegalArgumentException(
                        "El ítem no existe"
                );
            }

            Factura factura =
                    facturaDAO.buscarPorIdParaActualizar(
                            item.getIdFactura()
                    );

            validarFacturaEditable(
                    factura,
                    idProveedor
            );

            if (nuevoSku == null
                    || nuevoSku.isBlank()) {

                throw new IllegalArgumentException(
                        "Debes indicar un SKU válido"
                );
            }

            if (!esProductoActivo(
                    nuevoSku)) {

                throw new IllegalArgumentException(
                        "El SKU no existe o el producto se encuentra Inactivo: "
                                + nuevoSku
                );
            }

            String codigoProveedor =
                    item.getCodigoInternoProveedor();

            if (codigoProveedor != null
                    && !codigoProveedor.isBlank()) {

                if (equivalenciaDAO.existe(
                        idProveedor,
                        codigoProveedor)) {

                    equivalenciaDAO.actualizarSku(
                            idProveedor,
                            codigoProveedor,
                            nuevoSku
                    );

                } else {

                    equivalenciaDAO.insertar(
                            new Equivalencia(
                                    idProveedor,
                                    codigoProveedor,
                                    nuevoSku
                            )
                    );
                }
            }

            itemFacturaDAO.actualizarSkuYEstado(
                    idItem,
                    nuevoSku,
                    "Válido"
            );

            return null;
        });
    }

    public ResumenProcesamiento obtenerResumen(
            int idFactura) throws SQLException {

        Autorizacion.verificarAdministradorOBodeguero(
                Autorizacion.ACCESO_DENEGADO
        );

        List<ItemFactura> items =
                itemFacturaDAO.listarPorFactura(
                        idFactura
                );

        int leidos =
                items.size();

        int validos =
                (int) items.stream()
                        .filter(
                                i ->
                                        "Válido".equals(
                                                i.getEstadoItem()
                                        )
                        )
                        .count();

        int observados =
                (int) items.stream()
                        .filter(
                                i ->
                                        "Observado".equals(
                                                i.getEstadoItem()
                                        )
                        )
                        .count();

        int noProcesados =
                (int) items.stream()
                        .filter(
                                i ->
                                        "No Procesado".equals(
                                                i.getEstadoItem()
                                        )
                        )
                        .count();

        return new ResumenProcesamiento(
                leidos,
                validos,
                observados,
                noProcesados
        );
    }

    public List<ErrorImportacion> obtenerErroresProcesamiento(
            int idFactura) throws SQLException {

        Autorizacion.verificarAdministradorOBodeguero(
                Autorizacion.ACCESO_DENEGADO
        );

        List<ErrorImportacion> errores =
                new ArrayList<>();

        for (ItemFactura item :
                itemFacturaDAO.listarPorFactura(
                        idFactura)) {

            String codigo =
                    item.getCodigoInternoProveedor()
                            == null
                            ? ""
                            : item.getCodigoInternoProveedor();

            if ("Observado".equals(
                    item.getEstadoItem())) {

                errores.add(
                        new ErrorImportacion(
                                item.getIdItem(),
                                "equivalencia",
                                codigo,
                                "Equivalencia no encontrada para el código interno del proveedor"
                        )
                );

            } else if ("No Procesado".equals(
                    item.getEstadoItem())) {

                String referencia =
                        !codigo.isBlank()
                                ? codigo
                                : item.getSku() == null
                                ? ""
                                : item.getSku();

                errores.add(
                        new ErrorImportacion(
                                item.getIdItem(),
                                "SKU / equivalencia",
                                referencia,
                                "Ítem no procesado: no fue posible determinar un SKU válido"
                        )
                );
            }
        }

        return errores;
    }

    private record ResolucionItem(
            ItemFactura item,
            String sku,
            String estado) {
    }

    public record ResumenProcesamiento(
            int leidos,
            int validos,
            int observados,
            int noProcesados) {
    }
}