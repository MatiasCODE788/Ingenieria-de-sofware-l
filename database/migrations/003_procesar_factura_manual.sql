ALTER TABLE item_factura
    ADD COLUMN descripcion VARCHAR(150) NULL AFTER sku;

ALTER TABLE factura
    ADD COLUMN valor_total INT NOT NULL DEFAULT 0 AFTER ruta_archivo_digital,
    ADD CONSTRAINT chk_valor_total CHECK (valor_total >= 0);
