-- Permite conservar el historial de reprocesamientos administrativos de facturas.
-- Se reemplaza la unicidad rígida por una marca de vigencia: puede existir
-- historial de ingresos/reversiones por ítem, pero sólo un ingreso vigente.

-- Primero se crea un índice no único que mantenga soportada
-- la clave foránea fk_mov_item_factura.
CREATE INDEX idx_mov_item_factura
    ON movimiento_inventario(id_item_factura);

-- Una vez que la FK cuenta con otro índice compatible,
-- se puede eliminar la restricción UNIQUE anterior.
ALTER TABLE movimiento_inventario
    DROP INDEX uq_mov_item_factura,
    ADD COLUMN vigente TINYINT(1) NOT NULL DEFAULT 1 AFTER modalidad_ajuste;