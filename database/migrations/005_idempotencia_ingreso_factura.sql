-- Relaciona cada movimiento de ingreso por compra con el ítem de factura que
-- lo originó. La unicidad evita aplicar dos veces el stock del mismo ítem.
ALTER TABLE movimiento_inventario
    ADD COLUMN id_item_factura INT NULL AFTER id_factura,
    ADD CONSTRAINT uq_mov_item_factura UNIQUE (id_item_factura),
    ADD CONSTRAINT fk_mov_item_factura
        FOREIGN KEY (id_item_factura)
        REFERENCES item_factura(id_item)
        ON UPDATE CASCADE
        ON DELETE SET NULL;
