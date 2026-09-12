-- Sincroniza item_factura con el contrato usado por ItemFacturaDAO y el
-- procesamiento de equivalencias de facturas.
ALTER TABLE item_factura
    ADD COLUMN codigo_interno_proveedor VARCHAR(50) NULL AFTER id_factura;
